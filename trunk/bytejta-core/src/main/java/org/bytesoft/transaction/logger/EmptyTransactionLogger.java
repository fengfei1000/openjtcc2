package org.bytesoft.transaction.logger;

import java.util.ArrayList;
import java.util.List;

import org.bytesoft.transaction.archive.TransactionArchive;
import org.bytesoft.transaction.archive.XAResourceArchive;

public class EmptyTransactionLogger implements TransactionLogger {

	public void createTransaction(TransactionArchive archive) {
	}

	public void updateTransaction(TransactionArchive archive) {
	}

	public void deleteTransaction(TransactionArchive archive) {
	}

	public List<TransactionArchive> getTransactionArchiveList() {
		return new ArrayList<TransactionArchive>();
	}

	public void updateResource(XAResourceArchive archive) {
	}

}
