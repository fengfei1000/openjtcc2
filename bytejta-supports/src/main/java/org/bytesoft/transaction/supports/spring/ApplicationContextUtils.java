package org.bytesoft.transaction.supports.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ApplicationContextUtils implements ApplicationContextAware {
	private static final ApplicationContextUtils instance = new ApplicationContextUtils();
	private ApplicationContext context;

	private ApplicationContextUtils() {
	}

	public static ApplicationContextUtils getInstance() {
		return instance;
	}

	public static ApplicationContext getCurrentApplicationContext() {
		return instance.getContext();
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		instance.setContext(applicationContext);
	}

	public ApplicationContext getApplicationContext() {
		return instance.getContext();
	}

	public ApplicationContext getContext() {
		return context;
	}

	public void setContext(ApplicationContext context) {
		this.context = context;
	}

}
