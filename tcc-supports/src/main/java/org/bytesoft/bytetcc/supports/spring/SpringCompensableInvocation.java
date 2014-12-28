package org.bytesoft.bytetcc.supports.spring;

import org.apache.log4j.Logger;
import org.bytesoft.bytetcc.internal.CompensableInvocationImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class SpringCompensableInvocation extends CompensableInvocationImpl implements ApplicationContextAware {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger("bytetcc");

	private Class<?> interfaceClass;
	private transient ApplicationContext applicationContext;

	public Object getCancellableObject() {
		String cancellableKey = this.getCancellableKey();
		if (cancellableKey != null) {
			try {
				Object cancellableObject = this.applicationContext.getBean(cancellableKey);
				if (this.interfaceClass.isInstance(cancellableObject)) {
					return cancellableObject;
				}
				throw new IllegalStateException();
			} catch (RuntimeException rex) {
				logger.warn("Get the cancel-object failed.");
				return null;
			}
		} else {
			return null;
		}
	}

	public Object getConfirmableObject() {
		String confirmableKey = this.getConfirmableKey();
		if (confirmableKey != null) {
			try {
				Object confirmableObject = this.applicationContext.getBean(confirmableKey);
				if (this.interfaceClass.isInstance(confirmableObject)) {
					return confirmableObject;
				}
				throw new IllegalStateException();
			} catch (RuntimeException rex) {
				logger.warn("Get the confirm-object failed.");
				return null;
			}
		} else {
			return null;
		}
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public Class<?> getInterfaceClass() {
		return interfaceClass;
	}

	public void setInterfaceClass(Class<?> interfaceClass) {
		this.interfaceClass = interfaceClass;
	}

}
