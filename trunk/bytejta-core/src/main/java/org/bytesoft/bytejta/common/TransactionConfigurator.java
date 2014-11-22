package org.bytesoft.bytejta.common;

import org.bytesoft.bytejta.TransactionManagerImpl;
import org.bytesoft.transaction.TransactionTimer;
import org.bytesoft.transaction.logger.TransactionLogger;
import org.bytesoft.transaction.logger.TransactionLoggerProxy;
import org.bytesoft.transaction.rpc.TransactionInterceptor;
import org.bytesoft.transaction.xa.XidFactory;

public final class TransactionConfigurator {
	private static final TransactionConfigurator instance = new TransactionConfigurator();

	private boolean optimizeEnabled = true;
	private TransactionManagerImpl transactionManager;
	private XidFactory xidFactory;
	private TransactionTimer transactionTimer;
	private final TransactionLoggerProxy transactionLogger = new TransactionLoggerProxy();
	private TransactionRepository transactionRepository;
	private TransactionInterceptor transactionInterceptor;

	public static TransactionConfigurator getInstance() {
		return instance;
	}

	public void setTransactionLogger(TransactionLogger transactionLogger) {
		instance.transactionLogger.setDelegate(transactionLogger);
	}

	public TransactionManagerImpl getTransactionManager() {
		return instance.transactionManager;
	}

	public void setTransactionManager(TransactionManagerImpl transactionManager) {
		instance.transactionManager = transactionManager;
	}

	public XidFactory getXidFactory() {
		return instance.xidFactory;
	}

	public void setXidFactory(XidFactory xidFactory) {
		instance.xidFactory = xidFactory;
	}

	public TransactionLogger getTransactionLogger() {
		return instance.transactionLogger;
	}

	public TransactionRepository getTransactionRepository() {
		return instance.transactionRepository;
	}

	public void setTransactionRepository(TransactionRepository transactionRepository) {
		instance.transactionRepository = transactionRepository;
	}

	public TransactionTimer getTransactionTimer() {
		return instance.transactionTimer;
	}

	public void setTransactionTimer(TransactionTimer transactionTimer) {
		instance.transactionTimer = transactionTimer;
	}

	public boolean isOptimizeEnabled() {
		return instance.optimizeEnabled;
	}

	public void setOptimizeEnabled(boolean optimizeEnabled) {
		instance.optimizeEnabled = optimizeEnabled;
	}

	public TransactionInterceptor getTransactionInterceptor() {
		return instance.transactionInterceptor;
	}

	public void setTransactionInterceptor(TransactionInterceptor transactionInterceptor) {
		instance.transactionInterceptor = transactionInterceptor;
	}

}
