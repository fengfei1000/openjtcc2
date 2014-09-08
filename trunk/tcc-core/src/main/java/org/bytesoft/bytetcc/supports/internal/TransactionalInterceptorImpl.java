/**
 * Copyright 2014 yangming.liu<liuyangming@gmail.com>.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, see <http://www.gnu.org/licenses/>.
 */
package org.bytesoft.bytetcc.supports.internal;

import java.util.logging.Logger;

import org.bytesoft.bytejta.common.TransactionXid;
import org.bytesoft.bytetcc.TransactionImpl;
import org.bytesoft.bytetcc.TransactionManagerImpl;
import org.bytesoft.bytetcc.supports.serialize.TerminatorMarshaller;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.rpc.TransactionalInterceptor;
import org.bytesoft.transaction.rpc.TransactionalRequest;
import org.bytesoft.transaction.rpc.TransactionalResponse;
import org.bytesoft.transaction.xa.XidFactory;

public class TransactionalInterceptorImpl implements TransactionalInterceptor {
	private static final Logger logger = Logger.getLogger("bytetcc");

	private TerminatorMarshaller terminatorMarshaller;
	private TransactionManagerImpl transactionManager;

	public void beforeSendRequest(TransactionalRequest request) throws IllegalStateException {
		TransactionImpl transaction = this.getCurrentTransaction();
		if (transaction != null) {
			TransactionContext transactionContext = transaction.getTransactionContext();
			XidFactory xidFactory = this.transactionManager.getXidFactory();
			TransactionXid branchXid = xidFactory.createBranchXid(transactionContext.getGlobalXid());
			TransactionContext propagationContext = transactionContext.clone();
			propagationContext.setCurrentXid(branchXid);
			// propagationContext.setInstanceKey(this.transactionManager.getInstanceKey());
			// propagationContext.setTerminalKey(this.transactionManager.getTerminalKey());

			request.setTransactionContext(propagationContext);

			logger.info(String.format("[%15s] method: %s", "before-send-req", request));
		}
	}

	public void afterReceiveResponse(TransactionalResponse response) throws IllegalStateException {
		TransactionContext propagationContext = (TransactionContext) response.getTransactionContext();
		if (propagationContext != null && propagationContext.isCompensable()) {
			logger.info(String.format("[%15s] method: %s", "after-recv-res", response));
			TransactionImpl transaction = this.getCurrentTransaction();
//			PropagationKey thisKey = this.transactionManager.getInstanceKey();
//			PropagationKey thatKey = propagationContext.getInstanceKey();

//			if (thisKey.equals(thatKey)) {
//				try {
//					XidImpl branchXid = propagationContext.getCurrentXid();
//					// TerminalKey terminalKey = propagationContext.getTerminalKey();
//
//					TerminatorInfo terminatorInfo = new TerminatorInfo();
//					// terminatorInfo.setApplication(terminalKey.getApplication());
//					// terminatorInfo.setEndpoint(terminalKey.getEndpoint());
//					terminatorInfo.setBranchXid(branchXid);
//
//					// TODO
//					// RemoteTerminator terminator = this.terminatorMarshaller.unmarshallTerminator(terminatorInfo);
//					// transaction.registerTerminator(terminator);
//					// } catch (IOException ex) {
//					// throw new IllegalStateException(ex);
//					// } catch (SystemException ex) {
//					// throw new IllegalStateException(ex);
//				} catch (RuntimeException ex) {
//					throw new IllegalStateException(ex);
//				}
//
//			}
		}
	}

	public void afterReceiveRequest(TransactionalRequest request) throws IllegalStateException {
		TransactionContext propagationContext = (TransactionContext) request.getTransactionContext();
//		if (propagationContext != null) {
//			logger.info(String.format("[%15s] method: %s", "after-recv-req", request));
//
//			CompensableTransaction transaction = this.transactionManager.getCurrentTransaction();
//			try {
//				transaction = this.transactionManager.begin(propagationContext);
//				TransactionContext transactionContext = transaction.getTransactionContext();
//				transactionContext.propagateTransactionContext(propagationContext);
//			} catch (NotSupportedException ex) {
//				throw new IllegalStateException(ex);
//			} catch (SystemException ex) {
//				throw new IllegalStateException(ex);
//			} catch (RuntimeException ex) {
//				throw new IllegalStateException(ex);
//			}
//
//		}
	}

	public void beforeSendResponse(TransactionalResponse response) throws IllegalStateException {
		TransactionImpl transaction = this.getCurrentTransaction();
//		if (transaction != null) {
//			TransactionContext transactionContext = transaction.getTransactionContext();
//			TransactionContext propagationContext = transactionContext.clone();
//			response.setTransactionContext(propagationContext);
//
//			transactionContext.revertTransactionContext();
//
//			this.transactionManager.unassociateTransaction();
//
//			logger.info(String.format("[%15s] method: %s", "before-send-res", response));
//		}
	}

	public TransactionImpl getCurrentTransaction() {
//		if (this.transactionManager == null) {
			return null;
//		} else {
//			return this.transactionManager.getCurrentTransaction();
//		}
	}

	public void setTransactionManager(TransactionManagerImpl transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTerminatorMarshaller(TerminatorMarshaller terminatorMarshaller) {
		this.terminatorMarshaller = terminatorMarshaller;
	}

}
