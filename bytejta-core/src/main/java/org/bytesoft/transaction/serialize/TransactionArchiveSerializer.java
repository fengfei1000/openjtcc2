package org.bytesoft.transaction.serialize;

import java.io.IOException;

import org.bytesoft.transaction.archive.TransactionArchive;

public interface TransactionArchiveSerializer {

	public byte[] serialize(TransactionArchive archive) throws IOException;

	public TransactionArchive deserialize(byte[] byteArray) throws IOException;

}
