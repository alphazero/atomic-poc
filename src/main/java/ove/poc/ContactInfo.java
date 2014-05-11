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

/**
 * @author: Joubin <alphazero@sensesay.net>
 * @date: 5/11/14
 */

import ove.atomic.Temporal;
import ove.atomic.ri.InMemory;

/**
 * POJO with Temporal attributes (i.e. Atomic State..) using the
 * InMemory proof-of-concept implementation.
 *
 * In the fully realized implementation (outside of scope of this POC) the
 * immutability of instances of this class would be enforced. Ultimately, this
 * is just an 'immutable value'.
 *
 * Further note that in a more developed variant, we would simply annotate the
 * stateful bits with Temporal annotations and instrument the class to inject
 * the Temporal.Reference related bits of code.
 *
 */
public class ContactInfo {
	/** 'immutable' attribute -- defines identity (not addressed in this POC) */
	private final String name;

	/** object state element - Note the reference itself is immutable */
	private final Temporal.Reference<String> email;

	/** object state element - Note the reference itself is immutable */
	private final Temporal.Reference<String> address;

	public ContactInfo(final String name, final String email, final String address) {
		this.name = name;
		this.email = new InMemory.TemporalReference<>(email);
		this.address = new InMemory.TemporalReference<>(address);
	}

	/** @return the invariant attribute of the object, regardless of the Temporal.Context */
	public final String getName() {
		return this.name;
	}

	/* -- state mutation -- */
	/* -- Set the state element of object for the Temporal.Context of the calling Temporal.Actor */

	public final void setAddress(final String address) {
		this.address.set(address);
	}

	public final void setEmail(final String email) {
		this.email.set(email);
	}
	/* -- state mutation -- */

	/* -- state access -- */
	/* -- @return the state element of object for the Temporal.Context of the calling Temporal.Actor */

	public final String getAddress() {
		return this.address.get();
	}

	public final String getEmail() {
		return this.email.get();
	}

	/** @return a string representation of the object, in context of the Temporal.Context */
	@Override final public String toString () {
		return String.format("%s - address: %-19s - email: %s", this.name, this.getAddress(), this.getEmail());
	}
	/* -- state access -- */

}
