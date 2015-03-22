package org.bytesoft.transaction.supports.spring;

import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;

import org.bytesoft.transaction.rpc.TransactionResource;
import org.bytesoft.transaction.serialize.XAResourceSerializer;
import org.bytesoft.transaction.xa.XAResourceDescriptor;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public abstract class AbstractXAResourceSerializer implements XAResourceSerializer, ApplicationContextAware {
	private static Pattern pattern = Pattern.compile("^[^:]+\\s*:\\s*\\d+$");
	private ApplicationContext applicationContext;

	public abstract TransactionResource deserializeTransactionResource(String identifier) throws IOException;

	public XAResourceDescriptor deserialize(String identifier) throws IOException {
		Object bean = null;
		try {
			bean = this.applicationContext.getBean(identifier);
		} catch (BeansException bex) {
			Matcher matcher = pattern.matcher(identifier);
			if (matcher.find()) {
				bean = this.deserializeTransactionResource(identifier);
			} else {
				throw new IllegalStateException(bex);
			}
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
		} else if (bean != null && TransactionResource.class.isInstance(bean)) {
			TransactionResource resource = (TransactionResource) bean;
			XAResourceDescriptor descriptor = new XAResourceDescriptor();
			descriptor.setDelegate(resource);
			descriptor.setIdentifier(resource.getIdentifier());
			descriptor.setRemote(true);
			descriptor.setSupportsXA(true);
			return descriptor;
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
