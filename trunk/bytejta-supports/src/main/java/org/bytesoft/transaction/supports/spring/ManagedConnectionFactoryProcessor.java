package org.bytesoft.transaction.supports.spring;

import java.lang.reflect.Proxy;

import javax.jms.XAConnectionFactory;
import javax.resource.spi.ManagedConnectionFactory;
import javax.sql.XADataSource;

import org.bytesoft.transaction.supports.resource.ManagedConnectionFactoryInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class ManagedConnectionFactoryProcessor implements BeanPostProcessor {

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

		Class<?> clazz = bean.getClass();
		ClassLoader cl = clazz.getClassLoader();

		Class<?>[] interfaces = clazz.getInterfaces();

		if (XADataSource.class.isInstance(bean)) {
			ManagedConnectionFactoryInterceptor interceptor = new ManagedConnectionFactoryInterceptor(bean);
			interceptor.setIdentifier(beanName);
			return Proxy.newProxyInstance(cl, interfaces, interceptor);
		} else if (XAConnectionFactory.class.isInstance(bean)) {
			ManagedConnectionFactoryInterceptor interceptor = new ManagedConnectionFactoryInterceptor(bean);
			interceptor.setIdentifier(beanName);
			return Proxy.newProxyInstance(cl, interfaces, interceptor);
		} else if (ManagedConnectionFactory.class.isInstance(bean)) {
			ManagedConnectionFactoryInterceptor interceptor = new ManagedConnectionFactoryInterceptor(bean);
			interceptor.setIdentifier(beanName);
			return Proxy.newProxyInstance(cl, interfaces, interceptor);
		} else {
			return bean;
		}
	}

}
