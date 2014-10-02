package org.bytesoft.bytejta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import org.bytesoft.transaction.CommitRequiredException;
import org.bytesoft.transaction.RollbackRequiredException;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.TransactionTimer;
import org.bytesoft.transaction.xa.TransactionXid;
import org.bytesoft.transaction.xa.XidFactory;

public class TransactionManagerImpl implements TransactionManager, TransactionTimer {

	private int transactionTimeout = 5 * 60;
	private final Map<Thread, TransactionImpl> associateds = new ConcurrentHashMap<Thread, TransactionImpl>();

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
		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		XidFactory xidFactory = transactionConfigurator.getXidFactory();
		TransactionXid globalXid = xidFactory.createGlobalXid();
		transactionContext.setCurrentXid(globalXid);

		TransactionImpl transaction = new TransactionImpl(transactionContext);

		transaction.setThread(Thread.currentThread());
		this.associateds.put(Thread.currentThread(), transaction);
		TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();
		transactionRepository.putTransaction(transactionContext.getGlobalXid(), transaction);
		// this.transactionStatistic.fireBeginTransaction(transaction);

	}

	public void propagationBegin(TransactionContext transactionContext) throws NotSupportedException, SystemException {

		if (this.getTransaction() != null) {
			throw new NotSupportedException();
		}

		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();

		TransactionXid propagationXid = transactionContext.getCurrentXid();
		TransactionXid globalXid = propagationXid.getGlobalXid();
		TransactionImpl transaction = transactionRepository.getTransaction(globalXid);
		if (transaction == null) {
			transaction = new TransactionImpl(transactionContext);

			// long createdTime = transactionContext.getCreatedTime();
			// long expiredTime = transactionContext.getExpiredTime();
			// transactionContext.setCreatedTime(createdTime);
			// transactionContext.setExpiredTime(expiredTime);

			transaction.setThread(Thread.currentThread());
			transactionRepository.putTransaction(transactionContext.getGlobalXid(), transaction);
		}

		this.associateds.put(Thread.currentThread(), transaction);
		// this.transactionStatistic.fireBeginTransaction(transaction);

	}

	public void propagationFinish(TransactionContext transactionContext) throws SystemException {
		this.associateds.remove(Thread.currentThread());
	}

	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
		TransactionImpl transaction = this.associateds.remove(Thread.currentThread());
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

		TransactionContext transactionContext = transaction.getTransactionContext();
		TransactionXid globalXid = transactionContext.getGlobalXid();
		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();
		boolean transactionDone = true;
		try {
			transaction.commit();
		} catch (CommitRequiredException crex) {
			transactionDone = false;
			transactionRepository.putErrorTransaction(globalXid, transaction);
		} catch (HeuristicMixedException hmex) {// TODO
			transactionDone = false;
			transactionRepository.putErrorTransaction(globalXid, transaction);
		} catch (RuntimeException rrex) {
			transactionDone = false;
			transactionRepository.putErrorTransaction(globalXid, transaction);
		} finally {
			if (transactionDone) {
				transactionRepository.removeErrorTransaction(globalXid);
				transactionRepository.removeTransaction(globalXid);
			}
		}

	}

	public int getStatus() throws SystemException {
		Transaction transaction = this.getTransaction();
		return transaction == null ? Status.STATUS_NO_TRANSACTION : transaction.getStatus();
	}

	public TransactionImpl getTransaction() throws SystemException {
		return this.associateds.get(Thread.currentThread());
	}

	public void resume(Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {

		if (TransactionImpl.class.isInstance(tobj) == false) {
			throw new InvalidTransactionException();
		} else if (this.getTransaction() != null) {
			throw new IllegalStateException();
		}

		TransactionImpl transaction = (TransactionImpl) tobj;
		transaction.setThread(Thread.currentThread());
		transaction.resume();
		this.associateds.put(Thread.currentThread(), transaction);

	}

	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		TransactionImpl transaction = this.associateds.remove(Thread.currentThread());
		if (transaction == null) {
			throw new IllegalStateException();
		} else if (transaction.getStatus() == Status.STATUS_ROLLEDBACK) {
			return;
		} else if (transaction.getStatus() == Status.STATUS_COMMITTED) {
			throw new SystemException();
		}

		TransactionContext transactionContext = transaction.getTransactionContext();
		TransactionXid globalXid = transactionContext.getGlobalXid();
		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();
		boolean transactionDone = true;
		try {
			transaction.rollback();
		} catch (RollbackRequiredException rrex) {
			transactionDone = false;
			transactionRepository.putErrorTransaction(globalXid, transaction);
		} catch (RuntimeException rrex) {
			transactionDone = false;
			transactionRepository.putErrorTransaction(globalXid, transaction);
		} finally {
			if (transactionDone) {
				transactionRepository.removeErrorTransaction(globalXid);
				transactionRepository.removeTransaction(globalXid);
			}
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
			synchronized (transaction) {
				TransactionContext transactionContext = transaction.getTransactionContext();
				long createdTime = transactionContext.getCreatedTime();
				transactionContext.setExpiredTime(createdTime + seconds * 1000L);
			}
		}
	}

	public TransactionImpl suspend() throws SystemException {
		TransactionImpl transaction = this.associateds.remove(Thread.currentThread());
		transaction.suspend();
		return transaction;
	}

	public void timingExecution() {
		List<TransactionImpl> expiredTransactions = new ArrayList<TransactionImpl>();
		List<TransactionImpl> activeTransactions = new ArrayList<TransactionImpl>(this.associateds.values());
		long current = System.currentTimeMillis();
		Iterator<TransactionImpl> activeItr = activeTransactions.iterator();
		while (activeItr.hasNext()) {
			TransactionImpl transaction = activeItr.next();
			synchronized (transaction) {
				if (transaction.isTiming()) {
					TransactionContext transactionContext = transaction.getTransactionContext();
					long expired = transactionContext.getExpiredTime();
					if (expired <= current) {
						expiredTransactions.add(transaction);
					}
				}// end-if (transaction.isTiming())
			}// end-synchronized
		}

		Iterator<TransactionImpl> expiredItr = expiredTransactions.iterator();
		while (activeItr.hasNext()) {
			TransactionImpl transaction = expiredItr.next();
			if (transaction.getStatus() == Status.STATUS_ROLLEDBACK) {
				// ignore
			} else if (transaction.getStatus() == Status.STATUS_COMMITTED) {
				// ignore
			} else {
				TransactionContext transactionContext = transaction.getTransactionContext();
				TransactionXid globalXid = transactionContext.getGlobalXid();
				TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
				TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();
				try {
					transaction.rollback();
					transactionRepository.removeTransaction(globalXid);
				} catch (Exception ex) {
					transactionRepository.putErrorTransaction(globalXid, transaction);
				}
			}// end-else
		}// end-while
	}

	public void stopTiming(Transaction tx) {
		if (TransactionImpl.class.isInstance(tx) == false) {
			return;
		}

		TransactionImpl transaction = (TransactionImpl) tx;
		synchronized (transaction) {
			transaction.setTiming(false);
		}
	}

}
