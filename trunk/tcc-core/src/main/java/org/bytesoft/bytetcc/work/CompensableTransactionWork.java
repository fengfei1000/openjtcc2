package org.bytesoft.bytetcc.work;

import javax.resource.spi.work.Work;

import org.apache.log4j.Logger;
import org.bytesoft.bytetcc.common.TransactionConfigurator;
import org.bytesoft.transaction.recovery.TransactionRecovery;

public class CompensableTransactionWork implements Work {
	static final Logger logger = Logger.getLogger(CompensableTransactionWork.class.getSimpleName());

	static final long SECOND_MILLIS = 1000L;
	private long stopTimeMillis = -1;
	private long delayOfStoping = SECOND_MILLIS * 15;
	private long recoveryInterval = SECOND_MILLIS * 60;

	public void run() {

		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		TransactionRecovery transactionRecovery = configurator.getTransactionRecovery();
		try {
			transactionRecovery.startupRecover();
		} catch (RuntimeException rex) {
			logger.error(rex.getMessage(), rex);
		}

		long nextRecoveryTime = System.currentTimeMillis() + this.recoveryInterval;
		while (this.currentActive()) {

			long current = System.currentTimeMillis();

			if (current >= nextRecoveryTime) {
				nextRecoveryTime = current + this.recoveryInterval;
				try {
					transactionRecovery.timingRecover();
				} catch (RuntimeException rex) {
					logger.error(rex.getMessage(), rex);
				}
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

}
