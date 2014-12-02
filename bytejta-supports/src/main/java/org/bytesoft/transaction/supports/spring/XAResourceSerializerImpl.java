package org.bytesoft.transaction.supports.spring;

import java.io.IOException;
import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;

import org.bytesoft.transaction.serialize.XAResourceSerializer;
import org.bytesoft.transaction.xa.XAResourceDescriptor;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class XAResourceSerializerImpl implements XAResourceSerializer, ApplicationContextAware {
	private ApplicationContext applicationContext;

	public XAResourceDescriptor deserialize(String identifier) throws IOException {
		Object bean = null;
		try {
			bean = this.applicationContext.getBean(identifier);
		} catch (BeansException bex) {
			throw new IllegalStateException(bex);
		}
		if (bean != null && XADataSource.class.isInstance(bean)) {
			try {
				XADataSource xaDataSource = (XADataSource) bean;
				XAConnection xaConnection = xaDataSource.getXAConnection();
				XAResource xaResource = xaConnection.getXAResource();
				XAResourceDescriptor descriptor = new XAResourceDescriptor();
				descriptor.setIdentifier(identifier);
				descriptor.setDelegate(xaResource);
				descriptor.setRemote(false);
				descriptor.setSupportsXA(true);
				return descriptor;
			} catch (SQLException ex) {
				throw new IOException(ex);
			}
		}
		throw new IllegalStateException();
	}

	public String serialize(XAResourceDescriptor descriptor) throws IOException {
		return descriptor.getIdentifier();
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

}