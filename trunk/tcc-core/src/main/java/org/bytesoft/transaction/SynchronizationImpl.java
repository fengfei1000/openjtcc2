package org.bytesoft.transaction;

import javax.transaction.Synchronization;

public class SynchronizationImpl implements Synchronization {
	private Synchronization delegate;
	private boolean beforeCompletionRequired;
	private boolean afterCompletionRequired;

	public SynchronizationImpl(Synchronization sync) {
		if (sync == null) {
			throw new IllegalArgumentException();
		} else {
			this.delegate = sync;
			this.beforeCompletionRequired = true;
			this.afterCompletionRequired = true;
		}
	}

	public void beforeCompletion() {
		if (this.beforeCompletionRequired) {
			try {
				this.delegate.beforeCompletion();
			} finally {
				this.beforeCompletionRequired = false;
			}
		}
	}

	public void afterCompletion(int status) {
		if (this.afterCompletionRequired) {
			try {
				this.delegate.afterCompletion(status);
			} finally {
				this.afterCompletionRequired = false;
			}
		}
	}

	public String toString() {
		return String.format("[%s] delegate: %s", this.getClass().getSimpleName(), this.delegate);
	}
}
