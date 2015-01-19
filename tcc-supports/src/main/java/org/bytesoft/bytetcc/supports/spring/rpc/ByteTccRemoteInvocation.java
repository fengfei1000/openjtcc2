package org.bytesoft.bytetcc.supports.spring.rpc;

import java.lang.reflect.Proxy;

import org.bytesoft.byterpc.RemoteInvocation;
import org.bytesoft.byterpc.common.RemoteDestination;
import org.bytesoft.byterpc.remote.RemoteRequestor;
import org.bytesoft.byterpc.supports.RemoteRequestorAware;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.rpc.TransactionRequest;
import org.bytesoft.transaction.rpc.TransactionResource;

public class ByteTccRemoteInvocation extends RemoteInvocation implements RemoteDestination, RemoteRequestorAware,
		TransactionRequest {
	private static final long serialVersionUID = 1L;

	private transient Object destination;
	private transient RemoteRequestor requestor;
	private TransactionContext transaction;

	public void setRemoteRequestor(RemoteRequestor requestor) {
		this.requestor = requestor;
	}

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
		ByteTccRemoteTransactionStub stub = new ByteTccRemoteTransactionStub();
		stub.setRequestor(this.requestor);
		stub.setIdentifier(String.valueOf(this.destination));
		Class<?> interfaceClass = TransactionResource.class;
		ClassLoader cl = interfaceClass.getClassLoader();
		Object proxyObject = Proxy.newProxyInstance(cl, new Class<?>[] { interfaceClass }, stub);
		return TransactionResource.class.cast(proxyObject);
	}

	public void setTransactionContext(TransactionContext transactionContext) {
		this.transaction = transactionContext;
	}

}
