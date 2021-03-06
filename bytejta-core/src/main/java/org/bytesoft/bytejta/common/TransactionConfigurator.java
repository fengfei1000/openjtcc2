/**
 * Copyright 2014-2015 yangming.liu<liuyangming@gmail.com>.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, see <http://www.gnu.org/licenses/>.
 */
package org.bytesoft.bytejta.common;

import javax.transaction.xa.XAResource;

import org.bytesoft.bytejta.TransactionManagerImpl;
import org.bytesoft.transaction.TransactionTimer;
import org.bytesoft.transaction.logger.TransactionLogger;
import org.bytesoft.transaction.logger.TransactionLoggerProxy;
import org.bytesoft.transaction.recovery.TransactionRecovery;
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
	private TransactionRecovery transactionRecovery;
	private XAResource transactionSkeleton;

	public static TransactionConfigurator getInstance() {
		return instance;
	}

	public void setTransactionLogger(TransactionLogger transactionLogger) {
		if (this == instance) {
			this.transactionLogger.setDelegate(transactionLogger);
		} else {
			instance.setTransactionLogger(transactionLogger);
		}
	}

	public TransactionManagerImpl getTransactionManager() {
		if (this == instance) {
			return this.transactionManager;
		} else {
			return instance.getTransactionManager();
		}
	}

	public void setTransactionManager(TransactionManagerImpl transactionManager) {
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

	public TransactionLogger getTransactionLogger() {
		if (this == instance) {
			return this.transactionLogger;
		} else {
			return instance.getTransactionLogger();
		}
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

	public TransactionTimer getTransactionTimer() {
		if (this == instance) {
			return this.transactionTimer;
		} else {
			return instance.getTransactionTimer();
		}
	}

	public void setTransactionTimer(TransactionTimer transactionTimer) {
		if (this == instance) {
			this.transactionTimer = transactionTimer;
		} else {
			instance.setTransactionTimer(transactionTimer);
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
