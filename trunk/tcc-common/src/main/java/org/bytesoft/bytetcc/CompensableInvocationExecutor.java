package org.bytesoft.bytetcc;

import java.util.List;

public interface CompensableInvocationExecutor {

	public void confirm(List<CompensableInvocation> compensables) throws RuntimeException;

	public void cancel(List<CompensableInvocation> compensables) throws RuntimeException;

}
