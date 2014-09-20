package org.bytesoft.transaction.serialize;

import java.io.IOException;

import org.bytesoft.transaction.xa.XAResourceDescriptor;

public interface XAResourceSerializer {

	public String serialize(XAResourceDescriptor resource) throws IOException;

	public XAResourceDescriptor deserialize(String identifier) throws IOException;

}
