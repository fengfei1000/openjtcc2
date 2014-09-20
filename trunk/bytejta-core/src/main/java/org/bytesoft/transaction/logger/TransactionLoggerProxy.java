package org.bytesoft.transaction.logger;

import java.util.List;

import org.bytesoft.transaction.archive.TransactionArchive;
import org.bytesoft.transaction.archive.XAResourceArchive;

public class TransactionLoggerProxy implements TransactionLogger {
	private static final EmptyTransactionLogger empty = new EmptyTransactionLogger();
	private TransactionLogger delegate = empty;

	public void createTransaction(TransactionArchive archive) {
		delegate.createTransaction(archive);
	}

	public void updateTransaction(TransactionArchive archive) {
		delegate.updateTransaction(archive);
	}

	public void deleteTransaction(TransactionArchive archive) {
		delegate.deleteTransaction(archive);
	}

	public List<TransactionArchive> getTransactionArchiveList() {
		return delegate.getTransactionArchiveList();
	}

	public void updateResource(XAResourceArchive archive) {
		delegate.updateResource(archive);
	}

	public void setDelegate(TransactionLogger delegate) {
		if (delegate == null) {
			this.delegate = empty;
		} else {
			this.delegate = delegate;
		}
	}

	public TransactionLogger getDelegate() {
		return delegate;
	}
}
