package org.bytesoft.bytetcc.supports.logger;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

public class SimpleTransactionLoggerEntry {

	private FileChannel fileChannel;
	private RandomAccessFile accessFile;

	public RandomAccessFile getAccessFile() {
		return accessFile;
	}

	public FileChannel getFileChannel() {
		return fileChannel;
	}

	public void setFileChannel(FileChannel fileChannel) {
		this.fileChannel = fileChannel;
	}

	public void setAccessFile(RandomAccessFile accessFile) {
		this.accessFile = accessFile;
	}

}
