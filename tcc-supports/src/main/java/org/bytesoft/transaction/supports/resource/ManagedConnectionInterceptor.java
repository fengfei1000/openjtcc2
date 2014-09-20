package org.bytesoft.transaction.supports.resource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import javax.resource.spi.ManagedConnection;
import javax.transaction.xa.XAResource;

import org.bytesoft.transaction.xa.XAResourceDescriptor;

public class ManagedConnectionInterceptor implements InvocationHandler {

	private final Object delegate;
	private String identifier;

	public ManagedConnectionInterceptor(Object managed) {
		this.delegate = managed;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Class<?> declaringClass = method.getDeclaringClass();
		Class<?> returningClass = method.getReturnType();

		Object resultObject = method.invoke(this.delegate, args);

		if (javax.sql.XAConnection.class.equals(declaringClass) && XAResource.class.equals(returningClass)) {
			return this.createProxyResource((XAResource) resultObject);
		} else if (javax.jms.XAConnection.class.equals(declaringClass) && XAResource.class.equals(returningClass)) {
			return this.createProxyResource((XAResource) resultObject);
		} else if (ManagedConnection.class.equals(declaringClass) && XAResource.class.equals(returningClass)) {
			return this.createProxyResource((XAResource) resultObject);
		} else {
			return resultObject;
		}
	}

	private XAResource createProxyResource(XAResource xares) {
		XAResourceDescriptor descriptor = new XAResourceDescriptor();
		descriptor.setIdentifier(this.identifier);
		descriptor.setDelegate(xares);
		descriptor.setRemote(false);
		descriptor.setSupportsXA(true);
		return descriptor;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

}
