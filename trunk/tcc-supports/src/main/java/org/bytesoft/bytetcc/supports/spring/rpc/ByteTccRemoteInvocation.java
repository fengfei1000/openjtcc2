package org.bytesoft.bytetcc.supports.spring.rpc;

import org.bytesoft.byterpc.RemoteInvocation;
import org.bytesoft.byterpc.common.RemoteDestination;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.rpc.TransactionRequest;
import org.bytesoft.transaction.rpc.TransactionResource;

public class ByteTccRemoteInvocation extends RemoteInvocation implements RemoteDestination, TransactionRequest {
	private static final long serialVersionUID = 1L;

	private transient Object destination;
	private TransactionContext transaction;

	public Object getDestination() {
		return destination;
	}

	public void setDestination(Object destination) {
		this.destination = destination;
	}

	public TransactionContext getTransactionContext() {
		return this.transaction;
	}

	public TransactionResource getTransactionResource() {
		return null;
	}

	public void setTransactionContext(TransactionContext transactionContext) {
		this.transaction = transactionContext;
	}

}
