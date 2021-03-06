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
