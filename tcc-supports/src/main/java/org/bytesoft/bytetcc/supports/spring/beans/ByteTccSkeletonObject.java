package org.bytesoft.bytetcc.supports.spring.beans;

public interface ByteTccSkeletonObject {

	public Class<?> getInterfaceClass();

	public String getTargetId();

	public void setTarget(Object target);

}
