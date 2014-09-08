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
