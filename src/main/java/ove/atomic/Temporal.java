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

package ove.atomic;

import ove.lang.semantics.Nil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: Joubin <alphazero@sensesay.net>
 * @date: 5/10/14
 */
public interface Temporal {

	/* REVU:
	 * Note that this is not quite correct. We need to distinguish between notions of
	 * Point-of-View, Time-line, and, Flows, Epochs. But it is 'good enough' to
	 * try out the conceptual model and gain insight into various implementation
	 * strategies for JVM.
	 *
	 * There is also the issue that the POC impl. equates an Actor with Context,
	 * and Actors are threads, so it is not suitable for light-weight ops, AND,
	 * would preclude use of thread pools e.g. Executors. (This can be addressed
	 * at the cost of reducing the elegance of the impl, by passing along the
	 * context as a token to the thread.)
	 *
	 * As of now,
	 *      Latest Context is a Point of View AND a Flow
	 *        -- there is fluid (auto-commit)
	 *
	 *      Context-chain is a Timeline
	 *        -- it is permissible to go back and change a given Context.
	 *
	 *      Identity for the (in-mem) POC is simply object hashcode of the
	 *      Temporal.Reference. The conceptual model of ADAM (not described
	 *      here) addresses the methodology for deriving Universal Identities
	 *      that would scale this approach across process/node boundaries.
	 *
	 * That said, it is an effective validation of the conceptual model of
	 * ADAM/Atomic/POV:
	 *
	 */
	///////////////////////////////////////////////////////////////////////////
	/// Temporal.Context //////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	/** */
	public interface Context {
		/** */
		Temporal.Context branch ();
		/** */
		Temporal.Context branch (String name);

		/** */
		Temporal.Context merge (Temporal.Context...contexts);

//		/** */
//		Temporal.Flow flow();

		/** */
		<T>void write (int id, T value);

		/** */
		<T> T read (int id);

		@Deprecated // REVU: for the initial sketch-POC only
		int[] modset ();

		Context.Op[]  operations();

		/** */
		Temporal.Context parent ();

		// ------------------------------------------------------------------
		// Context.Op
		// ------------------------------------------------------------------
		/** */
		public interface Op {
			/** */
			public enum Code { initialize, commit, branch, merge }

			/** */
			public static class Exception extends java.lang.Exception {
				public final Context.Op op;
				public Exception (final Context.Op op) {
					this (op, Nil.string);
				}
				public Exception (final Context.Op op, final String message) {
					super(message);
					this.op = op;
				}
			}

			/** */
			Context.Op.Code code();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	/// Temporal.Flow //////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	/** REVU: clarify semantics. This is TODO and not used in the initial POC */
	public interface Flow {

		// ------------------------------------------------------------------
		// Reference.Op
		// ------------------------------------------------------------------
		/** */
		public interface Op {

			/** */
			public enum Code {
				declare(true), read, write(true), lock, freeze, delete(true);

				boolean sideEffecting;
				Code() { this(false); }
				Code(final boolean sideEffecting) { this.sideEffecting = sideEffecting; }
			}

			/** */
			public static class Exception extends java.lang.Exception {
				public final Flow.Op op;
				public Exception (final Flow.Op op) {
					this (op, Nil.string);
				}
				public Exception (final Flow.Op op, final String message) {
					super(message);
					this.op = op;
				}
			}

			/** */
			Flow.Op.Code code ();

			/** */
			boolean sideEffects ();

			/** */
			<X> Reference<X> reference ();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	/// Temporal.Reference ////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	/** */
	public interface Reference<T> {

		/** */
		T get ();

		/** */
		void set (final T value);

		/** */
		T previousRevision ();

		// ------------------------------------------------------------------
		// Reference.Versioned
		// ------------------------------------------------------------------

		/** */
		@Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD})
		public @interface Versioned { }

	}

	///////////////////////////////////////////////////////////////////////////
	/// Temporal.Actor ////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	/** */
	public interface Actor {
		/** */
		Actor fork(Runnable task, String name);

		void merge(final Temporal.Actor...actors) ;

		/** */
		Temporal.Context getTemporalContext ();

		void start();

		void goBackInTime();
	}
}
