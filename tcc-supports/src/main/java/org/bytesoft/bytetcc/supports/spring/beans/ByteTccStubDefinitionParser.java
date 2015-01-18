package org.bytesoft.bytetcc.supports.spring.beans;

import java.lang.reflect.Proxy;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

public class ByteTccStubDefinitionParser extends AbstractSingleBeanDefinitionParser {

	protected Class<?> getBeanClass(Element element) {
		return ProxyFactoryBean.class;
	}

	protected void doParse(Element element, BeanDefinitionBuilder bean) {

		String provider = element.getAttribute("provider");
		String serviceId = element.getAttribute("serviceId");
		String interfaceClassName = element.getAttribute("interface");

		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		ByteTccStubInvocationHandler target = new ByteTccStubInvocationHandler();
		Class<?> interfaceClass = null;
		try {
			interfaceClass = cl.loadClass(interfaceClassName);
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
		target.setProvider(provider);
		target.setServiceId(serviceId);
		target.setInterfaceClass(interfaceClass);

		Object proxyBean = Proxy.newProxyInstance(cl, new Class<?>[] { ByteTccStubObject.class, interfaceClass }, target);

		bean.addPropertyValue("target", proxyBean);
		bean.addPropertyValue("interfaces", new Class<?>[] { ByteTccStubObject.class, interfaceClass });

	}
}
