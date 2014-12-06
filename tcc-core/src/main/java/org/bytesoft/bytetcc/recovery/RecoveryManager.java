/**
 * Copyright 2014 yangming.liu<liuyangming@gmail.com>.
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
package org.bytesoft.bytetcc.recovery;

import javax.transaction.HeuristicMixedException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.bytesoft.bytetcc.CompensableTransaction;
import org.bytesoft.bytetcc.CompensableTransactionManager;
import org.bytesoft.bytetcc.archive.TransactionArchive;
import org.bytesoft.transaction.TransactionStatistic;
import org.bytesoft.transaction.xa.TransactionXid;

public class RecoveryManager {

	private TransactionManager transactionManager;
	private TransactionStatistic transactionStatistic;

	public void reconstruct() {
		// TransactionRepository repository = this.getTransactionRepository();
		// TransactionLogger transactionLogger = repository.getTransactionLogger();
		// Set<TransactionArchive> transactions = transactionLogger.getLoggedTransactionSet();
		// Iterator<TransactionArchive> itr = transactions.iterator();
		// while (itr.hasNext()) {
		// TransactionArchive archive = itr.next();
		// RecoveredTransactionImpl transaction = this.reconstructTransaction(archive);
		//
		// TransactionContext transactionContext = null;// TODO transaction.getTransactionContext();
		// TransactionXid globalXid = transactionContext.getGlobalXid();
		//
		// repository.putTransaction(globalXid, transaction);
		// repository.putErrorTransaction(globalXid, transaction);
		//
		// // this.transactionStatistic.fireRecoverTransaction(transaction);
		// }
	}

	public RecoveredTransactionImpl reconstructTransaction(TransactionArchive archive) {
		// TransactionRepository repository = this.getTransactionRepository();
		// CompensableTransactionManager txManager = (CompensableTransactionManager) this.transactionManager;

		RecoveredTransactionImpl transaction = new RecoveredTransactionImpl();
		// // transaction.setTransactionStatistic(txManager.getTransactionStatistic());
		//
		// TransactionContext transactionContext = null;// TODO archive.getTransactionContext();
		// // transaction.setTransactionRecovery(true);
		//
		// TransactionStatus transactionStatus = archive.getTransactionStatus();
		//
		// transaction.setTransactionContext(transactionContext);
		// transaction.setTransactionStatus(transactionStatus);
		// transaction.setTransactionManager(txManager);
		// transaction.setTransactionLogger(repository.getTransactionLogger());
		//
		// // transaction.getCompensableArchiveMap().putAll(archive.getCompensableArchiveMap());
		// // TODO
		// // transaction.getAppToTerminatorMap().putAll(archive.getAppToTerminatorMap());
		//
		// if (transactionStatus.isActive()) {
		// transaction.setRecoveryRollbackOnly(true);
		// } else if (transactionStatus.isMarkedRollbackOnly()) {
		// transaction.setRecoveryRollbackOnly(true);
		// } else if (transactionStatus.isPreparing()) {
		// this.confirmPreparedTerminator(transaction);
		// if (transaction.isRecoveryRollbackOnly()) {
		// // ignore
		// } else {
		// this.confirmPreparedLaunchService(transaction);
		// }
		// } else if (transactionStatus.isPrepared()) {
		// this.confirmPreparedLaunchService(transaction);
		// } else if (transactionStatus.isRollingBack()) {
		// transaction.setRecoveryRollbackOnly(true);
		// } else if (transactionStatus.isRolledBack()) {
		// transaction.setRecoveryRollbackOnly(true);
		// }

		return transaction;
	}

	private void confirmPreparedLaunchService(RecoveredTransactionImpl transaction) {

		// Map<XidImpl, CompensableArchive> xidToSvcMap = transaction.getCompensableArchiveMap();
		// Iterator<Map.Entry<XidImpl, CompensableArchive>> itr = xidToSvcMap.entrySet().iterator();
		// while (itr.hasNext()) {
		// Map.Entry<XidImpl, CompensableArchive> entry = itr.next();
		// CompensableArchive holder = entry.getValue();
		// if (holder.launchSvc) {
		// TransactionStatus transactionStatus = transaction.getTransactionStatus();
		// int statusTrace = transactionStatus.getInnerStatusTrace();
		// int xorStatus = TransactionStatus.STATUS_MARKED_ROLLBACK & statusTrace;
		// boolean rollbackRequired = (xorStatus == TransactionStatus.STATUS_MARKED_ROLLBACK);
		//
		// if (rollbackRequired) {
		// transaction.setRecoveryRollbackOnly(true);
		// } else if (holder.tryCommitted) {
		// transaction.setRecoveryRollbackOnly(false);
		// } else {
		// transaction.setRecoveryRollbackOnly(true);
		// }
		// }
		// }
	}

	// TODO
	private void confirmPreparedTerminator(RecoveredTransactionImpl transaction) {
		// Map<String, TerminatorArchive> appToTerminatorMap = transaction.getAppToTerminatorMap();
		// Iterator<Map.Entry<String, TerminatorArchive>> itr = appToTerminatorMap.entrySet().iterator();
		// boolean errorExists = false;
		// while (itr.hasNext()) {
		// Map.Entry<String, TerminatorArchive> entry = itr.next();
		// TerminatorArchive holder = entry.getValue();
		// if (holder.prepared) {
		// // ignore
		// } else {
		// errorExists = true;
		// break;
		// }
		// }
		//
		// if (errorExists) {
		// transaction.setRecoveryRollbackOnly(true);
		// }

	}

	/**
	 * commit/rollback the uncompleted transactions.
	 */
	public void recover() {
//		TransactionRepository repository = this.getTransactionRepository();
		// Iterator<TransactionImpl> itr = repository.getErrorTransactionSet().iterator();
		// while (itr.hasNext()) {
		// TransactionImpl transaction = itr.next();
		// try {
		// this.recoverTransaction(transaction);
		// } catch (HeuristicMixedException ex) {
		// ex.printStackTrace();
		// } catch (SystemException ex) {
		// ex.printStackTrace();
		// } catch (RuntimeException ex) {
		// ex.printStackTrace();
		// }
		// }
	}

	/**
	 * commit/rollback the specific transaction.
	 * 
	 * @param globalXid
	 * @throws HeuristicMixedException
	 * @throws SystemException
	 */
	public void recoverTransaction(TransactionXid globalXid) throws HeuristicMixedException, SystemException {
//		TransactionRepository repository = this.getTransactionRepository();
		// TransactionImpl transaction = repository.getErrorTransaction(globalXid);
		// this.recoverTransaction(transaction);
	}

	public void recoverTransaction(CompensableTransaction transaction) throws HeuristicMixedException, SystemException {
		// CompensableTransactionManager txManager = this.getTransactionManagerImpl();
		// try {
		// txManager.associateTransaction(transaction);
		// if (RecoveredTransactionImpl.class.isInstance(transaction)) {
		// RecoveredTransactionImpl recoveredTransaction = (RecoveredTransactionImpl) transaction;
		// this.recoveredTransactionRecovery(recoveredTransaction);
		// } else {
		// this.activeTransactionRecovery(transaction);
		// }
		// } finally {
		// txManager.unassociateTransaction();
		// }
	}

	private void recoveredTransactionRecovery(RecoveredTransactionImpl transaction) throws HeuristicMixedException,
			SystemException {
//		TransactionContext transactionContext = transaction.getTransactionContext();
//		TransactionStatus transactionStatus = transaction.getTransactionStatus();
//		if (transactionContext.isCoordinator()) {
//			if (transaction.isRecoveryRollbackOnly()) {
//				transaction.rollback();
//			} else {
//				try {
//					transaction.commit();
//				} catch (SecurityException ex) {
//					// ignore
//				} catch (HeuristicRollbackException ex) {
//					// ignore
//				} catch (RollbackException ex) {
//					// ignore
//				}
//			}
//		} else if (transactionStatus.isActive() || transactionStatus.isMarkedRollbackOnly()) {
//			transaction.rollback();
//			// try {
//			// transaction.cleanup();
//			// } catch (Exception rex) {
//			// SystemException exception = new SystemException();
//			// exception.initCause(rex);
//			// throw exception;
//			// }
//		}
	}

	private void activeTransactionRecovery(CompensableTransaction transaction) throws HeuristicMixedException,
			SystemException {
//		TransactionContext transactionContext = transaction.getTransactionContext();
//		if (transactionContext.isCoordinator()) {
//			this.coordinateTransactionRecovery(transaction);
//		}
	}

	private void coordinateTransactionRecovery(CompensableTransaction transaction) throws HeuristicMixedException,
			SystemException {
//		TransactionStatus status = transaction.getTransactionStatus();
//
//		switch (status.getTransactionStatus()) {
//		case Status.STATUS_ACTIVE:
//		case Status.STATUS_MARKED_ROLLBACK:
//			transaction.rollback();
//			break;
//		case Status.STATUS_PREPARING:
//			transaction.rollback();
//			break;
//		case Status.STATUS_PREPARED:
//		case Status.STATUS_COMMITTING:
//			try {
//				transaction.commit();
//			} catch (SecurityException ex) {
//				// ignore
//			} catch (HeuristicRollbackException ex) {
//				// ignore
//			} catch (RollbackException ex) {
//				// ignore
//			}
//			break;
//		case Status.STATUS_ROLLING_BACK:
//			transaction.rollback();
//			break;
//		case Status.STATUS_COMMITTED:
//		case Status.STATUS_ROLLEDBACK:
//			// try {
//			// transaction.cleanup();
//			// } catch (RemoteException rex) {
//			// SystemException exception = new SystemException();
//			// exception.initCause(rex);
//			// throw exception;
//			// } catch (RuntimeException rex) {
//			// SystemException exception = new SystemException();
//			// exception.initCause(rex);
//			// throw exception;
//			// }
//			break;
//		case Status.STATUS_UNKNOWN:
//			// should be processed manually.
//			throw new SystemException();
//		default:
//			throw new SystemException();
//		}
	}

//	public TransactionRepository getTransactionRepository() {
//		CompensableTransactionManager txm = (CompensableTransactionManager) this.transactionManager;
//		return txm.getTransactionRepository();
//	}

	public CompensableTransactionManager getTransactionManagerImpl() {
		return (CompensableTransactionManager) transactionManager;
	}

	public TransactionManager getTransactionManager() {
		return transactionManager;
	}

	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTransactionStatistic(TransactionStatistic transactionStatistic) {
		this.transactionStatistic = transactionStatistic;
	}

}
