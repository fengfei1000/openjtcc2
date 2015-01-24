package org.bytesoft.bytetcc.supports.spring.beans;

import org.springframework.context.ApplicationContextAware;

public interface ByteTccSkeletonObject extends ApplicationContextAware {

	public Class<?> getInterfaceClass();

}
