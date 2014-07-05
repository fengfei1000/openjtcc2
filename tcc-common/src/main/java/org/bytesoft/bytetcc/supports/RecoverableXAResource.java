package org.bytesoft.bytetcc.supports;

import javax.transaction.xa.XAResource;

public final class RecoverableXAResource {

	private boolean supportXA;
	private XAResource delegate;
	private String recoveryURI;

	public boolean isSupportXA() {
		return supportXA;
	}

	public void setSupportXA(boolean supportXA) {
		this.supportXA = supportXA;
	}

	public XAResource getDelegate() {
		return delegate;
	}

	public void setDelegate(XAResource delegate) {
		this.delegate = delegate;
	}

	public String getDelegateURI() {
		return recoveryURI;
	}

	public void setDelegateURI(String delegateURI) {
		this.recoveryURI = delegateURI;
	}

}
