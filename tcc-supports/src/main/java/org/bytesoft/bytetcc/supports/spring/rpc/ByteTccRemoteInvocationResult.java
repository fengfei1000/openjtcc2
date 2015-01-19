package org.bytesoft.bytetcc.supports.spring.rpc;

import java.lang.reflect.Proxy;

import org.bytesoft.byterpc.RemoteInvocationResult;
import org.bytesoft.byterpc.remote.RemoteRequestor;
import org.bytesoft.byterpc.supports.RemoteInvocationFactory;
import org.bytesoft.byterpc.supports.RemoteInvocationFactoryAware;
import org.bytesoft.byterpc.supports.RemoteMethodFactory;
import org.bytesoft.byterpc.supports.RemoteMethodFactoryAware;
import org.bytesoft.byterpc.supports.RemoteRequestorAware;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.rpc.TransactionResource;
import org.bytesoft.transaction.rpc.TransactionResponse;

public class ByteTccRemoteInvocationResult extends RemoteInvocationResult implements RemoteRequestorAware,
		RemoteInvocationFactoryAware, RemoteMethodFactoryAware, TransactionResponse {
	private static final long serialVersionUID = 1L;

	private TransactionContext transaction;
	private transient RemoteRequestor requestor;
	private transient RemoteInvocationFactory invocationFactory;
	private transient RemoteMethodFactory remoteMethodFactory;

	public ByteTccRemoteInvocationResult(ByteTccRemoteInvocation invocation) {
		super(invocation);
	}

	public TransactionContext getTransactionContext() {
		return this.transaction;
	}

	public TransactionResource getTransactionResource() {
		ByteTccRemoteTransactionStub stub = new ByteTccRemoteTransactionStub();
		stub.setRequestor(this.requestor);
		ByteTccRemoteInvocation invocation = (ByteTccRemoteInvocation) this.getInvocation();
		stub.setIdentifier(String.valueOf(invocation.getDestination()));
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

	public void setRemoteRequestor(RemoteRequestor requestor) {
		this.requestor = requestor;
	}

	public void setRemoteInvocationFactory(RemoteInvocationFactory invocationFactory) {
		this.invocationFactory = invocationFactory;
	}

	public void setRemoteMethodFactory(RemoteMethodFactory remoteMethodFactory) {
		this.remoteMethodFactory = remoteMethodFactory;
	}

}
