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

import java.util.List;

import javax.transaction.Status;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.log4j.Logger;
import org.bytesoft.bytejta.TransactionImpl;
import org.bytesoft.bytetcc.CompensableJtaTransaction;
import org.bytesoft.bytetcc.CompensableTccTransaction;
import org.bytesoft.bytetcc.CompensableTransaction;
import org.bytesoft.bytetcc.CompensableTransactionManager;
import org.bytesoft.bytetcc.archive.CompensableArchive;
import org.bytesoft.bytetcc.archive.CompensableTransactionArchive;
import org.bytesoft.bytetcc.common.TransactionConfigurator;
import org.bytesoft.bytetcc.common.TransactionRepository;
import org.bytesoft.bytetcc.supports.CompensableTransactionLogger;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.TransactionStatistic;
import org.bytesoft.transaction.archive.TransactionArchive;
import org.bytesoft.transaction.archive.XAResourceArchive;
import org.bytesoft.transaction.recovery.TransactionRecovery;
import org.bytesoft.transaction.xa.TransactionXid;

public class CompensableTransactionRecovery implements TransactionRecovery {
	static final Logger logger = Logger.getLogger(CompensableTransactionRecovery.class.getSimpleName());

	private TransactionStatistic transactionStatistic;

	public CompensableTransaction reconstructTransaction(CompensableTransactionArchive archive) {
		TransactionContext transactionContext = new TransactionContext();
		transactionContext.setCurrentXid((TransactionXid) archive.getXid());
		transactionContext.setRecovery(true);
		transactionContext.setOptimized(archive.isOptimized());
		transactionContext.setCoordinator(archive.isCoordinator());
		transactionContext.setCompensable(archive.isCompensable());

		CompensableTransaction transaction = null;
		if (archive.isCompensable()) {
			CompensableTccTransaction tccTransaction = new CompensableTccTransaction(transactionContext);
			List<CompensableArchive> compensables = archive.getCompensables();
			for (int i = 0; i < compensables.size(); i++) {
				CompensableArchive compensable = compensables.get(i);
				if (compensable.isCoordinator()) {
					tccTransaction.getCoordinatorArchives().add(compensable);
				} else {
					tccTransaction.getParticipantArchives().add(compensable);
				}
			}
			List<XAResourceArchive> resources = archive.getRemoteResources();
			for (int i = 0; i < resources.size(); i++) {
				XAResourceArchive resource = resources.get(i);
				Xid xid = resource.getXid();
				tccTransaction.getResourceArchives().put(xid, resource);
			}
			tccTransaction.setTransactionStatus(archive.getStatus());
			tccTransaction.setCompensableStatus(archive.getCompensableStatus());
			transaction = tccTransaction;
		} else {
			transaction = new CompensableJtaTransaction(transactionContext);
		}

		if (archive.getVote() == XAResource.XA_RDONLY) {
			throw new IllegalStateException();
		}

		return transaction;
	}

	/**
	 * commit/rollback the uncompleted transactions.
	 */
	public synchronized void startupRecover(boolean recoverImmediately) {
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		TransactionRepository transactionRepository = configurator.getTransactionRepository();
		CompensableTransactionLogger transactionLogger = configurator.getTransactionLogger();
		List<TransactionArchive> archives = transactionLogger.getTransactionArchiveList();
		for (int i = 0; i < archives.size(); i++) {
			CompensableTransaction transaction = null;
			try {
				CompensableTransactionArchive archive = (CompensableTransactionArchive) archives.get(i);
				transaction = this.reconstructTransaction(archive);
			} catch (RuntimeException rex) {
				continue;
			}
			TransactionContext transactionContext = transaction.getTransactionContext();
			TransactionXid globalXid = transactionContext.getGlobalXid();
			if (CompensableTccTransaction.class.isInstance(transaction)) {
				this.reconstructTccTransaction((CompensableTccTransaction) transaction);
			} else {
				this.reconstructJtaTransaction((CompensableJtaTransaction) transaction);
			}
			transactionRepository.putTransaction(globalXid, transaction);
			transactionRepository.putErrorTransaction(globalXid, transaction);
		}

		if (recoverImmediately) {
			this.timingRecover();
		}

	}

	/**
	 * TODO
	 */
	private void reconstructJtaTransaction(CompensableJtaTransaction transaction) {
		org.bytesoft.bytejta.common.TransactionConfigurator jtaConfigurator = org.bytesoft.bytejta.common.TransactionConfigurator
				.getInstance();
		org.bytesoft.bytejta.common.TransactionRepository jtaRepository = jtaConfigurator.getTransactionRepository();
		Xid xid = transaction.getTransactionContext().getCurrentXid();
		TransactionXid jtaGlobalXid = jtaConfigurator.getXidFactory().createGlobalXid(xid.getGlobalTransactionId());
		TransactionImpl jtaTransaction = jtaRepository.getErrorTransaction(jtaGlobalXid);
		if (jtaTransaction != null) {
			jtaTransaction.registerTransactionListener(transaction);
		}
	}

	/**
	 * TODO
	 */
	private void reconstructTccTransaction(CompensableTccTransaction transaction) {
		org.bytesoft.bytejta.common.TransactionConfigurator jtaConfigurator = org.bytesoft.bytejta.common.TransactionConfigurator
				.getInstance();
		org.bytesoft.bytejta.common.TransactionRepository jtaRepository = jtaConfigurator.getTransactionRepository();
		List<CompensableArchive> coordinators = transaction.getCoordinatorArchives();
		for (int i = 0; i < coordinators.size(); i++) {
			CompensableArchive archive = coordinators.get(i);
			Xid xid = archive.getXid();
			TransactionXid jtaGlobalXid = jtaConfigurator.getXidFactory().createGlobalXid(xid.getGlobalTransactionId());
			TransactionImpl jtaTransaction = jtaRepository.getErrorTransaction(jtaGlobalXid);
			if (jtaTransaction != null) {
				jtaTransaction.registerTransactionListener(transaction);
			}
		}
		List<CompensableArchive> participants = transaction.getParticipantArchives();
		for (int i = 0; i < participants.size(); i++) {
			CompensableArchive archive = participants.get(i);
			Xid xid = archive.getXid();
			TransactionXid jtaGlobalXid = jtaConfigurator.getXidFactory().createGlobalXid(xid.getGlobalTransactionId());
			TransactionImpl jtaTransaction = jtaRepository.getErrorTransaction(jtaGlobalXid);
			if (jtaTransaction != null) {
				jtaTransaction.registerTransactionListener(transaction);
			}
		}
	}

	public synchronized void timingRecover() {
		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		TransactionRepository transactionRepository = transactionConfigurator.getTransactionRepository();
		List<CompensableTransaction> transactions = transactionRepository.getErrorTransactionList();
		for (int i = 0; i < transactions.size(); i++) {
			CompensableTransaction transaction = transactions.get(i);
			// if (CompensableTccTransaction.class.isInstance(transaction)) {
			this.recoverTransaction((CompensableTccTransaction) transaction);
			// } else {
			// this.recoverTransaction((CompensableJtaTransaction) transaction);
			// }
		}
	}

	// public void recoverTransaction(CompensableJtaTransaction transaction) {
	// }

	public void recoverTransaction(CompensableTccTransaction transaction) {
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		CompensableTransactionLogger transactionLogger = configurator.getTransactionLogger();
		CompensableTransactionManager transactionManager = configurator.getTransactionManager();
		TransactionRepository transactionRepository = configurator.getTransactionRepository();

		TransactionXid globalXid = transaction.getTransactionContext().getGlobalXid();
		int compensableStatus = transaction.getCompensableStatus();
		int transactionStatus = transaction.getStatus();
		switch (transactionStatus) {
		case Status.STATUS_PREPARED:
			transaction.setCompensableStatus(CompensableTccTransaction.STATUS_TRIED);
			transaction.setTransactionStatus(Status.STATUS_COMMITTING);
			transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CONFIRMING);
		case Status.STATUS_COMMITTING:
			if (compensableStatus != CompensableTccTransaction.STATUS_CONFIRMED) {
				try {
					transactionManager.processNativeConfirm(transaction);
				} catch (RuntimeException ex) {
					logger.warn(ex.getMessage(), ex);
					break;
				}
				transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CONFIRMED);
				transactionLogger.updateTransaction(transaction.getTransactionArchive());
			}

			try {
				transaction.remoteConfirm();
			} catch (Exception ex) {
				logger.debug(ex.getMessage(), ex);
				break;
			}

			transaction.setTransactionStatus(Status.STATUS_COMMITTED);
			transactionLogger.deleteTransaction(transaction.getTransactionArchive());
			transactionRepository.removeTransaction(globalXid);
			transactionRepository.removeErrorTransaction(globalXid);

			break;
		case Status.STATUS_PREPARING:
			// TODO
			transaction.setTransactionStatus(Status.STATUS_ROLLING_BACK);
			transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CANCELLING);
		case Status.STATUS_ROLLING_BACK:
			if (compensableStatus != CompensableTccTransaction.STATUS_CANCELLED) {
				try {
					transactionManager.processNativeCancel(transaction);
				} catch (RuntimeException ex) {
					logger.warn(ex.getMessage(), ex);
					break;
				}
				transaction.setCompensableStatus(CompensableTccTransaction.STATUS_CANCELLED);
				transactionLogger.updateTransaction(transaction.getTransactionArchive());
			}

			try {
				transaction.remoteCancel();
			} catch (Exception ex) {
				logger.debug(ex.getMessage(), ex);
				break;
			}

			transaction.setTransactionStatus(Status.STATUS_ROLLEDBACK);
			transactionLogger.deleteTransaction(transaction.getTransactionArchive());
			transactionRepository.removeTransaction(globalXid);
			transactionRepository.removeErrorTransaction(globalXid);

			break;
		case Status.STATUS_COMMITTED:
			transactionLogger.deleteTransaction(transaction.getTransactionArchive());
			break;
		case Status.STATUS_ROLLEDBACK:
			transactionLogger.deleteTransaction(transaction.getTransactionArchive());
			break;
		}
	}

	public void setTransactionStatistic(TransactionStatistic transactionStatistic) {
		this.transactionStatistic = transactionStatistic;
	}

	public TransactionStatistic getTransactionStatistic() {
		return transactionStatistic;
	}

}
