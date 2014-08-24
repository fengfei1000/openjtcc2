package org.bytesoft.bytetcc;

import java.lang.reflect.Method;

public interface CompensableInvocation {

	public Method getMethod();

	public Object[] getArgs();

	public Object getConfirmableObject();

	public Object getCancellableObject();

}
