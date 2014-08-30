package org.bytesoft.transaction.xa.supports.rm;

import javax.transaction.xa.XAResource;

import org.bytesoft.transaction.xa.XAResourceDescriptor;

public interface ResourceRecognizer {

	public XAResourceDescriptor recognize(XAResource xares) throws ResourceNotSupportedException;

}
