package org.bytesoft.bytetcc.supports.spring.beans;

import javax.transaction.xa.XAResource;

import org.bytesoft.byterpc.svc.ServiceFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class ByteTccPostProcessor implements BeanFactoryPostProcessor {

	private ServiceFactory serviceFactory;
	private XAResource transactionSkeleton;

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// this.serviceFactory = beanFactory.getBean(ServiceFactory.class);
		this.processStubObjects(beanFactory);
		this.processSkeletonObjects(beanFactory);
		this.processTransactionalObjects();
	}

	private void processTransactionalObjects() {
		// TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		// XAResource transactionSkeleton = configurator.getTransactionSkeleton();
		serviceFactory.putServiceObject(XAResource.class.getName(), XAResource.class, this.transactionSkeleton);
	}

	private void processStubObjects(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		String[] beanNames = beanFactory.getBeanNamesForType(ByteTccStubObject.class);
		for (int i = 0; i < beanNames.length; i++) {
			String beanName = beanNames[i];
			ByteTccStubObject bean = (ByteTccStubObject) beanFactory.getBean(beanName);
			Class<?> interfaceClass = bean.getInterfaceClass();
			this.serviceFactory.putServiceObject(beanName, interfaceClass, bean);
		}
	}

	private void processSkeletonObjects(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		String[] beanNames = beanFactory.getBeanNamesForType(ByteTccSkeletonObject.class);
		for (int i = 0; i < beanNames.length; i++) {
			String beanName = beanNames[i];
			ByteTccSkeletonObject bean = (ByteTccSkeletonObject) beanFactory.getBean(beanName);
			String targetId = bean.getTargetId();
			bean.setTarget(beanFactory.getBean(targetId));
			Class<?> interfaceClass = bean.getInterfaceClass();
			this.serviceFactory.putServiceObject(beanName, interfaceClass, bean);
		}
	}

	public ServiceFactory getServiceFactory() {
		return serviceFactory;
	}

	public void setServiceFactory(ServiceFactory serviceFactory) {
		this.serviceFactory = serviceFactory;
	}

	public XAResource getTransactionSkeleton() {
		return transactionSkeleton;
	}

	public void setTransactionSkeleton(XAResource transactionSkeleton) {
		this.transactionSkeleton = transactionSkeleton;
	}

}
