package org.bytesoft.bytetcc;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.bytesoft.bytetcc.jta.JtaTransaction;
import org.bytesoft.bytetcc.jta.JtaTransactionManager;

public class TransactionManagerImpl extends JtaTransactionManager implements TransactionManager {

	public void begin() throws NotSupportedException, SystemException {
		// TODO Auto-generated method stub

	}

	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
		// TODO Auto-generated method stub

	}

	public int getStatus() throws SystemException {
		// TODO Auto-generated method stub
		return 0;
	}

	public TransactionImpl getTransaction() throws SystemException {
		// TODO Auto-generated method stub
		return null;
	}

	public TransactionImpl getCurrentTransaction() {
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

}
