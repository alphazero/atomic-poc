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

package ove.poc;

import ove.atomic.Temporal;
import ove.atomic.ri.InMemory;

/**
 *
 * @author: Joubin <alphazero@sensesay.net>
 * @date: 5/11/14
 */
public class List<T> {
	/* -- List.Node -------------------------------------------------- */
	public class Node {
		final T value;
		final Temporal.Reference<List<T>.Node> next;
		Node(final T value) {
			this.value = value;
			this.next = new InMemory.TemporalReference<>(null);
		}
		void setNext(final Node next) {
			this.next.set(next);
		}
		Node next() { return next.get(); }
	}

	// REVU: this can/should be pulled up (TODO) as a general utility class
	public static class TemporalInteger extends InMemory.TemporalReference<Integer> {
		public TemporalInteger (Integer initialValue) {
			super(initialValue);
		}
		public void increment() {
			this.set(this.get()+1);
		}
		public void decrement() {
			this.set(this.get()-1);
		}
	}

	/* -- List ------------------------------------------------------- */

	final TemporalInteger          size;
	final Temporal.Reference<Node> head;

	final Node terminal = new Node(null);

	public List() {
		this.head = new InMemory.TemporalReference<>(terminal);
		this.size = new TemporalInteger(0);
	}

	private Node head() { return head.get(); }

	public int size() {
		return size.get();
	}

	public void add(final T value) {
		final Node current_head = head();
		final Node node = new Node(value);
		node.setNext(current_head);
		head.set(node);
		size.increment();
	}

	/** remove first element */
	public T remove() {
		assertNodeIndex(0);

		final Node node = head();
		head.set(node.next());
		size.decrement();

		return node.value;
	}

	/** remove element at index */
	public T remove (final int index) {
		if(index == 0) {
			return remove();
		}

		assertNodeIndex(index);

		final Node prev = nodeAt(index-1);
		final Node node = prev.next();
		prev.setNext(node.next());
		size.decrement();

		return node.value;
	}

	private void assertNodeIndex(final int index) {
		if(!indexWithinBounds(index)) {
			final String err = String.format("index out of bounds: %d", index);
			throw new IllegalArgumentException(err);
		}
	}

	private boolean indexWithinBounds (final int index) {
		return index >= 0 && index < size.get();
	}

	public T get(final int index) {
		assertNodeIndex(index);
		return nodeAt(index).value;
	}

	private Node nodeAt(final int index) {
		Node node = head();
		for(int n = 1; n <= index; n++) {
			node = node.next();
		}
		return node;
	}

	public T[] toArray (final T[] arr) {
		int i = 0;
		Node n = head();
		while (n != terminal) {
			arr [i++] = n.value;
			n = n.next();
		}
		return arr;
	}
	/* -- test ------------------------------------------------------- */

	public static void main (String[] args) throws InterruptedException{

		final InMemory.TemporalActor rootActor = new InMemory.TemporalActor( () -> {

			final Temporal.Actor self = InMemory.TemporalActor.currentActor();
			final List<String> list = new List<>();
			list.add("common-node-1");
			list.add("common-node-2");
			list.add("common-node-3");
			list.add("common-node-4");

			final Temporal.Actor task_1 = self.fork(task1(list), "task-1");

			task_1.start();

			topLevelFunc(list);

			self.merge(task_1);
			debug(list);

			self.goBackInTime();
			debug(list);

		}, "test-root"
		);
		rootActor.start();
		rootActor.join();
	}

	static Runnable task1(final List<String> list) {
		return () -> {
			list.add("Hi");
			list.add("There!");
		};
	}
	static void topLevelFunc(final List<String> list) {
		puts("init size: %d", list.size());

		list.remove();

		list.add("Salaam");
		list.add("Be");
		list.add("Upon");
		list.add("You!");

		debug(list);

		puts("top-level -- DONE");
	}
	public static void debug(final List<String> list) {
		puts("size: %d", list.size());
		final String[] items = list.toArray(new String[list.size()]);
		for(final String item : items) {
			puts("%s", item);
		}
	}
	public static void puts(String fmt, Object...args) {
		System.out.format(fmt + "\n", args);
	}
}
