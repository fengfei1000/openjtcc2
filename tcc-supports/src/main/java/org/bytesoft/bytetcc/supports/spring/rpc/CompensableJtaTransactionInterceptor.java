package org.bytesoft.bytetcc.supports.spring.rpc;

import org.apache.log4j.Logger;
import org.bytesoft.transaction.rpc.TransactionInterceptor;
import org.bytesoft.transaction.rpc.TransactionRequest;
import org.bytesoft.transaction.rpc.TransactionResponse;

public class CompensableJtaTransactionInterceptor implements TransactionInterceptor {
	static final Logger logger = Logger.getLogger(CompensableJtaTransactionInterceptor.class.getSimpleName());

	public void beforeSendRequest(TransactionRequest request) throws IllegalStateException {
	}

	public void afterReceiveRequest(TransactionRequest request) throws IllegalStateException {
	}

	public void beforeSendResponse(TransactionResponse response) throws IllegalStateException {
	}

	public void afterReceiveResponse(TransactionResponse response) throws IllegalStateException {
	}

}
