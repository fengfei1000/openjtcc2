package org.bytesoft.bytetcc.work;

import javax.resource.spi.work.Work;

import org.apache.log4j.Logger;
import org.bytesoft.transaction.recovery.TransactionRecovery;

public class CompensableTransactionWork implements Work {
	static final Logger logger = Logger.getLogger(CompensableTransactionWork.class.getSimpleName());

	static final long SECOND_MILLIS = 1000L;
	private long stopTimeMillis = -1;
	private long delayOfStoping = SECOND_MILLIS * 15;
	private long recoveryInterval = SECOND_MILLIS * 60;

	public void run() {

		org.bytesoft.bytejta.common.TransactionConfigurator jtaConfigurator = org.bytesoft.bytejta.common.TransactionConfigurator
				.getInstance();
		TransactionRecovery jtaRecovery = jtaConfigurator.getTransactionRecovery();
		try {
			jtaRecovery.startupRecover(false);
		} catch (RuntimeException rex) {
			logger.error(rex.getMessage(), rex);
		}

		org.bytesoft.bytetcc.common.TransactionConfigurator tccConfigurator = org.bytesoft.bytetcc.common.TransactionConfigurator
				.getInstance();
		TransactionRecovery tccRecovery = tccConfigurator.getTransactionRecovery();
		try {
			tccRecovery.startupRecover(false);
		} catch (RuntimeException rex) {
			logger.error(rex.getMessage(), rex);
		}

		// TODO

		long nextRecoveryTime = 0;
		while (this.currentActive()) {

			long current = System.currentTimeMillis();

			if (current >= nextRecoveryTime) {
				nextRecoveryTime = current + this.recoveryInterval;
				try {
					jtaRecovery.timingRecover();
				} catch (RuntimeException rex) {
					logger.error(rex.getMessage(), rex);
				}

				try {
					tccRecovery.timingRecover();
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
