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
import org.bytesoft.transaction.rpc.TransactionalInterceptor;
import org.bytesoft.transaction.rpc.TransactionalRequest;
import org.bytesoft.transaction.rpc.TransactionalResource;
import org.bytesoft.transaction.rpc.TransactionalResponse;
import org.bytesoft.transaction.xa.TransactionXid;
import org.bytesoft.transaction.xa.XAResourceDescriptor;

public class TransactionalInterceptorImpl implements TransactionalInterceptor {
	static final Logger logger = Logger.getLogger(TransactionalInterceptorImpl.class.getSimpleName());

	public void beforeSendRequest(TransactionalRequest request) throws IllegalStateException {
		TransactionImpl transaction = this.getCurrentTransaction();
		if (transaction != null) {
			TransactionContext srcTransactionContext = transaction.getTransactionContext();
			TransactionContext transactionContext = srcTransactionContext.clone();
			TransactionXid currentXid = srcTransactionContext.getCurrentXid();
			TransactionXid globalXid = currentXid.getGlobalXid();
			transactionContext.setCurrentXid(globalXid);
			byte[] bytes = currentXid.getBranchQualifier();
			TransactionalCredential credential = new TransactionalCredential(bytes);
			transactionContext.setPropagated(credential);
			request.setTransactionContext(transactionContext);

			try {
				TransactionalResource resource = request.getTransactionalResource();
				boolean nonxaResourceExists = transactionContext.isNonxaResourceAllowed() == false;
				XAResourceDescriptor descriptor = this.createResourceDescriptor(resource, nonxaResourceExists);
				transaction.enlistResource(descriptor);
			} catch (IllegalStateException ex) {
				logger.throwing(TransactionalInterceptorImpl.class.getName(),
						"beforeSendRequest(TransactionalRequest)", ex);
				throw ex;
			} catch (RollbackException ex) {
				transaction.setRollbackOnlyQuietly();
				logger.throwing(TransactionalInterceptorImpl.class.getName(),
						"beforeSendRequest(TransactionalRequest)", ex);
				throw new IllegalStateException(ex);
			} catch (SystemException ex) {
				logger.throwing(TransactionalInterceptorImpl.class.getName(),
						"beforeSendRequest(TransactionalRequest)", ex);
				throw new IllegalStateException(ex);
			}
		}
	}

	public void beforeSendResponse(TransactionalResponse response) throws IllegalStateException {
		TransactionImpl transaction = this.getCurrentTransaction();
		if (transaction != null) {
			TransactionContext srcTransactionContext = transaction.getTransactionContext();
			TransactionContext transactionContext = srcTransactionContext.clone();
			response.setTransactionContext(transactionContext);
		}
	}

	public void afterReceiveRequest(TransactionalRequest request) throws IllegalStateException {

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

	public void afterReceiveResponse(TransactionalResponse response) throws IllegalStateException {

		TransactionImpl transaction = this.getCurrentTransaction();
		if (transaction != null) {
			TransactionContext nativeTransactionContext = transaction.getTransactionContext();
			TransactionContext remoteTransactionContext = response.getTransactionContext();
			if (remoteTransactionContext != null) {

				TransactionXid currentXid = nativeTransactionContext.getCurrentXid();
				byte[] bytes = currentXid.getBranchQualifier();
				Object nativeCredential = new TransactionalCredential(bytes);
				Object remoteCredential = remoteTransactionContext.getPropagated();

				if (nativeCredential.equals(remoteCredential)) {
					try {
						TransactionalResource resource = response.getTransactionalResource();
						boolean nativeAllowedNonxaResource = nativeTransactionContext.isNonxaResourceAllowed();
						boolean remoteAllowedNonxaResource = remoteTransactionContext.isNonxaResourceAllowed();
						if (nativeAllowedNonxaResource && remoteAllowedNonxaResource == false) {
							nativeTransactionContext.setNonxaResourceAllowed(false);
						}
						XAResourceDescriptor descriptor = this.createResourceDescriptor(resource,
								remoteAllowedNonxaResource);
						transaction.delistResource(descriptor, XAResource.TMSUCCESS);
					} catch (IllegalStateException ex) {
						logger.throwing(TransactionalInterceptorImpl.class.getName(),
								"afterReceiveResponse(TransactionalRequest)", ex);
						throw ex;
					} catch (SystemException ex) {
						logger.throwing(TransactionalInterceptorImpl.class.getName(),
								"afterReceiveResponse(TransactionalRequest)", ex);
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

	private XAResourceDescriptor createResourceDescriptor(TransactionalResource resource, boolean nonxaResourceExists) {
		XAResourceDescriptor descriptor = new XAResourceDescriptor();
		descriptor.setDelegate(resource);
		descriptor.setIdentifier(resource.getIdentifier());
		descriptor.setRemote(true);
		descriptor.setSupportsXA(nonxaResourceExists);
		return descriptor;
	}
}
