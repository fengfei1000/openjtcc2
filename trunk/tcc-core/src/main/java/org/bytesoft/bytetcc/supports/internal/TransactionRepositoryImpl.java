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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bytesoft.bytetcc.jta.JtaTransaction;
import org.bytesoft.bytetcc.supports.TransactionLogger;
import org.bytesoft.bytetcc.supports.TransactionRepository;
import org.bytesoft.bytetcc.xa.XidImpl;

public class TransactionRepositoryImpl implements TransactionRepository {
	private final Map<XidImpl, JtaTransaction> xidToTxMap = new ConcurrentHashMap<XidImpl, JtaTransaction>();
	private final Map<XidImpl, JtaTransaction> xidToErrTxMap = new ConcurrentHashMap<XidImpl, JtaTransaction>();
	private final TransactionLoggerImpl transactionLoggerWrapper = new TransactionLoggerImpl();

	public void putTransaction(XidImpl globalXid, JtaTransaction transaction) {
		this.xidToTxMap.put(globalXid, transaction);
	}

	public JtaTransaction getTransaction(XidImpl globalXid) {
		return this.xidToTxMap.get(globalXid);
	}

	public JtaTransaction removeTransaction(XidImpl globalXid) {
		return this.xidToTxMap.remove(globalXid);
	}

	public TransactionLogger getTransactionLogger() {
		return this.transactionLoggerWrapper;
	}

	public void setTransactionLogger(TransactionLogger transactionLogger) {
		if (transactionLogger == null) {
			this.transactionLoggerWrapper.setDelegate(TransactionLogger.defaultTransactionLogger);
		} else {
			this.transactionLoggerWrapper.setDelegate(transactionLogger);
		}
	}

	public void putErrorTransaction(XidImpl globalXid, JtaTransaction transaction) {
		this.xidToErrTxMap.put(globalXid, transaction);
	}

	public JtaTransaction getErrorTransaction(XidImpl globalXid) {
		return this.xidToErrTxMap.get(globalXid);
	}

	public JtaTransaction removeErrorTransaction(XidImpl globalXid) {
		return this.xidToErrTxMap.remove(globalXid);
	}

	public Set<JtaTransaction> getErrorTransactionSet() {
		return new HashSet<JtaTransaction>(this.xidToErrTxMap.values());
	}

	public Set<JtaTransaction> getActiveTransactionSet() {
		return new HashSet<JtaTransaction>(this.xidToTxMap.values());
	}

}
