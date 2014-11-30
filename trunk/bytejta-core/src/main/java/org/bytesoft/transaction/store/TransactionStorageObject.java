package org.bytesoft.transaction.store;

public interface TransactionStorageObject {

	// public boolean isEnabled();

	public TransactionStorageKey getStorageKey();

	public byte[] getContentByteArray();

}
