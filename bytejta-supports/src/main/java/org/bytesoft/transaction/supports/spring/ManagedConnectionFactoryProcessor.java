/**
 * Copyright 2014-2015 yangming.liu<liuyangming@gmail.com>.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, see <http://www.gnu.org/licenses/>.
 */
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
