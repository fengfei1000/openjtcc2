package org.bytesoft.bytetcc.supports.spring.rpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.bytesoft.byterpc.RemoteInvocation;
import org.bytesoft.byterpc.RemoteInvocationResult;
import org.bytesoft.byterpc.remote.RemoteRequestor;
import org.bytesoft.byterpc.supports.RemoteInvocationFactory;
import org.bytesoft.bytetcc.supports.spring.beans.ByteTccSkeletonObject;
import org.bytesoft.transaction.rpc.TransactionResource;
import org.bytesoft.utils.CommonUtils;

public class ByteTccRemoteTransactionStub implements InvocationHandler, TransactionResource {
	private String identifier;
	private RemoteRequestor requestor;
	private RemoteInvocationFactory invocationFactory;

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (Object.class.equals(method.getDeclaringClass())) {
			try {
				return method.invoke(this, args);
			} catch (IllegalAccessException ex) {
				throw new RuntimeException(ex);
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		} else if (ByteTccSkeletonObject.class.equals(method.getDeclaringClass())) {
			try {
				return method.invoke(this, args);
			} catch (IllegalAccessException ex) {
				throw new RuntimeException(ex);
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		} else if (TransactionResource.class.equals(method.getDeclaringClass())) {
			try {
				return method.invoke(this, args);
			} catch (IllegalAccessException ex) {
				throw new RuntimeException(ex);
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		} else if (XAResource.class.equals(method.getDeclaringClass())) {
			try {
				return method.invoke(this, args);
			} catch (IllegalAccessException ex) {
				throw new RuntimeException(ex);
			} catch (InvocationTargetException ex) {
				Throwable targetEx = ex.getTargetException();
				if (IllegalStateException.class.isInstance(targetEx)) {
					RemoteInvocationResult result = null;
					RemoteInvocation invocation = this.invocationFactory.createRemoteInvocation();
					// TODO
					result = this.requestor.fireRequest(invocation);
					if (result.isFailure()) {
						throw (Throwable) result.getValue();
					} else {
						return result.getValue();
					}
				} else {
					throw ex.getTargetException();
				}
			}
		} else {
			try {
				return method.invoke(this, args);
			} catch (IllegalAccessException ex) {
				throw new RuntimeException(ex);
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

	public int hashCode() {
		return 37 + 41 * (this.identifier == null ? 0 : this.identifier.hashCode());
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		} else if (Proxy.isProxyClass(obj.getClass()) == false) {
			return false;
		}
		InvocationHandler handler = Proxy.getInvocationHandler(obj);
		if (ByteTccRemoteTransactionStub.class.isInstance(handler) == false) {
			return false;
		}
		ByteTccRemoteTransactionStub that = (ByteTccRemoteTransactionStub) handler;
		return CommonUtils.equals(this.identifier, that.identifier);
	}

	public String toString() {
		return String.format("ByteTccRemoteTransactionStub(identifier: %s)", this.identifier);
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
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

	public void commit(Xid arg0, boolean arg1) throws XAException {
		throw new IllegalStateException();
	}

	public void end(Xid arg0, int arg1) throws XAException {
	}

	public void forget(Xid arg0) throws XAException {
	}

	public int getTransactionTimeout() throws XAException {
		return 0;
	}

	public boolean isSameRM(XAResource xares) throws XAException {
		return this.equals(xares);
	}

	public int prepare(Xid arg0) throws XAException {
		throw new IllegalStateException();
	}

	public Xid[] recover(int arg0) throws XAException {
		throw new IllegalStateException();
	}

	public void rollback(Xid arg0) throws XAException {
		throw new IllegalStateException();
	}

	public boolean setTransactionTimeout(int arg0) throws XAException {
		return false;
	}

	public void start(Xid arg0, int arg1) throws XAException {
	}

}
