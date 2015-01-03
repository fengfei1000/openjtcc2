package org.bytesoft.bytetcc;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.bytesoft.bytejta.TransactionImpl;
import org.bytesoft.bytejta.TransactionManagerImpl;
import org.bytesoft.bytetcc.common.TransactionConfigurator;
import org.bytesoft.bytetcc.common.TransactionRepository;
import org.bytesoft.bytetcc.supports.CompensableTransactionLogger;
import org.bytesoft.transaction.CommitRequiredException;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.xa.TransactionXid;
import org.bytesoft.transaction.xa.XidFactory;

public class CompensableTransactionManager implements TransactionManager/* , TransactionTimer */{

	private boolean transactionManagerInitialized = false;
	private TransactionManagerImpl jtaTransactionManager;

	/* it's unnecessary for compensable-transaction to do the timing, the jta-transaction will do it. */
	private int timeoutSeconds = 5 * 60;
	private final ThreadLocal<CompensableInvocation> compensables = new ThreadLocal<CompensableInvocation>();
	private final Map<Thread, CompensableTransaction> associateds = new ConcurrentHashMap<Thread, CompensableTransaction>();

	public CompensableInvocation beforeCompensableExecution(CompensableInvocation lastest) {
		CompensableInvocation original = this.compensables.get();
		this.compensables.set(lastest);
		return original;
	}

	public void afterCompensableCompletion(CompensableInvocation original) {

		try {
			CompensableTransaction transaction = (CompensableTransaction) this.getCurrentTransaction();
			if (CompensableTccTransaction.class.isInstance(transaction)) {
				this.delistCompensableInvocationIfRequired((CompensableTccTransaction) transaction);
			}
		} finally {
			this.compensables.set(original);
		}

	}

	private void delistCompensableInvocationIfRequired(CompensableTccTransaction transaction) {
		CompensableInvocation compensable = this.compensables.get();
		if (transaction != null) {
			transaction.delistCompensableInvocation(compensable);
		}
	}

	public void begin() throws NotSupportedException, SystemException {

		this.initializeTransactionManagerIfRequired();

		CompensableInvocation lastest = compensables.get();
		boolean compensable = (lastest != null);
		if (compensable) {
			this.beginCompensableTransaction();
		} else {
			this.beginJtaTransaction();
		}

	}

	private void initializeTransactionManagerIfRequired() {
		if (this.transactionManagerInitialized == false) {
			synchronized (CompensableTransactionManager.class) {
				if (this.transactionManagerInitialized == false) {
					if (this.jtaTransactionManager.getTimeoutSeconds() != this.timeoutSeconds) {
						this.jtaTransactionManager.setTimeoutSeconds(this.timeoutSeconds);
					}
				}
			} // end-synchronized (CompensableTransactionManager.class)
		}
	}

	private void beginJtaTransaction() throws NotSupportedException, SystemException {
		if (this.getTransaction() != null) {
			throw new NotSupportedException();
		}

		TransactionContext transactionContext = new TransactionContext();
		transactionContext.setCoordinator(true);
		transactionContext.setCompensable(false);
		long current = System.currentTimeMillis();
		transactionContext.setCreatedTime(current);
		transactionContext.setExpiredTime(current + this.timeoutSeconds);

		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		XidFactory xidFactory = configurator.getXidFactory();
		TransactionXid global = xidFactory.createGlobalXid();
		org.bytesoft.bytejta.common.TransactionCommonXid jtaXid = new org.bytesoft.bytejta.common.TransactionCommonXid(
				global.getGlobalTransactionId());
		transactionContext.setCurrentXid(jtaXid);

		CompensableJtaTransaction transaction = new CompensableJtaTransaction(transactionContext);
		TransactionRepository transactionRepository = configurator.getTransactionRepository();

		try {
			this.jtaTransactionManager.begin(transactionContext);
			TransactionImpl jtaTransaction = this.jtaTransactionManager.getCurrentTransaction();
			transaction.setJtaTransaction(jtaTransaction);
			jtaTransaction.registerTransactionListener(transaction);
		} catch (SystemException ex) {
			try {
				this.jtaTransactionManager.rollback();
			} catch (Exception ignore) {
				// ignore
			}
			throw ex;
		}

		this.associateds.put(Thread.currentThread(), transaction);
		transactionRepository.putTransaction(transactionContext.getGlobalXid(), transaction);

	}

	private void beginCompensableTransaction() throws NotSupportedException, SystemException {
		if (this.getTransaction() != null) {
			throw new NotSupportedException();
		}

		TransactionContext transactionContext = new TransactionContext();
		transactionContext.setCoordinator(true);
		transactionContext.setCompensable(true);
		long current = System.currentTimeMillis();
		transactionContext.setCreatedTime(current);
		transactionContext.setExpiredTime(current + this.timeoutSeconds);

		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		XidFactory xidFactory = configurator.getXidFactory();
		TransactionXid globalXid = xidFactory.createGlobalXid();
		TransactionXid branchXid = xidFactory.createBranchXid(globalXid);
		transactionContext.setCurrentXid(branchXid);

		CompensableTccTransaction transaction = new CompensableTccTransaction(transactionContext);
		TransactionRepository transactionRepository = configurator.getTransactionRepository();

		TransactionContext jtaTransactionContext = transactionContext.clone();
		jtaTransactionContext.setCoordinator(true);
		// TransactionXid jtaGlobalXid = xidFactory.createGlobalXid(branchXid.getBranchQualifier());
		org.bytesoft.bytejta.common.TransactionCommonXid jtaGlobalXid = new org.bytesoft.bytejta.common.TransactionCommonXid(
				branchXid.getBranchQualifier());
		jtaTransactionContext.setCurrentXid(jtaGlobalXid);
		try {
			this.jtaTransactionManager.begin(jtaTransactionContext);
			TransactionImpl jtaTransaction = this.jtaTransactionManager.getCurrentTransaction();
			transaction.setJtaTransaction(jtaTransaction);
			jtaTransaction.registerTransactionListener(transaction);
		} catch (SystemException ex) {
			try {
				this.jtaTransactionManager.rollback();
			} catch (Exception ignore) {
				// ignore
			}
			throw ex;
		}
		this.associateds.put(Thread.currentThread(), transaction);
		transactionRepository.putTransaction(transactionContext.getGlobalXid(), transaction);

		CompensableTransactionLogger transactionLogger = configurator.getTransactionLogger();
		transactionLogger.createTransaction(transaction.getTransactionArchive());
	}

	public void propagationBegin(TransactionContext transactionContext) throws NotSupportedException, SystemException {

		this.initializeTransactionManagerIfRequired();

		if (this.getCurrentTransaction() != null) {
			throw new NotSupportedException();
		}

		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();

		TransactionXid propagationXid = transactionContext.getCurrentXid();
		TransactionXid globalXid = propagationXid.getGlobalXid();
		CompensableTccTransaction transaction = (CompensableTccTransaction) transactionRepository.getTransaction(globalXid);

		TransactionContext jtaTransactionContext = transactionContext.clone();

		if (transaction == null) {
			transaction = new CompensableTccTransaction(transactionContext);
			try {
				this.jtaTransactionManager.begin(jtaTransactionContext);
				TransactionImpl jtaTransaction = this.jtaTransactionManager.getCurrentTransaction();
				transaction.setJtaTransaction(jtaTransaction);
				jtaTransaction.registerTransactionListener(transaction);
			} catch (SystemException ex) {
				try {
					this.jtaTransactionManager.rollback();
				} catch (Exception ignore) {
					// ignore
				}
				throw ex;
			}
			transactionRepository.putTransaction(transactionContext.getGlobalXid(), transaction);
		} else {
			transaction.propagationBegin(transactionContext);
		}

		this.associateds.put(Thread.currentThread(), transaction);
		// this.transactionStatistic.fireBeginTransaction(transaction);

	}

	public void propagationFinish(TransactionContext transactionContext) throws SystemException {

		CompensableTccTransaction transaction = (CompensableTccTransaction) this.getCurrentTransaction();
		transaction.propagationFinish(transactionContext);
		this.associateds.remove(Thread.currentThread());

		try {
			this.jtaTransactionManager.commit();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RollbackException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HeuristicMixedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HeuristicRollbackException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException,
			IllegalStateException, SystemException {
		CompensableTransaction transaction = this.associateds.remove(Thread.currentThread());
		if (transaction == null) {
			throw new IllegalStateException();
		}

		TransactionContext transactionContext = transaction.getTransactionContext();
		if (transactionContext.isCompensable()) {
			this.commitTccTransaction((CompensableTccTransaction) transaction);
		} else {
			this.commitJtaTransaction((CompensableJtaTransaction) transaction);
		}
	}

	private void internalCommitJtaTransaction() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
		CompensableTransaction transaction = this.associateds.remove(Thread.currentThread());
		if (transaction == null) {
			throw new IllegalStateException();
		} else if (CompensableJtaTransaction.class.isInstance(transaction) == false) {
			throw new IllegalStateException();
		}

		this.commitJtaTransaction((CompensableJtaTransaction) transaction);
	}

	public void commitJtaTransaction(CompensableJtaTransaction transaction) throws RollbackException, HeuristicMixedException,
			HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
		this.jtaTransactionManager.commit();
	}

	public void commitTccTransaction(CompensableTccTransaction transaction) throws RollbackException, HeuristicMixedException,
			HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {

		if (transaction == null) {
			throw new IllegalStateException();
		} else if (transaction.getStatus() == Status.STATUS_ROLLEDBACK) {
			throw new RollbackException();
		} else if (transaction.getStatus() == Status.STATUS_COMMITTED) {
			return;
		} else if (transaction.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
			this.rollback();
			throw new HeuristicRollbackException();
		} else if (transaction.getStatus() != Status.STATUS_ACTIVE) {
			throw new IllegalStateException();
		}

		this.delistCompensableInvocationIfRequired(transaction);

		TransactionContext transactionContext = transaction.getTransactionContext();
		TransactionXid globalXid = transactionContext.getGlobalXid();
		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();
		CompensableTransactionLogger transactionLogger = transactionConfigurator.getTransactionLogger();

		transaction.setTransactionStatus(Status.STATUS_PREPARING);
		transactionLogger.updateTransaction(transaction.getTransactionArchive());

		// step1: commit try-phase-transaction
		try {
			this.jtaTransactionManager.commit();
		} catch (CommitRequiredException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			SystemException sysEx = new SystemException();
			if (ex.getCause() != null) {
				sysEx.initCause(ex.getCause());
			}
			throw sysEx;
		} catch (RollbackException ex) {
			throw ex;
		} catch (HeuristicRollbackException ex) {
			throw ex;
		} catch (HeuristicMixedException ex) {
			this.rollbackTccTransaction(transaction);
			throw new HeuristicRollbackException();
		} catch (SystemException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw ex;
		} catch (Exception ex)/* SecurityException | IllegalStateException | RuntimeException */{
			transactionRepository.putErrorTransaction(globalXid, transaction);
			SystemException sysEx = new SystemException();
			sysEx.initCause(ex);
			throw sysEx;
		}

		transaction.setTransactionStatus(Status.STATUS_PREPARED);
		transaction.setCompensableStatus(CompensableTccTransaction.STATUS_TRIED);

		transaction.setTransactionStatus(Status.STATUS_COMMITTING);
		transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CONFIRMING);

		transactionLogger.updateTransaction(transaction.getTransactionArchive());

		// step2: create confirm-phase-transaction
		CompensableJtaTransaction confirmTransaction = null;
		try {
			this.beginJtaTransaction();
			confirmTransaction = (CompensableJtaTransaction) this.getCurrentTransaction();
			confirmTransaction.setCompensableTccTransaction(transaction);
			transaction.setCompensableJtaTransaction(confirmTransaction);
		} catch (SystemException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new CommittingException(ex);
		} catch (NotSupportedException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new CommittingException(ex);
		} catch (RuntimeException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new CommittingException(ex);
		}

		// step3: confirm
		try {
			transaction.nativeConfirm();
		} catch (Exception ex) /* RollbackRequiredException | RuntimeException */{

			TransactionXid confirmXid = confirmTransaction.getTransactionContext().getCurrentXid();
			try {
				this.internalRollbackJtaTransaction();
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
			throw new CommittingException(ex);
		}

		try {
			this.internalCommitJtaTransaction();
		} catch (CommitRequiredException ex) {
			transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CONFIRM_FAILURE);
			transactionLogger.updateTransaction(transaction.getTransactionArchive());

			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new CommittingException(ex);
		} catch (RollbackException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new CommittingException(ex);
		} catch (HeuristicMixedException ex) {
			transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CONFIRM_FAILURE);
			transactionLogger.updateTransaction(transaction.getTransactionArchive());

			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new CommittingException(ex);
		} catch (HeuristicRollbackException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new CommittingException(ex);
		} catch (SecurityException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new CommittingException(ex);
		} catch (IllegalStateException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new CommittingException(ex);
		} catch (SystemException ex) {
			transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CONFIRM_FAILURE);
			transactionLogger.updateTransaction(transaction.getTransactionArchive());

			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new CommittingException(ex);
		} catch (RuntimeException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new CommittingException(ex);
		}

		transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CONFIRMED);
		transactionLogger.updateTransaction(transaction.getTransactionArchive());

		try {
			transaction.remoteConfirm();
		} catch (SystemException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new CommittingException(ex);
		} catch (RemoteException rex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new CommittingException(rex);
		} catch (RuntimeException rex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new CommittingException(rex);
		}

		transaction.setTransactionStatus(Status.STATUS_COMMITTED);
		transactionLogger.deleteTransaction(transaction.getTransactionArchive());

	}

	public int getStatus() throws SystemException {
		Transaction transaction = this.getTransaction();
		return transaction == null ? Status.STATUS_NO_TRANSACTION : transaction.getStatus();
	}

	public CompensableTransaction getTransaction() throws SystemException {
		return this.associateds.get(Thread.currentThread());
	}

	public CompensableTransaction getCurrentTransaction() {
		return this.associateds.get(Thread.currentThread());
	}

	public void resume(Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {
		if (CompensableTransaction.class.isInstance(tobj) == false) {
			throw new InvalidTransactionException();
		} else if (this.getTransaction() != null) {
			throw new IllegalStateException();
		}

		CompensableTransaction transaction = (CompensableTransaction) tobj;
		TransactionImpl jtaTransaction = transaction.getJtaTransaction();
		CompensableInvocation compensableObject = transaction.getCompensableObject();
		this.compensables.set(compensableObject);
		transaction.setCompensableObject(null);
		this.jtaTransactionManager.resume(jtaTransaction);
		this.associateds.put(Thread.currentThread(), transaction);
	}

	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		CompensableTransaction transaction = this.associateds.remove(Thread.currentThread());
		if (transaction == null) {
			throw new IllegalStateException();
		}

		TransactionContext transactionContext = transaction.getTransactionContext();
		if (transactionContext.isCompensable()) {
			this.rollbackTccTransaction((CompensableTccTransaction) transaction);
		} else {
			this.rollbackJtaTransaction((CompensableJtaTransaction) transaction);
		}
	}

	private void internalRollbackJtaTransaction() throws IllegalStateException, SecurityException, SystemException {
		CompensableTransaction transaction = this.associateds.remove(Thread.currentThread());
		if (transaction == null) {
			throw new IllegalStateException();
		} else if (CompensableJtaTransaction.class.isInstance(transaction) == false) {
			throw new IllegalStateException();
		}

		this.rollbackJtaTransaction((CompensableJtaTransaction) transaction);
	}

	public void rollbackJtaTransaction(CompensableJtaTransaction transaction) throws IllegalStateException, SecurityException,
			SystemException {
		this.jtaTransactionManager.rollback();
	}

	public void rollbackTccTransaction(CompensableTccTransaction transaction) throws IllegalStateException, SecurityException,
			SystemException {

		if (transaction == null) {
			throw new IllegalStateException();
		} else if (transaction.getStatus() == Status.STATUS_ROLLEDBACK) {
			return;
		} else if (transaction.getStatus() == Status.STATUS_COMMITTED) {
			throw new SystemException();
		}

		this.delistCompensableInvocationIfRequired(transaction);

		TransactionContext transactionContext = transaction.getTransactionContext();
		TransactionXid globalXid = transactionContext.getGlobalXid();
		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();
		CompensableTransactionLogger transactionLogger = transactionConfigurator.getTransactionLogger();

		int transactionStatus = transaction.getStatus();
		if (transactionStatus == Status.STATUS_ACTIVE //
				|| transactionStatus == Status.STATUS_MARKED_ROLLBACK) {
			transaction.setTransactionStatus(Status.STATUS_PREPARING);
			// rollback try-phase-transaction
			try {
				this.jtaTransactionManager.rollback();
				transaction.setTransactionStatus(Status.STATUS_ROLLING_BACK);
				transaction.setCompensableStatus(CompensableTccTransaction.STATUS_TRY_FAILURE);
			} catch (SystemException ex) {
				transactionRepository.putErrorTransaction(globalXid, transaction);
				throw ex;
			}/* IllegalStateException | SecurityException | SystemException */
			catch (Exception ex) {
				transactionRepository.putErrorTransaction(globalXid, transaction);
				SystemException sysEx = new SystemException();
				sysEx.initCause(ex);
				throw sysEx;
			}
		} else if (transactionStatus == Status.STATUS_PREPARING) {
			transaction.setTransactionStatus(Status.STATUS_PREPARING);
			// rollback try-phase-transaction
			TransactionImpl jtaTransaction = this.jtaTransactionManager.getCurrentTransaction();
			if (jtaTransaction != null) {
				/* should has been committed/rolledback in this.commitTccTransaction() */
				throw new IllegalStateException();
			}
			transaction.setTransactionStatus(Status.STATUS_ROLLING_BACK);
			transaction.setCompensableStatus(CompensableTccTransaction.STATUS_TRIED);
		} else if (transaction.getStatus() == Status.STATUS_PREPARED) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new CommittingException();/* never happen */
		}

		CompensableJtaTransaction originTransaction = transaction.getCompensableJtaTransaction();
		if (originTransaction != null) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw new CommittingException();/* never happen */
		}

		transaction.setTransactionStatus(Status.STATUS_ROLLING_BACK);

		if (transaction.getCompensableStatus() == CompensableTccTransaction.STATUS_TRIED) {
			transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CANCELLING);
			transactionLogger.updateTransaction(transaction.getTransactionArchive());

			// step1: create cancel-phase-transaction
			CompensableJtaTransaction cancelTransaction = null;
			try {
				this.beginJtaTransaction();
				cancelTransaction = (CompensableJtaTransaction) this.getCurrentTransaction();
				cancelTransaction.setCompensableTccTransaction(transaction);
				transaction.setCompensableJtaTransaction(cancelTransaction);
			} catch (SystemException ex) {
				transactionRepository.putErrorTransaction(globalXid, transaction);
				throw ex;
			} catch (NotSupportedException ex) {
				transactionRepository.putErrorTransaction(globalXid, transaction);
				SystemException sysEx = new SystemException();
				sysEx.initCause(ex);
				throw sysEx;
			} catch (RuntimeException ex) {
				transactionRepository.putErrorTransaction(globalXid, transaction);
				SystemException sysEx = new SystemException();
				sysEx.initCause(ex);
				throw sysEx;
			}

			// step2: cancel
			try {
				transaction.nativeCancel();
			} catch (Exception ex) /* RollbackRequiredException | RuntimeException */{
				TransactionXid cancelXid = cancelTransaction.getTransactionContext().getCurrentXid();
				try {
					this.internalRollbackJtaTransaction();
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

				SystemException sysEx = new SystemException();
				sysEx.initCause(ex);
				throw sysEx;
			}

			try {
				this.internalCommitJtaTransaction();
			} catch (CommitRequiredException ex) {
				transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CANCEL_FAILURE);
				transactionLogger.updateTransaction(transaction.getTransactionArchive());

				transactionRepository.putErrorTransaction(globalXid, transaction);
				SystemException sysEx = new SystemException();
				sysEx.initCause(ex);
				throw sysEx;
			} catch (RollbackException ex) {
				transactionRepository.putErrorTransaction(globalXid, transaction);
				SystemException sysEx = new SystemException();
				sysEx.initCause(ex);
				throw sysEx;
			} catch (HeuristicMixedException ex) {
				transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CANCEL_FAILURE);
				transactionLogger.updateTransaction(transaction.getTransactionArchive());

				transactionRepository.putErrorTransaction(globalXid, transaction);
				SystemException sysEx = new SystemException();
				sysEx.initCause(ex);
				throw sysEx;
			} catch (HeuristicRollbackException ex) {
				transactionRepository.putErrorTransaction(globalXid, transaction);
				SystemException sysEx = new SystemException();
				sysEx.initCause(ex);
				throw sysEx;
			} catch (SecurityException ex) {
				transactionRepository.putErrorTransaction(globalXid, transaction);
				SystemException sysEx = new SystemException();
				sysEx.initCause(ex);
				throw sysEx;
			} catch (IllegalStateException ex) {
				transactionRepository.putErrorTransaction(globalXid, transaction);
				SystemException sysEx = new SystemException();
				sysEx.initCause(ex);
				throw sysEx;
			} catch (SystemException ex) {
				transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CANCEL_FAILURE);
				transactionLogger.updateTransaction(transaction.getTransactionArchive());

				transactionRepository.putErrorTransaction(globalXid, transaction);
				SystemException sysEx = new SystemException();
				sysEx.initCause(ex);
				throw sysEx;
			} catch (RuntimeException ex) {
				transactionRepository.putErrorTransaction(globalXid, transaction);
				SystemException sysEx = new SystemException();
				sysEx.initCause(ex);
				throw sysEx;
			}
			transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CANCELLED);
			transactionLogger.updateTransaction(transaction.getTransactionArchive());

		} // end-if (transaction.getCompensableStatus() == CompensableTccTransaction.STATUS_TRIED)

		try {
			transaction.remoteCancel();
		} catch (SystemException ex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw ex;
		} catch (RemoteException rex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			SystemException sysEx = new SystemException();
			sysEx.initCause(rex);
			throw sysEx;
		} catch (RuntimeException rex) {
			transactionRepository.putErrorTransaction(globalXid, transaction);
			SystemException sysEx = new SystemException();
			sysEx.initCause(rex);
			throw sysEx;
		}

		transaction.setTransactionStatus(Status.STATUS_ROLLEDBACK);
		transactionLogger.deleteTransaction(transaction.getTransactionArchive());

	}

	public void setRollbackOnly() throws IllegalStateException, SystemException {
		CompensableTransaction transaction = this.getTransaction();
		if (transaction == null) {
			throw new SystemException();
		}
		transaction.setRollbackOnly();
	}

	public void setTransactionTimeout(int seconds) throws SystemException {
		CompensableTransaction transaction = this.getTransaction();
		if (transaction == null) {
			throw new SystemException();
		} else if (seconds < 0) {
			throw new SystemException();
		} else if (seconds == 0) {
			// ignore
		} else {
			synchronized (transaction) {
				TransactionContext transactionContext = transaction.getTransactionContext();
				long createdTime = transactionContext.getCreatedTime();
				transactionContext.setExpiredTime(createdTime + seconds * 1000L);
			}
		}
	}

	public CompensableTransaction suspend() throws SystemException {
		CompensableTransaction transaction = this.associateds.remove(Thread.currentThread());
		TransactionImpl jtaTransaction = this.jtaTransactionManager.suspend();
		transaction.setJtaTransaction(jtaTransaction);
		CompensableInvocation compensableObject = this.compensables.get();
		this.compensables.remove();
		transaction.setCompensableObject(compensableObject);
		return transaction;
	}

	/* it's unnecessary for compensable-transaction to do the timing. */
	// public void timingExecution() {}
	// public void stopTiming(Transaction tx) {}

	public int getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public TransactionManagerImpl getJtaTransactionManager() {
		return jtaTransactionManager;
	}

	public void setJtaTransactionManager(TransactionManagerImpl jtaTransactionManager) {
		this.jtaTransactionManager = jtaTransactionManager;
	}

}
