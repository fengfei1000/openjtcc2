package org.bytesoft.bytejta.common;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bytesoft.bytejta.TransactionImpl;

public class TransactionRepository {
	static TransactionRepository instance;
	private final Map<XidImpl, TransactionImpl> xidToTxMap = new ConcurrentHashMap<XidImpl, TransactionImpl>();
	private final Map<XidImpl, TransactionImpl> xidToErrTxMap = new ConcurrentHashMap<XidImpl, TransactionImpl>();

	public void putTransaction(XidImpl globalXid, TransactionImpl transaction) {
		this.xidToTxMap.put(globalXid, transaction);
	}

	public TransactionImpl getTransaction(XidImpl globalXid) {
		return this.xidToTxMap.get(globalXid);
	}

	public TransactionImpl removeTransaction(XidImpl globalXid) {
		return this.xidToTxMap.remove(globalXid);
	}

	public void putErrorTransaction(XidImpl globalXid, TransactionImpl transaction) {
		this.xidToErrTxMap.put(globalXid, transaction);
	}

	public TransactionImpl getErrorTransaction(XidImpl globalXid) {
		return this.xidToErrTxMap.get(globalXid);
	}

	public TransactionImpl removeErrorTransaction(XidImpl globalXid) {
		return this.xidToErrTxMap.remove(globalXid);
	}

	public Set<TransactionImpl> getErrorTransactionSet() {
		return new HashSet<TransactionImpl>(this.xidToErrTxMap.values());
	}

	public Set<TransactionImpl> getActiveTransactionSet() {
		return new HashSet<TransactionImpl>(this.xidToTxMap.values());
	}

	private TransactionRepository() {
		if (instance == null) {
			initialize(this);
		} else {
			throw new IllegalStateException();
		}
	}

	private static synchronized void initialize(TransactionRepository inst) throws IllegalStateException {
		if (instance == null) {
			instance = inst;
		} else {
			throw new IllegalStateException();
		}
	}

	public static TransactionRepository getInstance() {
		return getInstance(true);
	}

	public static TransactionRepository getInstance(boolean create) {
		if (create) {
			initializeIfRequired();
		}
		return instance;
	}

	private static TransactionRepository initializeIfRequired() {
		if (instance == null) {
			try {
				return new TransactionRepository();
			} catch (IllegalStateException ex) {
				return instance;
			}
		}
		return instance;
	}
}
