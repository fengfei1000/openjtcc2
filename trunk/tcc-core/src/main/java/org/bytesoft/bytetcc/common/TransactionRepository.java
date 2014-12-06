package org.bytesoft.bytetcc.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bytesoft.bytetcc.CompensableTransaction;
import org.bytesoft.transaction.xa.TransactionXid;

public class TransactionRepository {
	private final Map<TransactionXid, CompensableTransaction> xidToTxMap = new ConcurrentHashMap<TransactionXid, CompensableTransaction>();
	private final Map<TransactionXid, CompensableTransaction> xidToErrTxMap = new ConcurrentHashMap<TransactionXid, CompensableTransaction>();

	public void putTransaction(TransactionXid globalXid, CompensableTransaction transaction) {
		this.xidToTxMap.put(globalXid, transaction);
	}

	public CompensableTransaction getTransaction(TransactionXid globalXid) {
		return this.xidToTxMap.get(globalXid);
	}

	public CompensableTransaction removeTransaction(TransactionXid globalXid) {
		return this.xidToTxMap.remove(globalXid);
	}

	public void putErrorTransaction(TransactionXid globalXid, CompensableTransaction transaction) {
		this.xidToErrTxMap.put(globalXid, transaction);
	}

	public CompensableTransaction getErrorTransaction(TransactionXid globalXid) {
		return this.xidToErrTxMap.get(globalXid);
	}

	public CompensableTransaction removeErrorTransaction(TransactionXid globalXid) {
		return this.xidToErrTxMap.remove(globalXid);
	}

	public List<CompensableTransaction> getErrorTransactionList() {
		return new ArrayList<CompensableTransaction>(this.xidToErrTxMap.values());
	}

	public List<CompensableTransaction> getActiveTransactionList() {
		return new ArrayList<CompensableTransaction>(this.xidToTxMap.values());
	}

}
