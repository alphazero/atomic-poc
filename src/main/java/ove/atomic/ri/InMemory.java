/* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! */
/*                             ~!!! As-Salaam !!!~                             */
/*                               ~!!! Doost !!!~                               */
/* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! */

/*
 *   Copyright 2014 Joubin Muhammad Houshyar
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ove.atomic.ri;

import ove.atomic.Temporal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Proof-Of-Concept JVM-global, In-Memory sketch-implementation of
 * Temporal concurrency model for Java.
 *
 * @author: Joubin <alphazero@sensesay.net>
 * @date: 5/11/14
 */
public interface InMemory {

	///////////////////////////////////////////////////////////////////////////
	/// Temporal.Context //////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	static final Temporal.Context NilContext = new Temporal.Context() {
		@Override final public Temporal.Context branch () { throw new RuntimeException("BUG"); }
		@Override public Temporal.Context branch (String name) { throw new RuntimeException("BUG"); }
		@Override final public Temporal.Context merge (Temporal.Context... contexts) { throw new RuntimeException("BUG"); }
		@Override final public <T> void write (int id, T value) { }
		@Override final public <T> T read (int id) { throw new RuntimeException("BUG"); }
		@Override final public int[] modset () { throw new RuntimeException("BUG"); }
		@Override final public Op[] operations () { throw new RuntimeException("BUG"); }
		@Override final public Temporal.Context parent () { throw new RuntimeException("BUG");}
	};

	public static class TemporalContext implements Temporal.Context {

		/**
		 * @throws java.lang.AssertionError if current thread is not a Temporal.Actor
		 * @return the Temporal.Context of the current (acting) Temporal.Actor
		 */
		public static Temporal.Context get() {
			return TemporalActor.currentActor().getTemporalContext();
		}

		public static Temporal.Context newRootContext () {
			final String name = String.format("anon-root-temporal-context-%d", System.nanoTime());
			return new TemporalContext(name);
		}

		public static Temporal.Context newRootContext (final String name) {
			return new TemporalContext(name);
		}

		/** */
		private final Map<Integer, Object> map = new HashMap<>();

		/** */
		private final Set<Temporal.Context.Op> operations = new HashSet<>();

		/** */
		private final Set<Integer> modSet = new HashSet<>();

		/** */
		public final String name;

		/** */
		final Temporal.Context parent;

		protected TemporalContext (final String name) {
			this(NilContext, name);
		}

		protected TemporalContext (final Temporal.Context parent, final String name) {
			if(parent != NilContext) {
				assert parent instanceof TemporalContext : "ERR-Only InMemory.TemporalContext supported";
			}
			assert name != null : "name is null";
			this.name = name;
			this.parent = parent;
		}

		@Override final public Temporal.Context branch () {
			final String name = String.format("anon-child-temporal-context-%d", System.currentTimeMillis());
			return this.branch (name);
		}

		@Override final public Temporal.Context branch (final String name) {
			return new TemporalContext(this, name);
		}

		// REVU: TODO: this needs to use Temporal.Context#operations() & check for merge conflicts
		//
		@Override final public Temporal.Context merge (Temporal.Context... contexts) {
			final String name = String.format("anon-merged-temporal-context-%d", System.nanoTime());
			final Temporal.Context mergeContext = this.branch(name);
			for(final Temporal.Context context : contexts) {
				for(final int id : context.modset()) {
					mergeContext.write(id, context.read(id));
				}
			}
			return mergeContext;
		}

		@Override final public <T> void write (int id, T value) {
			map.put(id, value);
			modSet.add(id);
		}

		@Override final public <T> T read (int id) {
			@SuppressWarnings("unchecked")
			T value = (T) map.get(id);
			if(value == null && parent != NilContext) {
				value = parent.read(id);
			}
			return value;
		}

		@Override final public int[] modset () {
			final int[] ids = new int[modSet.size()];
			final Object[] _ids = modSet.toArray();
			for(int i = 0; i < ids.length; i++) {
				ids[i] = ((Integer) _ids[i]);
			}
			return ids;
		}

		@Override final public Op[] operations () {
			throw new RuntimeException("Not implemented - TODO"); // TODO
		}

		@Override final public Temporal.Context parent () {
			return parent;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	/// Temporal.Reference ////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	public static class TemporalReference<T> implements Temporal.Reference<T> {

		public TemporalReference (final T initialValue) {
			context().write (this.id(), initialValue);
		}

		@Override final public T get () {
			return context().read (this.id());
		}

		@Override final public void set (T value) {
			context().write (this.id(), value);
		}

		// REVU: not quite correct but good enough for POC
		// TODO: it needs to walk the timeline and use the last Flow.Op.Code.write context
		@Override final public T previousRevision () {
			final Temporal.Context prevContext = context().parent();
			final Temporal.Context ctx = prevContext == null ? context() : prevContext;
			return ctx.read (this.id());
		}

		/** extension-point TODO: ADAMic Naming */
		protected int id() {
			return this.hashCode();
		}

		protected static Temporal.Context context() {
			return InMemory.TemporalContext.get();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	/// Temporal.Actor ////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	public static class TemporalActor extends Thread implements Temporal.Actor {

		private Temporal.Context temporalContext;

		protected TemporalActor (final Temporal.Context temporalContext, final Runnable task) {
			this(temporalContext, task, String.format("actor-anon-%d", System.currentTimeMillis()));
		}

		protected TemporalActor (final Temporal.Context temporalContext, final Runnable task, final String name) {
			super(task, name);
			assert temporalContext != null : "temporalContext is null";
			assert temporalContext instanceof TemporalContext : "ERR-Only InMemory.TemporalContext supported";
			assert task != null : "task is null";
			this.temporalContext = (TemporalContext) temporalContext;
		}

		public TemporalActor (final Runnable task, final String name) {
			super(task, name);
			this.temporalContext = InMemory.TemporalContext.newRootContext();
		}

		@Override final public String toString () {
			return String.format("actor: %s", getName());
		}

		@Override final public Temporal.Actor fork (final Runnable task, final String name) {
			return new InMemory.TemporalActor(this.temporalContext.branch(), task, name);
		}

		@Override final public void merge (Temporal.Actor... actors) {
			try {
				// gather contexts
				final Temporal.Context[] contexts = new Temporal.Context[actors.length];
				int i = 0;
				for (final Temporal.Actor actor : actors) {
					assert actor instanceof InMemory.TemporalActor : "ERR- only InMemory.TemporalActors supported";
					final InMemory.TemporalActor _actor = (TemporalActor) actor;
					_actor.join();
					contexts [i++] = actor.getTemporalContext();
				}

				// merge them
				this.temporalContext = this.temporalContext.merge(contexts);
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException("", e);
			}
		}

		@Override final public void goBackInTime() {
			// TODO: guard against null parent
			this.temporalContext = this.temporalContext.parent();
		}
		@Override final public Temporal.Context getTemporalContext () {
			return temporalContext;
		}

		/**
		 * @throws java.lang.AssertionError if current thread is not a Temporal.Actor
		 * @return the current (acting) Temporal.Actor
		 */
		public static Temporal.Actor currentActor () {
			final Thread currentThread = Thread.currentThread();
			assert currentThread instanceof Temporal.Actor : "ERR-current thread is not a Temporal.Actor";
			return (Temporal.Actor) currentThread;
		}
	}
}
