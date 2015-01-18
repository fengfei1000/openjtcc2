package org.bytesoft.bytetcc.supports.spring.beans;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class ByteTccNamespaceHandler extends NamespaceHandlerSupport {

	public void init() {
		this.registerBeanDefinitionParser("stub", new ByteTccStubDefinitionParser());
		this.registerBeanDefinitionParser("skeleton", new ByteTccSkeletonDefinitionParser());
	}

}
