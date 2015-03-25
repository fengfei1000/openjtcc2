/**
 * Copyright 2014-2015 yangming.liu<liuyangming@gmail.com>.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, see <http://www.gnu.org/licenses/>.
 */
package org.bytesoft.bytejta.logger.store;

import java.util.Arrays;

import org.bytesoft.transaction.store.TransactionStorageKey;

public class SimpleTransactionStorageKey implements TransactionStorageKey {
	private final byte[] instanceKey;

	public SimpleTransactionStorageKey(byte[] bytes) throws IllegalArgumentException {
		if (bytes == null) {
			throw new IllegalArgumentException();
		}
		this.instanceKey = bytes;
	}

	public byte[] getInstanceKey() {
		return instanceKey;
	}

	public int hashCode() {
		return Arrays.hashCode(this.instanceKey);
	}

	public boolean equals(Object obj) {
		if (TransactionStorageKey.class.isInstance(obj) == false) {
			return false;
		}
		TransactionStorageKey that = TransactionStorageKey.class.cast(obj);
		return Arrays.equals(this.instanceKey, that.getInstanceKey());
	}
}
