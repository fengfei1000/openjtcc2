package org.bytesoft.bytetcc.common;

import javax.transaction.xa.XAResource;

import org.bytesoft.bytetcc.CompensableInvocationExecutor;
import org.bytesoft.bytetcc.CompensableTransactionManager;
import org.bytesoft.bytetcc.supports.CompensableTransactionLogger;
import org.bytesoft.transaction.recovery.TransactionRecovery;
import org.bytesoft.transaction.rpc.TransactionInterceptor;
import org.bytesoft.transaction.xa.XidFactory;

public final class TransactionConfigurator {
	private static final TransactionConfigurator instance = new TransactionConfigurator();

	private boolean optimizeEnabled = true;
	private CompensableTransactionManager transactionManager;
	private XidFactory xidFactory;
	private CompensableTransactionLogger transactionLogger = CompensableTransactionLogger.defaultTransactionLogger;
	private TransactionRepository transactionRepository;
	private TransactionInterceptor transactionInterceptor;
	private TransactionRecovery transactionRecovery;
	private XAResource transactionSkeleton;
	private CompensableInvocationExecutor compensableInvocationExecutor;

	public static TransactionConfigurator getInstance() {
		return instance;
	}

	public void setTransactionLogger(CompensableTransactionLogger transactionLogger) {
		this.transactionLogger = transactionLogger;
	}

	public CompensableTransactionManager getTransactionManager() {
		if (this == instance) {
			return this.transactionManager;
		} else {
			return instance.getTransactionManager();
		}
	}

	public void setTransactionManager(CompensableTransactionManager transactionManager) {
		if (this == instance) {
			this.transactionManager = transactionManager;
		} else {
			instance.setTransactionManager(transactionManager);
		}
	}

	public XidFactory getXidFactory() {
		if (this == instance) {
			return this.xidFactory;
		} else {
			return instance.getXidFactory();
		}
	}

	public void setXidFactory(XidFactory xidFactory) {
		if (this == instance) {
			this.xidFactory = xidFactory;
		} else {
			instance.setXidFactory(xidFactory);
		}
	}

	public CompensableTransactionLogger getTransactionLogger() {
		return this.transactionLogger;
	}

	public TransactionRepository getTransactionRepository() {
		if (this == instance) {
			return this.transactionRepository;
		} else {
			return instance.getTransactionRepository();
		}
	}

	public void setTransactionRepository(TransactionRepository transactionRepository) {
		if (this == instance) {
			this.transactionRepository = transactionRepository;
		} else {
			instance.setTransactionRepository(transactionRepository);
		}
	}

	public boolean isOptimizeEnabled() {
		if (this == instance) {
			return this.optimizeEnabled;
		} else {
			return instance.isOptimizeEnabled();
		}
	}

	public void setOptimizeEnabled(boolean optimizeEnabled) {
		if (this == instance) {
			this.optimizeEnabled = optimizeEnabled;
		} else {
			instance.setOptimizeEnabled(optimizeEnabled);
		}
	}

	public TransactionInterceptor getTransactionInterceptor() {
		if (this == instance) {
			return this.transactionInterceptor;
		} else {
			return instance.getTransactionInterceptor();
		}
	}

	public void setTransactionInterceptor(TransactionInterceptor transactionInterceptor) {
		if (this == instance) {
			this.transactionInterceptor = transactionInterceptor;
		} else {
			instance.setTransactionInterceptor(transactionInterceptor);
		}
	}

	public TransactionRecovery getTransactionRecovery() {
		if (this == instance) {
			return this.transactionRecovery;
		} else {
			return instance.getTransactionRecovery();
		}
	}

	public void setTransactionRecovery(TransactionRecovery transactionRecovery) {
		if (this == instance) {
			this.transactionRecovery = transactionRecovery;
		} else {
			instance.setTransactionRecovery(transactionRecovery);
		}
	}

	public CompensableInvocationExecutor getCompensableInvocationExecutor() {
		if (this == instance) {
			return this.compensableInvocationExecutor;
		} else {
			return instance.getCompensableInvocationExecutor();
		}
	}

	public void setCompensableInvocationExecutor(CompensableInvocationExecutor compensableInvocationExecutor) {
		if (this == instance) {
			this.compensableInvocationExecutor = compensableInvocationExecutor;
		} else {
			instance.setCompensableInvocationExecutor(compensableInvocationExecutor);
		}
	}

	public XAResource getTransactionSkeleton() {
		if (this == instance) {
			return this.transactionSkeleton;
		} else {
			return instance.getTransactionSkeleton();
		}
	}

	public void setTransactionSkeleton(XAResource transactionSkeleton) {
		if (this == instance) {
			this.transactionSkeleton = transactionSkeleton;
		} else {
			instance.setTransactionSkeleton(transactionSkeleton);
		}
	}

}
