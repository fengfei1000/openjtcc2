package org.bytesoft.bytetcc.supports.spring;

import java.util.logging.Logger;

import org.bytesoft.bytetcc.internal.CompensableInvocationImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class SpringCompensableInvocation extends CompensableInvocationImpl implements ApplicationContextAware {
	private static final Logger logger = Logger.getLogger("bytetcc");
	private transient Class<?> confirmClass;
	private transient Class<?> cancellClass;
	// private transient String confirmBeanName;
	// private transient String concallBeanName;
	private ApplicationContext applicationContext;

	public Object getCancellableObject() {
		if (this.confirmClass != null) {
			try {
				return this.applicationContext.getBean(this.cancellClass);
			} catch (RuntimeException rex) {
				logger.warning("Get the cancel-object failed.");
				return null;
			}
		} else {
			return null;
		}
	}

	public Object getConfirmableObject() {
		if (this.confirmClass != null) {
			try {
				return this.applicationContext.getBean(this.confirmClass);
			} catch (RuntimeException rex) {
				logger.warning("Get the confirm-object failed.");
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

	public Class<?> getConfirmClass() {
		return confirmClass;
	}

	public void setConfirmClass(Class<?> confirmClass) {
		this.confirmClass = confirmClass;
	}

	public Class<?> getCancellClass() {
		return cancellClass;
	}

	public void setCancellClass(Class<?> cancellClass) {
		this.cancellClass = cancellClass;
	}

	// public String getConfirmBeanName() {
	// return confirmBeanName;
	// }
	//
	// public void setConfirmBeanName(String confirmBeanName) {
	// this.confirmBeanName = confirmBeanName;
	// }
	//
	// public String getConcallBeanName() {
	// return concallBeanName;
	// }
	//
	// public void setConcallBeanName(String concallBeanName) {
	// this.concallBeanName = concallBeanName;
	// }

}
