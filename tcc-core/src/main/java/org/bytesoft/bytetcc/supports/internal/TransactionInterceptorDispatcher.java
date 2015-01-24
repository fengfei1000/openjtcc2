package org.bytesoft.bytetcc.supports.internal;

import org.bytesoft.bytetcc.CompensableTransaction;
import org.bytesoft.bytetcc.CompensableTransactionManager;
import org.bytesoft.bytetcc.common.TransactionConfigurator;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.rpc.TransactionInterceptor;
import org.bytesoft.transaction.rpc.TransactionRequest;
import org.bytesoft.transaction.rpc.TransactionResponse;

public class TransactionInterceptorDispatcher implements TransactionInterceptor {

	private TransactionInterceptor jtaTransactionInterceptor;
	private TransactionInterceptor tccTransactionInterceptor;

	public void beforeSendRequest(TransactionRequest request) throws IllegalStateException {
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		CompensableTransactionManager ctm = configurator.getTransactionManager();
		CompensableTransaction transaction = ctm.getCurrentTransaction();
		if (transaction == null) {
			return;
		} else if (transaction.getTransactionContext().isCompensable()) {
			this.tccTransactionInterceptor.beforeSendRequest(request);
		} else {
			this.jtaTransactionInterceptor.beforeSendRequest(request);
		}
	}

	public void afterReceiveRequest(TransactionRequest request) throws IllegalStateException {
		TransactionContext transactionContext = request.getTransactionContext();
		if (transactionContext == null) {
			return;
		} else if (transactionContext.isCompensable()) {
			this.tccTransactionInterceptor.afterReceiveRequest(request);
		} else {
			this.jtaTransactionInterceptor.afterReceiveRequest(request);
		}
	}

	public void beforeSendResponse(TransactionResponse response) throws IllegalStateException {
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		CompensableTransactionManager ctm = configurator.getTransactionManager();
		CompensableTransaction transaction = ctm.getCurrentTransaction();
		if (transaction == null) {
			return;
		} else if (transaction.getTransactionContext().isCompensable()) {
			this.tccTransactionInterceptor.beforeSendResponse(response);
		} else {
			this.jtaTransactionInterceptor.beforeSendResponse(response);
		}
	}

	public void afterReceiveResponse(TransactionResponse response) throws IllegalStateException {
		TransactionContext transactionContext = response.getTransactionContext();
		if (transactionContext == null) {
			return;
		} else if (transactionContext.isCompensable()) {
			this.tccTransactionInterceptor.afterReceiveResponse(response);
		} else {
			this.jtaTransactionInterceptor.afterReceiveResponse(response);
		}
	}

	public TransactionInterceptor getJtaTransactionInterceptor() {
		return jtaTransactionInterceptor;
	}

	public void setJtaTransactionInterceptor(TransactionInterceptor jtaTransactionInterceptor) {
		this.jtaTransactionInterceptor = jtaTransactionInterceptor;
	}

	public TransactionInterceptor getTccTransactionInterceptor() {
		return tccTransactionInterceptor;
	}

	public void setTccTransactionInterceptor(TransactionInterceptor tccTransactionInterceptor) {
		this.tccTransactionInterceptor = tccTransactionInterceptor;
	}

}
