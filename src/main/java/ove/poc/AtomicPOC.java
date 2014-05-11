package ove.poc;

import ove.atomic.Temporal;
import ove.atomic.ri.InMemory;

/**
 * Basic proof-of-concept for atomic temporality for Java.
 * This poc uses a stateful POJO (ContactInfo) and 3 temporal actors.
 */
public class AtomicPOC {
	/**
	 *
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main (String[] args) throws InterruptedException{
		/*
		 * Each actor represents a point of view. Actors are hierarchical and are intended to
		 * (loosely) represent a 'world-view'.
		 *
		 * Here we start with 'the-world' (top level). The top-level actor represents the
		 * global mind/view.
		 *
		 * Forked (child) actors represents co-evolving fragments of this world.
		 *
		 * Merging (joining) of the fragments is the consensual evolution of the shared (higher, here
		 * top-level) world view.
		 */
		final InMemory.TemporalActor rootActor = new InMemory.TemporalActor(createAndRunWorld(), "root");

		// start the world and wait until process completion
		rootActor.start();
		rootActor.join();
	}

	/**
	 * The top level task or world evolution.
	 *
	 * Here we create a minimal world consisting of a single object (a ContactInfo instance) and subject
	 * it to co-evolution by 3 concurrent actors.
	 *
	 */
	public static final Runnable createAndRunWorld () {
		return () -> {

			// ContactInfo is a plain-old-java-object (POJO) but has its stateful attributes
			// defined in terms of Temporal.References.
			//
			// Whenever we instantiate an object with Temporal.References, in context of a running Temporal.Actor,
			// it is transparently added to the Temporal.Context of that actor.
			//
			// When we fork in a running task, the child (forked) actor (if provided references to the POJO or
			// explicitly the Temporal.References) can perform operations on the references.
			//
			// [Note that transparent propagation of temporal objects is orthogonal to this proof-of-concept and
			// is addressed via the universal-naming (ADAM) project.]
			//
			//
			final ContactInfo contactInfo = new ContactInfo("Joubin", "alphazero@sensesay.net", "321 Infinity Unloop");

			// In the context of a running actor, we can always obtain reference to the acting agency.
			// (This is just like accessing Thread.currentThread())
			// here 'self' is the reference to the running actor executing this code.

			final Temporal.Actor self = InMemory.TemporalActor.currentActor();

			// We fork off 2 child actors/points-of-views.
			// See taskn() for details, but basically each is mutating a specific (state element) of the
			// contactInfo object.
			//
			// Again, note here we are passing the reference to the POJO. We could of course pass a list/map
			// of objects of interest, or even inject them, etc.

			Temporal.Actor actor_1 = self.fork ( task1 (contactInfo), "actor-1");
			Temporal.Actor actor_2 = self.fork ( task2 (contactInfo), "actor-2");

			// The forked actors are not yet activated.
			// Let's record the state of object.

			log_info ("-- initial state of ContactInfo --");
			log_state ( contactInfo );

			// now start the child actors
			// (Each wll emit its own log for the
			log_info ("-- run the parallel tasks --");

			actor_1.start();
			actor_2.start();

			// merge is just like Thread#join with additional semantics
			// that we merge the state changes affected in the merged actors
			// with the state of the world of the merging actor.
			//
			// this is just like merging branches in a VCS like Git.

			log_info ("-- merge beings --");

			self.merge(actor_1, actor_2);

			// Let's note the state of the world after merge
			log_info ("-- merge end --");

			log_state ( contactInfo );

			// Let's go back in time.
			// this is just like resetting 'HEAD' in a VCS like Git.
			log_info ("-- go back in time --");

			self.goBackInTime ();
			log_state ( contactInfo );
		};
	}

	// ------------------------------------------------------------------------
	// world-state-mutating sub-tasks.
	// ------------------------------------------------------------------------

	/** Mutate state of a ContactInfo - change email */
	public static final Runnable task1 (final ContactInfo contactInfo) {
		return () -> {

			contactInfo.setEmail("joubin@inch.com");

			log_state ( contactInfo );
		};
	}

	/** Mutate state of a ContactInfo - change address */
	public static final Runnable task2 (final ContactInfo contactInfo) {
		return () -> {

			contactInfo.setAddress("123 Main Street");

			log_state ( contactInfo );
		};
	}

	// ------------------------------------------------------------------------
	// santa's little helpers
	// ------------------------------------------------------------------------
	static final void log_info (final String info) {
		System.out.println (info);
	}
	static final void log_state (final ContactInfo contactInfo) {
		System.out.format ("%-17s - %s\n\n", InMemory.TemporalActor.currentActor(), contactInfo.toString());
	}
}