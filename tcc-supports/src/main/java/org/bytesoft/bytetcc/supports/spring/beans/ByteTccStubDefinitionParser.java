package org.bytesoft.bytetcc.supports.spring.beans;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Dispatcher;
import net.sf.cglib.proxy.Enhancer;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

public class ByteTccStubDefinitionParser extends AbstractSingleBeanDefinitionParser {

	protected Class<?> getBeanClass(Element element) {
		String provider = element.getAttribute("provider");
		String serviceId = element.getAttribute("serviceId");
		String interfaceClassName = element.getAttribute("interface");
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Class<?> interfaceClass = null;
		try {
			interfaceClass = cl.loadClass(interfaceClassName);
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}

		final Class<?>[] interfaces = new Class<?>[] { ByteTccStubObject.class, interfaceClass };
		Enhancer enhancer = new Enhancer();
		// enhancer.setSuperclass(ByteTccStubInvocationHandler.class);
		enhancer.setInterfaces(interfaces);
		// enhancer.setCallbackType(ByteTccStubInvocationHandler.class);
		enhancer.setCallbackType(Dispatcher.class);
		Class<?> clazz = enhancer.createClass();

		final ByteTccStubInvocationHandler stub = new ByteTccStubInvocationHandler();
		stub.setProvider(provider);
		stub.setServiceId(serviceId);
		stub.setInterfaceClass(interfaceClass);
		// Enhancer.registerCallbacks(clazz, new Callback[] { stub });
		Enhancer.registerCallbacks(clazz, new Callback[] { new Dispatcher() {
			public Object loadObject() throws Exception {
				return Enhancer.create(Object.class, interfaces, stub);
			}
		} });

		return clazz;
	}

	protected void doParse(Element element, BeanDefinitionBuilder bean) {

		// String provider = element.getAttribute("provider");
		// String serviceId = element.getAttribute("serviceId");
		// String interfaceClassName = element.getAttribute("interface");
		//
		// ClassLoader cl = Thread.currentThread().getContextClassLoader();
		// Class<?> interfaceClass = null;
		// try {
		// interfaceClass = cl.loadClass(interfaceClassName);
		// } catch (ClassNotFoundException ex) {
		// throw new RuntimeException(ex);
		// }
		//
		// Enhancer enhancer = new Enhancer();
		// enhancer.setSuperclass(ByteTccStubInvocationHandler.class);
		// enhancer.setInterfaces(new Class<?>[] { ByteTccStubObject.class, interfaceClass });
		// enhancer.setCallbackType(ByteTccStubInvocationHandler.class);
		//
		// bean.addPropertyValue("provider", provider);
		// bean.addPropertyValue("serviceId", serviceId);
		// bean.addPropertyValue("interfaceClass", interfaceClass);

	}
}