package org.bytesoft.bytetcc.supports.spring.rpc;

import org.apache.log4j.Logger;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.rpc.TransactionInterceptor;
import org.bytesoft.transaction.rpc.TransactionRequest;
import org.bytesoft.transaction.rpc.TransactionResponse;

public class CompensableTransactionInterceptorDispatcher implements TransactionInterceptor {
	static final Logger logger = Logger.getLogger(CompensableTransactionInterceptorDispatcher.class.getSimpleName());

	private CompensableJtaTransactionInterceptor jtaTransactionInterceptor;
	private CompensableTccTransactionInterceptor tccTransactionInterceptor;

	public void beforeSendRequest(TransactionRequest request) throws IllegalStateException {
		TransactionContext transactionContext = request.getTransactionContext();
		if (transactionContext == null) {
			return;
		} else if (transactionContext.isCompensable()) {
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
		TransactionContext transactionContext = response.getTransactionContext();
		if (transactionContext == null) {
			return;
		} else if (transactionContext.isCompensable()) {
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

	public CompensableJtaTransactionInterceptor getJtaTransactionInterceptor() {
		return jtaTransactionInterceptor;
	}

	public void setJtaTransactionInterceptor(CompensableJtaTransactionInterceptor jtaTransactionInterceptor) {
		this.jtaTransactionInterceptor = jtaTransactionInterceptor;
	}

	public CompensableTccTransactionInterceptor getTccTransactionInterceptor() {
		return tccTransactionInterceptor;
	}

	public void setTccTransactionInterceptor(CompensableTccTransactionInterceptor tccTransactionInterceptor) {
		this.tccTransactionInterceptor = tccTransactionInterceptor;
	}

}
