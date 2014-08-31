package org.bytesoft.bytejta;

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

import org.bytesoft.bytejta.common.TransactionConfigurator;
import org.bytesoft.bytejta.common.TransactionRepository;
import org.bytesoft.bytejta.common.XidImpl;
import org.bytesoft.transaction.AssociatedContext;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.xa.XidFactory;

public class TransactionManagerImpl implements TransactionManager {

	private int transactionTimeout = 5 * 60;
	private final Map<Thread, AssociatedContext<TransactionImpl>> associateds = new ConcurrentHashMap<Thread, AssociatedContext<TransactionImpl>>();

	public void begin() throws NotSupportedException, SystemException {
		if (this.getTransaction() != null) {
			throw new NotSupportedException();
		}

		int timeoutSeconds = this.transactionTimeout;

		TransactionContext transactionContext = new TransactionContext();
		transactionContext.setCoordinator(true);
		long createdTime = System.currentTimeMillis();
		long expiredTime = createdTime + (timeoutSeconds * 1000L);
		transactionContext.setCreatedTime(createdTime);
		transactionContext.setExpiredTime(expiredTime);
		// transactionContext.setTerminalKey(this.getTerminalKey());// TODO
		XidFactory xidFactory = TransactionConfigurator.getInstance().getXidFactory();
		XidImpl globalXid = xidFactory.createGlobalXid();
		// transactionContext.setGlobalXid(globalXid);
		// transactionContext.setCreationXid(globalXid);
		transactionContext.setCurrentXid(globalXid);

		TransactionImpl transaction = new TransactionImpl(transactionContext);
		// transaction.setTransactionStatistic(this.transactionStatistic);

		// transaction.setTransactionTimeout(timeoutSeconds);
		// transaction.setTransactionManager(this);
		// transaction.setTransactionLogger(this.transactionRepository.getTransactionLogger());

		// transaction.associateInternalTransaction();

		AssociatedContext<TransactionImpl> actx = new AssociatedContext<TransactionImpl>();
		actx.setTransaction(transaction);
		actx.setThread(Thread.currentThread());

		this.associateds.put(Thread.currentThread(), actx);
		TransactionRepository.getInstance().putTransaction(transactionContext.getGlobalXid(), transaction);
		// this.transactionStatistic.fireBeginTransaction(transaction);

		// TransactionLogger transactionLogger = this.transactionRepository.getTransactionLogger();
		// transactionLogger.beginTransaction(transaction);

	}

	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
		AssociatedContext<TransactionImpl> associated = this.associateds.remove(Thread.currentThread());
		Transaction transaction = associated == null ? null : associated.getTransaction();
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

		try {
			transaction.commit();
		} catch (RollbackException ex) {
			throw ex;
		} catch (HeuristicMixedException ex) {
			throw ex;
		} catch (HeuristicRollbackException ex) {
			throw ex;
		} catch (SecurityException ex) {
			throw ex;
		} catch (IllegalStateException ex) {
			throw ex;
		} catch (SystemException ex) {
			throw ex;
		} catch (RuntimeException ex) {
			throw ex;
		}

	}

	public int getStatus() throws SystemException {
		Transaction transaction = this.getTransaction();
		return transaction == null ? Status.STATUS_NO_TRANSACTION : transaction.getStatus();
	}

	public TransactionImpl getTransaction() throws SystemException {
		AssociatedContext<TransactionImpl> associated = this.associateds.get(Thread.currentThread());
		return associated == null ? null : associated.getTransaction();
	}

	public void resume(Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {

		if (TransactionImpl.class.isInstance(tobj) == false) {
			throw new InvalidTransactionException();
		} else if (this.getTransaction() != null) {
			throw new IllegalStateException();
		}

		AssociatedContext<TransactionImpl> associated = new AssociatedContext<TransactionImpl>();
		associated.setTransaction((TransactionImpl) tobj);
		associated.setThread(Thread.currentThread());
		this.associateds.put(Thread.currentThread(), associated);

	}

	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		AssociatedContext<TransactionImpl> associated = this.associateds.remove(Thread.currentThread());
		Transaction transaction = associated == null ? null : associated.getTransaction();
		if (transaction == null) {
			throw new IllegalStateException();
		} else if (transaction.getStatus() == Status.STATUS_ROLLEDBACK) {
			return;
		} else if (transaction.getStatus() == Status.STATUS_COMMITTED) {
			throw new SystemException();
		}

		try {
			transaction.rollback();
		} catch (IllegalStateException ex) {
			throw ex;
		} catch (SystemException ex) {
			throw ex;
		} catch (RuntimeException ex) {
			throw ex;
		}
	}

	public void setRollbackOnly() throws IllegalStateException, SystemException {
		TransactionImpl transaction = this.getTransaction();
		if (transaction == null) {
			throw new SystemException();
		}
		transaction.setRollbackOnly();
	}

	public void setTransactionTimeout(int seconds) throws SystemException {
		TransactionImpl transaction = this.getTransaction();
		if (transaction == null) {
			throw new SystemException();
		} else if (seconds < 0) {
			throw new SystemException();
		} else if (seconds == 0) {
			// ignore
		} else {
			TransactionContext transactionContext = transaction.getTransactionContext();
			long createdTime = transactionContext.getCreatedTime();
			transactionContext.setExpiredTime(createdTime + seconds * 1000L);
		}
	}

	public TransactionImpl suspend() throws SystemException {
		AssociatedContext<TransactionImpl> associated = this.associateds.remove(Thread.currentThread());
		return associated == null ? null : associated.getTransaction();
	}

	public void initialize() {
		TransactionConfigurator.getInstance().setTransactionManager(this);
	}

}
