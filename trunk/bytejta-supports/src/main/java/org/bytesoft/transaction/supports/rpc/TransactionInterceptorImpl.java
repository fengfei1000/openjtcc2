package org.bytesoft.transaction.supports.rpc;

import java.util.logging.Logger;

import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;

import org.bytesoft.bytejta.TransactionImpl;
import org.bytesoft.bytejta.TransactionManagerImpl;
import org.bytesoft.bytejta.common.TransactionConfigurator;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.rpc.TransactionInterceptor;
import org.bytesoft.transaction.rpc.TransactionRequest;
import org.bytesoft.transaction.rpc.TransactionResource;
import org.bytesoft.transaction.rpc.TransactionResponse;
import org.bytesoft.transaction.xa.TransactionXid;
import org.bytesoft.transaction.xa.XAResourceDescriptor;

public class TransactionInterceptorImpl implements TransactionInterceptor {
	static final Logger logger = Logger.getLogger(TransactionInterceptorImpl.class.getSimpleName());

	public void beforeSendRequest(TransactionRequest request) throws IllegalStateException {
		TransactionImpl transaction = this.getCurrentTransaction();
		if (transaction != null) {
			TransactionContext srcTransactionContext = transaction.getTransactionContext();
			TransactionContext transactionContext = srcTransactionContext.clone();
			TransactionXid currentXid = srcTransactionContext.getCurrentXid();
			TransactionXid globalXid = currentXid.getGlobalXid();
			transactionContext.setCurrentXid(globalXid);
			byte[] bytes = currentXid.getBranchQualifier();
			TransactionCredential credential = new TransactionCredential(bytes);
			transactionContext.setPropagated(credential);
			request.setTransactionContext(transactionContext);

			try {
				TransactionResource resource = request.getTransactionResource();
				boolean nonxaResourceExists = transactionContext.isNonxaResourceAllowed() == false;
				XAResourceDescriptor descriptor = this.createResourceDescriptor(resource, nonxaResourceExists);
				transaction.enlistResource(descriptor);
			} catch (IllegalStateException ex) {
				logger.throwing(TransactionInterceptorImpl.class.getName(), "beforeSendRequest(TransactionRequest)", ex);
				throw ex;
			} catch (RollbackException ex) {
				transaction.setRollbackOnlyQuietly();
				logger.throwing(TransactionInterceptorImpl.class.getName(), "beforeSendRequest(TransactionRequest)", ex);
				throw new IllegalStateException(ex);
			} catch (SystemException ex) {
				logger.throwing(TransactionInterceptorImpl.class.getName(), "beforeSendRequest(TransactionRequest)", ex);
				throw new IllegalStateException(ex);
			}
		}
	}

	public void beforeSendResponse(TransactionResponse response) throws IllegalStateException {
		TransactionImpl transaction = this.getCurrentTransaction();
		if (transaction != null) {
			TransactionContext srcTransactionContext = transaction.getTransactionContext();
			TransactionContext transactionContext = srcTransactionContext.clone();
			response.setTransactionContext(transactionContext);
		}
	}

	public void afterReceiveRequest(TransactionRequest request) throws IllegalStateException {

		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		TransactionManagerImpl transactionManager = transactionConfigurator.getTransactionManager();

		TransactionContext srcTransactionContext = request.getTransactionContext();
		if (srcTransactionContext != null) {
			TransactionContext transactionContext = srcTransactionContext.clone();
			try {
				transactionManager.propagationBegin(transactionContext);
			} catch (SystemException ex) {
				IllegalStateException exception = new IllegalStateException();
				exception.initCause(ex);
				throw exception;
			} catch (NotSupportedException ex) {
				IllegalStateException exception = new IllegalStateException();
				exception.initCause(ex);
				throw exception;
			}
		}
	}

	public void afterReceiveResponse(TransactionResponse response) throws IllegalStateException {

		TransactionImpl transaction = this.getCurrentTransaction();
		if (transaction != null) {
			TransactionContext nativeTransactionContext = transaction.getTransactionContext();
			TransactionContext remoteTransactionContext = response.getTransactionContext();
			if (remoteTransactionContext != null) {

				TransactionXid currentXid = nativeTransactionContext.getCurrentXid();
				byte[] bytes = currentXid.getBranchQualifier();
				Object nativeCredential = new TransactionCredential(bytes);
				Object remoteCredential = remoteTransactionContext.getPropagated();

				if (nativeCredential.equals(remoteCredential)) {
					try {
						TransactionResource resource = response.getTransactionResource();
						boolean nativeAllowedNonxaResource = nativeTransactionContext.isNonxaResourceAllowed();
						boolean remoteAllowedNonxaResource = remoteTransactionContext.isNonxaResourceAllowed();
						if (nativeAllowedNonxaResource && remoteAllowedNonxaResource == false) {
							nativeTransactionContext.setNonxaResourceAllowed(false);
						}
						XAResourceDescriptor descriptor = this.createResourceDescriptor(resource,
								remoteAllowedNonxaResource);
						transaction.delistResource(descriptor, XAResource.TMSUCCESS);
					} catch (IllegalStateException ex) {
						logger.throwing(TransactionInterceptorImpl.class.getName(),
								"afterReceiveResponse(TransactionRequest)", ex);
						throw ex;
					} catch (SystemException ex) {
						logger.throwing(TransactionInterceptorImpl.class.getName(),
								"afterReceiveResponse(TransactionRequest)", ex);
						throw new IllegalStateException(ex);
					}
				}
			}
		}// end-if(transaction!=null)

	}

	private TransactionImpl getCurrentTransaction() throws IllegalStateException {
		TransactionConfigurator transactionConfigurator = TransactionConfigurator.getInstance();
		TransactionManagerImpl transactionManager = transactionConfigurator.getTransactionManager();

		try {
			return transactionManager.getTransaction();
		} catch (SystemException ex) {
			return null;
		}
	}

	private XAResourceDescriptor createResourceDescriptor(TransactionResource resource, boolean nonxaResourceExists) {
		XAResourceDescriptor descriptor = new XAResourceDescriptor();
		descriptor.setDelegate(resource);
		descriptor.setIdentifier(resource.getIdentifier());
		descriptor.setRemote(true);
		// TODO descriptor.setSupportsXA(nonxaResourceExists);
		descriptor.setSupportsXA(true);
		return descriptor;
	}
}
