package org.bytesoft.bytetcc.supports.logger;

import java.util.Set;

import org.bytesoft.bytetcc.archive.CompensableArchive;
import org.bytesoft.bytetcc.archive.TransactionArchive;
import org.bytesoft.bytetcc.supports.TransactionLogger;
import org.bytesoft.transaction.TransactionContext;

public class FileTransactionLogger implements TransactionLogger {

	public void beginTransaction(TransactionArchive arg0) {
		// TODO Auto-generated method stub

	}

	public void cancelService(TransactionContext arg0, CompensableArchive arg1) {
		// TODO Auto-generated method stub

	}

	public void cleanupTransaction(TransactionArchive arg0) {
		// TODO Auto-generated method stub

	}

	public void commitService(TransactionContext arg0, CompensableArchive arg1) {
		// TODO Auto-generated method stub

	}

	public void completeTransaction(TransactionArchive arg0) {
		// TODO Auto-generated method stub

	}

	public void confirmService(TransactionContext arg0, CompensableArchive arg1) {
		// TODO Auto-generated method stub

	}

	public void delistService(TransactionContext arg0, CompensableArchive arg1) {
		// TODO Auto-generated method stub

	}

	public void enlistService(TransactionContext arg0, CompensableArchive arg1) {
		// TODO Auto-generated method stub

	}

	public Set<TransactionArchive> getLoggedTransactionSet() {
		// TODO Auto-generated method stub
		return null;
	}

	public void prepareTransaction(TransactionArchive arg0) {
		// TODO Auto-generated method stub

	}

	public void rollbackService(TransactionContext arg0, CompensableArchive arg1) {
		// TODO Auto-generated method stub

	}

	public void updateService(TransactionContext arg0, CompensableArchive arg1) {
		// TODO Auto-generated method stub

	}

	public void updateTransaction(TransactionArchive arg0) {
		// TODO Auto-generated method stub

	}
}
