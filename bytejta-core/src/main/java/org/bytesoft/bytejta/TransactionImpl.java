package org.bytesoft.bytejta;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.bytesoft.bytejta.common.TransactionConfigurator;
import org.bytesoft.bytejta.common.TransactionXid;
import org.bytesoft.bytejta.utils.ByteUtils;
import org.bytesoft.bytejta.utils.CommonUtils;
import org.bytesoft.bytejta.xa.XATerminatorImpl;
import org.bytesoft.transaction.CommitRequiredException;
import org.bytesoft.transaction.RemoteSystemException;
import org.bytesoft.transaction.RollbackRequiredException;
import org.bytesoft.transaction.SynchronizationImpl;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.TransactionTimer;
import org.bytesoft.transaction.archive.TransactionArchive;
import org.bytesoft.transaction.logger.TransactionLogger;
import org.bytesoft.transaction.xa.RemoteXAException;
import org.bytesoft.transaction.xa.XAResourceDescriptor;
import org.bytesoft.transaction.xa.XATerminator;

public class TransactionImpl implements Transaction {
	static final Logger logger = Logger.getLogger(TransactionImpl.class.getSimpleName());

	private transient Thread thread;
	private transient boolean timing = true;

	private transient XATerminator firstTerminator;
	private transient XATerminator lastTerminator;

	private int transactionStatus;
	private final TransactionContext transactionContext;
	private final XATerminatorImpl nativeTerminator;
	private final XATerminatorImpl remoteTerminator;

	private final List<SynchronizationImpl> synchronizations = new ArrayList<SynchronizationImpl>();

	public TransactionImpl(TransactionContext txContext) {
		this.transactionContext = txContext;
		this.nativeTerminator = new XATerminatorImpl(this.transactionContext);
		this.remoteTerminator = new XATerminatorImpl(this.transactionContext);
	}

	private void beforeCompletion() {
		int length = this.synchronizations.size();
		for (int i = 0; i < length; i++) {
			SynchronizationImpl synchronization = this.synchronizations.get(i);
			try {
				synchronization.beforeCompletion();
			} catch (RuntimeException rex) {
				// ignore
			}
		}// end-for
	}

	private void afterCompletion() {
		int length = this.synchronizations.size();
		// int status = this.transactionStatus.getTransactionStatus();
		for (int i = 0; i < length; i++) {
			SynchronizationImpl synchronization = this.synchronizations.get(i);
			try {
				synchronization.afterCompletion(this.transactionStatus);
			} catch (RuntimeException rex) {
				// ignore
			}
		}// end-for
	}

	private void delistAllResource() throws IllegalStateException, SystemException {
		boolean stateExpExists = false;
		boolean systemExpExists = false;
		try {
			this.nativeTerminator.delistAllResource();
		} catch (SystemException ex) {
			stateExpExists = true;
		} catch (RuntimeException ex) {
			systemExpExists = true;
		}

		try {
			this.remoteTerminator.delistAllResource();
		} catch (SystemException ex) {
			stateExpExists = true;
		} catch (RuntimeException ex) {
			systemExpExists = true;
		}

		if (stateExpExists) {
			throw new IllegalStateException();
		} else if (systemExpExists) {
			throw new SystemException();
		}

	}

	private synchronized void checkBeforeCommit() throws RollbackException, IllegalStateException,
			RollbackRequiredException, CommitRequiredException {

		if (this.transactionStatus == Status.STATUS_ROLLEDBACK) {
			throw new RollbackException();
		} else if (this.transactionStatus == Status.STATUS_ROLLING_BACK) {
			throw new RollbackRequiredException();
		} else if (this.transactionStatus == Status.STATUS_MARKED_ROLLBACK) {
			throw new RollbackRequiredException();
		} else if (this.transactionStatus == Status.STATUS_ACTIVE) {
			throw new CommitRequiredException();
		} else if (this.transactionStatus == Status.STATUS_COMMITTED) {
			// ignore
		} else {
			throw new IllegalStateException();
		}
	}

	private synchronized boolean analysisTerminator() {
		if (this.firstTerminator == null || this.lastTerminator == null) {

			int nativeResNum = this.nativeTerminator.getResourceArchives().size();
			int remoteResNum = this.remoteTerminator.getResourceArchives().size();
			boolean nativeValid = nativeResNum > 0;
			boolean remoteValid = remoteResNum > 0;

			if (nativeValid == false && remoteValid == false) {
				this.firstTerminator = this.nativeTerminator;
				this.lastTerminator = this.remoteTerminator;
			} else if (nativeValid == false) {
				this.firstTerminator = this.nativeTerminator;
				this.lastTerminator = this.remoteTerminator;
			} else if (remoteValid == false) {
				this.firstTerminator = this.remoteTerminator;
				this.lastTerminator = this.nativeTerminator;
			} else {
				boolean nativeSupportsXA = false;
				try {
					nativeSupportsXA = this.nativeTerminator.xaSupports();
				} catch (RemoteSystemException rsex) {
					// ignore
				}
				if (nativeSupportsXA) {
					this.firstTerminator = this.nativeTerminator;
					this.lastTerminator = this.remoteTerminator;
				} else {
					this.firstTerminator = this.remoteTerminator;
					this.lastTerminator = this.nativeTerminator;
				}
			}
			return (nativeResNum + remoteResNum) <= 1;
		} else {
			int firstResNum = this.firstTerminator.getResourceArchives().size();
			int lastResNum = this.lastTerminator.getResourceArchives().size();
			return (firstResNum + lastResNum) <= 1;
		}
	}

	public synchronized void recoveryCommit() throws RollbackException, HeuristicMixedException,
			HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {

		TransactionXid xid = this.transactionContext.getGlobalXid();

		this.transactionStatus = Status.STATUS_COMMITTING;// .setStatusCommiting();
		boolean nativeReadOnly = this.nativeTerminator.checkReadOnlyForRecovery();
		boolean remoteReadOnly = this.remoteTerminator.checkReadOnlyForRecovery();
		boolean committedExists = this.transactionContext.isOptimized();
		boolean rolledbackExists = false;
		boolean mixedExists = false;
		boolean errorExists = false;

		if (nativeReadOnly == false) {
			try {
				this.nativeTerminator.commit(xid, false);
				committedExists = true;
			} catch (XAException xaex) {
				switch (xaex.errorCode) {
				case XAException.XA_HEURHAZ:
				case XAException.XA_HEURMIX:
					mixedExists = true;
					break;
				case XAException.XA_HEURCOM:
					committedExists = true;
					break;
				case XAException.XAER_RMERR:
					errorExists = true;
					break;
				case XAException.XA_HEURRB:
					rolledbackExists = true;
					break;
				default:
					errorExists = true;
				}
			} catch (RuntimeException rex) {
				errorExists = true;
			}
		}

		if (remoteReadOnly) {
			try {
				this.remoteTerminator.commit(xid, false);
				committedExists = true;
			} catch (RemoteXAException xaex) {
				errorExists = true;
			} catch (XAException xaex) {
				switch (xaex.errorCode) {
				case XAException.XA_HEURHAZ:
				case XAException.XA_HEURMIX:
					mixedExists = true;
					break;
				case XAException.XA_HEURCOM:
					committedExists = true;
					break;
				case XAException.XAER_RMERR:
					errorExists = true;
					break;
				case XAException.XA_HEURRB:
					rolledbackExists = true;
					break;
				default:
					errorExists = true;
				}
			} catch (RuntimeException rex) {
				errorExists = true;
			}
		}

		int txStatus = Status.STATUS_COMMITTED;
		boolean transactionDone = true;
		try {
			if (mixedExists) {
				transactionDone = false;// TODO
				throw new HeuristicMixedException();
			} else if (committedExists && rolledbackExists) {
				transactionDone = false;// TODO
				throw new HeuristicMixedException();
			} else if (errorExists) {
				transactionDone = false;
				throw new SystemException();
			} else if (rolledbackExists) {
				txStatus = Status.STATUS_ROLLEDBACK;
				throw new HeuristicRollbackException();
			}
		} finally {
			if (transactionDone) {
				this.deleteRecoveryTransaction(txStatus);
			}
		}

	}

	private void deleteRecoveryTransaction(int txStatus) {
		TransactionXid xid = this.transactionContext.getGlobalXid();
		TransactionArchive archive = new TransactionArchive();
		archive.setOptimized(this.transactionContext.isOptimized());
		archive.setVote(XAResource.XA_OK);
		archive.setXid(xid);
		archive.setCompensable(this.transactionContext.isCompensable());
		archive.setCoordinator(this.transactionContext.isCoordinator());
		archive.getNativeResources().addAll(this.nativeTerminator.getResourceArchives());
		archive.getRemoteResources().addAll(this.remoteTerminator.getResourceArchives());

		this.transactionStatus = txStatus;
		archive.setStatus(this.transactionStatus);
		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		TransactionLogger transactionLogger = transactionConfigurator.getTransactionLogger();
		transactionLogger.deleteTransaction(archive);
	}

	public synchronized void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {

		try {
			this.checkBeforeCommit();
			return;
		} catch (RollbackRequiredException rrex) {
			this.rollback();
			throw new HeuristicRollbackException();
		} catch (CommitRequiredException crex) {
			// ignore
		}

		// stop-timing
		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		TransactionTimer transactionTimer = transactionConfigurator.getTransactionTimer();
		transactionTimer.stopTiming(this);

		// before-completion
		this.beforeCompletion();
		this.delistAllResource();

		// analysis
		boolean opcEnabled = false;
		try {
			opcEnabled = this.analysisTerminator();
		} catch (RuntimeException xaex) {
			this.rollback();
			throw new HeuristicRollbackException();
		}

		try {
			if (opcEnabled) {
				this.opcCommit();
			} else if (this.transactionContext.isOptimized()) {
				this.optimizeCommit();
			} else {
				this.regularCommit();
			}
		} finally {
			this.afterCompletion();
		}

	}

	private void opcCommit() throws SystemException, HeuristicRollbackException, HeuristicMixedException {
		TransactionXid xid = this.transactionContext.getGlobalXid();
		try {
			lastTerminator.commit(xid, true);
		} catch (RemoteXAException xaex) {
			SystemException ex = new SystemException();
			ex.initCause(xaex);
			throw ex;
		} catch (XAException xaex) {
			switch (xaex.errorCode) {
			case XAException.XA_HEURHAZ:
			case XAException.XA_HEURMIX:
				throw new HeuristicMixedException();
			case XAException.XA_HEURCOM:
				break;
			case XAException.XAER_RMERR:
				throw new SystemException();
			case XAException.XA_HEURRB:
			default:
				throw new HeuristicRollbackException();
			}
		} catch (RuntimeException rex) {
			SystemException ex = new SystemException();
			ex.initCause(rex);
			throw ex;
		}
	}

	private void regularCommit() throws SystemException, HeuristicRollbackException, HeuristicMixedException {
		TransactionXid xid = this.transactionContext.getGlobalXid();

		TransactionArchive archive = new TransactionArchive();
		archive.setOptimized(false);
		archive.setVote(-1);
		archive.setXid(this.transactionContext.getGlobalXid());
		archive.setCompensable(this.transactionContext.isCompensable());
		archive.setCoordinator(this.transactionContext.isCoordinator());
		archive.getNativeResources().addAll(this.nativeTerminator.getResourceArchives());
		archive.getRemoteResources().addAll(this.remoteTerminator.getResourceArchives());

		int firstVote = XAResource.XA_RDONLY;
		try {
			this.transactionStatus = Status.STATUS_PREPARING;// .setStatusPreparing();
			archive.setStatus(this.transactionStatus);
			TransactionConfigurator.getInstance().getTransactionLogger().createTransaction(archive);

			firstVote = this.firstTerminator.prepare(xid);
		} catch (XAException xaex) {
			this.rollback();
			throw new HeuristicRollbackException();
		} catch (RuntimeException xaex) {
			this.rollback();
			throw new HeuristicRollbackException();
		}

		int lastVote = XAResource.XA_RDONLY;
		try {
			lastVote = this.lastTerminator.prepare(xid);
		} catch (XAException xaex) {
			this.rollback();
			throw new HeuristicRollbackException();
		} catch (RuntimeException xaex) {
			this.rollback();
			throw new HeuristicRollbackException();
		}

		if (firstVote == XAResource.XA_OK || lastVote == XAResource.XA_OK) {
			this.transactionStatus = Status.STATUS_PREPARED;// .setStatusPrepared();
			archive.setVote(XAResource.XA_OK);
			archive.setStatus(this.transactionStatus);
			TransactionConfigurator.getInstance().getTransactionLogger().updateTransaction(archive);

			this.transactionStatus = Status.STATUS_COMMITTING;// .setStatusCommiting();

			if (firstVote == XAResource.XA_OK) {
				try {
					firstTerminator.commit(xid, false);
				} catch (RemoteXAException xaex) {
					this.rollback();
					throw new HeuristicRollbackException();
				} catch (XAException xaex) {
					this.rollback();
					throw new HeuristicRollbackException();
				} catch (RuntimeException rex) {
					this.rollback();
					throw new HeuristicRollbackException();
				}
			}

			if (lastVote == XAResource.XA_OK) {
				try {
					lastTerminator.commit(xid, false);
				} catch (RemoteXAException xaex) {
					SystemException ex = new SystemException();
					ex.initCause(xaex);
					throw ex;
				} catch (XAException xaex) {
					switch (xaex.errorCode) {
					case XAException.XA_HEURHAZ:
					case XAException.XA_HEURMIX:
						throw new HeuristicMixedException();
					case XAException.XA_HEURCOM:
						break;
					case XAException.XAER_RMERR:
						if (firstVote == XAResource.XA_RDONLY) {
							this.rollback();
							throw new HeuristicRollbackException();
						} else {
							throw new SystemException();
						}
					case XAException.XA_HEURRB:
					default:
						if (firstVote == XAResource.XA_RDONLY) {
							throw new HeuristicRollbackException();
						} else {
							throw new HeuristicMixedException();
						}
					}
				} catch (RuntimeException rex) {
					SystemException ex = new SystemException();
					ex.initCause(rex);
					throw ex;
				}
			}

			this.transactionStatus = Status.STATUS_COMMITTED;// .setStatusCommitted();
			archive.setStatus(this.transactionStatus);
			TransactionConfigurator.getInstance().getTransactionLogger().deleteTransaction(archive);
		} else {
			this.transactionStatus = Status.STATUS_PREPARED;// .setStatusPrepared();
			archive.setVote(XAResource.XA_RDONLY);
			archive.setStatus(this.transactionStatus);
			TransactionConfigurator.getInstance().getTransactionLogger().deleteTransaction(archive);
		}

	}

	private void optimizeCommit() throws SystemException, HeuristicRollbackException, HeuristicMixedException {
		TransactionXid xid = this.transactionContext.getGlobalXid();

		TransactionArchive archive = new TransactionArchive();
		archive.setOptimized(true);
		archive.setVote(-1);
		archive.setXid(this.transactionContext.getGlobalXid());
		archive.setCompensable(this.transactionContext.isCompensable());
		archive.setCoordinator(this.transactionContext.isCoordinator());
		archive.getNativeResources().addAll(this.nativeTerminator.getResourceArchives());
		archive.getRemoteResources().addAll(this.remoteTerminator.getResourceArchives());

		try {
			this.transactionStatus = Status.STATUS_PREPARING;
			archive.setStatus(this.transactionStatus);
			TransactionConfigurator.getInstance().getTransactionLogger().createTransaction(archive);

			this.firstTerminator.prepare(xid);
		} catch (XAException xaex) {
			this.rollback();
			throw new HeuristicRollbackException();
		} catch (RuntimeException xaex) {
			this.rollback();
			throw new HeuristicRollbackException();
		}

		try {
			lastTerminator.commit(xid, true);
		} catch (RemoteXAException xaex) {
			this.rollback();
			throw new HeuristicRollbackException();
		} catch (XAException xaex) {
			this.rollback();
			throw new HeuristicRollbackException();
		} catch (RuntimeException rex) {
			this.rollback();
			throw new HeuristicRollbackException();
		}

		this.transactionStatus = Status.STATUS_COMMITTING;
		archive.setStatus(this.transactionStatus);
		archive.setVote(XAResource.XA_OK);
		TransactionConfigurator.getInstance().getTransactionLogger().updateTransaction(archive);

		try {
			firstTerminator.commit(xid, false);
		} catch (RemoteXAException xaex) {
			SystemException ex = new SystemException();
			ex.initCause(xaex);
			throw ex;
		} catch (XAException xaex) {
			switch (xaex.errorCode) {
			case XAException.XA_HEURHAZ:
			case XAException.XA_HEURMIX:
				throw new HeuristicMixedException();
			case XAException.XA_HEURCOM:
				break;
			case XAException.XAER_RMERR:
				throw new SystemException();
			case XAException.XA_HEURRB:
			default:
				throw new HeuristicMixedException();
			}
		} catch (RuntimeException rex) {
			SystemException ex = new SystemException();
			ex.initCause(rex);
			throw ex;
		}

		this.transactionStatus = Status.STATUS_COMMITTED;
		archive.setStatus(this.transactionStatus);
		TransactionConfigurator.getInstance().getTransactionLogger().deleteTransaction(archive);

	}

	public synchronized boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException,
			SystemException {
		if (this.getStatus() != Status.STATUS_ACTIVE && this.getStatus() != Status.STATUS_MARKED_ROLLBACK) {
			throw new IllegalStateException();
		}

		XAResourceDescriptor descriptor = null;
		if (XAResourceDescriptor.class.isInstance(xaRes)) {
			descriptor = (XAResourceDescriptor) xaRes;
		} else {
			descriptor = this.recognizeResource(xaRes);
		}

		if (descriptor.isRemote()) {
			return this.remoteTerminator.delistResource(descriptor, flag);
		} else {
			return this.nativeTerminator.delistResource(descriptor, flag);
		}

	}

	private XAResourceDescriptor recognizeResource(XAResource xaRes) {
		XAResourceDescriptor descriptor;
		descriptor = new XAResourceDescriptor();
		descriptor.setDelegate(xaRes);
		descriptor.setRemote(false);
		descriptor.setSupportsXA(false);
		return descriptor;
	}

	public synchronized boolean enlistResource(XAResource xaRes) throws RollbackException, IllegalStateException,
			SystemException {

		if (this.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
			throw new RollbackException();
		} else if (this.getStatus() == Status.STATUS_ACTIVE) {
			XAResourceDescriptor descriptor = null;
			if (XAResourceDescriptor.class.isInstance(xaRes)) {
				descriptor = (XAResourceDescriptor) xaRes;
			} else {
				descriptor = this.recognizeResource(xaRes);
			}

			if (descriptor.isSupportsXA()) {
				if (descriptor.isRemote()) {
					return this.remoteTerminator.enlistResource(descriptor);
				} else {
					return this.nativeTerminator.enlistResource(descriptor);
				}
			} else {
				if (descriptor.isRemote()) {
					boolean nativeSupportsXA = this.nativeTerminator.xaSupports();
					if (nativeSupportsXA) {
						return this.remoteTerminator.enlistResource(descriptor);
					} else {
						throw new SystemException("There already has a non-xa resource exists.");
					}
				} else {
					boolean remoteSupportsXA = this.remoteTerminator.xaSupports();
					if (remoteSupportsXA) {
						return this.nativeTerminator.enlistResource(descriptor);
					} else {
						throw new SystemException("There already has a non-xa resource exists.");
					}
				}
			}
		} else {
			throw new IllegalStateException();
		}

	}

	public int getStatus() /* throws SystemException */{
		return this.transactionStatus;
	}

	public synchronized void registerSynchronization(Synchronization sync) throws RollbackException,
			IllegalStateException, SystemException {

		if (this.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
			throw new RollbackException();
		} else if (this.getStatus() == Status.STATUS_ACTIVE) {
			SynchronizationImpl synchronization = new SynchronizationImpl(sync);
			this.synchronizations.add(synchronization);
			logger.info(String.format(
					"[%s] register-sync: sync= %s"//
					, ByteUtils.byteArrayToString(this.transactionContext.getCurrentXid().getGlobalTransactionId()),
					sync));
		} else {
			throw new IllegalStateException();
		}

	}

	private void checkBeforeRollback() throws IllegalStateException, RollbackRequiredException {

		if (this.transactionStatus == Status.STATUS_MARKED_ROLLBACK) {
			throw new RollbackRequiredException();
		} else if (this.transactionStatus == Status.STATUS_ACTIVE) {
			throw new RollbackRequiredException();
		} else if (this.transactionStatus == Status.STATUS_ROLLEDBACK) {
			// ignore
		} else if (this.transactionStatus == Status.STATUS_COMMITTED) {
			throw new IllegalStateException();
		} else {
			throw new RollbackRequiredException();
		}

	}

	public synchronized void rollback() throws IllegalStateException, SystemException {

		try {
			this.checkBeforeRollback();
			return;
		} catch (RollbackRequiredException rrex) {
			// ignore
		}

		// stop-timing
		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		TransactionTimer transactionTimer = transactionConfigurator.getTransactionTimer();
		transactionTimer.stopTiming(this);

		// before-completion
		this.beforeCompletion();

		// rollback the first-resource
		TransactionXid xid = this.transactionContext.getGlobalXid();
		SystemException systemErr = null;
		RuntimeException runtimeErr = null;
		try {
			firstTerminator.rollback(xid);
		} catch (RemoteXAException xaex) {
			RemoteSystemException rsex = new RemoteSystemException();
			rsex.initCause(xaex);
			systemErr = rsex;
		} catch (XAException xaex) {
			switch (xaex.errorCode) {
			case XAException.XA_HEURRB:
				// ignore
				break;
			case XAException.XA_HEURMIX:
			case XAException.XA_HEURHAZ:
			case XAException.XA_HEURCOM:
			case XAException.XAER_RMERR:
			default:
				SystemException rsex = new SystemException();
				rsex.initCause(xaex);
				systemErr = rsex;
				break;
			}
		} catch (RuntimeException rex) {
			runtimeErr = rex;
		}

		// rollback the last-resource
		try {
			lastTerminator.rollback(xid);
		} catch (RemoteXAException xaex) {
			RemoteSystemException rsex = new RemoteSystemException();
			rsex.initCause(xaex);
			throw rsex;
		} catch (XAException xaex) {
			switch (xaex.errorCode) {
			case XAException.XA_HEURRB:
				// ignore
				break;
			case XAException.XA_HEURMIX:
			case XAException.XA_HEURHAZ:
			case XAException.XA_HEURCOM:
			case XAException.XAER_RMERR:
			default:
				SystemException rsex = new SystemException();
				rsex.initCause(xaex);
				throw rsex;
			}
		} catch (RuntimeException rex) {
			throw rex;
		} finally {
			this.afterCompletion();
		}

		if (systemErr != null) {
			throw systemErr;
		} else if (runtimeErr != null) {
			throw runtimeErr;
		}

	}

	public synchronized void setRollbackOnly() throws IllegalStateException, SystemException {
		if (this.transactionStatus == Status.STATUS_ACTIVE || this.transactionStatus == Status.STATUS_MARKED_ROLLBACK) {
			this.transactionStatus = Status.STATUS_MARKED_ROLLBACK;
		} else {
			throw new IllegalStateException();
		}
	}

	public int hashCode() {
		TransactionXid transactionXid = this.transactionContext == null ? null : this.transactionContext.getGlobalXid();
		int hash = transactionXid == null ? 0 : transactionXid.hashCode();
		return hash;
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		} else if (TransactionImpl.class.equals(obj.getClass()) == false) {
			return false;
		}
		TransactionImpl that = (TransactionImpl) obj;
		TransactionContext thisContext = this.transactionContext;
		TransactionContext thatContext = that.transactionContext;
		TransactionXid thisXid = thisContext == null ? null : thisContext.getGlobalXid();
		TransactionXid thatXid = thatContext == null ? null : thatContext.getGlobalXid();
		return CommonUtils.equals(thisXid, thatXid);
	}

	public TransactionContext getTransactionContext() {
		return transactionContext;
	}

	public XATerminatorImpl getNativeTerminator() {
		return nativeTerminator;
	}

	public XATerminatorImpl getRemoteTerminator() {
		return remoteTerminator;
	}

	public boolean isTiming() {
		return timing;
	}

	public void setTiming(boolean timing) {
		this.timing = timing;
	}

	public Thread getThread() {
		return thread;
	}

	public void setThread(Thread thread) {
		this.thread = thread;
	}

	public int getTransactionStatus() {
		return transactionStatus;
	}

	public void setTransactionStatus(int transactionStatus) {
		this.transactionStatus = transactionStatus;
	}

}