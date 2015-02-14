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

import org.bytesoft.bytetcc.CompensableTransaction;
import org.bytesoft.transaction.TransactionStatistic;
import org.bytesoft.transaction.archive.TransactionArchive;
import org.bytesoft.transaction.xa.TransactionXid;

public class RecoveryManager {

	private TransactionStatistic transactionStatistic;

	public void reconstruct() {
	}

	public RecoveredTransactionImpl reconstructTransaction(TransactionArchive archive) {
		return null;
	}

	/**
	 * commit/rollback the uncompleted transactions.
	 */
	public void recover() {
	}

	/**
	 * commit/rollback the specific transaction.
	 * 
	 * @param globalXid
	 * @throws HeuristicMixedException
	 * @throws SystemException
	 */
	public void recoverTransaction(TransactionXid globalXid) throws HeuristicMixedException, SystemException {
	}

	public void recoverTransaction(CompensableTransaction transaction) throws HeuristicMixedException, SystemException {
	}

	public void setTransactionStatistic(TransactionStatistic transactionStatistic) {
		this.transactionStatistic = transactionStatistic;
	}

	public TransactionStatistic getTransactionStatistic() {
		return transactionStatistic;
	}

}
