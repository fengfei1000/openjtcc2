package org.bytesoft.bytetcc.supports.logger;

import java.util.HashSet;
import java.util.Set;

import org.bytesoft.bytetcc.archive.CompensableArchive;
import org.bytesoft.bytetcc.archive.TransactionArchive;
import org.bytesoft.bytetcc.common.TransactionContext;
import org.bytesoft.bytetcc.supports.TransactionLogger;

public class FileTransactionLogger implements TransactionLogger {

	public void enlistService(TransactionContext transactionContext, CompensableArchive holder) {
		// TODO Auto-generated method stub

	}

	public void delistService(TransactionContext transactionContext, CompensableArchive holder) {
		// TODO Auto-generated method stub

	}

	public void updateService(TransactionContext transactionContext, CompensableArchive holder) {
		// TODO Auto-generated method stub

	}

	public void confirmService(TransactionContext transactionContext, CompensableArchive holder) {
		// TODO Auto-generated method stub

	}

	public void cancelService(TransactionContext transactionContext, CompensableArchive holder) {
		// TODO Auto-generated method stub

	}

	public void commitService(TransactionContext transactionContext, CompensableArchive holder) {
		// TODO Auto-generated method stub

	}

	public void rollbackService(TransactionContext transactionContext, CompensableArchive holder) {
		// TODO Auto-generated method stub

	}

	public void beginTransaction(TransactionArchive transaction) {
		// TODO Auto-generated method stub

	}

	public void prepareTransaction(TransactionArchive transaction) {
		// TODO Auto-generated method stub

	}

	public void updateTransaction(TransactionArchive transaction) {
		// TODO Auto-generated method stub

	}

	public void completeTransaction(TransactionArchive transaction) {
		// TODO Auto-generated method stub

	}

	public void cleanupTransaction(TransactionArchive transaction) {
		// TODO Auto-generated method stub

	}

	public Set<TransactionArchive> getLoggedTransactionSet() {
		// TODO Auto-generated method stub
		return new HashSet<TransactionArchive>();
	}

}
