package org.bytesoft.bytejta.logger.store;

import org.bytesoft.transaction.store.TransactionStorageKey;
import org.bytesoft.transaction.store.TransactionStorageObject;
import org.bytesoft.transaction.xa.XidFactory;

public class SimpleTransactionStorageObject implements TransactionStorageObject {
	private final byte[] byteArray;

	public SimpleTransactionStorageObject(byte[] contentByteArray) {
		this.byteArray = contentByteArray;
	}

	public TransactionStorageKey getStorageKey() {
		byte[] instanceKey = new byte[XidFactory.GLOBAL_TRANSACTION_LENGTH];
		System.arraycopy(this.byteArray, 0, instanceKey, 0, instanceKey.length);
		return new SimpleTransactionStorageKey(instanceKey);
	}

	public byte[] getContentByteArray() {
		return this.byteArray;
	}

}
