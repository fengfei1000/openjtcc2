package org.bytesoft.transaction;

import javax.transaction.Transaction;

public interface TransactionTimer {

	public void timingExecution();

	public void stopTiming(Transaction tx);

}
