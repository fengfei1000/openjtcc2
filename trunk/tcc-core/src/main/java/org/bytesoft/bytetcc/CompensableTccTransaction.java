package org.bytesoft.bytetcc;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;

import org.bytesoft.bytetcc.common.TransactionConfigurator;
import org.bytesoft.transaction.TransactionContext;

public class CompensableTccTransaction extends CompensableTransaction {
	private int transactionStatus;
	private final List<CompensableInvocation> compensables = new ArrayList<CompensableInvocation>();

	public CompensableTccTransaction(TransactionContext transactionContext) {
		super(transactionContext);
	}

	public void delistCompensableInvocation(CompensableInvocation compensable) {
		this.compensables.add(compensable);
	}

	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		CompensableInvocationExecutor executor = configurator.getCompensableInvocationExecutor();
		if (executor != null) {
			try {
				executor.confirm(this.compensables);
			} catch (Throwable thrown) {
				SystemException ex = new SystemException();
				ex.initCause(thrown);
				throw ex;
			}
		}
	}

	public boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException, SystemException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean enlistResource(XAResource xaRes) throws RollbackException, IllegalStateException, SystemException {
		// TODO Auto-generated method stub
		return false;
	}

	public int getStatus() throws SystemException {
		return this.transactionStatus;
	}

	public void registerSynchronization(Synchronization sync) throws RollbackException, IllegalStateException,
			SystemException {
		// TODO Auto-generated method stub

	}

	public void rollback() throws IllegalStateException, SystemException {
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		CompensableInvocationExecutor executor = configurator.getCompensableInvocationExecutor();
		if (executor != null) {
			try {
				executor.cancel(this.compensables);
			} catch (Throwable thrown) {
				SystemException ex = new SystemException();
				ex.initCause(thrown);
				throw ex;
			}
		}
	}

	public void setRollbackOnly() throws IllegalStateException, SystemException {
		if (this.transactionStatus == Status.STATUS_ACTIVE || this.transactionStatus == Status.STATUS_MARKED_ROLLBACK) {
			this.transactionStatus = Status.STATUS_MARKED_ROLLBACK;
		} else {
			throw new IllegalStateException();
		}
	}

	public boolean isRollbackOnly() {
		return this.transactionStatus == Status.STATUS_MARKED_ROLLBACK;
	}
}
