package org.bytesoft.bytetcc.supports.spring;

import javax.transaction.TransactionManager;

import org.bytesoft.bytetcc.Compensable;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class CompensablePostProcessor implements BeanFactoryPostProcessor, BeanPostProcessor, ApplicationContextAware {

	private ApplicationContext applicationContext;
	private TransactionManager transactionManager;

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (TargetClassAware.class.isInstance(bean)) {
			TargetClassAware tca = (TargetClassAware) bean;
			Class<?> targetClass = tca.getTargetClass();
			Compensable compensable = targetClass.getAnnotation(Compensable.class);

			if (compensable != null) {
				ProxyFactoryBean pfb = new ProxyFactoryBean();
				CompensableNativeHandler handler = new CompensableNativeHandler();
				handler.setBeanName(beanName);
				Object target = null;
				ClassLoader classLoader = targetClass.getClassLoader();
				Class<?>[] interfaces = targetClass.getInterfaces();

				if (java.lang.reflect.Proxy.isProxyClass(bean.getClass())) {
					java.lang.reflect.InvocationHandler delegate = java.lang.reflect.Proxy.getInvocationHandler(bean);
					handler.setDelegate(delegate);
					target = java.lang.reflect.Proxy.newProxyInstance(classLoader, interfaces, handler);
				} else if (net.sf.cglib.proxy.Proxy.isProxyClass(bean.getClass())) {
					net.sf.cglib.proxy.InvocationHandler delegate = net.sf.cglib.proxy.Proxy.getInvocationHandler(bean);
					handler.setDelegate(delegate);
					target = net.sf.cglib.proxy.Proxy.newProxyInstance(classLoader, interfaces, handler);
				} else if (org.springframework.cglib.proxy.Proxy.isProxyClass(bean.getClass())) {
					org.springframework.cglib.proxy.InvocationHandler delegate = org.springframework.cglib.proxy.Proxy
							.getInvocationHandler(bean);
					handler.setDelegate(delegate);
					target = org.springframework.cglib.proxy.Proxy.newProxyInstance(classLoader, interfaces, handler);
				} else {
					return bean;
				}

				Class<?> attemptClass = compensable.interfaceClass();
				// String confirmableKey = compensable.confirmableKey();
				// String cancellableKey = compensable.cancellableKey();

				if (attemptClass.isInterface() == false) {
					throw new IllegalStateException("Compensable's attempt Class must be a interface.");
				} /*
				 * else if (confirmClass.isInterface()) { throw new
				 * IllegalStateException("Compensable's confirm Class cannot be a interface."); } else if
				 * (cancellClass.isInterface()) { throw new
				 * IllegalStateException("Compensable's cancel Class cannot be a interface."); }
				 */

				handler.setTargetClass(targetClass);
				handler.setAttemptClass(attemptClass);
				// if (Object.class.equals(confirmClass) == false) {
				// handler.setConfirmClass(confirmClass);
				// }
				// if (Object.class.equals(cancellClass) == false) {
				// handler.setCancellClass(cancellClass);
				// }

				pfb.setTarget(target);
				pfb.setInterfaces(interfaces);

				return pfb;
			} else {
				return bean;
			}

		} else {
			return bean;
		}

	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public TransactionManager getTransactionManager() {
		return transactionManager;
	}

	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

}
