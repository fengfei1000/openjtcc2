package org.bytesoft.bytetcc.supports.internal;

import org.apache.log4j.Logger;
import org.bytesoft.transaction.rpc.TransactionInterceptor;
import org.bytesoft.transaction.rpc.TransactionRequest;
import org.bytesoft.transaction.rpc.TransactionResponse;

public class JtaTransactionInterceptor implements TransactionInterceptor {
	static final Logger logger = Logger.getLogger("bytetcc");

	// private TerminatorMarshaller terminatorMarshaller;
	// private CompensableTransactionManager transactionManager;

	public void beforeSendRequest(TransactionRequest request) throws IllegalStateException {
	}

	public void afterReceiveResponse(TransactionResponse response) throws IllegalStateException {
	}

	public void afterReceiveRequest(TransactionRequest request) throws IllegalStateException {
	}

	public void beforeSendResponse(TransactionResponse response) throws IllegalStateException {
	}

}
