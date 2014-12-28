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
import java.util.Set;

import org.bytesoft.bytetcc.archive.CompensableArchive;
import org.bytesoft.bytetcc.supports.CompensableTransactionLogger;
import org.bytesoft.transaction.archive.TransactionArchive;

public class TransactionLoggerImpl/* implements CompensableTransactionLogger */{
	private CompensableTransactionLogger delegate = CompensableTransactionLogger.defaultTransactionLogger;

	public void updateCompensable(CompensableArchive holder) {
	}

	public void beginTransaction(TransactionArchive transaction) {
	}

	public void prepareTransaction(TransactionArchive transaction) {
	}

	public void updateTransaction(TransactionArchive transaction) {
	}

	public void completeTransaction(TransactionArchive transaction) {
	}

	public void cleanupTransaction(TransactionArchive transaction) {
	}

	public Set<TransactionArchive> getLoggedTransactionSet() {
		return new HashSet<TransactionArchive>();
	}

	public CompensableTransactionLogger getDelegate() {
		return delegate;
	}

	public void setDelegate(CompensableTransactionLogger delegate) {
		this.delegate = delegate;
	}

}
