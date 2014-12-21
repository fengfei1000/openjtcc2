package org.bytesoft.bytetcc.xa;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.bytesoft.bytetcc.CompensableTccTransaction;
import org.bytesoft.bytetcc.CompensableTransaction;

public class CompensableTccTransactionSkeleton implements CompensableTransactionSkeleton {

	private final CompensableTccTransaction transaction;

	public CompensableTccTransactionSkeleton(CompensableTccTransaction tx) {
		this.transaction = tx;
	}

	public void commit(Xid xid, boolean onePhase) throws XAException {
		// TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		// TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();
		// boolean transactionDone = true;
		// TransactionContext transactionContext = this.transaction.getTransactionContext();
		// TransactionXid globalXid = transactionContext.getGlobalXid();
		// try {
		// if (onePhase) {
		// this.transaction.commit();
		// } else {
		// this.transaction.participantCommit();
		// }
		// } catch (SecurityException ignore) {
		// transactionDone = false;
		// transactionRepository.putErrorTransaction(globalXid, transaction);
		// throw new XAException(XAException.XAER_RMERR);
		// } catch (IllegalStateException ignore) {
		// transactionDone = false;
		// transactionRepository.putErrorTransaction(globalXid, transaction);
		// throw new XAException(XAException.XAER_RMERR);
		// } catch (CommitRequiredException ignore) {
		// transactionDone = false;
		// transactionRepository.putErrorTransaction(globalXid, transaction);
		// throw new XAException(XAException.XAER_RMERR);
		// } catch (RollbackException ignore) {
		// throw new XAException(XAException.XA_HEURRB);
		// } catch (HeuristicMixedException ignore) {
		// transactionDone = false;// TODO
		// transactionRepository.putErrorTransaction(globalXid, transaction);
		// throw new XAException(XAException.XA_HEURMIX);
		// } catch (HeuristicRollbackException ignore) {
		// throw new XAException(XAException.XA_HEURRB);
		// } catch (SystemException ignore) {
		// transactionDone = false;
		// transactionRepository.putErrorTransaction(globalXid, transaction);
		// throw new XAException(XAException.XAER_RMERR);
		// } finally {
		// if (transactionDone) {
		// transactionRepository.removeErrorTransaction(globalXid);
		// transactionRepository.removeTransaction(globalXid);
		// }
		// }
	}

	public void end(Xid xid, int flags) throws XAException {
	}

	public void forget(Xid xid) throws XAException {
	}

	public int getTransactionTimeout() throws XAException {
		return 0;
	}

	public boolean isSameRM(XAResource xares) throws XAException {
		return false;
	}

	public int prepare(Xid xid) throws XAException {
		// try {
		// this.transaction.participantPrepare();
		// } catch (CommitRequiredException crex) {
		// return XAResource.XA_OK;
		// } catch (RollbackRequiredException rrex) {
		// throw new XAException(XAException.XAER_RMERR);
		// }
		return XAResource.XA_RDONLY;
	}

	public Xid[] recover(int flag) throws XAException {
		return null;
	}

	public void rollback(Xid xid) throws XAException {
		// TransactionContext transactionContext = transaction.getTransactionContext();
		// TransactionXid globalXid = transactionContext.getGlobalXid();
		// TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		// TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();
		// boolean transactionDone = true;
		// try {
		// this.transaction.rollback();
		// } catch (RollbackRequiredException rrex) {
		// transactionDone = false;
		// transactionRepository.putErrorTransaction(globalXid, transaction);
		// throw new XAException(XAException.XAER_RMERR);
		// } catch (SystemException ex) {
		// transactionDone = false;
		// transactionRepository.putErrorTransaction(globalXid, transaction);
		// throw new XAException(XAException.XAER_RMERR);
		// } catch (RuntimeException rrex) {
		// transactionDone = false;
		// transactionRepository.putErrorTransaction(globalXid, transaction);
		// SystemException ex = new SystemException();
		// ex.initCause(rrex);
		// throw new XAException(XAException.XAER_RMERR);
		// } finally {
		// if (transactionDone) {
		// transactionRepository.removeErrorTransaction(globalXid);
		// transactionRepository.removeTransaction(globalXid);
		// }
		// }
	}

	public boolean setTransactionTimeout(int seconds) throws XAException {
		return false;
	}

	public void start(Xid xid, int flags) throws XAException {
	}

	public CompensableTransaction getTransaction() {
		return transaction;
	}

}
