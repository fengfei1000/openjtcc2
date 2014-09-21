package org.bytesoft.bytejta;

import java.util.Iterator;
import java.util.List;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;

import org.bytesoft.bytejta.common.TransactionConfigurator;
import org.bytesoft.bytejta.common.TransactionRepository;
import org.bytesoft.bytejta.common.TransactionXid;
import org.bytesoft.bytejta.xa.XATerminatorImpl;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.archive.TransactionArchive;
import org.bytesoft.transaction.archive.XAResourceArchive;
import org.bytesoft.transaction.logger.TransactionLogger;
import org.bytesoft.transaction.recovery.TransactionRecovery;

public class TransactionRecoveryImpl implements TransactionRecovery {

	public synchronized void timingRecover() {
		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();
		List<TransactionImpl> transactions = transactionRepository.getErrorTransactionList();
		for (int i = 0; i < transactions.size(); i++) {
			TransactionImpl transaction = transactions.get(i);
			TransactionContext transactionContext = transaction.getTransactionContext();
			int status = transaction.getStatus();

			switch (status) {
			case Status.STATUS_ACTIVE:
			case Status.STATUS_MARKED_ROLLBACK:
			case Status.STATUS_PREPARING:
			case Status.STATUS_ROLLING_BACK:
			case Status.STATUS_UNKNOWN:
				try {
					transaction.rollback();
					TransactionXid globalXid = transactionContext.getGlobalXid();
					transactionRepository.removeErrorTransaction(globalXid);
					transactionRepository.removeTransaction(globalXid);
				} catch (Exception ex) {
					// ignore
				}
			case Status.STATUS_PREPARED:
			case Status.STATUS_COMMITTING:
				try {
					transaction.commit();
					TransactionXid globalXid = transactionContext.getGlobalXid();
					transactionRepository.removeErrorTransaction(globalXid);
					transactionRepository.removeTransaction(globalXid);
				} catch (Exception ex) {
					// ignore
				}
			case Status.STATUS_COMMITTED:
			case Status.STATUS_ROLLEDBACK:
			default:
				// ignore
			}
		}
	}

	public synchronized void startupRecover() {
		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();
		TransactionLogger transactionLogger = transactionConfigurator.getTransactionLogger();
		List<TransactionArchive> archives = transactionLogger.getTransactionArchiveList();
		for (int i = 0; i < archives.size(); i++) {
			TransactionArchive archive = archives.get(i);
			TransactionImpl transaction = null;
			try {
				transaction = this.reconstructTransaction(archive);
			} catch (IllegalStateException ex) {
				continue;
			}
			TransactionContext transactionContext = transaction.getTransactionContext();
			TransactionXid globalXid = transactionContext.getGlobalXid();
			transactionRepository.putTransaction(globalXid, transaction);
			transactionRepository.putErrorTransaction(globalXid, transaction);
		}
	}

	private TransactionImpl reconstructTransaction(TransactionArchive archive) throws IllegalStateException {
		TransactionContext transactionContext = new TransactionContext();
		transactionContext.setCurrentXid((TransactionXid) archive.getXid());
		transactionContext.setRecovery(true);
		transactionContext.setOptimized(archive.isOptimized());
		transactionContext.setCoordinator(archive.isCoordinator());
		transactionContext.setCompensable(archive.isCompensable());

		TransactionImpl transaction = new TransactionImpl(transactionContext);
		transaction.setTransactionStatus(archive.getStatus());

		XATerminatorImpl nativeTerminator = transaction.getNativeTerminator();
		List<XAResourceArchive> nativeResources = archive.getNativeResources();
		nativeTerminator.initializeForRecovery(nativeResources);

		XATerminatorImpl remoteTerminator = transaction.getRemoteTerminator();
		List<XAResourceArchive> remoteResources = archive.getRemoteResources();
		remoteTerminator.initializeForRecovery(remoteResources);

		this.analysisTransaction(transaction, archive.getVote());

		return transaction;
	}

	private void analysisTransaction(TransactionImpl transaction, int transactionVote) throws IllegalStateException {
		if (transactionVote == XAResource.XA_RDONLY) {
			throw new IllegalStateException();
		}
		// else if (transactionVote == -1) {
		// // rollback.
		// } else {
		// List<XAResourceArchive> nativeResources = transaction.getNativeTerminator().getResourceArchives();
		// for (int i = 0; i < nativeResources.size(); i++) {
		// XAResourceArchive archive = nativeResources.get(i);
		// }
		// }
	}

}
