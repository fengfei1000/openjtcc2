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

	private CompensableTccTransaction compensableTccTransaction;

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
		if (this.compensableTccTransaction != null) {
			this.compensableTccTransaction.prepareStart();
		}
	}

	public void prepareComplete(boolean success) {
		if (this.compensableTccTransaction != null) {
			this.compensableTccTransaction.prepareComplete(success);
		}
	}

	public void commitStart() {
		if (this.compensableTccTransaction != null) {
			this.compensableTccTransaction.commitStart();
		}
	}

	public void commitComplete(boolean success) {
		if (this.compensableTccTransaction != null) {
			this.compensableTccTransaction.commitComplete(success);
		}
	}

	public void rollbackStart() {
		if (this.compensableTccTransaction != null) {
			this.compensableTccTransaction.rollbackStart();
		}
	}

	public void rollbackComplete(boolean success) {
		if (this.compensableTccTransaction != null) {
			this.compensableTccTransaction.rollbackComplete(success);
		}
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

	public CompensableTccTransaction getCompensableTccTransaction() {
		return compensableTccTransaction;
	}

	public void setCompensableTccTransaction(CompensableTccTransaction compensableTccTransaction) {
		this.compensableTccTransaction = compensableTccTransaction;
	}

}
