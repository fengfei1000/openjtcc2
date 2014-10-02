package org.bytesoft.bytejta.common;

import org.bytesoft.bytejta.TransactionManagerImpl;
import org.bytesoft.transaction.TransactionTimer;
import org.bytesoft.transaction.logger.TransactionLogger;
import org.bytesoft.transaction.logger.TransactionLoggerProxy;
import org.bytesoft.transaction.xa.XidFactory;

public final class TransactionConfigurator {
	private static TransactionConfigurator instance;

	private boolean optimizedEnabled = true;
	private TransactionManagerImpl transactionManager;
	private XidFactory xidFactory;
	private TransactionTimer transactionTimer;
	private final TransactionLoggerProxy transactionLogger = new TransactionLoggerProxy();
	private TransactionRepository transactionRepository;

	private TransactionConfigurator() throws IllegalStateException {
		if (instance == null) {
			initialize(this);
		} else {
			throw new IllegalStateException();
		}
	}

	private static synchronized void initialize(TransactionConfigurator inst) throws IllegalStateException {
		if (instance == null) {
			instance = inst;
		} else {
			throw new IllegalStateException();
		}
	}

	public static TransactionConfigurator getInstance() {
		return getInstance(true);
	}

	public static TransactionConfigurator getInstance(boolean create) {
		if (create) {
			initializeIfRequired();
		}
		return instance;
	}

	private static TransactionConfigurator initializeIfRequired() {
		if (instance == null) {
			try {
				return new TransactionConfigurator();
			} catch (IllegalStateException ex) {
				return instance;
			}
		}
		return instance;
	}

	public void setTransactionLogger(TransactionLogger transactionLogger) {
		this.transactionLogger.setDelegate(transactionLogger);
	}

	public TransactionManagerImpl getTransactionManager() {
		return transactionManager;
	}

	public void setTransactionManager(TransactionManagerImpl transactionManager) {
		this.transactionManager = transactionManager;
	}

	public XidFactory getXidFactory() {
		return xidFactory;
	}

	public void setXidFactory(XidFactory xidFactory) {
		this.xidFactory = xidFactory;
	}

	public TransactionLogger getTransactionLogger() {
		return transactionLogger;
	}

	public TransactionRepository getTransactionRepository() {
		return transactionRepository;
	}

	public void setTransactionRepository(TransactionRepository transactionRepository) {
		this.transactionRepository = transactionRepository;
	}

	public TransactionTimer getTransactionTimer() {
		return transactionTimer;
	}

	public void setTransactionTimer(TransactionTimer transactionTimer) {
		this.transactionTimer = transactionTimer;
	}

	public boolean isOptimizedEnabled() {
		return optimizedEnabled;
	}

	public void setOptimizedEnabled(boolean optimizedEnabled) {
		this.optimizedEnabled = optimizedEnabled;
	}

}
