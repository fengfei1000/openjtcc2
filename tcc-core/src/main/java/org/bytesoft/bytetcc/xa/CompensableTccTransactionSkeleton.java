package org.bytesoft.bytetcc.xa;

import java.rmi.RemoteException;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.bytesoft.bytetcc.CompensableJtaTransaction;
import org.bytesoft.bytetcc.CompensableTccTransaction;
import org.bytesoft.bytetcc.CompensableTransactionManager;
import org.bytesoft.bytetcc.common.TransactionConfigurator;
import org.bytesoft.bytetcc.common.TransactionRepository;
import org.bytesoft.bytetcc.supports.CompensableTransactionLogger;
import org.bytesoft.transaction.CommitRequiredException;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.xa.TransactionXid;

public class CompensableTccTransactionSkeleton implements XAResource {

	public void commit(Xid xid, boolean opc) throws XAException {
		TransactionXid transactionXid = (TransactionXid) xid;
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		CompensableTransactionManager transactionManager = configurator.getTransactionManager();
		TransactionRepository repository = configurator.getTransactionRepository();
		CompensableTccTransaction transaction = (CompensableTccTransaction) repository.getTransaction(transactionXid);
		if (transaction == null) {
			throw new XAException(XAException.XAER_NOTA);
		} else if (transaction.getStatus() == Status.STATUS_ROLLEDBACK) {
			throw new XAException(XAException.XA_HEURRB);
		} else if (transaction.getStatus() == Status.STATUS_COMMITTED) {
			return;
		} else if (transaction.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
			this.rollback(xid);
			throw new XAException(XAException.XA_HEURMIX);
		} else if (transaction.getStatus() != Status.STATUS_ACTIVE) {
			// ignore
		}

		TransactionContext transactionContext = transaction.getTransactionContext();

		TransactionXid globalXid = transactionContext.getGlobalXid();
		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();
		CompensableTransactionLogger transactionLogger = transactionConfigurator.getTransactionLogger();

		// transaction.setTransactionStatus(Status.STATUS_PREPARING);
		// transactionLogger.updateTransaction(transaction.getTransactionArchive());
		// transaction.setTransactionStatus(Status.STATUS_PREPARED);
		// transaction.setCompensableStatus(CompensableTccTransaction.STATUS_TRIED);

		transaction.setTransactionStatus(Status.STATUS_COMMITTING);
		transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CONFIRMING);

		transactionLogger.updateTransaction(transaction.getTransactionArchive());

		// step2: create confirm-phase-transaction
		CompensableJtaTransaction confirmTransaction = null;
		try {
			transactionManager.beginJtaTransaction();
			confirmTransaction = (CompensableJtaTransaction) transactionManager.getCurrentTransaction();
			confirmTransaction.setCompensableTccTransaction(transaction);
			transaction.setCompensableJtaTransaction(confirmTransaction);
		} catch (SystemException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (NotSupportedException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (RuntimeException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		}

		// step3: confirm
		try {
			transaction.nativeConfirm();
		} catch (Exception ex) /* RollbackRequiredException | RuntimeException */{

			TransactionXid confirmXid = confirmTransaction.getTransactionContext().getCurrentXid();
			try {
				transactionManager.internalRollbackJtaTransaction();
				transaction.setCompensableJtaTransaction(null);
			} catch (IllegalStateException otherEx) {
				transactionRepository.putErrorTransaction(confirmXid, confirmTransaction);
			} catch (SecurityException otherEx) {
				transactionRepository.putErrorTransaction(confirmXid, confirmTransaction);
			} catch (SystemException otherEx) {
				transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CONFIRM_FAILURE);
				transactionLogger.updateTransaction(transaction.getTransactionArchive());

				transactionRepository.putErrorTransaction(confirmXid, confirmTransaction);
			} catch (RuntimeException otherEx) {
				transactionRepository.putErrorTransaction(confirmXid, confirmTransaction);
			}

			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		}

		try {
			transactionManager.internalCommitJtaTransaction();
		} catch (CommitRequiredException ex) {
			transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CONFIRM_FAILURE);
			transactionLogger.updateTransaction(transaction.getTransactionArchive());

			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (RollbackException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (HeuristicMixedException ex) {
			transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CONFIRM_FAILURE);
			transactionLogger.updateTransaction(transaction.getTransactionArchive());

			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (HeuristicRollbackException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (SecurityException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (IllegalStateException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (SystemException ex) {
			transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CONFIRM_FAILURE);
			transactionLogger.updateTransaction(transaction.getTransactionArchive());

			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (RuntimeException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		}

		transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CONFIRMED);
		transactionLogger.updateTransaction(transaction.getTransactionArchive());

		try {
			transaction.remoteConfirm();
		} catch (SystemException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (RemoteException rex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (RuntimeException rex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		}

		transaction.setTransactionStatus(Status.STATUS_COMMITTED);
		transactionLogger.deleteTransaction(transaction.getTransactionArchive());

	}

	public void rollback(Xid xid) throws XAException {
		TransactionXid transactionXid = (TransactionXid) xid;
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		CompensableTransactionManager transactionManager = configurator.getTransactionManager();
		TransactionRepository repository = configurator.getTransactionRepository();
		CompensableTccTransaction transaction = (CompensableTccTransaction) repository.getTransaction(transactionXid);

		if (transaction == null) {
			throw new IllegalStateException();
		} else if (transaction.getStatus() == Status.STATUS_ROLLEDBACK) {
			return;
		} else if (transaction.getStatus() == Status.STATUS_COMMITTED) {
			throw new XAException(XAException.XAER_RMERR);
		}

		TransactionContext transactionContext = transaction.getTransactionContext();

		TransactionXid globalXid = transactionContext.getGlobalXid();
		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();
		CompensableTransactionLogger transactionLogger = transactionConfigurator.getTransactionLogger();

		transaction.setTransactionStatus(Status.STATUS_ROLLING_BACK);
		transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CANCELLING);
		transactionLogger.updateTransaction(transaction.getTransactionArchive());

		// step1: create cancel-phase-transaction
		CompensableJtaTransaction cancelTransaction = null;
		try {
			transactionManager.beginJtaTransaction();
			cancelTransaction = (CompensableJtaTransaction) transactionManager.getCurrentTransaction();
			cancelTransaction.setCompensableTccTransaction(transaction);
			transaction.setCompensableJtaTransaction(cancelTransaction);
		} catch (SystemException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (NotSupportedException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (RuntimeException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		}

		// step2: cancel
		try {
			transaction.nativeCancel();
		} catch (Exception ex) /* RollbackRequiredException | RuntimeException */{
			TransactionXid cancelXid = cancelTransaction.getTransactionContext().getCurrentXid();
			try {
				transactionManager.internalRollbackJtaTransaction();
				transaction.setCompensableJtaTransaction(null);
			} catch (IllegalStateException otherEx) {
				transactionRepository.putErrorTransaction(cancelXid, cancelTransaction);
			} catch (SecurityException otherEx) {
				transactionRepository.putErrorTransaction(cancelXid, cancelTransaction);
			} catch (SystemException otherEx) {
				transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CANCEL_FAILURE);
				transactionLogger.updateTransaction(transaction.getTransactionArchive());

				transactionRepository.putErrorTransaction(cancelXid, cancelTransaction);
			} catch (RuntimeException otherEx) {
				transactionRepository.putErrorTransaction(cancelXid, cancelTransaction);
			}

			throw new XAException(XAException.XAER_RMERR);
		}

		try {
			transactionManager.internalCommitJtaTransaction();
		} catch (CommitRequiredException ex) {
			transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CANCEL_FAILURE);
			transactionLogger.updateTransaction(transaction.getTransactionArchive());

			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (RollbackException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (HeuristicMixedException ex) {
			transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CANCEL_FAILURE);
			transactionLogger.updateTransaction(transaction.getTransactionArchive());

			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (HeuristicRollbackException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (SecurityException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (IllegalStateException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (SystemException ex) {
			transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CANCEL_FAILURE);
			transactionLogger.updateTransaction(transaction.getTransactionArchive());

			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (RuntimeException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		}
		transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CANCELLED);
		transactionLogger.updateTransaction(transaction.getTransactionArchive());

		try {
			transaction.remoteCancel();
		} catch (SystemException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (RemoteException rex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		} catch (RuntimeException rex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new XAException(XAException.XAER_RMERR);
		}

		transaction.setTransactionStatus(Status.STATUS_ROLLEDBACK);
		transactionLogger.deleteTransaction(transaction.getTransactionArchive());

	}

	public Xid[] recover(int arg0) throws XAException {
		return new Xid[0];
	}

	public void forget(Xid arg0) throws XAException {
	}

	public int getTransactionTimeout() throws XAException {
		throw new XAException(XAException.XAER_PROTO);
	}

	public boolean isSameRM(XAResource arg0) throws XAException {
		throw new XAException(XAException.XAER_PROTO);
	}

	public int prepare(Xid arg0) throws XAException {
		throw new XAException(XAException.XAER_PROTO);
	}

	public boolean setTransactionTimeout(int arg0) throws XAException {
		throw new XAException(XAException.XAER_PROTO);
	}

	public void start(Xid arg0, int arg1) throws XAException {
		throw new XAException(XAException.XAER_PROTO);
	}

	public void end(Xid arg0, int arg1) throws XAException {
		throw new XAException(XAException.XAER_PROTO);
	}

}
