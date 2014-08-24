package org.bytesoft.bytetcc;

import java.io.Serializable;

public interface CompensableContext {

	public Serializable getCompensableVariable() throws IllegalStateException;

	public void setCompensableVariable(Serializable variable) throws IllegalStateException;

	public boolean isRollbackOnly() throws IllegalStateException;

	public void setRollbackOnly() throws IllegalStateException;

}
