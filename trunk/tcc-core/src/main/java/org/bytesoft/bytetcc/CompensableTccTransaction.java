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
import org.bytesoft.bytetcc.archive.CompensableTransactionArchive;
import org.bytesoft.bytetcc.common.TransactionConfigurator;
import org.bytesoft.bytetcc.supports.CompensableTransactionLogger;
import org.bytesoft.transaction.RollbackRequiredException;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.TransactionListener;
import org.bytesoft.transaction.archive.XAResourceArchive;
import org.bytesoft.transaction.xa.TransactionXid;

public class CompensableTccTransaction extends CompensableTransaction {
	public static int STATUS_UNKNOWN = 0;
	public static int STATUS_TRY_FAILURE = 1;
	public static int STATUS_TRIED = 2;
	public static int STATUS_CONFIRMING = 3;
	public static int STATUS_CONFIRM_FAILURE = 4;
	public static int STATUS_CONFIRMED = 5;
	public static int STATUS_CANCELLING = 6;
	public static int STATUS_CANCEL_FAILURE = 7;
	public static int STATUS_CANCELLED = 8;

	private int transactionStatus;
	private int compensableStatus;
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
				archive.setCoordinator(true);
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
		throw new IllegalStateException();
	}

	public synchronized void nativeConfirm() throws RollbackRequiredException {
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		CompensableInvocationExecutor executor = configurator.getCompensableInvocationExecutor();
		try {
			this.compensableStatus = CompensableTccTransaction.STATUS_CONFIRMING;

			if (this.transactionContext.isCoordinator()) {
				Iterator<CompensableArchive> coordinatorItr = this.coordinatorArchives.iterator();
				while (coordinatorItr.hasNext()) {
					CompensableArchive archive = coordinatorItr.next();
					executor.confirm(archive.getCompensable());
				}
			}

			Iterator<CompensableArchive> participantItr = this.participantArchives.iterator();
			while (participantItr.hasNext()) {
				CompensableArchive archive = participantItr.next();
				this.compensableStatus = CompensableTccTransaction.STATUS_CONFIRMING;
				executor.confirm(archive.getCompensable());
			}
		} catch (RuntimeException runtimeEx) {
			RollbackRequiredException rrex = new RollbackRequiredException();
			rrex.initCause(runtimeEx);
			throw rrex;
		}
	}

	public synchronized void remoteConfirm() throws SystemException, RemoteException {

		TransactionXid globalXid = this.transactionContext.getGlobalXid();
		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		CompensableTransactionLogger transactionLogger = transactionConfigurator.getTransactionLogger();

		Set<Entry<Xid, XAResourceArchive>> entrySet = this.resourceArchives.entrySet();
		Iterator<Map.Entry<Xid, XAResourceArchive>> resourceItr = entrySet.iterator();
		while (resourceItr.hasNext()) {
			Map.Entry<Xid, XAResourceArchive> entry = resourceItr.next();
			Xid key = entry.getKey();
			XAResourceArchive archive = entry.getValue();
			if (archive.isCompleted()) {
				if (archive.isCommitted()) {
					// TODO
				} else if (archive.isRolledback()) {
					// TODO
				} else {
					// TODO
				}
			} else {
				try {
					archive.commit(key, true);
					archive.setCommitted(true);
					archive.setCompleted(true);
				} catch (XAException xaex) {
					// TODO
					switch (xaex.errorCode) {
					case XAException.XA_HEURCOM:
					case XAException.XA_HEURRB:
					case XAException.XA_HEURMIX:
					case XAException.XAER_NOTA:
					case XAException.XAER_RMFAIL:
					case XAException.XAER_RMERR:
					}
				}

				transactionLogger.updateResource(globalXid, archive);
			}

		} // end-while (resourceItr.hasNext())

	}

	public synchronized boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException, SystemException {
		return this.jtaTransaction.delistResource(xaRes, flag);
	}

	public synchronized boolean enlistResource(XAResource xaRes) throws RollbackException, IllegalStateException,
			SystemException {
		return this.jtaTransaction.enlistResource(xaRes);
	}

	public int getStatus() throws SystemException {
		return this.transactionStatus;
	}

	public synchronized void setTransactionStatus(int transactionStatus) {
		this.transactionStatus = transactionStatus;
	}

	public synchronized void registerSynchronization(Synchronization sync) throws RollbackException, IllegalStateException,
			SystemException {
		// TODO
		this.jtaTransaction.registerSynchronization(sync);
	}

	public synchronized void rollback() throws IllegalStateException, SystemException {
		throw new IllegalStateException();
	}

	public synchronized void nativeCancel() throws RollbackRequiredException {
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		CompensableInvocationExecutor executor = configurator.getCompensableInvocationExecutor();
		try {
			this.compensableStatus = CompensableTccTransaction.STATUS_CANCELLING;
			Iterator<CompensableArchive> participantItr = this.participantArchives.iterator();
			while (participantItr.hasNext()) {
				CompensableArchive archive = participantItr.next();
				executor.cancel(archive.getCompensable());
			}
		} catch (RuntimeException runtimeEx) {
			RollbackRequiredException rrex = new RollbackRequiredException();
			rrex.initCause(runtimeEx);
			throw rrex;
		}
	}

	public synchronized void remoteCancel() throws SystemException, RemoteException {
	}

	public CompensableTransactionArchive getTransactionArchive() {
		TransactionContext transactionContext = this.getTransactionContext();
		CompensableTransactionArchive transactionArchive = new CompensableTransactionArchive();
		transactionArchive.setXid(transactionContext.getGlobalXid());
		transactionArchive.setStatus(this.transactionStatus);
		transactionArchive.setCompensableStatus(this.compensableStatus);
		transactionArchive.setCompensable(transactionContext.isCompensable());
		transactionArchive.setCoordinator(transactionContext.isCoordinator());

		transactionArchive.getCompensables().addAll(this.coordinatorArchives);
		transactionArchive.getCompensables().addAll(this.participantArchives);

		transactionArchive.getRemoteResources().addAll(this.resourceArchives.values());

		return transactionArchive;
	}

	public synchronized void setRollbackOnly() throws IllegalStateException, SystemException {
		if (this.transactionStatus == Status.STATUS_ACTIVE || this.transactionStatus == Status.STATUS_MARKED_ROLLBACK) {
			if (this.jtaTransaction != null) {
				this.jtaTransaction.setRollbackOnlyQuietly();
			}
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

	public void commitSuccess() {
		if (this.transactionStatus == Status.STATUS_PREPARING) {
			// TODO transaction-log
			this.compensableStatus = CompensableTccTransaction.STATUS_TRIED;
		} else if (this.transactionStatus == Status.STATUS_COMMITTING) {
			// TODO transaction-log
			this.compensableStatus = CompensableTccTransaction.STATUS_CONFIRMED;
		} else if (this.transactionStatus == Status.STATUS_ROLLING_BACK) {
			// TODO transaction-log
			this.compensableStatus = CompensableTccTransaction.STATUS_CANCELLED;
		}
	}

	public void commitFailure(int optcode) {
		if (this.transactionStatus == Status.STATUS_PREPARING) {
			this.completeFailureInPreparing(optcode);
		} else if (this.transactionStatus == Status.STATUS_COMMITTING) {
			this.completeFailureInCommitting(optcode);
		} else if (this.transactionStatus == Status.STATUS_ROLLING_BACK) {
			this.completeFailureInRollingback(optcode);
		}
	}

	private void completeFailureInPreparing(int optcode) {
		if (optcode == TransactionListener.OPT_DEFAULT) {
			this.compensableStatus = CompensableTccTransaction.STATUS_TRY_FAILURE;
		} else if (optcode == TransactionListener.OPT_HEURCOM) {
			this.compensableStatus = CompensableTccTransaction.STATUS_TRIED;
		} else if (optcode == TransactionListener.OPT_HEURRB) {
			// ignore
		} else if (optcode == TransactionListener.OPT_HEURMIX) {
			this.compensableStatus = CompensableTccTransaction.STATUS_TRY_FAILURE;
		}
	}

	private void completeFailureInCommitting(int optcode) {
		if (optcode == TransactionListener.OPT_DEFAULT) {
			this.compensableStatus = CompensableTccTransaction.STATUS_CONFIRM_FAILURE;
		} else if (optcode == TransactionListener.OPT_HEURCOM) {
			this.compensableStatus = CompensableTccTransaction.STATUS_CONFIRMED;
		} else if (optcode == TransactionListener.OPT_HEURRB) {
			// ignore
		} else if (optcode == TransactionListener.OPT_HEURMIX) {
			this.compensableStatus = CompensableTccTransaction.STATUS_CONFIRM_FAILURE;
		}
	}

	private void completeFailureInRollingback(int optcode) {
		if (optcode == TransactionListener.OPT_DEFAULT) {
			this.compensableStatus = CompensableTccTransaction.STATUS_CANCEL_FAILURE;
		} else if (optcode == TransactionListener.OPT_HEURCOM) {
			this.compensableStatus = CompensableTccTransaction.STATUS_CANCELLED;
		} else if (optcode == TransactionListener.OPT_HEURRB) {
			// ignore
		} else if (optcode == TransactionListener.OPT_HEURMIX) {
			this.compensableStatus = CompensableTccTransaction.STATUS_CANCEL_FAILURE;
		}
	}

	public void rollbackStart() {
	}

	public void rollbackSuccess() {
		if (this.transactionStatus == Status.STATUS_PREPARING) {
			// ignore
		} else if (this.transactionStatus == Status.STATUS_COMMITTING) {
			// ignore
		} else if (this.transactionStatus == Status.STATUS_ROLLING_BACK) {
			// ignore
		}
	}

	public void rollbackFailure(int optcode) {
		if (this.transactionStatus == Status.STATUS_PREPARING) {
			this.completeFailureInPreparing(optcode);
		} else if (this.transactionStatus == Status.STATUS_COMMITTING) {
			this.completeFailureInCommitting(optcode);
		} else if (this.transactionStatus == Status.STATUS_ROLLING_BACK) {
			this.completeFailureInRollingback(optcode);
		}
	}

	public CompensableJtaTransaction getCompensableJtaTransaction() {
		return compensableJtaTransaction;
	}

	public void setCompensableJtaTransaction(CompensableJtaTransaction compensableJtaTransaction) {
		this.compensableJtaTransaction = compensableJtaTransaction;
	}

	public int getCompensableStatus() {
		return compensableStatus;
	}

	public void setCompensableStatus(int compensableStatus) {
		this.compensableStatus = compensableStatus;
	}

}
