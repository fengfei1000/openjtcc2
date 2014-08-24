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
package org.bytesoft.bytetcc.supports.internal;

public class TransactionStatisticImpl /* implements TransactionStatistic */{
	//
	// private AtomicLong totalAtomic = new AtomicLong();
	// private AtomicLong errorTotalAtomic = new AtomicLong();
	//
	// private AtomicLong errorAtomic = new AtomicLong();
	// private AtomicLong activeAtomic = new AtomicLong();
	// private AtomicLong preparingAtomic = new AtomicLong();
	// private AtomicLong preparedAtomic = new AtomicLong();
	// private AtomicLong committingAtomic = new AtomicLong();
	// private AtomicLong committedAtomic = new AtomicLong();
	// private AtomicLong rollingbackAtomic = new AtomicLong();
	// private AtomicLong rolledbackAtomic = new AtomicLong();
	//
	// private void unmarkActive(CompensableTransaction transaction) {
	// if (transaction.containTransientFlags(TransactionStatistic.FLAGS_ACTIVE)) {
	// this.activeAtomic.decrementAndGet();
	// transaction.resetTransientFlags(TransactionStatistic.FLAGS_ACTIVE);
	// }
	// }
	//
	// private void markPreparing(CompensableTransaction transaction) {
	// if (transaction.containTransientFlags(TransactionStatistic.FLAGS_PREPARING) == false) {
	// this.preparingAtomic.incrementAndGet();
	// transaction.affixTransientFlags(TransactionStatistic.FLAGS_PREPARING);
	// }
	// }
	//
	// private void unmarkPreparing(CompensableTransaction transaction) {
	// if (transaction.containTransientFlags(TransactionStatistic.FLAGS_PREPARING)) {
	// this.preparingAtomic.decrementAndGet();
	// transaction.resetTransientFlags(TransactionStatistic.FLAGS_PREPARING);
	// }
	// }
	//
	// private void markPrepared(CompensableTransaction transaction) {
	// if (transaction.containTransientFlags(TransactionStatistic.FLAGS_PREPARED) == false) {
	// this.preparedAtomic.incrementAndGet();
	// transaction.affixTransientFlags(TransactionStatistic.FLAGS_PREPARED);
	// }
	// }
	//
	// private void unmarkPrepared(CompensableTransaction transaction) {
	// if (transaction.containTransientFlags(TransactionStatistic.FLAGS_PREPARED)) {
	// this.preparedAtomic.decrementAndGet();
	// transaction.resetTransientFlags(TransactionStatistic.FLAGS_PREPARED);
	// }
	// }
	//
	// private void markCommitting(CompensableTransaction transaction) {
	// if (transaction.containTransientFlags(TransactionStatistic.FLAGS_COMMITTING) == false) {
	// this.committingAtomic.incrementAndGet();
	// transaction.affixTransientFlags(TransactionStatistic.FLAGS_COMMITTING);
	// }
	// }
	//
	// private void unmarkCommitting(CompensableTransaction transaction) {
	// if (transaction.containTransientFlags(TransactionStatistic.FLAGS_COMMITTING)) {
	// this.committingAtomic.decrementAndGet();
	// transaction.resetTransientFlags(TransactionStatistic.FLAGS_COMMITTING);
	// }
	// }
	//
	// private void markCommitted(CompensableTransaction transaction) {
	// if (transaction.containTransientFlags(TransactionStatistic.FLAGS_COMMITTED) == false) {
	// this.committedAtomic.incrementAndGet();
	// transaction.affixTransientFlags(TransactionStatistic.FLAGS_COMMITTED);
	// }
	// }
	//
	// private void unmarkCommitted(CompensableTransaction transaction) {
	// if (transaction.containTransientFlags(TransactionStatistic.FLAGS_COMMITTED)) {
	// this.committedAtomic.decrementAndGet();
	// transaction.resetTransientFlags(TransactionStatistic.FLAGS_COMMITTED);
	// }
	// }
	//
	// private void markRollingback(CompensableTransaction transaction) {
	// if (transaction.containTransientFlags(TransactionStatistic.FLAGS_ROLLINGBACK) == false) {
	// this.rollingbackAtomic.incrementAndGet();
	// transaction.affixTransientFlags(TransactionStatistic.FLAGS_ROLLINGBACK);
	// }
	// }
	//
	// private void unmarkRollingback(CompensableTransaction transaction) {
	// if (transaction.containTransientFlags(TransactionStatistic.FLAGS_ROLLINGBACK)) {
	// this.rollingbackAtomic.decrementAndGet();
	// transaction.resetTransientFlags(TransactionStatistic.FLAGS_ROLLINGBACK);
	// }
	// }
	//
	// private void markRolledback(CompensableTransaction transaction) {
	// if (transaction.containTransientFlags(TransactionStatistic.FLAGS_ROLEDBACK) == false) {
	// this.rolledbackAtomic.incrementAndGet();
	// transaction.affixTransientFlags(TransactionStatistic.FLAGS_ROLEDBACK);
	// }
	// }
	//
	// private void unmarkRolledback(CompensableTransaction transaction) {
	// if (transaction.containTransientFlags(TransactionStatistic.FLAGS_ROLEDBACK)) {
	// this.rolledbackAtomic.decrementAndGet();
	// transaction.resetTransientFlags(TransactionStatistic.FLAGS_ROLEDBACK);
	// }
	// }
	//
	// private void markError(CompensableTransaction transaction) {
	// if (transaction.containTransientFlags(TransactionStatistic.FLAGS_ERROR) == false) {
	// this.errorAtomic.incrementAndGet();
	// transaction.affixTransientFlags(TransactionStatistic.FLAGS_ERROR);
	// }
	//
	// if (transaction.containTransientFlags(TransactionStatistic.FLAGS_ERROR_TOTAL) == false) {
	// this.errorTotalAtomic.incrementAndGet();
	// transaction.affixTransientFlags(TransactionStatistic.FLAGS_ERROR_TOTAL);
	// }
	// }
	//
	// private void unmarkErrork(CompensableTransaction transaction) {
	// if (transaction.containTransientFlags(TransactionStatistic.FLAGS_ERROR)) {
	// this.errorAtomic.decrementAndGet();
	// transaction.resetTransientFlags(TransactionStatistic.FLAGS_ERROR);
	// }
	// }
	//
	// public void fireRecoverTransaction(CompensableTransaction transaction) {
	// this.totalAtomic.incrementAndGet();
	// this.errorTotalAtomic.incrementAndGet();
	// this.errorAtomic.incrementAndGet();
	// }
	//
	// public void fireBeginTransaction(CompensableTransaction transaction) {
	// this.totalAtomic.incrementAndGet();
	// this.activeAtomic.incrementAndGet();
	// transaction.affixTransientFlags(TransactionStatistic.FLAGS_ACTIVE);
	// }
	//
	// public void firePreparingTransaction(CompensableTransaction transaction) {
	// this.unmarkActive(transaction);
	//
	// this.unmarkErrork(transaction);
	// this.markPreparing(transaction);
	// }
	//
	// public void firePreparedTransaction(CompensableTransaction transaction) {
	// this.unmarkActive(transaction);
	// this.unmarkPreparing(transaction);
	//
	// this.unmarkErrork(transaction);
	// this.markPrepared(transaction);
	// }
	//
	// public void fireCommittingTransaction(CompensableTransaction transaction) {
	// this.unmarkActive(transaction);
	// this.unmarkPreparing(transaction);
	// this.unmarkPrepared(transaction);
	//
	// this.unmarkErrork(transaction);
	// this.markCommitting(transaction);
	// }
	//
	// public void fireCommittedTransaction(CompensableTransaction transaction) {
	// this.unmarkActive(transaction);
	// this.unmarkPreparing(transaction);
	// this.unmarkPrepared(transaction);
	// this.unmarkCommitting(transaction);
	//
	// this.unmarkErrork(transaction);
	// this.markCommitted(transaction);
	// }
	//
	// public void fireRollingBackTransaction(CompensableTransaction transaction) {
	// this.unmarkActive(transaction);
	// this.unmarkPreparing(transaction);
	// this.unmarkPrepared(transaction);
	// this.unmarkCommitting(transaction);
	//
	// this.unmarkErrork(transaction);
	// this.markRollingback(transaction);
	// }
	//
	// public void fireRolledbackTransaction(CompensableTransaction transaction) {
	// this.unmarkActive(transaction);
	// this.unmarkPreparing(transaction);
	// this.unmarkPrepared(transaction);
	// this.unmarkCommitting(transaction);
	// this.unmarkRollingback(transaction);
	//
	// this.unmarkErrork(transaction);
	// this.markRolledback(transaction);
	// }
	//
	// public void fireCompleteFailure(CompensableTransaction transaction) {
	// this.markError(transaction);
	// }
	//
	// public void fireCleanupTransaction(CompensableTransaction transaction) {
	// this.unmarkActive(transaction);
	//
	// this.unmarkPreparing(transaction);
	// this.unmarkPrepared(transaction);
	//
	// this.unmarkCommitting(transaction);
	// this.unmarkCommitted(transaction);
	//
	// this.unmarkRollingback(transaction);
	// this.unmarkRolledback(transaction);
	//
	// this.unmarkErrork(transaction);
	// }
	//
}
