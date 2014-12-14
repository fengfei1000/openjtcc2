package org.bytesoft.bytetcc.supports.spring;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import org.bytesoft.bytetcc.CompensableInvocation;
import org.bytesoft.bytetcc.CompensableInvocationExecutor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class CompensableInvocationExecutorImpl implements CompensableInvocationExecutor, ApplicationContextAware {
	private ApplicationContext applicationContext;

	public void confirm(List<CompensableInvocation> invocations) throws RuntimeException {
		Iterator<CompensableInvocation> itr = invocations.iterator();
		while (itr.hasNext()) {
			CompensableInvocation invocation = itr.next();
			Method method = invocation.getMethod();
			Object[] args = invocation.getArgs();
			String beanName = invocation.getConfirmableKey();
			Object instance = this.applicationContext.getBean(beanName);
			try {
				method.invoke(instance, args);
			} catch (InvocationTargetException itex) {
				throw new RuntimeException(itex.getTargetException());
			} catch (Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		}
	}

	public void cancel(List<CompensableInvocation> invocations) throws RuntimeException {
		Iterator<CompensableInvocation> itr = invocations.iterator();
		while (itr.hasNext()) {
			CompensableInvocation invocation = itr.next();
			Method method = invocation.getMethod();
			Object[] args = invocation.getArgs();
			String beanName = invocation.getCancellableKey();
			Object instance = this.applicationContext.getBean(beanName);
			try {
				method.invoke(instance, args);
			} catch (InvocationTargetException itex) {
				throw new RuntimeException(itex.getTargetException());
			} catch (Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		}
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}
}
