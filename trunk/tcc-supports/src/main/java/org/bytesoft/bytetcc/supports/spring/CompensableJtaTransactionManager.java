package org.bytesoft.bytetcc.supports.spring;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.bytesoft.bytetcc.TransactionManagerImpl;
import org.bytesoft.bytetcc.jta.JtaTransaction;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.jta.JtaTransactionObject;

public class CompensableJtaTransactionManager extends JtaTransactionManager {
	private static final long serialVersionUID = 1L;

	protected void doJtaBegin(JtaTransactionObject txObject, TransactionDefinition definition)
			throws NotSupportedException, SystemException {
		TransactionManager txManager = this.getTransactionManager();
		if (txManager == null) {
			throw new IllegalStateException();
		} else if (TransactionManagerImpl.class.isInstance(txManager) == false) {
			throw new IllegalStateException();
		}
		org.bytesoft.bytetcc.jta.JtaTransactionManager transactionManager = (org.bytesoft.bytetcc.jta.JtaTransactionManager) txManager;
		JtaTransaction existsTransaction = transactionManager.getCurrentTransaction();
		super.doJtaBegin(txObject, definition);
		if (existsTransaction == null) {
			JtaTransaction transaction = transactionManager.getCurrentTransaction();
			transaction.afterInitialization(null);
		} else {
			// TransactionContext transactionContext = existsTransaction.getTransactionContext();
			// boolean compensable = transactionContext.isCompensable();
			// boolean coordinator = transactionContext.isCoordinator();
			// if (compensable && coordinator == false) {
			// JtaTransaction transaction = transactionManager.getCurrentTransaction();
			// transaction.afterInitialization(null);
			// }
		}
	}

}
