package org.bytesoft.bytetcc.internal;

import java.rmi.RemoteException;

import javax.transaction.HeuristicCommitException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.bytesoft.bytetcc.TransactionImpl;
import org.bytesoft.bytetcc.supports.TransactionLogger;
import org.bytesoft.transaction.TransactionStatistic;
import org.bytesoft.transaction.TransactionStatus;
import org.bytesoft.transaction.xa.RemoteXAException;

public class TerminatorSkeleton implements XAResource {

	private TransactionImpl transaction;

	public TerminatorSkeleton(TransactionImpl tx) {
		this.transaction = tx;
	}

	public void cleanup() throws RemoteException {
		synchronized (transaction) {
			// transaction.cleanup();
		}
	}

	public void prepare() throws SystemException, RemoteException {

		// TransactionLogger transactionLogger = transaction.getTransactionLogger();
		// TransactionStatistic transactionStatistic = transaction.getTransactionStatistic();
		//
		// synchronized (transaction) {
		// transaction.beforeCompletion();
		// transaction.setTransactionCompleting(true);
		//
		// TransactionStatus transactionStatus = transaction.getTransactionStatus();
		// if (CompensableTransaction.class.equals(transaction.getClass()) == false) {
		// throw new SystemException();
		// } else if (transactionStatus.isActive() || transactionStatus.isMarkedRollbackOnly()) {
		// transactionStatus.setStatusPreparing();
		// transactionStatistic.firePreparingTransaction(transaction);
		// }
		//
		// boolean prepareSuccess = false;
		// try {
		// transaction.prepareParticipant();
		// prepareSuccess = true;
		// } catch (IllegalStateException ex) {
		// // NoSuchTransactionException nstex = new NoSuchTransactionException();
		// // nstex.initCause(ex);
		// // throw nstex;
		// throw ex;// TODO
		// } catch (SystemException ex) {
		// throw ex;
		// } catch (RuntimeException rex) {
		// SystemException exception = new SystemException();
		// exception.initCause(rex);
		// throw exception;
		// } finally {
		// if (transactionStatus.isPreparing()) {
		// if (prepareSuccess) {
		// transactionStatus.setStatusPrepared();
		// transactionStatistic.firePreparedTransaction(transaction);
		// } else {
		// transactionStatus.setStatusPrepareFail();
		// }
		// transactionLogger.prepareTransaction(transaction);
		// }
		// }
		// }
	}

	public void commit() throws HeuristicMixedException, HeuristicRollbackException, SystemException, RemoteException {

		// TransactionLogger transactionLogger = transaction.getTransactionLogger();
		// TransactionStatistic transactionStatistic = transaction.getTransactionStatistic();
		//
		// synchronized (transaction) {
		// TransactionStatus transactionStatus = transaction.getTransactionStatus();
		// if (transactionStatus.isActive()) {
		// throw new IllegalStateException();
		// } else if (transactionStatus.isMarkedRollbackOnly()) {
		// throw new IllegalStateException();
		// } else if (transactionStatus.isRolledBack()) {
		// throw new HeuristicRollbackException();
		// } else if (transactionStatus.isRollingBack()) {
		// try {
		// this.rollback();
		// throw new HeuristicRollbackException();
		// } catch (IllegalStateException ex) {
		// SystemException exception = new SystemException();
		// exception.initCause(ex);
		// throw exception;
		// } catch (HeuristicCommitException e) {
		// return;
		// }
		// } else if (transactionStatus.isCommitted()) {
		// return;
		// }
		//
		// transaction.setTransactionCompleting(true);
		//
		// try {
		// if (transactionStatus.isPrepared()) {
		// transactionStatus.setStatusCommiting();
		// transactionStatistic.fireCommittingTransaction(transaction);
		// transactionLogger.updateTransaction(transaction);
		// } else if (transactionStatus.isCommitting()) {
		// // ignore
		// } else {
		// throw new IllegalStateException();
		// }
		//
		// transaction.participantCommit();
		//
		// if (transactionStatus.isCommitting()) {
		// transactionStatus.setStatusCommitted();
		// transactionStatistic.fireCommittedTransaction(transaction);
		// transactionLogger.completeTransaction(transaction);
		// } else if (transactionStatus.isCommitted()) {
		// // ignore
		// } else {
		// throw new IllegalStateException();
		// }
		//
		// } finally {
		// transaction.afterCompletion();
		// }
		// }
	}

	public void rollback() throws HeuristicMixedException, HeuristicCommitException, SystemException, RemoteException {

		// TransactionLogger transactionLogger = transaction.getTransactionLogger();
		// TransactionStatistic transactionStatistic = transaction.getTransactionStatistic();
		//
		// synchronized (transaction) {
		// TransactionStatus transactionStatus = transaction.getTransactionStatus();
		// if (transactionStatus.isActive()) {
		// throw new IllegalStateException();
		// } else if (transactionStatus.isMarkedRollbackOnly()) {
		// throw new IllegalStateException();
		// } else if (transactionStatus.isCommitted()) {
		// throw new IllegalStateException();
		// } else if (transactionStatus.isRolledBack()) {
		// return;
		// }
		//
		// try {
		// if (transactionStatus.isActive() || transactionStatus.isMarkedRollbackOnly()) {
		// transactionStatus.setStatusRollingback();
		// transactionStatistic.fireRollingBackTransaction(transaction);
		// transactionLogger.updateTransaction(transaction);
		// } else if (transactionStatus.isPrepareFail() || transactionStatus.isPrepared()) {
		// transactionStatus.setStatusRollingback();
		// transactionStatistic.fireRollingBackTransaction(transaction);
		// transactionLogger.updateTransaction(transaction);
		// } else if (transactionStatus.isRollingBack()) {
		// // ignore
		// } else if (transactionStatus.isCommitting() || transactionStatus.isCommitFail()) {
		// transactionStatus.setStatusRollingback();
		// transactionStatistic.fireRollingBackTransaction(transaction);
		// transactionLogger.updateTransaction(transaction);
		// } else {
		// throw new IllegalStateException();
		// }
		//
		// transaction.setTransactionCompleting(true);
		//
		// transaction.participantRollback();
		//
		// if (transactionStatus.isRollingBack()) {
		// transactionStatus.setStatusRolledback();
		// transactionStatistic.fireRolledbackTransaction(transaction);
		// } else if (transactionStatus.isRolledBack()) {
		// // ignore
		// } else {
		// throw new IllegalStateException();
		// }
		//
		// transactionLogger.completeTransaction(transaction);
		//
		// } finally {
		// transaction.afterCompletion();
		// }
		// }
	}

	public void commit(Xid xid, boolean opc) throws XAException {
		try {
			this.commit();
		} catch (RemoteException ex) {
			RemoteXAException xae = new RemoteXAException();
			xae.initCause(ex);
			throw xae;
		} catch (HeuristicMixedException ex) {
			XAException xae = new XAException(RemoteXAException.XA_HEURMIX);
			xae.initCause(ex);
			throw xae;
		} catch (HeuristicRollbackException ex) {
			XAException xae = new XAException(RemoteXAException.XA_HEURRB);
			xae.initCause(ex);
			throw xae;
		} catch (SystemException ex) {
			XAException xae = new XAException(RemoteXAException.XAER_RMERR);
			xae.initCause(ex);
			throw xae;
		} catch (RuntimeException ex) {
			XAException xae = new XAException(RemoteXAException.XAER_RMERR);
			xae.initCause(ex);
			throw xae;
		}
	}

	public void end(Xid xid, int flags) throws XAException {
	}

	public void forget(Xid xid) throws XAException {
		try {
			this.cleanup();
		} catch (RemoteException ex) {
			RemoteXAException xae = new RemoteXAException();
			xae.initCause(ex);
			throw xae;
		} catch (RuntimeException ex) {
			XAException xae = new XAException(RemoteXAException.XAER_RMERR);
			xae.initCause(ex);
			throw xae;
		}
	}

	public int getTransactionTimeout() throws XAException {
		return 0;
	}

	public boolean isSameRM(XAResource xares) throws XAException {
		return false;
	}

	public int prepare(Xid xid) throws XAException {
		try {
			this.prepare();
		} catch (RemoteException ex) {
			RemoteXAException xae = new RemoteXAException();
			xae.initCause(ex);
			throw xae;
		} catch (SystemException ex) {
			XAException xae = new XAException(RemoteXAException.XAER_RMERR);
			xae.initCause(ex);
			throw xae;
		} catch (RuntimeException ex) {
			XAException xae = new XAException(RemoteXAException.XAER_RMERR);
			xae.initCause(ex);
			throw xae;
		}
		return 0;
	}

	public Xid[] recover(int flags) throws XAException {
		return new Xid[0];
	}

	public void rollback(Xid xid) throws XAException {
		try {
			this.rollback();
		} catch (RemoteException ex) {
			RemoteXAException xae = new RemoteXAException();
			xae.initCause(ex);
			throw xae;
		} catch (HeuristicMixedException ex) {
			XAException xae = new XAException(RemoteXAException.XA_HEURMIX);
			xae.initCause(ex);
			throw xae;
		} catch (HeuristicCommitException ex) {
			XAException xae = new XAException(RemoteXAException.XA_HEURCOM);
			xae.initCause(ex);
			throw xae;
		} catch (SystemException ex) {
			XAException xae = new XAException(RemoteXAException.XAER_RMERR);
			xae.initCause(ex);
			throw xae;
		} catch (RuntimeException ex) {
			XAException xae = new XAException(RemoteXAException.XAER_RMERR);
			xae.initCause(ex);
			throw xae;
		}
	}

	public boolean setTransactionTimeout(int timeout) throws XAException {
		return false;
	}

	public void start(Xid xid, int flags) throws XAException {
	}

	public TransactionImpl getTransaction() {
		return transaction;
	}

	public void setTransaction(TransactionImpl transaction) {
		this.transaction = transaction;
	}
}
