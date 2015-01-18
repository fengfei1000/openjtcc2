package org.bytesoft.bytetcc.supports.spring.rpc;

import org.bytesoft.byterpc.RemoteInvocationResult;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.rpc.TransactionResource;
import org.bytesoft.transaction.rpc.TransactionResponse;

public class ByteTccRemoteInvocationResult extends RemoteInvocationResult implements TransactionResponse {
	private static final long serialVersionUID = 1L;

	private TransactionContext transaction;

	public ByteTccRemoteInvocationResult(ByteTccRemoteInvocation invocation) {
		super(invocation);
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
