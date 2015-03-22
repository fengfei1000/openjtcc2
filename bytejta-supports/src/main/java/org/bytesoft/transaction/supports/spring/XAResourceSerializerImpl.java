package org.bytesoft.transaction.supports.spring;

import java.io.IOException;

import org.bytesoft.transaction.rpc.TransactionResource;

public class XAResourceSerializerImpl extends AbstractXAResourceSerializer {

	public TransactionResource deserializeTransactionResource(String identifier) throws IOException {
		throw new IOException("Not supported yet!");
	}

}
