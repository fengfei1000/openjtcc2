package org.bytesoft.bytetcc;

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
import org.bytesoft.transaction.CommitRequiredException;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.TransactionTimer;
import org.bytesoft.transaction.xa.TransactionXid;
import org.bytesoft.transaction.xa.XidFactory;

public class CompensableTransactionManager implements TransactionManager, TransactionTimer {

	private boolean transactionManagerInitialized = false;
	private TransactionManagerImpl jtaTransactionManager;

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
		TransactionXid xid = xidFactory.createGlobalXid();
		TransactionXid globalXid = xidFactory.createBranchXid(xid, xid.getGlobalTransactionId());
		transactionContext.setCurrentXid(globalXid);

		CompensableJtaTransaction transaction = new CompensableJtaTransaction(transactionContext);
		TransactionRepository transactionRepository = configurator.getTransactionRepository();

		this.jtaTransactionManager.begin(transactionContext);
		try {
			TransactionImpl jtaTransaction = this.jtaTransactionManager.getTransaction();
			transaction.setJtaTransaction(jtaTransaction);
		} catch (SystemException ex) {
			this.jtaTransactionManager.rollback();
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

		CompensableTransaction transaction = new CompensableTccTransaction(transactionContext);
		TransactionRepository transactionRepository = configurator.getTransactionRepository();

		TransactionContext jtaTransactionContext = transactionContext.clone();
		jtaTransactionContext.setCoordinator(true);

		TransactionXid jtaGlobalXid = xidFactory.createGlobalXid(branchXid.getBranchQualifier());
		jtaTransactionContext.setCurrentXid(jtaGlobalXid);
		this.jtaTransactionManager.begin(jtaTransactionContext);
		this.associateds.put(Thread.currentThread(), transaction);
		transactionRepository.putTransaction(transactionContext.getGlobalXid(), transaction);

		// CompensableTransactionLogger transactionLogger = configurator.getTransactionLogger();
		// transactionLogger.createTransaction(archive);
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
		CompensableTccTransaction transaction = (CompensableTccTransaction) transactionRepository
				.getTransaction(globalXid);

		TransactionContext jtaTransactionContext = transactionContext.clone();
		this.jtaTransactionManager.begin(jtaTransactionContext);
		if (transaction == null) {
			transaction = new CompensableTccTransaction(transactionContext);

			// long createdTime = transactionContext.getCreatedTime();
			// long expiredTime = transactionContext.getExpiredTime();
			// transactionContext.setCreatedTime(createdTime);
			// transactionContext.setExpiredTime(expiredTime);

			transactionRepository.putTransaction(transactionContext.getGlobalXid(), transaction);
		} else {
			// long createdTime = transactionContext.getCreatedTime();
			// long expiredTime = transactionContext.getExpiredTime();
			// transactionContext.setCreatedTime(createdTime);
			// transactionContext.setExpiredTime(expiredTime);

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

	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
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

	public void commitJtaTransaction(CompensableJtaTransaction transaction) throws RollbackException,
			HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException,
			SystemException {
		this.jtaTransactionManager.commit();
	}

	public void commitTccTransaction(CompensableTccTransaction transaction) throws RollbackException,
			HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException,
			SystemException {

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

		// step1: commit try-phase-transaction
		boolean tryPhaseTransactionDone = true;
		try {
			this.jtaTransactionManager.commit();// TODO
		} catch (CommitRequiredException crex) {
			tryPhaseTransactionDone = false;
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw crex;
		} catch (HeuristicMixedException hmex) {
			tryPhaseTransactionDone = false;// TODO
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw hmex;
		} catch (SystemException ex) {
			tryPhaseTransactionDone = false;
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw ex;
		} catch (RuntimeException rrex) {
			tryPhaseTransactionDone = false;
			transactionRepository.putErrorTransaction(globalXid, transaction);
			SystemException ex = new SystemException();
			ex.initCause(rrex);
			throw ex;
		} finally {
			if (tryPhaseTransactionDone) {
				transactionRepository.removeErrorTransaction(globalXid);
				transactionRepository.removeTransaction(globalXid);
			}
		}

		// step2: create confirm-phase-transaction
		try {
			this.beginJtaTransaction();
		} catch (NotSupportedException ex) {
			// TODO
		}

		// step3: confirm
		try {
			transaction.confirm();
		} catch (SystemException ex) {
			// TODO
		} catch (RuntimeException rex) {
			// TODO
		}

		boolean confirmPhaseTransactionDone = true;
		try {
			this.jtaTransactionManager.commit();// TODO
		} catch (CommitRequiredException crex) {
			confirmPhaseTransactionDone = false;
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw crex;
		} catch (HeuristicMixedException hmex) {
			confirmPhaseTransactionDone = false;// TODO
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw hmex;
		} catch (SystemException ex) {
			confirmPhaseTransactionDone = false;
			transactionRepository.putErrorTransaction(globalXid, transaction);
			throw ex;
		} catch (RuntimeException rrex) {
			confirmPhaseTransactionDone = false;
			transactionRepository.putErrorTransaction(globalXid, transaction);
			SystemException ex = new SystemException();
			ex.initCause(rrex);
			throw ex;
		} finally {
			if (confirmPhaseTransactionDone) {
				transactionRepository.removeErrorTransaction(globalXid);
				transactionRepository.removeTransaction(globalXid);
			}
		}

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
		// transaction.setSuspendJtaTransaction(null);
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

	public void rollbackJtaTransaction(CompensableJtaTransaction transaction) throws IllegalStateException,
			SecurityException, SystemException {
		this.jtaTransactionManager.rollback();
	}

	public void rollbackTccTransaction(CompensableTccTransaction transaction) throws IllegalStateException,
			SecurityException, SystemException {

		if (transaction == null) {
			throw new IllegalStateException();
		} else if (transaction.getStatus() == Status.STATUS_ROLLEDBACK) {
			return;
		} else if (transaction.getStatus() == Status.STATUS_COMMITTED) {
			throw new SystemException();
		}

		this.delistCompensableInvocationIfRequired(transaction);

		// TransactionContext transactionContext = transaction.getTransactionContext();
		// TransactionXid globalXid = transactionContext.getGlobalXid();
		// TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		// TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();
		// boolean transactionDone = true;
		// try {
		// transaction.rollback();
		// } catch (RollbackRequiredException rrex) {
		// transactionDone = false;
		// transactionRepository.putErrorTransaction(globalXid, transaction);
		// throw rrex;
		// } catch (SystemException ex) {
		// transactionDone = false;
		// transactionRepository.putErrorTransaction(globalXid, transaction);
		// throw ex;
		// } catch (RuntimeException rrex) {
		// transactionDone = false;
		// transactionRepository.putErrorTransaction(globalXid, transaction);
		// SystemException ex = new SystemException();
		// ex.initCause(rrex);
		// throw ex;
		// } finally {
		// if (transactionDone) {
		// transactionRepository.removeErrorTransaction(globalXid);
		// transactionRepository.removeTransaction(globalXid);
		// }
		// }

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

	public void timingExecution() {
	}

	public void stopTiming(Transaction tx) {
	}

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
