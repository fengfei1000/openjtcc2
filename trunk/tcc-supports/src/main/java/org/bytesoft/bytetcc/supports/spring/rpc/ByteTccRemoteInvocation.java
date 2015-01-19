package org.bytesoft.bytetcc.supports.spring.rpc;

import java.lang.reflect.Proxy;

import org.bytesoft.byterpc.RemoteInvocation;
import org.bytesoft.byterpc.common.RemoteDestination;
import org.bytesoft.byterpc.remote.RemoteRequestor;
import org.bytesoft.byterpc.supports.RemoteInvocationFactory;
import org.bytesoft.byterpc.supports.RemoteInvocationFactoryAware;
import org.bytesoft.byterpc.supports.RemoteMethodFactory;
import org.bytesoft.byterpc.supports.RemoteMethodFactoryAware;
import org.bytesoft.byterpc.supports.RemoteRequestorAware;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.rpc.TransactionRequest;
import org.bytesoft.transaction.rpc.TransactionResource;

public class ByteTccRemoteInvocation extends RemoteInvocation implements RemoteDestination, RemoteRequestorAware,
		RemoteInvocationFactoryAware, RemoteMethodFactoryAware, TransactionRequest {
	private static final long serialVersionUID = 1L;

	private TransactionContext transaction;
	private transient Object destination;
	private transient RemoteRequestor requestor;
	private transient RemoteInvocationFactory invocationFactory;
	private transient RemoteMethodFactory remoteMethodFactory;

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
		stub.setRemoteMethodFactory(this.remoteMethodFactory);
		stub.setInvocationFactory(this.invocationFactory);
		Class<?> interfaceClass = TransactionResource.class;
		ClassLoader cl = interfaceClass.getClassLoader();
		Object proxyObject = Proxy.newProxyInstance(cl, new Class<?>[] { interfaceClass }, stub);
		return TransactionResource.class.cast(proxyObject);
	}

	public void setTransactionContext(TransactionContext transactionContext) {
		this.transaction = transactionContext;
	}

	public void setRemoteInvocationFactory(RemoteInvocationFactory invocationFactory) {
		this.invocationFactory = invocationFactory;
	}

	public void setRemoteMethodFactory(RemoteMethodFactory remoteMethodFactory) {
		this.remoteMethodFactory = remoteMethodFactory;
	}
}
