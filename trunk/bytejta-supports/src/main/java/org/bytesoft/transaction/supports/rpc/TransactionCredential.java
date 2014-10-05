package org.bytesoft.transaction.supports.rpc;

import java.io.Serializable;
import java.util.Arrays;

public class TransactionCredential implements Serializable {
	private static final long serialVersionUID = 1L;

	private final byte[] credential;

	public TransactionCredential(byte[] bytes) {
		this.credential = bytes;
	}

	public byte[] getCredential() {
		return credential;
	}

	public int hashCode() {
		if (this.credential == null) {
			return 0;
		} else {
			return 17 * Arrays.hashCode(this.credential);
		}
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		} else if (TransactionCredential.class.isInstance(obj) == false) {
			return false;
		}
		TransactionCredential that = (TransactionCredential) obj;
		byte[] thisCredential = this.credential;
		byte[] thatCredential = that.credential;
		if (thisCredential == null && thatCredential == null) {
			return true;
		} else if (thisCredential == null) {
			return false;
		} else if (thatCredential == null) {
			return false;
		} else {
			return Arrays.equals(thisCredential, thatCredential);
		}
	}

}
