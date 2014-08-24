package org.bytesoft.bytetcc.jta;

import javax.transaction.Synchronization;

public class JtaSynchronization implements Synchronization {
	protected Synchronization delegate;

	public void beforeCompletion() {
		delegate.beforeCompletion();
	}

	public void afterCompletion(int status) {
		delegate.afterCompletion(status);
	}

	public Synchronization getDelegate() {
		return delegate;
	}

	public void setDelegate(Synchronization delegate) {
		this.delegate = delegate;
	}

}
