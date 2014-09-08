package org.bytesoft.bytejta.common;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bytesoft.bytejta.TransactionImpl;

public class TransactionRepository {
	static TransactionRepository instance;
	private final Map<TransactionXid, TransactionImpl> xidToTxMap = new ConcurrentHashMap<TransactionXid, TransactionImpl>();
	private final Map<TransactionXid, TransactionImpl> xidToErrTxMap = new ConcurrentHashMap<TransactionXid, TransactionImpl>();

	public void putTransaction(TransactionXid globalXid, TransactionImpl transaction) {
		this.xidToTxMap.put(globalXid, transaction);
	}

	public TransactionImpl getTransaction(TransactionXid globalXid) {
		return this.xidToTxMap.get(globalXid);
	}

	public TransactionImpl removeTransaction(TransactionXid globalXid) {
		return this.xidToTxMap.remove(globalXid);
	}

	public void putErrorTransaction(TransactionXid globalXid, TransactionImpl transaction) {
		this.xidToErrTxMap.put(globalXid, transaction);
	}

	public TransactionImpl getErrorTransaction(TransactionXid globalXid) {
		return this.xidToErrTxMap.get(globalXid);
	}

	public TransactionImpl removeErrorTransaction(TransactionXid globalXid) {
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
