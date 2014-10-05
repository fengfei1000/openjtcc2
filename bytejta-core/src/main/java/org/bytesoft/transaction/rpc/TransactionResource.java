package org.bytesoft.transaction.rpc;

import javax.transaction.xa.XAResource;

public interface TransactionResource extends XAResource {

	public String getIdentifier();

}
