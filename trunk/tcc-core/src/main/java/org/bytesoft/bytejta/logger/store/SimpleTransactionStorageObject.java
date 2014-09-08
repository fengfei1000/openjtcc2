package org.bytesoft.bytejta.logger.store;

import javax.transaction.Status;
import javax.transaction.xa.XAResource;

import org.bytesoft.transaction.archive.TransactionArchive;
import org.bytesoft.transaction.serialize.TransactionArchiveSerializer;
import org.bytesoft.transaction.store.TransactionStorageKey;
import org.bytesoft.transaction.store.TransactionStorageObject;
import org.bytesoft.transaction.xa.XidFactory;

public class SimpleTransactionStorageObject implements TransactionStorageObject {
	private final byte[] byteArray;
	private TransactionArchiveSerializer transactionArchiveSerializer;

	public SimpleTransactionStorageObject(byte[] contentByteArray) {
		this.byteArray = contentByteArray;
	}

	public boolean isEnabled() {
		try {
			TransactionArchive archive = this.transactionArchiveSerializer.deserialize(this.byteArray);
			int status = archive.getStatus();
			if (status == Status.STATUS_COMMITTED || status == Status.STATUS_ROLLEDBACK) {
				return false;
			}
			int vote = archive.getVote();
			if (vote == XAResource.XA_RDONLY) {
				return false;
			}
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	public TransactionStorageKey getStorageKey() {
		byte[] instanceKey = new byte[XidFactory.GLOBAL_TRANSACTION_LENGTH];
		System.arraycopy(this.byteArray, 0, instanceKey, 0, instanceKey.length);
		return new SimpleTransactionStorageKey(instanceKey);
	}

	public byte[] getContentByteArray() {
		return this.byteArray;
	}

	public void setTransactionArchiveSerializer(TransactionArchiveSerializer transactionArchiveSerializer) {
		this.transactionArchiveSerializer = transactionArchiveSerializer;
	}
}
