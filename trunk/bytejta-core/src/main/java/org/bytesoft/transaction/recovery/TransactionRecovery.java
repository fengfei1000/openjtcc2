package org.bytesoft.transaction.recovery;

public interface TransactionRecovery {

	public void timingRecover();

	public void startupRecover(boolean recoverImmediately);

}
