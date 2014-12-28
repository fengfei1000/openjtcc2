package org.bytesoft.bytetcc.common;

import java.io.Serializable;

import javax.transaction.xa.Xid;

import org.bytesoft.bytejta.common.TransactionConfigurator;
import org.bytesoft.transaction.xa.TransactionXid;
import org.bytesoft.transaction.xa.XidFactory;

public class TransactionCommonXid extends TransactionXid implements Xid, Serializable {
	private static final long serialVersionUID = 1L;

	public TransactionCommonXid(byte[] global) {
		this(global, new byte[0]);
	}

	public TransactionCommonXid(byte[] global, byte[] branch) {
		super(global, branch);
	}

	public int getFormatId() {
		return XidFactory.TCC_FORMAT_ID;
	}

	public TransactionXid getGlobalXid() {
		if (this.globalTransactionId == null || this.globalTransactionId.length == 0) {
			throw new IllegalStateException();
		} else if (this.branchQualifier != null && this.branchQualifier.length > 0) {
			TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
			XidFactory xidFactory = transactionConfigurator.getXidFactory();
			return xidFactory.createGlobalXid(this.globalTransactionId);
		} else {
			return this;
		}
	}

	public TransactionXid createBranchXid() {
		if (this.globalTransactionId == null || this.globalTransactionId.length == 0) {
			throw new IllegalStateException();
		} else if (this.branchQualifier != null && this.branchQualifier.length > 0) {
			throw new IllegalStateException();
		} else {
			TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
			XidFactory xidFactory = transactionConfigurator.getXidFactory();
			return xidFactory.createBranchXid(this);
		}
	}

}
