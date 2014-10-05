package org.bytesoft.transaction.supports.spring;

import java.lang.reflect.Proxy;

import javax.activation.DataSource;
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
		if (XADataSource.class.isInstance(bean)) {
			ManagedConnectionFactoryInterceptor interceptor = new ManagedConnectionFactoryInterceptor(bean);
			interceptor.setIdentifier(beanName);
			if (DataSource.class.isInstance(bean)) {
				return Proxy.newProxyInstance(bean.getClass().getClassLoader(), new Class<?>[] { DataSource.class,
						XADataSource.class }, interceptor);
			} else {
				return Proxy.newProxyInstance(bean.getClass().getClassLoader(), new Class<?>[] { XADataSource.class },
						interceptor);
			}
		} else if (XAConnectionFactory.class.isInstance(bean)) {
			ManagedConnectionFactoryInterceptor interceptor = new ManagedConnectionFactoryInterceptor(bean);
			interceptor.setIdentifier(beanName);
			if (DataSource.class.isInstance(bean)) {
				return Proxy.newProxyInstance(bean.getClass().getClassLoader(), new Class<?>[] {
						javax.jms.ConnectionFactory.class, XAConnectionFactory.class }, interceptor);
			} else {
				return Proxy.newProxyInstance(bean.getClass().getClassLoader(),
						new Class<?>[] { XAConnectionFactory.class }, interceptor);
			}
		} else if (ManagedConnectionFactory.class.isInstance(bean)) {
			ManagedConnectionFactoryInterceptor interceptor = new ManagedConnectionFactoryInterceptor(bean);
			interceptor.setIdentifier(beanName);
			if (DataSource.class.isInstance(bean)) {
				return Proxy.newProxyInstance(bean.getClass().getClassLoader(), new Class<?>[] {
						javax.resource.cci.ConnectionFactory.class, ManagedConnectionFactory.class }, interceptor);
			} else {
				return Proxy.newProxyInstance(bean.getClass().getClassLoader(),
						new Class<?>[] { ManagedConnectionFactory.class }, interceptor);
			}
		} else {
			return bean;
		}
	}

}
