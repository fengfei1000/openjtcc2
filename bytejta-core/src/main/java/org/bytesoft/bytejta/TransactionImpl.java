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
import org.bytesoft.transaction.xa.TransactionXid;
import org.bytesoft.transaction.xa.XAInternalException;
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

	private boolean currentOpcNecessary() {
		int nativeResNum = this.nativeTerminator.getResourceArchives().size();
		int remoteResNum = this.remoteTerminator.getResourceArchives().size();
		return (nativeResNum + remoteResNum) <= 1;
	}

	private synchronized void analysisTerminator() {

		if (this.transactionContext.isOptimized() == false) {

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
			this.transactionContext.setOptimized(true);

		}

	}

	public synchronized void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, CommitRequiredException, SystemException {

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

		// delist all resources
		try {
			this.delistAllResource();
		} catch (RollbackRequiredException rrex) {
			this.rollback();
			throw new HeuristicRollbackException();
		} catch (SystemException ex) {
			this.rollback();
			throw new HeuristicRollbackException();
		} catch (RuntimeException rex) {
			this.rollback();
			throw new HeuristicRollbackException();
		}

		try {
			if (this.currentOpcNecessary()) {
				this.opcCommit();
			} else if (transactionConfigurator.isOptimizeEnabled()) {
				// analysis
				this.analysisTerminator();
				this.optimizeCommit();
			} else {
				this.regularCommit();
			}
		} finally {
			this.afterCompletion();
		}

	}

	public synchronized void opcCommit() throws HeuristicRollbackException, HeuristicMixedException,
			CommitRequiredException, SystemException {
		TransactionXid xid = this.transactionContext.getGlobalXid();
		try {
			lastTerminator.commit(xid, true);
		} catch (XAInternalException xaex) {
			CommitRequiredException ex = new CommitRequiredException();
			ex.initCause(xaex);
			throw ex;
		} catch (XAException xaex) {
			switch (xaex.errorCode) {
			case XAException.XA_HEURMIX:
				throw new HeuristicMixedException();
			case XAException.XA_HEURCOM:
				return;
			case XAException.XA_HEURRB:
				throw new HeuristicRollbackException();
			default:
				logger.warning("Unknown state in committing transaction phase.");
				SystemException ex = new SystemException();
				ex.initCause(xaex);
				throw ex;
			}
		}
	}

	public synchronized void regularCommit() throws HeuristicRollbackException, HeuristicMixedException,
			CommitRequiredException, SystemException {
		TransactionXid xid = this.transactionContext.getGlobalXid();

		TransactionArchive archive = this.getTransactionArchive();// new TransactionArchive();

		int firstVote = XAResource.XA_RDONLY;
		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		TransactionLogger transactionLogger = transactionConfigurator.getTransactionLogger();
		try {
			this.transactionStatus = Status.STATUS_PREPARING;// .setStatusPreparing();
			archive.setStatus(this.transactionStatus);
			transactionLogger.createTransaction(archive);

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
			this.transactionContext.setPrepareVote(XAResource.XA_OK);
			archive.setVote(XAResource.XA_OK);
			archive.setStatus(this.transactionStatus);
			transactionLogger.updateTransaction(archive);

			this.transactionStatus = Status.STATUS_COMMITTING;// .setStatusCommiting();
			boolean mixedExists = false;
			boolean unFinishExists = false;
			try {
				firstTerminator.commit(xid, false);
			} catch (XAException xaex) {
				unFinishExists = XAInternalException.class.isInstance(xaex);

				switch (xaex.errorCode) {
				case XAException.XA_HEURCOM:
					break;
				case XAException.XA_HEURMIX:
					mixedExists = true;
					break;
				case XAException.XA_HEURRB:
					this.rollback();
					throw new HeuristicRollbackException();
				default:
					logger.warning("Unknown state in committing transaction phase.");
				}
			}

			boolean transactionCompleted = false;
			try {
				lastTerminator.commit(xid, false);
				if (unFinishExists == false) {
					transactionCompleted = true;
				}
			} catch (XAInternalException xaex) {
				CommitRequiredException ex = new CommitRequiredException();
				ex.initCause(xaex);
				throw ex;
			} catch (XAException xaex) {
				if (unFinishExists) {
					CommitRequiredException ex = new CommitRequiredException();
					ex.initCause(xaex);
					throw ex;
				} else if (mixedExists) {
					transactionCompleted = true;
					throw new HeuristicMixedException();
				} else {
					transactionCompleted = true;

					switch (xaex.errorCode) {
					case XAException.XA_HEURMIX:
						throw new HeuristicMixedException();
					case XAException.XA_HEURCOM:
						break;
					case XAException.XA_HEURRB:
						if (firstVote == XAResource.XA_RDONLY) {
							throw new HeuristicRollbackException();
						} else {
							throw new HeuristicMixedException();
						}
					default:
						logger.warning("Unknown state in committing transaction phase.");
					}
				}
			} finally {
				if (transactionCompleted) {
					this.transactionStatus = Status.STATUS_COMMITTED;// .setStatusCommitted();
					archive.setStatus(this.transactionStatus);
					transactionLogger.deleteTransaction(archive);
				}
			}
		} else {
			this.transactionStatus = Status.STATUS_PREPARED;// .setStatusPrepared();
			this.transactionContext.setPrepareVote(XAResource.XA_RDONLY);
			archive.setVote(XAResource.XA_RDONLY);
			archive.setStatus(this.transactionStatus);
			transactionLogger.deleteTransaction(archive);
		}

	}

	public synchronized void optimizeCommit() throws HeuristicRollbackException, HeuristicMixedException,
			CommitRequiredException, SystemException {
		TransactionXid xid = this.transactionContext.getGlobalXid();

		TransactionArchive archive = this.getTransactionArchive();// new TransactionArchive();

		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		TransactionLogger transactionLogger = transactionConfigurator.getTransactionLogger();
		try {
			this.transactionStatus = Status.STATUS_PREPARING;
			archive.setStatus(this.transactionStatus);
			transactionLogger.createTransaction(archive);

			this.firstTerminator.prepare(xid);
		} catch (XAException xaex) {
			this.rollback();
			throw new HeuristicRollbackException();
		} catch (RuntimeException xaex) {
			this.rollback();
			throw new HeuristicRollbackException();
		}

		boolean unFinishExists = false;
		boolean mixedExists = false;
		try {
			lastTerminator.commit(xid, true);
		} catch (XAException xaex) {
			unFinishExists = XAInternalException.class.isInstance(xaex);

			switch (xaex.errorCode) {
			case XAException.XA_HEURMIX:
				mixedExists = true;
				break;
			case XAException.XA_HEURCOM:
				break;
			case XAException.XA_HEURRB:
				this.rollback();
				throw new HeuristicRollbackException();
			default:
				logger.warning("Unknown state in committing transaction phase.");
			}
		}

		this.transactionStatus = Status.STATUS_COMMITTING;
		archive.setStatus(this.transactionStatus);
		archive.setVote(XAResource.XA_OK);
		this.transactionContext.setPrepareVote(XAResource.XA_OK);
		transactionLogger.updateTransaction(archive);

		boolean transactionCompleted = false;
		try {
			firstTerminator.commit(xid, false);
			if (unFinishExists == false) {
				transactionCompleted = true;
			}
		} catch (XAInternalException xaex) {
			CommitRequiredException ex = new CommitRequiredException();
			ex.initCause(xaex);
			throw ex;
		} catch (XAException xaex) {
			if (unFinishExists) {
				CommitRequiredException ex = new CommitRequiredException();
				ex.initCause(xaex);
				throw ex;
			} else if (mixedExists) {
				transactionCompleted = true;
				throw new HeuristicMixedException();
			} else {
				transactionCompleted = true;

				switch (xaex.errorCode) {
				case XAException.XA_HEURMIX:
					throw new HeuristicMixedException();
				case XAException.XA_HEURCOM:
					break;
				case XAException.XA_HEURRB:
					throw new HeuristicMixedException();
				default:
					logger.warning("Unknown state in committing transaction phase.");
				}
			}
		} finally {
			if (transactionCompleted) {
				this.transactionStatus = Status.STATUS_COMMITTED;
				archive.setStatus(this.transactionStatus);
				transactionLogger.deleteTransaction(archive);
			}
		}

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

			if (descriptor.isRemote()) {
				return this.enlistRemoteResource(descriptor);
			} else {
				return this.enlistNativeResource(descriptor);
			}
		} else {
			throw new IllegalStateException();
		}

	}

	public synchronized boolean enlistNativeResource(XAResourceDescriptor descriptor) throws RollbackException,
			IllegalStateException, SystemException {
		if (descriptor.isSupportsXA()) {
			return this.nativeTerminator.enlistResource(descriptor);
		} else if (this.transactionContext.isNonxaResourceAllowed()) {
			boolean remoteSupportsXA = false;
			try {
				remoteSupportsXA = this.remoteTerminator.xaSupports();
			} catch (RemoteSystemException rsex) {
				// logger.warning("Error occurred in enlist resource.");
				throw new SystemException("There already has a non-xa resource exists.");
			}
			if (remoteSupportsXA) {
				boolean enlisted = this.nativeTerminator.enlistResource(descriptor);
				this.transactionContext.setOptimized(true);
				this.transactionContext.setNonxaResourceAllowed(false);
				this.firstTerminator = this.remoteTerminator;
				this.lastTerminator = this.nativeTerminator;
				return enlisted;
			} else {
				throw new SystemException("There already has a non-xa resource exists.");
			}
		} else {
			throw new SystemException("Non-xa resource is not supported.");
		}
	}

	public synchronized boolean enlistRemoteResource(XAResourceDescriptor descriptor) throws RollbackException,
			IllegalStateException, SystemException {
		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		if (descriptor.isSupportsXA()) {
			return this.remoteTerminator.enlistResource(descriptor);
		} else if (transactionConfigurator.isOptimizeEnabled()) {
			boolean nativeSupportsXA = false;
			try {
				nativeSupportsXA = this.nativeTerminator.xaSupports();
			} catch (RemoteSystemException rsex) {
				// logger.warning("Error occurred in enlist resource.");
				throw new SystemException("There already has a non-xa resource exists.");
			}
			if (nativeSupportsXA) {
				boolean enlisted = this.remoteTerminator.enlistResource(descriptor);
				this.transactionContext.setOptimized(true);
				this.transactionContext.setNonxaResourceAllowed(false);
				this.firstTerminator = this.nativeTerminator;
				this.lastTerminator = this.remoteTerminator;
				return enlisted;
			} else {
				throw new SystemException("There already has a non-xa resource exists.");
			}
		} else {
			throw new SystemException("Non-xa resource is not supported.");
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

	public synchronized void rollback() throws IllegalStateException, RollbackRequiredException, SystemException {

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

		// delist all resources
		this.delistAllResourceQuietly();

		try {
			this.invokeRollback();
		} finally {
			this.afterCompletion();
		}

	}

	public synchronized void invokeRollback() throws IllegalStateException, RollbackRequiredException, SystemException {

		boolean unFinishExists = false;
		boolean commitExists = false;
		boolean mixedExists = false;
		boolean transactionCompleted = false;
		TransactionXid xid = this.transactionContext.getGlobalXid();

		TransactionArchive archive = this.getTransactionArchive();// new TransactionArchive();

		this.transactionStatus = Status.STATUS_ROLLING_BACK;
		archive.setStatus(this.transactionStatus);

		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		TransactionLogger transactionLogger = transactionConfigurator.getTransactionLogger();
		transactionLogger.createTransaction(archive);

		// rollback the first-resource
		try {
			firstTerminator.rollback(xid);
		} catch (XAException xaex) {
			unFinishExists = XAInternalException.class.isInstance(xaex);

			switch (xaex.errorCode) {
			case XAException.XA_HEURRB:
				break;
			case XAException.XA_HEURMIX:
				mixedExists = true;
				break;
			case XAException.XA_HEURCOM:
				commitExists = true;
				break;
			default:
				logger.warning("Unknown state in rollingback transaction phase.");
			}
		}

		// rollback the last-resource
		try {
			lastTerminator.rollback(xid);
			if (unFinishExists == false) {
				transactionCompleted = true;
			}
		} catch (XAInternalException xaex) {
			RollbackRequiredException ex = new RollbackRequiredException();
			ex.initCause(xaex);
			throw ex;
		} catch (XAException xaex) {
			if (unFinishExists) {
				RollbackRequiredException ex = new RollbackRequiredException();
				ex.initCause(xaex);
				throw ex;
			} else if (mixedExists) {
				transactionCompleted = true;
				SystemException systemErr = new SystemException();
				systemErr.initCause(new XAException(XAException.XA_HEURMIX));
				throw systemErr;
			} else {
				transactionCompleted = true;

				switch (xaex.errorCode) {
				case XAException.XA_HEURRB:
					if (commitExists) {
						SystemException systemErr = new SystemException();
						systemErr.initCause(new XAException(XAException.XA_HEURMIX));
						throw systemErr;
					}
					break;
				case XAException.XA_HEURMIX:
					SystemException systemErr = new SystemException();
					systemErr.initCause(new XAException(XAException.XA_HEURMIX));
					throw systemErr;
				case XAException.XA_HEURCOM:
					if (commitExists) {
						SystemException committedErr = new SystemException();
						committedErr.initCause(new XAException(XAException.XA_HEURCOM));
						throw committedErr;
					} else {
						SystemException mixedErr = new SystemException();
						mixedErr.initCause(new XAException(XAException.XA_HEURMIX));
						throw mixedErr;
					}
				default:
					logger.warning("Unknown state in rollingback transaction phase.");
				}
			}
		} finally {
			if (transactionCompleted) {
				this.transactionStatus = Status.STATUS_COMMITTED;// .setStatusCommitted();
				archive.setStatus(this.transactionStatus);
				transactionLogger.deleteTransaction(archive);
			}
		}

	}

	public void suspend() throws SystemException {
		SystemException throwable = null;
		if (this.nativeTerminator != null) {
			try {
				this.nativeTerminator.suspendAllResource();
			} catch (RollbackException rex) {
				this.setRollbackOnlyQuietly();
				throwable = new SystemException();
				throwable.initCause(rex);
			} catch (SystemException ex) {
				throwable = ex;
			}
		}

		if (this.remoteTerminator != null) {
			try {
				this.remoteTerminator.suspendAllResource();
			} catch (RollbackException rex) {
				this.setRollbackOnlyQuietly();
				throwable = new SystemException();
				throwable.initCause(rex);
			} catch (SystemException ex) {
				throwable = ex;
			}
		}

		if (throwable != null) {
			throw throwable;
		}

	}

	public void resume() throws SystemException {
		SystemException throwable = null;
		if (this.nativeTerminator != null) {
			try {
				this.nativeTerminator.resumeAllResource();
			} catch (RollbackException rex) {
				this.setRollbackOnlyQuietly();
				throwable = new SystemException();
				throwable.initCause(rex);
			} catch (SystemException ex) {
				throwable = ex;
			}
		}

		if (this.remoteTerminator != null) {
			try {
				this.remoteTerminator.resumeAllResource();
			} catch (RollbackException rex) {
				this.setRollbackOnlyQuietly();
				throwable = new SystemException();
				throwable.initCause(rex);
			} catch (SystemException ex) {
				throwable = ex;
			}
		}

		if (throwable != null) {
			throw throwable;
		}

	}

	private void delistAllResourceQuietly() {
		try {
			this.delistAllResource();
		} catch (RollbackRequiredException rrex) {
			logger.warning(rrex.getMessage());
		} catch (SystemException ex) {
			logger.warning(ex.getMessage());
		} catch (RuntimeException rex) {
			logger.warning(rex.getMessage());
		}
	}

	private void delistAllResource() throws RollbackRequiredException, SystemException {
		RollbackRequiredException rrex = null;
		SystemException systemEx = null;
		if (this.nativeTerminator != null) {
			try {
				this.nativeTerminator.delistAllResource();
			} catch (RollbackException rex) {
				rrex = new RollbackRequiredException();
			} catch (SystemException ex) {
				systemEx = ex;
			}
		}

		if (this.remoteTerminator != null) {
			try {
				this.remoteTerminator.delistAllResource();
			} catch (RollbackException rex) {
				rrex = new RollbackRequiredException();
			} catch (SystemException ex) {
				systemEx = ex;
			}
		}

		if (rrex != null) {
			throw rrex;
		} else if (systemEx != null) {
			throw systemEx;
		}

	}

	public void setRollbackOnlyQuietly() {
		try {
			this.setRollbackOnly();
		} catch (Exception ignore) {
			// ignore
		}
	}

	public synchronized void setRollbackOnly() throws IllegalStateException, SystemException {
		if (this.transactionStatus == Status.STATUS_ACTIVE || this.transactionStatus == Status.STATUS_MARKED_ROLLBACK) {
			this.transactionStatus = Status.STATUS_MARKED_ROLLBACK;
		} else {
			throw new IllegalStateException();
		}
	}

	public synchronized void cleanup() {

		TransactionXid xid = this.transactionContext.getGlobalXid();

		try {
			this.nativeTerminator.forget(xid);
		} catch (XAException ex) {
			// ignore
		}

		try {
			this.remoteTerminator.forget(xid);
		} catch (XAException ex) {
			// ignore
		}

	}

	public synchronized void recoveryRollback() throws RollbackRequiredException, SystemException {

		TransactionXid xid = this.transactionContext.getGlobalXid();

		boolean optimized = this.transactionContext.isOptimized();
		boolean committedExists = optimized && this.transactionStatus == Status.STATUS_COMMITTING;
		boolean rolledbackExists = false;
		this.transactionStatus = Status.STATUS_ROLLING_BACK;

		boolean unFinishExists = false;
		boolean transactionCompleted = false;
		try {
			this.nativeTerminator.rollback(xid);
			rolledbackExists = true;
		} catch (XAException xaex) {
			unFinishExists = XAInternalException.class.isInstance(xaex);

			switch (xaex.errorCode) {
			case XAException.XA_HEURMIX:
				committedExists = true;
				rolledbackExists = true;
				break;
			case XAException.XA_HEURCOM:
				committedExists = true;
				break;
			case XAException.XA_HEURRB:
				rolledbackExists = true;
				break;
			default:
				logger.warning("Unknown state in recovery-rollingback phase.");
			}
		}

		try {
			this.remoteTerminator.rollback(xid);
			rolledbackExists = true;
			if (unFinishExists == false) {
				transactionCompleted = true;
			}
		} catch (XAInternalException xaex) {
			RollbackRequiredException rrex = new RollbackRequiredException();
			rrex.initCause(xaex);
			throw rrex;
		} catch (XAException xaex) {
			if (unFinishExists) {
				RollbackRequiredException rrex = new RollbackRequiredException();
				rrex.initCause(xaex);
				throw rrex;
			} else if (committedExists && rolledbackExists) {
				transactionCompleted = true;
				SystemException ex = new SystemException(XAException.XA_HEURMIX);
				ex.initCause(xaex);
				throw ex;
			} else {
				transactionCompleted = true;

				switch (xaex.errorCode) {
				case XAException.XA_HEURMIX:
					SystemException ex = new SystemException(XAException.XA_HEURMIX);
					ex.initCause(xaex);
					throw ex;
				case XAException.XA_HEURCOM:
					if (rolledbackExists) {
						SystemException mixedErr = new SystemException(XAException.XA_HEURMIX);
						mixedErr.initCause(xaex);
						throw mixedErr;
					} else {
						SystemException committedErr = new SystemException(XAException.XA_HEURCOM);
						committedErr.initCause(xaex);
						throw committedErr;
					}
				case XAException.XA_HEURRB:
					if (committedExists) {
						SystemException mixedErr = new SystemException(XAException.XA_HEURMIX);
						mixedErr.initCause(xaex);
						throw mixedErr;
					}
					break;
				default:
					logger.warning("Unknown state in recovery-committing phase.");
				}
			}
		} finally {

			if (transactionCompleted) {
				TransactionArchive archive = this.getTransactionArchive();// new TransactionArchive();
				TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
				TransactionLogger transactionLogger = transactionConfigurator.getTransactionLogger();

				this.transactionStatus = Status.STATUS_COMMITTED;
				archive.setStatus(this.transactionStatus);
				transactionLogger.deleteTransaction(archive);
			}

		}

	}

	public synchronized void recoveryCommit() throws HeuristicMixedException, CommitRequiredException, SystemException {

		TransactionXid xid = this.transactionContext.getGlobalXid();

		this.transactionStatus = Status.STATUS_COMMITTING;// .setStatusCommiting();
		// boolean nativeReadOnly = this.nativeTerminator.checkReadOnlyForRecovery();
		// boolean remoteReadOnly = this.remoteTerminator.checkReadOnlyForRecovery();
		boolean committedExists = this.transactionContext.isOptimized();
		boolean rolledbackExists = false;

		boolean unFinishExists = false;
		boolean transactionCompleted = false;
		try {
			this.nativeTerminator.commit(xid, false);
			committedExists = true;
		} catch (XAException xaex) {
			unFinishExists = XAInternalException.class.isInstance(xaex);

			switch (xaex.errorCode) {
			case XAException.XA_HEURMIX:
				committedExists = true;
				rolledbackExists = true;
				break;
			case XAException.XA_HEURCOM:
				committedExists = true;
				break;
			case XAException.XA_HEURRB:
				rolledbackExists = true;
				break;
			default:
				logger.warning("Unknown state in recovery-committing phase.");
			}
		}

		try {
			this.remoteTerminator.commit(xid, false);
			committedExists = true;
			if (unFinishExists == false) {
				transactionCompleted = true;
			}
		} catch (XAInternalException xaex) {
			CommitRequiredException ex = new CommitRequiredException();
			ex.initCause(xaex);
			throw ex;
		} catch (XAException xaex) {
			if (unFinishExists) {
				CommitRequiredException ex = new CommitRequiredException();
				ex.initCause(xaex);
				throw ex;
			} else if (committedExists && rolledbackExists) {
				transactionCompleted = true;
				throw new HeuristicMixedException();
			} else {
				transactionCompleted = true;

				switch (xaex.errorCode) {
				case XAException.XA_HEURMIX:
					committedExists = true;
					rolledbackExists = true;
					break;
				case XAException.XA_HEURCOM:
					committedExists = true;
					break;
				case XAException.XA_HEURRB:
					rolledbackExists = true;
					break;
				default:
					logger.warning("Unknown state in recovery-committing phase.");
				}
			}
		} finally {

			if (transactionCompleted) {
				TransactionArchive archive = this.getTransactionArchive();// new TransactionArchive();

				TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
				TransactionLogger transactionLogger = transactionConfigurator.getTransactionLogger();

				this.transactionStatus = Status.STATUS_COMMITTED;
				archive.setStatus(this.transactionStatus);
				transactionLogger.deleteTransaction(archive);
			}

		}

	}

	public TransactionArchive getTransactionArchive() {
		TransactionArchive transactionArchive = new TransactionArchive();
		transactionArchive.setOptimized(this.transactionContext.isOptimized());
		transactionArchive.setVote(this.transactionContext.getPrepareVote());
		transactionArchive.setXid(this.transactionContext.getGlobalXid());
		transactionArchive.setCompensable(this.transactionContext.isCompensable());
		transactionArchive.setCoordinator(this.transactionContext.isCoordinator());
		transactionArchive.getNativeResources().addAll(this.nativeTerminator.getResourceArchives());
		transactionArchive.getRemoteResources().addAll(this.remoteTerminator.getResourceArchives());
		transactionArchive.setStatus(this.transactionStatus);
		return transactionArchive;
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
