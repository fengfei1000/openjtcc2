package org.bytesoft.bytetcc.internal;

import java.io.Serializable;
import java.lang.reflect.Method;

import javax.transaction.SystemException;

import org.bytesoft.bytetcc.CompensableContext;
import org.bytesoft.bytetcc.CompensableInvocation;
import org.bytesoft.bytetcc.TransactionImpl;
import org.bytesoft.bytetcc.supports.CompensableSynchronization;
import org.bytesoft.transaction.xa.TransactionXid;

public class CompensableInvocationImpl extends CompensableSynchronization implements CompensableContext,
		CompensableInvocation {

	private transient Method method;
	private Object[] args;
	private transient Object confirmableObject;
	private transient Object cancellableObject;
	private Serializable variable;
	private transient TransactionImpl transaction;

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

	public Object getConfirmableObject() {
		return confirmableObject;
	}

	public void setConfirmableObject(Object confirmableObject) {
		this.confirmableObject = confirmableObject;
	}

	public Object getCancellableObject() {
		return cancellableObject;
	}

	public void setCancellableObject(Object cancellableObject) {
		this.cancellableObject = cancellableObject;
	}

	public TransactionImpl getTransaction() {
		return transaction;
	}

	public void setTransaction(TransactionImpl transaction) {
		this.transaction = transaction;
	}

}
