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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bytesoft.bytejta.common.TransactionXid;
import org.bytesoft.bytetcc.archive.CompensableArchive;
import org.bytesoft.bytetcc.archive.TransactionArchive;
import org.bytesoft.bytetcc.supports.TransactionLogger;
import org.bytesoft.transaction.TransactionContext;

public class TransactionLoggerImpl implements TransactionLogger {
	private TransactionLogger delegate = TransactionLogger.defaultTransactionLogger;
	private final Map<TransactionXid, ArchiveObject> xidToRecMap = new ConcurrentHashMap<TransactionXid, ArchiveObject>();

	public void enlistService(TransactionContext transactionContext, CompensableArchive holder) {
		TransactionXid globalXid = transactionContext.getGlobalXid();
		ArchiveObject object = this.xidToRecMap.get(globalXid);
		if (object == null) {
			delegate.enlistService(transactionContext, holder);
		} else if (object.xidToNativeSvrMap.containsKey(holder.branchXid)) {
			// ignore
		} else {
			delegate.enlistService(transactionContext, holder);
			object.xidToNativeSvrMap.put(holder.branchXid, holder);
		}
	}

	public void delistService(TransactionContext transactionContext, CompensableArchive holder) {
		TransactionXid globalXid = transactionContext.getGlobalXid();
		ArchiveObject object = this.xidToRecMap.get(globalXid);
		if (object == null) {
			// ignore
		} else if (object.xidToNativeSvrMap.containsKey(holder.branchXid)) {
			// ignore
		} else {
			this.enlistService(transactionContext, holder);
		}
		delegate.delistService(transactionContext, holder);
	}

	public void updateService(TransactionContext transactionContext, CompensableArchive holder) {
		TransactionXid globalXid = transactionContext.getGlobalXid();
		ArchiveObject object = this.xidToRecMap.get(globalXid);
		if (object == null) {
			// ignore
		} else if (object.xidToNativeSvrMap.containsKey(holder.branchXid)) {
			// ignore
		} else {
			this.enlistService(transactionContext, holder);
		}
		delegate.updateService(transactionContext, holder);
	}

	public void confirmService(TransactionContext transactionContext, CompensableArchive holder) {
		TransactionXid globalXid = transactionContext.getGlobalXid();
		ArchiveObject object = this.xidToRecMap.get(globalXid);
		if (object == null) {
			// ignore
		} else if (object.xidToNativeSvrMap.containsKey(holder.branchXid)) {
			// ignore
		} else {
			this.enlistService(transactionContext, holder);
		}
		delegate.confirmService(transactionContext, holder);
	}

	public void cancelService(TransactionContext transactionContext, CompensableArchive holder) {
		TransactionXid globalXid = transactionContext.getGlobalXid();
		ArchiveObject object = this.xidToRecMap.get(globalXid);
		if (object == null) {
			// ignore
		} else if (object.xidToNativeSvrMap.containsKey(holder.branchXid)) {
			// ignore
		} else {
			this.enlistService(transactionContext, holder);
		}
		delegate.cancelService(transactionContext, holder);
	}

	public void commitService(TransactionContext transactionContext, CompensableArchive holder) {
		TransactionXid globalXid = transactionContext.getGlobalXid();
		ArchiveObject object = this.xidToRecMap.get(globalXid);
		if (object == null) {
			// ignore
		} else if (object.xidToNativeSvrMap.containsKey(holder.branchXid)) {
			// ignore
		} else {
			this.enlistService(transactionContext, holder);
		}
		delegate.commitService(transactionContext, holder);
	}

	public void rollbackService(TransactionContext transactionContext, CompensableArchive holder) {
		TransactionXid globalXid = transactionContext.getGlobalXid();
		ArchiveObject object = this.xidToRecMap.get(globalXid);
		if (object == null) {
			// ignore
		} else if (object.xidToNativeSvrMap.containsKey(holder.branchXid)) {
			// ignore
		} else {
			this.enlistService(transactionContext, holder);
		}
		delegate.rollbackService(transactionContext, holder);
	}

	// public void enlistTerminator(TransactionContext transactionContext, TerminatorArchive holder) {
	// XidImpl globalXid = transactionContext.getGlobalXid();
	// RecordObject object = this.xidToRecMap.get(globalXid);
	// if (object == null) {
	// // ignore
	// } else if (object.terminators.contains(holder)) {
	// // ignore
	// } else {
	// this.registerTerminator(transactionContext, holder.terminator);
	// }
	// delegate.enlistTerminator(transactionContext, holder);
	// }

	// public void delistTerminator(TransactionContext transactionContext, TerminatorArchive holder) {
	// XidImpl globalXid = transactionContext.getGlobalXid();
	// RecordObject object = this.xidToRecMap.get(globalXid);
	// if (object == null) {
	// // ignore
	// } else if (object.terminators.contains(holder)) {
	// // ignore
	// } else {
	// this.registerTerminator(transactionContext, holder.terminator);
	// }
	// delegate.delistTerminator(transactionContext, holder);
	// }

	public void beginTransaction(TransactionArchive transaction) {
		TransactionContext transactionContext = transaction.getTransactionContext();
		TransactionXid globalXid = transactionContext.getGlobalXid();
		ArchiveObject object = this.xidToRecMap.get(globalXid);
		if (object == null) {
			delegate.beginTransaction(transaction);
			object = new ArchiveObject();
			this.xidToRecMap.put(globalXid, object);
		}
	}

	public void prepareTransaction(TransactionArchive transaction) {
		TransactionContext transactionContext = transaction.getTransactionContext();
		TransactionXid globalXid = transactionContext.getGlobalXid();
		ArchiveObject object = this.xidToRecMap.get(globalXid);
		if (object == null) {
			this.beginTransaction(transaction);
		}
		delegate.prepareTransaction(transaction);
	}

	public void updateTransaction(TransactionArchive transaction) {
		TransactionContext transactionContext = transaction.getTransactionContext();
		TransactionXid globalXid = transactionContext.getGlobalXid();
		ArchiveObject object = this.xidToRecMap.get(globalXid);
		if (object == null) {
			this.beginTransaction(transaction);
		}
		delegate.updateTransaction(transaction);
	}

	public void completeTransaction(TransactionArchive transaction) {
		TransactionContext transactionContext = transaction.getTransactionContext();
		TransactionXid globalXid = transactionContext.getGlobalXid();
		ArchiveObject object = this.xidToRecMap.get(globalXid);
		if (object == null) {
			this.beginTransaction(transaction);
		}
		delegate.completeTransaction(transaction);
	}

	public void cleanupTransaction(TransactionArchive transaction) {
		TransactionContext transactionContext = transaction.getTransactionContext();
		TransactionXid globalXid = transactionContext.getGlobalXid();
		this.xidToRecMap.remove(globalXid);

		delegate.cleanupTransaction(transaction);
	}

	public Set<TransactionArchive> getLoggedTransactionSet() {
		Set<TransactionArchive> metas = delegate.getLoggedTransactionSet();
		Iterator<TransactionArchive> itr = metas.iterator();
		while (itr.hasNext()) {
			TransactionArchive meta = itr.next();
			TransactionContext transactionContext = meta.getTransactionContext();
			TransactionXid globalXid = transactionContext.getGlobalXid();
			ArchiveObject object = new ArchiveObject();
			this.xidToRecMap.put(globalXid, object);

			Map<TransactionXid, CompensableArchive> nativeSvrMap = meta.getCompensableArchiveMap();
			Iterator<Map.Entry<TransactionXid, CompensableArchive>> nativeSvrItr = nativeSvrMap.entrySet().iterator();
			while (nativeSvrItr.hasNext()) {
				Map.Entry<TransactionXid, CompensableArchive> entry = nativeSvrItr.next();
				TransactionXid branchXid = entry.getKey();
				CompensableArchive holder = entry.getValue();
				object.xidToNativeSvrMap.put(branchXid, holder);
			}

			// TODO
			// Map<String, TerminatorArchive> terminatorMap = meta.getAppToTerminatorMap();
			// Iterator<Map.Entry<String, TerminatorArchive>> terminatorItr = terminatorMap.entrySet().iterator();
			// while (terminatorItr.hasNext()) {
			// Map.Entry<String, TerminatorArchive> entry = terminatorItr.next();
			// TerminatorArchive holder = entry.getValue();
			// object.terminators.add(holder);
			// }

		}
		return metas;
	}

	public TransactionLogger getDelegate() {
		return delegate;
	}

	public void setDelegate(TransactionLogger delegate) {
		this.delegate = delegate;
	}

	public static class ArchiveObject {

		public Map<TransactionXid, CompensableArchive> xidToNativeSvrMap = new HashMap<TransactionXid, CompensableArchive>();
		// public Set<TerminatorArchive> terminators = new HashSet<TerminatorArchive>();
	}

}
