package org.bytesoft.bytejta.logger.store;

import java.nio.MappedByteBuffer;

import org.bytesoft.transaction.store.TransactionStorageKey;

public class SimpleStoragePosition {
	private boolean enabled;
	private transient TransactionStorageKey key;
	private MappedByteBuffer buffer;

	public TransactionStorageKey getKey() {
		return key;
	}

	public void setKey(TransactionStorageKey key) {
		this.key = key;
	}

	public MappedByteBuffer getBuffer() {
		return buffer;
	}

	public void setBuffer(MappedByteBuffer buffer) {
		this.buffer = buffer;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
