package org.bytesoft.bytetcc.supports.spring;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.bytesoft.bytetcc.internal.CompensableInvocationRegistryImpl;

public class CompensableNativeHandler implements java.lang.reflect.InvocationHandler,
		net.sf.cglib.proxy.InvocationHandler, org.springframework.cglib.proxy.InvocationHandler {

	private Object delegate;
	private String beanName;
	private Class<?> targetClass;
	private Class<?> attemptClass;
	private Class<?> confirmClass;
	private Class<?> cancellClass;

	private void checkIsCurrentCompensable(Object proxy, Method method, Object[] args) throws IllegalAccessException {

		Class<?> declaringClass = method.getDeclaringClass();
		if (declaringClass.equals(this.attemptClass)) {
			// ignore
		} else {
			throw new IllegalAccessException();
		}

	}

	private void checkSerialization(Object proxy, Method method, Object[] args) throws IllegalArgumentException {
		for (int i = 0; i < args.length; i++) {
			Object obj = args[i];
			if (Serializable.class.isInstance(obj) == false) {
				String format = "The param(index= %s, type: %s) of method(%s) cannot be serialize.";
				throw new IllegalArgumentException(String.format(format, i, obj.getClass().getName(), method));
			}
		}
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

		Class<?> declaring = method.getDeclaringClass();
		if (Object.class.equals(declaring)) {
			return this.handleInvocation(proxy, method, args);
		}

		try {
			this.checkIsCurrentCompensable(proxy, method, args);
		} catch (IllegalAccessException ex) {
			return this.handleInvocation(proxy, method, args);
		}

		this.checkSerialization(proxy, method, args);

		SpringCompensableInvocation invocation = new SpringCompensableInvocation();
		invocation.setMethod(method);
		invocation.setArgs(args);
		invocation.setConfirmClass(this.confirmClass);
		invocation.setCancellClass(this.cancellClass);

		CompensableInvocationRegistryImpl registry = CompensableInvocationRegistryImpl.getInstance();
		try {
			registry.registerCompensableInvocation(invocation);
			return this.handleInvocation(proxy, method, args);
		} finally {
			registry.unregisterCompensableInvocation(invocation);
		}

	}

	public Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
		if (java.lang.reflect.InvocationHandler.class.isInstance(this.delegate)) {
			return ((java.lang.reflect.InvocationHandler) this.delegate).invoke(proxy, method, args);
		} else if (net.sf.cglib.proxy.InvocationHandler.class.isInstance(this.delegate)) {
			return ((net.sf.cglib.proxy.InvocationHandler) this.delegate).invoke(proxy, method, args);
		} else if (org.springframework.cglib.proxy.InvocationHandler.class.isInstance(this.delegate)) {
			return ((org.springframework.cglib.proxy.InvocationHandler) this.delegate).invoke(proxy, method, args);
		} else {
			throw new RuntimeException();
		}
	}

	public Object getDelegate() {
		return delegate;
	}

	public void setDelegate(Object delegate) {
		this.delegate = delegate;
	}

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public Class<?> getAttemptClass() {
		return attemptClass;
	}

	public void setAttemptClass(Class<?> attemptClass) {
		this.attemptClass = attemptClass;
	}

	public Class<?> getConfirmClass() {
		return confirmClass;
	}

	public void setConfirmClass(Class<?> confirmClass) {
		this.confirmClass = confirmClass;
	}

	public Class<?> getCancellClass() {
		return cancellClass;
	}

	public void setCancellClass(Class<?> cancellClass) {
		this.cancellClass = cancellClass;
	}

	public Class<?> getTargetClass() {
		return targetClass;
	}

	public void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

}
