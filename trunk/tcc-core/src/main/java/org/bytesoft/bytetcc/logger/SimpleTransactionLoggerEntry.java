package org.bytesoft.bytetcc.logger;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;

public class SimpleTransactionLoggerEntry {

	private MappedByteBuffer byteBuffer;
	private RandomAccessFile accessFile;

	public MappedByteBuffer getByteBuffer() {
		return byteBuffer;
	}

	public void setByteBuffer(MappedByteBuffer byteBuffer) {
		this.byteBuffer = byteBuffer;
	}

	public RandomAccessFile getAccessFile() {
		return accessFile;
	}

	public void setAccessFile(RandomAccessFile accessFile) {
		this.accessFile = accessFile;
	}

}
