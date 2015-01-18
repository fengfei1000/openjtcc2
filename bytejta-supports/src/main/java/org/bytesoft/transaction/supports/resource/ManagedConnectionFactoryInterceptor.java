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
		Class<?> clazz = resultObject.getClass();
		ClassLoader cl = clazz.getClassLoader();

		boolean containsReturningClass = false;
		Class<?>[] interfaces = clazz.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			Class<?> interfaceClass = interfaces[i];
			if (interfaceClass.equals(returningClass)) {
				containsReturningClass = true;
				break;
			}
		}

		if (XADataSource.class.equals(declaringClass) && javax.sql.XAConnection.class.equals(returningClass)) {
			ManagedConnectionInterceptor interceptor = new ManagedConnectionInterceptor(resultObject);
			interceptor.setIdentifier(this.identifier);

			Object finalObject = null;
			if (containsReturningClass) {
				finalObject = Proxy.newProxyInstance(cl, interfaces, interceptor);
			} else {
				Class<?>[] interfaceArray = new Class<?>[interfaces.length];
				System.arraycopy(interfaces, 0, interfaceArray, 0, interfaces.length);
				interfaceArray[interfaces.length] = javax.sql.XAConnection.class;
				finalObject = Proxy.newProxyInstance(cl, interfaceArray, interceptor);
			}
			return (javax.sql.XAConnection) finalObject;
		} else if (XAConnectionFactory.class.equals(declaringClass) && javax.jms.XAConnection.class.equals(returningClass)) {
			ManagedConnectionInterceptor interceptor = new ManagedConnectionInterceptor(resultObject);
			interceptor.setIdentifier(this.identifier);

			Object finalObject = null;
			if (containsReturningClass) {
				finalObject = Proxy.newProxyInstance(cl, interfaces, interceptor);
			} else {
				Class<?>[] interfaceArray = new Class<?>[interfaces.length];
				System.arraycopy(interfaces, 0, interfaceArray, 0, interfaces.length);
				interfaceArray[interfaces.length] = javax.jms.XAConnection.class;
				finalObject = Proxy.newProxyInstance(cl, interfaceArray, interceptor);
			}
			return (javax.jms.XAConnection) finalObject;
		} else if (ManagedConnectionFactory.class.equals(declaringClass) && ManagedConnection.class.equals(returningClass)) {
			ManagedConnectionInterceptor interceptor = new ManagedConnectionInterceptor(resultObject);
			interceptor.setIdentifier(this.identifier);

			Object finalObject = null;
			if (containsReturningClass) {
				finalObject = Proxy.newProxyInstance(cl, interfaces, interceptor);
			} else {
				Class<?>[] interfaceArray = new Class<?>[interfaces.length];
				System.arraycopy(interfaces, 0, interfaceArray, 0, interfaces.length);
				interfaceArray[interfaces.length] = ManagedConnection.class;
				finalObject = Proxy.newProxyInstance(cl, interfaceArray, interceptor);
			}

			return (ManagedConnection) finalObject;
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
