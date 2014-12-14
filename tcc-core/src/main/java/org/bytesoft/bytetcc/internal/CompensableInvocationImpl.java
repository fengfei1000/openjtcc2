package org.bytesoft.bytetcc.internal;

import java.io.Serializable;
import java.lang.reflect.Method;

import javax.transaction.SystemException;

import org.bytesoft.bytetcc.CompensableContext;
import org.bytesoft.bytetcc.CompensableInvocation;
import org.bytesoft.bytetcc.CompensableTccTransaction;
import org.bytesoft.bytetcc.supports.CompensableSynchronization;
import org.bytesoft.transaction.xa.TransactionXid;

public class CompensableInvocationImpl extends CompensableSynchronization implements CompensableContext,
		CompensableInvocation {

	private transient Object identifier;
	private transient Method method;
	private Object[] args;
	private transient String confirmableKey;
	private transient String cancellableKey;
	private Serializable variable;
	private transient CompensableTccTransaction transaction;

	public void suspend() {
	}

	public void resume() {
	}

	public Serializable getCompensableVariable() {
		return this.variable;
	}

	public void setCompensableVariable(Serializable variable) throws IllegalStateException {
		if (this.variable == null) {
			this.variable = variable;
		} else if (this.variable.equals(variable)) {
			// ignore
		} else {
			throw new IllegalStateException("The varable of current compensable-service has already been set!");
		}
	}

	public void afterInitialization(TransactionXid xid) {
	}

	public void beforeCompletion(TransactionXid xid) {
	}

	public void afterCompletion(TransactionXid xid, int status) {
	}

	public boolean isRollbackOnly() throws IllegalStateException {
		if (this.transaction == null) {
			throw new IllegalStateException();
		}
		return this.transaction.isRollbackOnly();
	}

	public void setRollbackOnly() throws IllegalStateException {
		if (this.transaction == null) {
			throw new IllegalStateException();
		}
		try {
			this.transaction.setRollbackOnly();
		} catch (SystemException ex) {
			throw new IllegalStateException(ex);
		}
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public String getConfirmableKey() {
		return confirmableKey;
	}

	public void setConfirmableKey(String confirmableKey) {
		this.confirmableKey = confirmableKey;
	}

	public String getCancellableKey() {
		return cancellableKey;
	}

	public void setCancellableKey(String cancellableKey) {
		this.cancellableKey = cancellableKey;
	}

	public CompensableTccTransaction getTransaction() {
		return transaction;
	}

	public void setTransaction(CompensableTccTransaction transaction) {
		this.transaction = transaction;
	}

	public Object getIdentifier() {
		return identifier;
	}

	public void setIdentifier(Object identifier) {
		this.identifier = identifier;
	}

}
