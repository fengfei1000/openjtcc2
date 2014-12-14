package org.bytesoft.bytetcc;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.bytesoft.bytetcc.archive.CompensableArchive;
import org.bytesoft.bytetcc.common.TransactionConfigurator;
import org.bytesoft.transaction.RollbackRequiredException;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.archive.XAResourceArchive;
import org.bytesoft.transaction.xa.TransactionXid;

public class CompensableTccTransaction extends CompensableTransaction {
	private int transactionStatus;
	private CompensableJtaTransaction compensableJtaTransaction;
	private final List<CompensableArchive> coordinatorArchives = new ArrayList<CompensableArchive>();
	private final List<CompensableArchive> participantArchives = new ArrayList<CompensableArchive>();
	private final Map<Xid, XAResourceArchive> resourceArchives = new ConcurrentHashMap<Xid, XAResourceArchive>();
	private ThreadLocal<TransactionContext> transientContexts = new ThreadLocal<TransactionContext>();

	public CompensableTccTransaction(TransactionContext transactionContext) {
		super(transactionContext);
	}

	public synchronized void propagationBegin(TransactionContext lastestTransactionContext) {
		this.transientContexts.set(this.transactionContext);
		this.transactionContext = lastestTransactionContext;
	}

	public synchronized void propagationFinish(TransactionContext lastestTransactionContext) {
		TransactionContext originalTransactionContext = this.transientContexts.get();
		this.transientContexts.remove();
		if (originalTransactionContext != null) {
			this.transactionContext = originalTransactionContext;
		}
	}

	public synchronized void delistCompensableInvocation(CompensableInvocation compensable) {
		Object identifier = compensable.getIdentifier();
		if (identifier == null) {
			TransactionXid currentXid = this.transactionContext.getCurrentXid();
			if (this.transactionContext.isCoordinator()) {
				CompensableArchive archive = new CompensableArchive();
				archive.setXid(currentXid);
				archive.setCompensable(compensable);
				this.coordinatorArchives.add(archive);
			} else {
				CompensableArchive archive = new CompensableArchive();
				archive.setXid(currentXid);
				archive.setCompensable(compensable);
				this.participantArchives.add(archive);
			}
			compensable.setIdentifier(currentXid);
		}

	}

	public synchronized void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
	}

	public synchronized void nativeConfirm() throws RollbackRequiredException {
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		CompensableInvocationExecutor executor = configurator.getCompensableInvocationExecutor();
		if (executor != null) {
			List<CompensableInvocation> compensables = new ArrayList<CompensableInvocation>();
			if (this.transactionContext.isCoordinator()) {
				Iterator<CompensableArchive> coordinatorItr = this.coordinatorArchives.iterator();
				while (coordinatorItr.hasNext()) {
					CompensableArchive archive = coordinatorItr.next();
					compensables.add(archive.getCompensable());
				}
			}

			Iterator<CompensableArchive> participantItr = this.participantArchives.iterator();
			while (participantItr.hasNext()) {
				CompensableArchive archive = participantItr.next();
				compensables.add(archive.getCompensable());
			}

			try {
				executor.confirm(compensables);
			} catch (RuntimeException runtimeEx) {
				RollbackRequiredException rrex = new RollbackRequiredException();
				rrex.initCause(runtimeEx);
				throw rrex;
			}
		}
	}

	public synchronized void remoteConfirm() throws SystemException, RemoteException {
		Set<Entry<Xid, XAResourceArchive>> entrySet = this.resourceArchives.entrySet();
		Iterator<Map.Entry<Xid, XAResourceArchive>> resourceItr = entrySet.iterator();
		while (resourceItr.hasNext()) {
			Map.Entry<Xid, XAResourceArchive> entry = resourceItr.next();
			Xid key = entry.getKey();
			XAResourceArchive archive = entry.getValue();
			try {
				archive.commit(key, true);
				archive.setCommitted(true);
				archive.setCompleted(true);
			} catch (XAException xaex) {
				// TODO
			}
		}
	}

	public synchronized boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException,
			SystemException {
		// TODO Auto-generated method stub
		return false;
	}

	public synchronized boolean enlistResource(XAResource xaRes) throws RollbackException, IllegalStateException,
			SystemException {
		// TODO Auto-generated method stub
		return false;
	}

	public int getStatus() throws SystemException {
		return this.transactionStatus;
	}

	public synchronized void registerSynchronization(Synchronization sync) throws RollbackException,
			IllegalStateException, SystemException {
		// TODO Auto-generated method stub

	}

	public synchronized void rollback() throws IllegalStateException, SystemException {
	}

	public synchronized void nativeCancel() throws RollbackRequiredException {
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		CompensableInvocationExecutor executor = configurator.getCompensableInvocationExecutor();
		if (executor != null) {
			// TODO
		}
	}

	public synchronized void remoteCancel() throws SystemException, RemoteException {
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

	public int getTransactionStatus() {
		return transactionStatus;
	}

	public void setTransactionStatus(int transactionStatus) {
		this.transactionStatus = transactionStatus;
	}

	public CompensableJtaTransaction getCompensableJtaTransaction() {
		return compensableJtaTransaction;
	}

	public void setCompensableJtaTransaction(CompensableJtaTransaction compensableJtaTransaction) {
		this.compensableJtaTransaction = compensableJtaTransaction;
	}

}
