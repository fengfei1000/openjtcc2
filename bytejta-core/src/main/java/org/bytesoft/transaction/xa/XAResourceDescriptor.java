package org.bytesoft.transaction.xa;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public class XAResourceDescriptor implements XAResource {
	private boolean remote;
	private boolean supportsXA;
	private XAResource delegate;
	private String identifier;
	private int descriptorId;

	public void commit(Xid arg0, boolean arg1) throws XAException {
		delegate.commit(arg0, arg1);
	}

	public void end(Xid arg0, int arg1) throws XAException {
		delegate.end(arg0, arg1);
	}

	public void forget(Xid arg0) throws XAException {
		delegate.forget(arg0);
	}

	public int getTransactionTimeout() throws XAException {
		return delegate.getTransactionTimeout();
	}

	public boolean isSameRM(XAResource arg0) throws XAException {
		if (XAResourceDescriptor.class.isInstance(arg0)) {
			XAResourceDescriptor descriptor = (XAResourceDescriptor) arg0;
			return delegate.isSameRM(descriptor.getDelegate());
		} else {
			return delegate.isSameRM(arg0);
		}
	}

	public int prepare(Xid arg0) throws XAException {
		return delegate.prepare(arg0);
	}

	public Xid[] recover(int arg0) throws XAException {
		return delegate.recover(arg0);
	}

	public void rollback(Xid arg0) throws XAException {
		delegate.rollback(arg0);
	}

	public boolean setTransactionTimeout(int arg0) throws XAException {
		return delegate.setTransactionTimeout(arg0);
	}

	public void start(Xid arg0, int arg1) throws XAException {
		delegate.start(arg0, arg1);
	}

	public String toString() {
		return String.format("xa-res[identifier= %s, descriptor= %s, support-xa= %s, remote-res= %s]"//
				, this.identifier, this.descriptorId, this.supportsXA, this.remote);
	}

	public boolean isRemote() {
		return remote;
	}

	public void setRemote(boolean remote) {
		this.remote = remote;
	}

	public XAResource getDelegate() {
		return delegate;
	}

	public void setDelegate(XAResource delegate) {
		this.delegate = delegate;
	}

	public boolean isSupportsXA() {
		return supportsXA;
	}

	public void setSupportsXA(boolean supportsXA) {
		this.supportsXA = supportsXA;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public int getDescriptorId() {
		return descriptorId;
	}

	public void setDescriptorId(int descriptorId) {
		this.descriptorId = descriptorId;
	}

}
