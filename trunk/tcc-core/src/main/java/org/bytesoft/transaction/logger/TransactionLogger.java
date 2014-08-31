package org.bytesoft.transaction.logger;

import java.util.List;

import org.bytesoft.transaction.archive.TransactionArchive;
import org.bytesoft.transaction.archive.XAResourceArchive;

public interface TransactionLogger {

	/* transaction */
	public void createTransaction(TransactionArchive archive);

	public void updateTransaction(TransactionArchive archive);

	public void deleteTransaction(TransactionArchive archive);

	public List<TransactionArchive> getTransactionArchiveList();

	/* resource */
	public void updateResource(XAResourceArchive archive);

}
