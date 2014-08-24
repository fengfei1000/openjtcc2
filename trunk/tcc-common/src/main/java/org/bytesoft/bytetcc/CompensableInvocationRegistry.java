package org.bytesoft.bytetcc;

public interface CompensableInvocationRegistry {

	public void registerCompensableInvocation(CompensableInvocation invocation);

	public void unregisterCompensableInvocation(CompensableInvocation invocation);

}
