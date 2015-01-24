package org.bytesoft.bytetcc.supports.spring.beans;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Dispatcher;
import net.sf.cglib.proxy.Enhancer;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

public class ByteTccSkeletonDefinitionParser extends AbstractSingleBeanDefinitionParser {

	protected Class<?> getBeanClass(Element element) {
		String serviceId = element.getAttribute("serviceId");
		String interfaceClassName = element.getAttribute("interface");

		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Class<?> interfaceClass = null;
		try {
			interfaceClass = cl.loadClass(interfaceClassName);
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}

		final Class<?>[] interfaces = new Class<?>[] { ByteTccSkeletonObject.class, interfaceClass };
		Enhancer enhancer = new Enhancer();
		enhancer.setInterfaces(interfaces);
		enhancer.setCallbackType(Dispatcher.class);
		// enhancer.setCallbackType(ByteTccSkeletonInvocationHandler.class);
		Class<?> clazz = enhancer.createClass();

		final ByteTccSkeletonInvocationHandler skeleton = new ByteTccSkeletonInvocationHandler();
		skeleton.setServiceId(serviceId);
		skeleton.setInterfaceClass(interfaceClass);
		// Enhancer.registerCallbacks(clazz, new Callback[] { skeleton });
		Enhancer.registerCallbacks(clazz, new Callback[] { new Dispatcher() {
			public Object loadObject() throws Exception {
				return Enhancer.create(Object.class, interfaces, skeleton);
			}
		} });

		return clazz;
	}

	protected void doParse(Element element, BeanDefinitionBuilder bean) {

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
		// bean.addPropertyValue("serviceId", serviceId);
		// bean.addPropertyValue("interfaceClass", interfaceClass);

	}
}
