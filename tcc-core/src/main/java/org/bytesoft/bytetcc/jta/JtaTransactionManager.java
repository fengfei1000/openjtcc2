package org.bytesoft.bytetcc.jta;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.bytesoft.bytetcc.supports.TransactionRepository;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.TransactionStatus;
import org.bytesoft.transaction.xa.TransactionXid;
import org.bytesoft.transaction.xa.XidFactory;

public class JtaTransactionManager implements TransactionManager {
	protected int transactionTimeout = 5 * 60;
	protected XidFactory xidFactory;
	protected TransactionRepository transactionRepository;

	// private final Map<Thread, AssociatedContext> threadToContextMap = new ConcurrentHashMap<Thread,
	// AssociatedContext>();

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
		TransactionXid globalXid = this.xidFactory.createGlobalXid();
		transactionContext.setCurrentXid(globalXid);

		JtaTransaction transaction = new JtaTransaction();
		// transaction.setTransactionStatistic(this.transactionStatistic);

		transaction.setTransactionContext(transactionContext);
		transaction.setTransactionStatus(new TransactionStatus());
		transaction.setTransactionManager(this);
		transaction.setTransactionLogger(this.transactionRepository.getTransactionLogger());

		// AssociatedContext associated = new AssociatedContext();
		// associated.setTransaction(transaction);
		// associated.setThread(Thread.currentThread());

		// this.threadToContextMap.put(Thread.currentThread(), associated);
		this.transactionRepository.putTransaction(transactionContext.getGlobalXid(), transaction);
		// this.transactionStatistic.fireBeginTransaction(transaction);

		// TransactionLogger transactionLogger = this.transactionRepository.getTransactionLogger();
		// transactionLogger.beginTransaction(transaction);

	}

	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
		// TODO Auto-generated method stub

	}

	public int getStatus() throws SystemException {
		// TODO Auto-generated method stub
		return 0;
	}

	public JtaTransaction getTransaction() throws SystemException {
		// TODO Auto-generated method stub
		return null;
	}

	public JtaTransaction getCurrentTransaction() {
		// TODO Auto-generated method stub
		return null;
	}

	public void resume(Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {
		// TODO Auto-generated method stub

	}

	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		// TODO Auto-generated method stub

	}

	public void setRollbackOnly() throws IllegalStateException, SystemException {
		// TODO Auto-generated method stub

	}

	public void setTransactionTimeout(int seconds) throws SystemException {
		// TODO Auto-generated method stub

	}

	public Transaction suspend() throws SystemException {
		// TODO Auto-generated method stub
		return null;
	}

	public XidFactory getXidFactory() {
		return xidFactory;
	}

	public void setXidFactory(XidFactory xidFactory) {
		this.xidFactory = xidFactory;
	}

	public TransactionRepository getTransactionRepository() {
		return transactionRepository;
	}

	public void setTransactionRepository(TransactionRepository transactionRepository) {
		this.transactionRepository = transactionRepository;
	}

}
