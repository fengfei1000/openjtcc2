package org.bytesoft.bytetcc.supports.spring.rpc;

import org.bytesoft.byterpc.RemoteInvocation;
import org.bytesoft.byterpc.RemoteInvocationResult;
import org.bytesoft.byterpc.wire.RemoteInterceptor;
import org.bytesoft.bytetcc.common.TransactionConfigurator;
import org.bytesoft.transaction.rpc.TransactionInterceptor;

public class ByteTccRemoteInterceptor implements RemoteInterceptor {

	public void onDeliverInvocation(RemoteInvocation invocation) {
		ByteTccRemoteInvocation request = (ByteTccRemoteInvocation) invocation;
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		TransactionInterceptor interceptor = configurator.getTransactionInterceptor();
		interceptor.beforeSendRequest(request);
	}

	public void onReceiveInvocation(RemoteInvocation invocation) {
		ByteTccRemoteInvocation request = (ByteTccRemoteInvocation) invocation;
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		TransactionInterceptor interceptor = configurator.getTransactionInterceptor();
		interceptor.afterReceiveRequest(request);
	}

	public void onDeliverInvocationResult(RemoteInvocationResult result) {
		ByteTccRemoteInvocationResult response = (ByteTccRemoteInvocationResult) result;
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		TransactionInterceptor interceptor = configurator.getTransactionInterceptor();
		interceptor.beforeSendResponse(response);
	}

	public void onReceiveInvocationResult(RemoteInvocationResult result) {
		ByteTccRemoteInvocationResult response = (ByteTccRemoteInvocationResult) result;
		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		TransactionInterceptor interceptor = configurator.getTransactionInterceptor();
		interceptor.afterReceiveResponse(response);
	}

}
