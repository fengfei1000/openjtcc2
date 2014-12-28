package org.bytesoft.bytetcc;

import java.io.Serializable;
import java.lang.reflect.Method;

public interface CompensableInvocation extends Serializable {

	public Method getMethod();

	public Object[] getArgs();

	public String getConfirmableKey();

	public String getCancellableKey();

	public Object getIdentifier();

	public void setIdentifier(Object identifier);

}
