package org.bytesoft.transaction.rpc;

import javax.transaction.xa.XAResource;

public interface TransactionalResource extends XAResource {

	public String getIdentifier();

}
