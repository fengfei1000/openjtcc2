package org.bytesoft.bytetcc;

public interface CompensableInvocationExecutor {

	public void confirm(CompensableInvocation compensable) throws RuntimeException;

	public void cancel(CompensableInvocation compensable) throws RuntimeException;

}
