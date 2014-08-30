package org.bytesoft.bytejta;

import java.util.ArrayList;
import java.util.List;

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
import org.bytesoft.bytejta.common.XidImpl;
import org.bytesoft.bytejta.xa.XATerminatorImpl;
import org.bytesoft.transaction.RemoteSystemException;
import org.bytesoft.transaction.RollbackRequiredException;
import org.bytesoft.transaction.SynchronizationImpl;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.TransactionStatus;
import org.bytesoft.transaction.xa.RemoteXAException;
import org.bytesoft.transaction.xa.XAResourceDescriptor;
import org.bytesoft.transaction.xa.XATerminator;
import org.bytesoft.utils.ByteUtils;

public class TransactionImpl implements Transaction {

	private TransactionStatus transactionStatus;
	private final TransactionContext transactionContext;
	private final XATerminator nativeTerminator;
	private final XATerminator remoteTerminator;

	private transient XATerminator firstTerminator;
	private transient XATerminator lastTerminator;

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
		int status = this.transactionStatus.getTransactionStatus();
		for (int i = 0; i < length; i++) {
			SynchronizationImpl synchronization = this.synchronizations.get(i);
			try {
				synchronization.afterCompletion(status);
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

	private synchronized boolean checkBeforeCommit() throws RollbackException, IllegalStateException,
			RollbackRequiredException {

		if (this.transactionStatus.isRolledBack()) {
			throw new RollbackException();
		} else if (this.transactionStatus.isRollingBack()) {
			throw new RollbackRequiredException();
		} else if (this.transactionStatus.isMarkedRollbackOnly()) {
			throw new RollbackRequiredException();
		} else if (this.transactionStatus.isActive()) {
			return false;
		} else if (this.transactionStatus.isCommitted()) {
			return true;
		}
		throw new IllegalStateException();

	}

	private synchronized void analysisTerminator() {
		if (this.firstTerminator == null || this.lastTerminator == null) {

			boolean nativeValid = this.nativeTerminator.valid();
			boolean remoteValid = this.remoteTerminator.valid();

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
		}
	}

	public synchronized void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {

		try {
			if (this.checkBeforeCommit()) {
				return;
			}
		} catch (RollbackRequiredException rex) {
			this.rollback();
			throw new HeuristicRollbackException();
		}

		XidImpl xid = this.transactionContext.getCurrentXid().getGlobalXid();

		// step1: before-completion
		this.beforeCompletion();
		this.delistAllResource();

		// step2: analysis
		try {
			this.analysisTerminator();
		} catch (RuntimeException xaex) {
			this.rollback();
			throw new HeuristicRollbackException();
		}

		// step3: log
		// TransactionConfigurator.getInstance().getTransactionLogger();

		// step4: prepare
		try {
			this.firstTerminator.prepare(xid);
		} catch (XAException xaex) {
			this.rollback();
			throw new HeuristicRollbackException();
		} catch (RuntimeException xaex) {
			this.rollback();
			throw new HeuristicRollbackException();
		}

		// step5: log

		// step6: one-phase-commit the last-resource
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

		// step7: two-phase-commit the first-resource
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
			case XAException.XAER_RMFAIL:
				throw new SystemException();
			case XAException.XAER_NOTA:
			case XAException.XAER_INVAL:
			case XAException.XAER_PROTO:
				// ignore
				break;
			case XAException.XA_HEURRB:
			default:
				throw new HeuristicRollbackException();
			}
		} catch (RuntimeException rex) {
			SystemException ex = new SystemException();
			ex.initCause(rex);
			throw ex;
		} finally {
			this.afterCompletion();
		}

		// step8: log

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
			try {
				descriptor = TransactionConfigurator.getInstance().getResourceRecognizer().recognize(xaRes);
				if (descriptor == null) {
					descriptor = new XAResourceDescriptor();
					descriptor.setDelegate(xaRes);
					descriptor.setRemote(false);
					descriptor.setSupportsXA(false);
				}
			} catch (Exception rnsex) {
				descriptor = new XAResourceDescriptor();
				descriptor.setDelegate(xaRes);
				descriptor.setRemote(false);
				descriptor.setSupportsXA(false);
			}
		}

		if (descriptor.isRemote()) {
			return this.remoteTerminator.delistResource(descriptor, flag);
		} else {
			return this.nativeTerminator.delistResource(descriptor, flag);
		}

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
				try {
					descriptor = TransactionConfigurator.getInstance().getResourceRecognizer().recognize(xaRes);
					if (descriptor == null) {
						descriptor = new XAResourceDescriptor();
						descriptor.setDelegate(xaRes);
						descriptor.setRemote(false);
						descriptor.setSupportsXA(false);
					}
				} catch (Exception rnsex) {
					descriptor = new XAResourceDescriptor();
					descriptor.setDelegate(xaRes);
					descriptor.setRemote(false);
					descriptor.setSupportsXA(false);
				}
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

	public int getStatus() throws SystemException {
		return this.transactionStatus.getTransactionStatus();
	}

	public synchronized void registerSynchronization(Synchronization sync) throws RollbackException,
			IllegalStateException, SystemException {

		if (this.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
			throw new RollbackException();
		} else if (this.getStatus() == Status.STATUS_ACTIVE) {
			SynchronizationImpl synchronization = new SynchronizationImpl(sync);
			this.synchronizations.add(synchronization);
			System.out.printf(String.format(
					"[%s] register-sync: sync= %s%n"//
					, ByteUtils.byteArrayToString(this.transactionContext.getCurrentXid().getGlobalTransactionId()),
					sync));
		} else {
			throw new IllegalStateException();
		}

	}

	private boolean checkBeforeRollback() throws IllegalStateException {

		if (this.transactionStatus.isMarkedRollbackOnly()) {
			return false;
		} else if (this.transactionStatus.isActive()) {
			return false;
		} else if (this.transactionStatus.isRolledBack()) {
			return true;
		} else if (this.transactionStatus.isCommitted()) {
			throw new IllegalStateException();
		}
		throw new IllegalStateException();

	}

	public synchronized void rollback() throws IllegalStateException, SystemException {

		if (this.checkBeforeRollback()) {
			return;
		}

		XidImpl xid = this.transactionContext.getCurrentXid().getGlobalXid();

		// step1: before-completion
		this.beforeCompletion();

		// step2: log
		// TransactionConfigurator.getInstance().getTransactionLogger();

		// step3: rollback the last-resource
		try {
			lastTerminator.rollback(xid);
		} catch (RemoteXAException xaex) {
			// TODO
		} catch (XAException xaex) {
			// TODO
		} catch (RuntimeException rex) {
			// TODO
		}

		// step4: rollback the first-resource
		try {
			firstTerminator.rollback(xid);
		} catch (RemoteXAException xaex) {
			// TODO
		} catch (XAException xaex) {
			// TODO
		} catch (RuntimeException rex) {
			// TODO
		} finally {
			this.afterCompletion();
		}

	}

	public synchronized void setRollbackOnly() throws IllegalStateException, SystemException {
		this.transactionStatus.markStatusRollback();
	}

	public TransactionStatus getTransactionStatus() {
		return transactionStatus;
	}

	public void setTransactionStatus(TransactionStatus transactionStatus) {
		this.transactionStatus = transactionStatus;
	}

	public TransactionContext getTransactionContext() {
		return transactionContext;
	}

	public XATerminator getNativeTerminator() {
		return nativeTerminator;
	}

	public XATerminator getRemoteTerminator() {
		return remoteTerminator;
	}

}
