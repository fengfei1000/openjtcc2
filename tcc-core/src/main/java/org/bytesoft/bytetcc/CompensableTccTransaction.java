package org.bytesoft.bytetcc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.bytesoft.bytetcc.common.TransactionConfigurator;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.archive.XAResourceArchive;

public class CompensableTccTransaction extends CompensableTransaction {
	private int transactionStatus;
	private final List<CompensableInvocation> compensables = new ArrayList<CompensableInvocation>();
	// private final Map<Xid, List<CompensableArchive>> compensableArchives = new ConcurrentHashMap<Xid,
	// List<CompensableArchive>>();
	private final Map<Xid, XAResourceArchive> resourceArchives = new ConcurrentHashMap<Xid, XAResourceArchive>();
	private ThreadLocal<TransactionContext> transients = new ThreadLocal<TransactionContext>();

	public CompensableTccTransaction(TransactionContext transactionContext) {
		super(transactionContext);
	}

	public synchronized void propagationBegin(TransactionContext lastestTransactionContext) {
		this.transients.set(this.transactionContext);
		this.transactionContext = lastestTransactionContext;
	}

	public synchronized void propagationFinish(TransactionContext lastestTransactionContext) {
		TransactionContext originalTransactionContext = this.transients.get();
		this.transients.remove();
		if (originalTransactionContext != null) {
			this.transactionContext = originalTransactionContext;
		}
	}

	public void delistCompensableInvocation(CompensableInvocation compensable) {
		this.compensables.add(compensable);
	}

	public synchronized void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
	}

	public synchronized void confirm() throws SystemException {
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

	public synchronized boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException, SystemException {
		// TODO Auto-generated method stub
		return false;
	}

	public synchronized boolean enlistResource(XAResource xaRes) throws RollbackException, IllegalStateException, SystemException {
		// TODO Auto-generated method stub
		return false;
	}

	public int getStatus() throws SystemException {
		return this.transactionStatus;
	}

	public synchronized void registerSynchronization(Synchronization sync) throws RollbackException, IllegalStateException,
			SystemException {
		// TODO Auto-generated method stub

	}

	public synchronized void rollback() throws IllegalStateException, SystemException {
	}

	public synchronized void cancel() throws SystemException {
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

	public synchronized void setRollbackOnly() throws IllegalStateException, SystemException {
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
