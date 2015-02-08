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

import org.apache.log4j.Logger;
import org.bytesoft.bytetcc.archive.CompensableArchive;
import org.bytesoft.bytetcc.archive.CompensableTransactionArchive;
import org.bytesoft.bytetcc.common.TransactionConfigurator;
import org.bytesoft.bytetcc.supports.CompensableTransactionLogger;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.TransactionListener;
import org.bytesoft.transaction.archive.XAResourceArchive;
import org.bytesoft.transaction.xa.TransactionXid;
import org.bytesoft.transaction.xa.XAResourceDescriptor;
import org.bytesoft.transaction.xa.XidFactory;

public class CompensableTccTransaction extends CompensableTransaction {
	static final Logger logger = Logger.getLogger(CompensableTccTransaction.class.getSimpleName());

	public static int STATUS_UNKNOWN = 0;

	public static int STATUS_TRY_FAILURE = 1;
	public static int STATUS_TRIED = 2;
	public static int STATUS_TRY_MIXED = 3;

	public static int STATUS_CONFIRMING = 4;
	public static int STATUS_CONFIRM_FAILURE = 5;
	public static int STATUS_CONFIRMED = 6;

	public static int STATUS_CANCELLING = 7;
	public static int STATUS_CANCEL_FAILURE = 8;
	public static int STATUS_CANCELLED = 9;

	private int transactionStatus;
	private int compensableStatus;
	private final List<CompensableArchive> coordinatorArchives = new ArrayList<CompensableArchive>();
	private final List<CompensableArchive> participantArchives = new ArrayList<CompensableArchive>();
	private final Map<Xid, XAResourceArchive> resourceArchives = new ConcurrentHashMap<Xid, XAResourceArchive>();
	private ThreadLocal<TransactionContext> transientContexts = new ThreadLocal<TransactionContext>();

	private transient CompensableArchive confirmArchive;
	private transient CompensableArchive cancellArchive;

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

	public synchronized void nativeConfirm() {
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		CompensableInvocationExecutor executor = configurator.getCompensableInvocationExecutor();
		this.compensableStatus = CompensableTccTransaction.STATUS_CONFIRMING;

		if (this.transactionContext.isCoordinator()) {
			Iterator<CompensableArchive> coordinatorItr = this.coordinatorArchives.iterator();
			while (coordinatorItr.hasNext()) {
				CompensableArchive archive = coordinatorItr.next();
				try {
					this.confirmArchive = archive;
					this.confirmArchive.setTxEnabled(false);
					executor.confirm(this.confirmArchive.getCompensable());
					if (this.confirmArchive.isTxEnabled() == false) {
						this.confirmArchive.setConfirmed(true);
					}
				} finally {
					this.confirmArchive.setTxEnabled(false);
					this.confirmArchive = null;
				}
			}
		}

		Iterator<CompensableArchive> participantItr = this.participantArchives.iterator();
		while (participantItr.hasNext()) {
			CompensableArchive archive = participantItr.next();
			this.compensableStatus = CompensableTccTransaction.STATUS_CONFIRMING;
			try {
				this.confirmArchive = archive;
				this.confirmArchive.setTxEnabled(false);
				executor.confirm(this.confirmArchive.getCompensable());
				if (this.confirmArchive.isTxEnabled() == false) {
					this.confirmArchive.setConfirmed(true);
				}
			} finally {
				this.confirmArchive.setTxEnabled(false);
				this.confirmArchive = null;
			}
		}
		this.compensableStatus = CompensableTccTransaction.STATUS_CONFIRMED;
		CompensableTransactionArchive archive = this.getTransactionArchive();
		CompensableTransactionLogger transactionLogger = configurator.getTransactionLogger();
		transactionLogger.updateTransaction(archive);
	}

	public synchronized void remoteConfirm() throws SystemException, RemoteException {

		TransactionXid globalXid = this.transactionContext.getGlobalXid();
		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		CompensableTransactionLogger transactionLogger = transactionConfigurator.getTransactionLogger();

		// boolean confirmExists = false;
		boolean cancellExists = false;
		boolean errorExists = false;
		Set<Entry<Xid, XAResourceArchive>> entrySet = this.resourceArchives.entrySet();
		Iterator<Map.Entry<Xid, XAResourceArchive>> resourceItr = entrySet.iterator();
		while (resourceItr.hasNext()) {
			Map.Entry<Xid, XAResourceArchive> entry = resourceItr.next();
			Xid key = entry.getKey();
			XAResourceArchive archive = entry.getValue();
			if (archive.isCompleted()) {
				if (archive.isCommitted()) {
					// confirmExists = true;
				} else if (archive.isRolledback()) {
					cancellExists = true;
				} else {
					// ignore
				}
			} else {
				try {
					archive.commit(key, true);
					archive.setCommitted(true);
					archive.setCompleted(true);
					// confirmExists = true;
				} catch (XAException xaex) {
					switch (xaex.errorCode) {
					case XAException.XA_HEURCOM:
						archive.setCommitted(true);
						archive.setCompleted(true);
						// confirmExists = true;
						break;
					case XAException.XA_HEURRB:
						archive.setRolledback(true);
						archive.setCompleted(true);
						cancellExists = true;
						break;
					case XAException.XA_HEURMIX:
					case XAException.XAER_NOTA:
					case XAException.XAER_RMFAIL:
					case XAException.XAER_RMERR:
					default:
						errorExists = true;
					}
				}

				transactionLogger.updateResource(globalXid, archive);
			}

		} // end-while (resourceItr.hasNext())

		if (errorExists) {
			throw new SystemException();
		} else if (cancellExists) {
			throw new SystemException();
		}

	}

	public synchronized boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException, SystemException {
		if (XAResourceDescriptor.class.isInstance(xaRes)) {
			XAResourceDescriptor descriptor = (XAResourceDescriptor) xaRes;
			if (descriptor.isRemote()) {
				Set<Entry<Xid, XAResourceArchive>> entrySet = this.resourceArchives.entrySet();
				Iterator<Map.Entry<Xid, XAResourceArchive>> itr = entrySet.iterator();
				while (itr.hasNext()) {
					Map.Entry<Xid, XAResourceArchive> entry = itr.next();
					XAResourceArchive archive = entry.getValue();
					XAResourceDescriptor resource = archive.getDescriptor();
					if (resource.equals(descriptor)) {
						return true;
					}
				}

				Iterator<Map.Entry<Xid, XAResourceArchive>> iterator = entrySet.iterator();
				while (iterator.hasNext()) {
					Map.Entry<Xid, XAResourceArchive> entry = iterator.next();
					XAResourceArchive archive = entry.getValue();
					XAResourceDescriptor resource = archive.getDescriptor();
					boolean isSameRM = false;
					try {
						isSameRM = resource.isSameRM(descriptor);
					} catch (XAException ex) {
						continue;
					}
					if (isSameRM) {
						return true;
					}
				}

				TransactionConfigurator configurator = TransactionConfigurator.getInstance();
				XidFactory xidFactory = configurator.getXidFactory();
				TransactionXid globalXid = this.transactionContext.getGlobalXid();
				TransactionXid branchXid = xidFactory.createBranchXid(globalXid);
				XAResourceArchive archive = new XAResourceArchive();
				archive.setDescriptor(descriptor);
				archive.setXid(branchXid);
				this.resourceArchives.put(branchXid, archive);

				return true;
			} else {
				return this.jtaTransaction.delistResource(xaRes, flag);
			}
		} else {
			return this.jtaTransaction.delistResource(xaRes, flag);
		}
	}

	public synchronized boolean enlistResource(XAResource xaRes) throws RollbackException, IllegalStateException,
			SystemException {
		if (XAResourceDescriptor.class.isInstance(xaRes)) {
			XAResourceDescriptor descriptor = (XAResourceDescriptor) xaRes;
			if (descriptor.isRemote()) {
				Set<Entry<Xid, XAResourceArchive>> entrySet = this.resourceArchives.entrySet();
				Iterator<Map.Entry<Xid, XAResourceArchive>> itr = entrySet.iterator();
				while (itr.hasNext()) {
					Map.Entry<Xid, XAResourceArchive> entry = itr.next();
					XAResourceArchive archive = entry.getValue();
					XAResourceDescriptor resource = archive.getDescriptor();
					if (resource.equals(descriptor)) {
						return true;
					}
				}

				Iterator<Map.Entry<Xid, XAResourceArchive>> iterator = entrySet.iterator();
				while (iterator.hasNext()) {
					Map.Entry<Xid, XAResourceArchive> entry = iterator.next();
					XAResourceArchive archive = entry.getValue();
					XAResourceDescriptor resource = archive.getDescriptor();
					boolean isSameRM = false;
					try {
						isSameRM = resource.isSameRM(descriptor);
					} catch (XAException ex) {
						continue;
					}
					if (isSameRM) {
						return true;
					}
				}

				TransactionConfigurator configurator = TransactionConfigurator.getInstance();
				XidFactory xidFactory = configurator.getXidFactory();
				TransactionXid globalXid = this.transactionContext.getGlobalXid();
				TransactionXid branchXid = xidFactory.createBranchXid(globalXid);
				XAResourceArchive archive = new XAResourceArchive();
				archive.setDescriptor(descriptor);
				archive.setXid(branchXid);
				this.resourceArchives.put(branchXid, archive);

				return true;
			} else {
				return this.jtaTransaction.enlistResource(xaRes);
			}
		} else {
			return this.jtaTransaction.enlistResource(xaRes);
		}
	}

	public int getStatus() /* throws SystemException */{
		return this.transactionStatus;
	}

	public synchronized void setTransactionStatus(int transactionStatus) {
		this.transactionStatus = transactionStatus;
	}

	public synchronized void registerSynchronization(Synchronization sync) throws RollbackException, IllegalStateException,
			SystemException {
		this.jtaTransaction.registerSynchronization(sync);
	}

	public synchronized void rollback() throws IllegalStateException, SystemException {
		throw new IllegalStateException();
	}

	public synchronized void nativeCancel() {
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		CompensableInvocationExecutor executor = configurator.getCompensableInvocationExecutor();
		this.compensableStatus = CompensableTccTransaction.STATUS_CANCELLING;
		Iterator<CompensableArchive> participantItr = this.participantArchives.iterator();
		while (participantItr.hasNext()) {
			CompensableArchive archive = participantItr.next();
			try {
				this.cancellArchive = archive;
				this.cancellArchive.setTxEnabled(false);
				executor.cancel(this.cancellArchive.getCompensable());
				if (this.cancellArchive.isTxEnabled() == false) {
					this.cancellArchive.setCancelled(true);
				}
			} finally {
				this.cancellArchive.setTxEnabled(false);
				this.cancellArchive = null;
			}
		}
		this.compensableStatus = CompensableTccTransaction.STATUS_CANCELLED;
		CompensableTransactionArchive archive = this.getTransactionArchive();
		CompensableTransactionLogger transactionLogger = configurator.getTransactionLogger();
		transactionLogger.updateTransaction(archive);
	}

	public synchronized void remoteCancel() throws SystemException, RemoteException {

		TransactionXid globalXid = this.transactionContext.getGlobalXid();
		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		CompensableTransactionLogger transactionLogger = transactionConfigurator.getTransactionLogger();

		boolean confirmExists = false;
		// boolean cancellExists = false;
		boolean errorExists = false;
		Set<Entry<Xid, XAResourceArchive>> entrySet = this.resourceArchives.entrySet();
		Iterator<Map.Entry<Xid, XAResourceArchive>> resourceItr = entrySet.iterator();
		while (resourceItr.hasNext()) {
			Map.Entry<Xid, XAResourceArchive> entry = resourceItr.next();
			Xid key = entry.getKey();
			XAResourceArchive archive = entry.getValue();
			if (archive.isCompleted()) {
				if (archive.isCommitted()) {
					confirmExists = true;
				} else if (archive.isRolledback()) {
					// cancellExists = true;
				} else {
					// ignore
				}
			} else {
				try {
					archive.rollback(key);
					archive.setRolledback(true);
					archive.setCompleted(true);
					// cancellExists = true;
				} catch (XAException xaex) {
					switch (xaex.errorCode) {
					case XAException.XA_HEURCOM:
						archive.setCommitted(true);
						archive.setCompleted(true);
						confirmExists = true;
						break;
					case XAException.XA_HEURRB:
						archive.setRolledback(true);
						archive.setCompleted(true);
						// cancellExists = true;
						break;
					case XAException.XA_HEURMIX:
					case XAException.XAER_NOTA:
					case XAException.XAER_RMFAIL:
					case XAException.XAER_RMERR:
					default:
						errorExists = true;
					}
				}

				transactionLogger.updateResource(globalXid, archive);
			}

		} // end-while (resourceItr.hasNext())

		if (errorExists) {
			throw new SystemException();
		} else if (confirmExists) {
			throw new SystemException();
		}

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

	private void markCompensableArchiveAsTxEnabledIfNeccessary() {
		if (this.transactionStatus == Status.STATUS_COMMITTING) {
			if (this.confirmArchive != null) {
				this.confirmArchive.setTxEnabled(true);
			}
		} else if (this.transactionStatus == Status.STATUS_ROLLING_BACK) {
			if (this.cancellArchive != null) {
				this.cancellArchive.setTxEnabled(true);
			}
		}
	}

	private void trySuccess() {
		this.compensableStatus = CompensableTccTransaction.STATUS_TRIED;
		CompensableTransactionArchive archive = this.getTransactionArchive();
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		CompensableTransactionLogger transactionLogger = configurator.getTransactionLogger();
		transactionLogger.updateTransaction(archive);
	}

	private void tryFailure() {
		this.compensableStatus = CompensableTccTransaction.STATUS_TRY_FAILURE;
		CompensableTransactionArchive archive = this.getTransactionArchive();
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		CompensableTransactionLogger transactionLogger = configurator.getTransactionLogger();
		transactionLogger.updateTransaction(archive);
	}

	private void tryMixed() {
		this.compensableStatus = CompensableTccTransaction.STATUS_TRY_MIXED;
		CompensableTransactionArchive archive = this.getTransactionArchive();
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		CompensableTransactionLogger transactionLogger = configurator.getTransactionLogger();
		transactionLogger.updateTransaction(archive);
	}

	public void prepareStart() {
		this.markCompensableArchiveAsTxEnabledIfNeccessary();
	}

	public void prepareComplete(boolean success) {
		this.markCompensableArchiveAsTxEnabledIfNeccessary();
	}

	public void commitStart() {
		this.markCompensableArchiveAsTxEnabledIfNeccessary();
	}

	public void commitSuccess() {
		this.markCompensableArchiveAsTxEnabledIfNeccessary();

		if (this.transactionStatus == Status.STATUS_PREPARING) {
			this.trySuccess();
		} else if (this.transactionStatus == Status.STATUS_COMMITTING) {
			if (this.confirmArchive != null) {
				this.confirmArchive.setConfirmed(true);
			}
		} else if (this.transactionStatus == Status.STATUS_ROLLING_BACK) {
			if (this.cancellArchive != null) {
				this.cancellArchive.setCancelled(true);
			}
		}
	}

	public void commitFailure(int optcode) {
		this.markCompensableArchiveAsTxEnabledIfNeccessary();

		if (this.transactionStatus == Status.STATUS_PREPARING) {
			this.completeFailureInPreparing(optcode);
		} else if (this.transactionStatus == Status.STATUS_COMMITTING) {
			this.completeFailureInCommitting(optcode);
		} else if (this.transactionStatus == Status.STATUS_ROLLING_BACK) {
			this.completeFailureInRollingback(optcode);
		}
	}

	private void completeFailureInPreparing(int optcode) {
		if (optcode == TransactionListener.OPT_HEURCOM) {
			this.trySuccess();
		} else if (optcode == TransactionListener.OPT_HEURRB) {
			this.tryFailure();
		} else if (optcode == TransactionListener.OPT_HEURMIX) {
			this.tryMixed();
		} else {
			this.tryFailure();
		}
	}

	private void completeFailureInCommitting(int optcode) {
		if (this.confirmArchive != null) {
			if (optcode == TransactionListener.OPT_DEFAULT) {
				// ignore
			} else if (optcode == TransactionListener.OPT_HEURCOM) {
				this.confirmArchive.setConfirmed(true);
			} else if (optcode == TransactionListener.OPT_HEURRB) {
				// ignore
			} else if (optcode == TransactionListener.OPT_HEURMIX) {
				this.confirmArchive.setTxMixed(true);
			}
		}
	}

	private void completeFailureInRollingback(int optcode) {
		if (this.cancellArchive != null) {
			if (optcode == TransactionListener.OPT_DEFAULT) {
				// ignore
			} else if (optcode == TransactionListener.OPT_HEURCOM) {
				this.cancellArchive.setConfirmed(true);
			} else if (optcode == TransactionListener.OPT_HEURRB) {
				// ignore
			} else if (optcode == TransactionListener.OPT_HEURMIX) {
				this.cancellArchive.setTxMixed(true);
			}
		}
	}

	public void rollbackStart() {
		this.markCompensableArchiveAsTxEnabledIfNeccessary();
	}

	public void rollbackSuccess() {
		this.markCompensableArchiveAsTxEnabledIfNeccessary();

		if (this.transactionStatus == Status.STATUS_PREPARING) {
			this.tryFailure();
		} else if (this.transactionStatus == Status.STATUS_COMMITTING) {
			// ignore
		} else if (this.transactionStatus == Status.STATUS_ROLLING_BACK) {
			// ignore
		}
	}

	public void rollbackFailure(int optcode) {
		this.markCompensableArchiveAsTxEnabledIfNeccessary();

		if (this.transactionStatus == Status.STATUS_PREPARING) {
			this.completeFailureInPreparing(optcode);
		} else if (this.transactionStatus == Status.STATUS_COMMITTING) {
			this.completeFailureInCommitting(optcode);
		} else if (this.transactionStatus == Status.STATUS_ROLLING_BACK) {
			this.completeFailureInRollingback(optcode);
		}
	}

	public int getCompensableStatus() {
		return compensableStatus;
	}

	public void setCompensableStatus(int compensableStatus) {
		this.compensableStatus = compensableStatus;
	}

}
