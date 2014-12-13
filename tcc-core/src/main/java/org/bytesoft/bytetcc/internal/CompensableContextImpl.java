package org.bytesoft.bytetcc.internal;

import java.io.Serializable;

import org.bytesoft.bytetcc.CompensableContext;

public class CompensableContextImpl implements CompensableContext {

	public Serializable getCompensableVariable() throws IllegalStateException {
		// CompensableContext context = CompensableInvocationRegistryImpl.getInstance().getCompensableInvocation();
		// if (context == null) {
		return null;
		// } else {
		// return context.getCompensableVariable();
		// }
	}

	public void setCompensableVariable(Serializable variable) throws IllegalStateException {
		// CompensableContext context = CompensableInvocationRegistryImpl.getInstance().getCompensableInvocation();
		// if (context == null) {
		// // ignore
		// } else {
		// context.setCompensableVariable(variable);
		// }
	}

	public boolean isRollbackOnly() throws IllegalStateException {
		// CompensableContext context = CompensableInvocationRegistryImpl.getInstance().getCompensableInvocation();
		// if (context == null) {
		return false;
		// } else {
		// return context.isRollbackOnly();
		// }
	}

	public void setRollbackOnly() throws IllegalStateException {
		// CompensableContext context = CompensableInvocationRegistryImpl.getInstance().getCompensableInvocation();
		// if (context == null) {
		// // ignore
		// } else {
		// context.setRollbackOnly();
		// }
	}

}
