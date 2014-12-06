package org.bytesoft.bytetcc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.bytesoft.bytejta.TransactionManagerImpl;
import org.bytesoft.bytetcc.common.TransactionConfigurator;
import org.bytesoft.bytetcc.common.TransactionRepository;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.xa.TransactionXid;
import org.bytesoft.transaction.xa.XidFactory;

public class CompensableTransactionManager implements TransactionManager {

	private boolean transactionManagerInitialized = false;
	private TransactionManagerImpl jtaTransactionManager;

	private int timeoutSeconds = 5 * 60;
	private final Map<Thread, CompensableTransaction> compensables = new ConcurrentHashMap<Thread, CompensableTransaction>();

	public void begin() throws NotSupportedException, SystemException {

		this.initializeTransactionManagerIfRequired();

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
		TransactionXid xid = xidFactory.createGlobalXid();
		TransactionXid globalXid = xidFactory.createBranchXid(xid, xid.getGlobalTransactionId());
		transactionContext.setCurrentXid(globalXid);

		CompensableTransaction transaction = new CompensableTransaction(transactionContext);
		TransactionRepository transactionRepository = configurator.getTransactionRepository();

		this.jtaTransactionManager.propagationBegin(transactionContext);

		this.compensables.put(Thread.currentThread(), transaction);
		transactionRepository.putTransaction(transactionContext.getGlobalXid(), transaction);
		
		configurator.getTransactionLogger();
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

	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
		// TODO Auto-generated method stub

	}

	public int getStatus() throws SystemException {
		// TODO Auto-generated method stub
		return 0;
	}

	public CompensableTransaction getTransaction() throws SystemException {
		// TODO Auto-generated method stub
		return null;
	}

	public CompensableTransaction getCurrentTransaction() {
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
