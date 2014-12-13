package org.bytesoft.bytetcc;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;

import org.bytesoft.bytejta.TransactionImpl;
import org.bytesoft.transaction.TransactionContext;

public class CompensableJtaTransaction extends CompensableTransaction {

	public CompensableJtaTransaction(TransactionContext transactionContext) {
		super(transactionContext);
	}

	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
		this.jtaTransaction.commit();
	}

	public boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException, SystemException {
		return this.jtaTransaction.delistResource(xaRes, flag);
	}

	public boolean enlistResource(XAResource xaRes) throws RollbackException, IllegalStateException, SystemException {
		return this.jtaTransaction.enlistResource(xaRes);
	}

	public int getStatus() throws SystemException {
		return this.jtaTransaction.getStatus();
	}

	public void registerSynchronization(Synchronization sync) throws RollbackException, IllegalStateException,
			SystemException {
		this.jtaTransaction.registerSynchronization(sync);
	}

	public void rollback() throws IllegalStateException, SystemException {
		this.jtaTransaction.rollback();
	}

	public void prepareStart() {
	}

	public void prepareComplete(boolean success) {
	}

	public void commitStart() {
	}

	public void commitComplete(boolean success) {
	}

	public void rollbackStart() {
	}

	public void rollbackComplete(boolean success) {
	}

	public void setRollbackOnly() throws IllegalStateException, SystemException {
		this.jtaTransaction.setRollbackOnly();
	}

	public TransactionImpl getJtaTransaction() {
		return jtaTransaction;
	}

	public void setJtaTransaction(TransactionImpl jtaTransaction) {
		this.jtaTransaction = jtaTransaction;
	}

}
