package org.bytesoft.transaction.work;

import javax.resource.spi.work.Work;

import org.bytesoft.transaction.TransactionTimer;
import org.bytesoft.transaction.recovery.TransactionRecovery;

public class TransactionWork implements Work {
	static final long SECOND_MILLIS = 1000L;
	private long stopTimeMillis = -1;
	private long delayOfStoping = SECOND_MILLIS * 15;
	private long recoveryInterval = SECOND_MILLIS * 60;

	private TransactionTimer transactionTimer;
	private TransactionRecovery transactionRecovery;

	public void run() {
		long nextExecutionTime = 0;
		long nextRecoveryTime = 0;
		while (this.currentActive()) {
			long current = System.currentTimeMillis();
			if (current >= nextExecutionTime) {
				nextExecutionTime = current + SECOND_MILLIS;
				this.transactionTimer.timingExecution();
			}

			if (current >= nextRecoveryTime) {
				nextRecoveryTime = current + this.recoveryInterval;
				this.transactionRecovery.timingRecover();
			}

			this.waitForMillis(100L);

		} // end-while (this.currentActive())
	}

	private void waitForMillis(long millis) {
		try {
			Thread.sleep(millis);
		} catch (Exception ignore) {
			// ignore
		}
	}

	public void release() {
		this.stopTimeMillis = System.currentTimeMillis() + this.delayOfStoping;
	}

	protected boolean currentActive() {
		return this.stopTimeMillis <= 0 || System.currentTimeMillis() < this.stopTimeMillis;
	}

	public long getDelayOfStoping() {
		return delayOfStoping;
	}

	public void setDelayOfStoping(long delayOfStoping) {
		this.delayOfStoping = delayOfStoping;
	}

	public TransactionTimer getTransactionTimer() {
		return transactionTimer;
	}

	public void setTransactionTimer(TransactionTimer transactionTimer) {
		this.transactionTimer = transactionTimer;
	}

	public TransactionRecovery getTransactionRecovery() {
		return transactionRecovery;
	}

	public void setTransactionRecovery(TransactionRecovery transactionRecovery) {
		this.transactionRecovery = transactionRecovery;
	}

}
