package org.bytesoft.bytetcc.supports.spring.beans;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ByteTccSkeletonInvocationHandler implements ByteTccSkeletonObject, InvocationHandler {
	private Class<?> interfaceClass;
	private String serviceId;
	private Object target;

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

		if (Object.class.equals(method.getDeclaringClass())) {
			return method.invoke(this, args);
		} else if (ByteTccSkeletonObject.class.equals(method.getDeclaringClass())) {
			return method.invoke(this, args);
		}

		try {
			return method.invoke(this.target, args);
		} catch (IllegalAccessException ex) {
			throw new RuntimeException(ex);
		} catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}

	}

	public String getTargetId() {
		return this.serviceId;
	}

	public Class<?> getInterfaceClass() {
		return interfaceClass;
	}

	public void setInterfaceClass(Class<?> interfaceClass) {
		this.interfaceClass = interfaceClass;
	}

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public Object getTarget() {
		return target;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

}
