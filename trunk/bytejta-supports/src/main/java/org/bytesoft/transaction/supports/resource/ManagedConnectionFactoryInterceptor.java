package org.bytesoft.transaction.supports.resource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.jms.XAConnectionFactory;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.sql.XADataSource;

public class ManagedConnectionFactoryInterceptor implements InvocationHandler {

	private final Object delegate;
	private String identifier;

	public ManagedConnectionFactoryInterceptor(Object xads) {
		this.delegate = xads;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Class<?> declaringClass = method.getDeclaringClass();
		Class<?> returningClass = method.getReturnType();

		Object resultObject = method.invoke(this.delegate, args);

		if (XADataSource.class.equals(declaringClass) && javax.sql.XAConnection.class.equals(returningClass)) {
			ManagedConnectionInterceptor interceptor = new ManagedConnectionInterceptor(resultObject);
			interceptor.setIdentifier(this.identifier);
			return (javax.sql.XAConnection) Proxy.newProxyInstance(resultObject.getClass().getClassLoader(),
					new Class<?>[] { javax.sql.XAConnection.class }, interceptor);
		} else if (XAConnectionFactory.class.equals(declaringClass)
				&& javax.jms.XAConnection.class.equals(returningClass)) {
			ManagedConnectionInterceptor interceptor = new ManagedConnectionInterceptor(resultObject);
			interceptor.setIdentifier(this.identifier);
			return (javax.jms.XAConnection) Proxy.newProxyInstance(resultObject.getClass().getClassLoader(),
					new Class<?>[] { javax.jms.XAConnection.class }, interceptor);
		} else if (ManagedConnectionFactory.class.equals(declaringClass)
				&& ManagedConnection.class.equals(returningClass)) {
			ManagedConnectionInterceptor interceptor = new ManagedConnectionInterceptor(resultObject);
			interceptor.setIdentifier(this.identifier);
			return (ManagedConnection) Proxy.newProxyInstance(resultObject.getClass().getClassLoader(),
					new Class<?>[] { ManagedConnection.class }, interceptor);
		} else {
			return resultObject;
		}
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

}
