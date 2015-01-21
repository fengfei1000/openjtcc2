package org.bytesoft.transaction.supports.rpc;

import javax.transaction.xa.XAResource;

import org.bytesoft.bytejta.common.TransactionConfigurator;
import org.bytesoft.byterpc.supports.ServiceFactoryImpl;
import org.bytesoft.byterpc.svc.ServiceFactory;

public class TransactionServiceFactory extends ServiceFactoryImpl implements ServiceFactory {

	public void initialize() {

		TransactionConfigurator configurator = TransactionConfigurator.getInstance();
		XAResource transactionSkeleton = configurator.getTransactionSkeleton();
		this.putServiceObject(XAResource.class.getName(), XAResource.class, transactionSkeleton);

	}

}
