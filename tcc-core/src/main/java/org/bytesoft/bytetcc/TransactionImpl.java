package org.bytesoft.bytetcc;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import org.bytesoft.bytetcc.jta.JtaTransaction;

public class TransactionImpl extends JtaTransaction implements Transaction {

	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
		// TODO Auto-generated method stub

	}

	public boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException, SystemException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean enlistResource(XAResource xaRes) throws RollbackException, IllegalStateException, SystemException {
		// TODO Auto-generated method stub
		return false;
	}

	public int getStatus() throws SystemException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void registerSynchronization(Synchronization sync) throws RollbackException, IllegalStateException,
			SystemException {
		// TODO Auto-generated method stub

	}

	public void rollback() throws IllegalStateException, SystemException {
		// TODO Auto-generated method stub

	}

	public void setRollbackOnly() throws IllegalStateException, SystemException {
		// TODO Auto-generated method stub

	}

}
