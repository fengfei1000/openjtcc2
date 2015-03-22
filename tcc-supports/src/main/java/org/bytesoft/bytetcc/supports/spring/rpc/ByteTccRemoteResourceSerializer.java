package org.bytesoft.bytetcc.supports.spring.rpc;

import java.io.IOException;
import java.lang.reflect.Proxy;

import org.bytesoft.byterpc.remote.RemoteRequestor;
import org.bytesoft.byterpc.supports.RemoteInvocationFactory;
import org.bytesoft.byterpc.supports.RemoteMethodFactory;
import org.bytesoft.transaction.rpc.TransactionResource;
import org.bytesoft.transaction.supports.spring.AbstractXAResourceSerializer;

public class ByteTccRemoteResourceSerializer extends AbstractXAResourceSerializer {
	private RemoteRequestor requestor;
	private RemoteInvocationFactory invocationFactory;
	private RemoteMethodFactory remoteMethodFactory;

	public TransactionResource deserializeTransactionResource(String identifier) throws IOException {
		ByteTccRemoteTransactionStub stub = new ByteTccRemoteTransactionStub();
		stub.setRequestor(this.requestor);
		stub.setIdentifier(identifier);
		stub.setRemoteMethodFactory(this.remoteMethodFactory);
		stub.setInvocationFactory(this.invocationFactory);
		Class<?> interfaceClass = TransactionResource.class;
		ClassLoader cl = interfaceClass.getClassLoader();
		Object proxyObject = Proxy.newProxyInstance(cl, new Class<?>[] { interfaceClass }, stub);
		return TransactionResource.class.cast(proxyObject);
	}

	public RemoteRequestor getRequestor() {
		return requestor;
	}

	public void setRequestor(RemoteRequestor requestor) {
		this.requestor = requestor;
	}

	public RemoteInvocationFactory getInvocationFactory() {
		return invocationFactory;
	}

	public void setInvocationFactory(RemoteInvocationFactory invocationFactory) {
		this.invocationFactory = invocationFactory;
	}

	public RemoteMethodFactory getRemoteMethodFactory() {
		return remoteMethodFactory;
	}

	public void setRemoteMethodFactory(RemoteMethodFactory remoteMethodFactory) {
		this.remoteMethodFactory = remoteMethodFactory;
	}

}
