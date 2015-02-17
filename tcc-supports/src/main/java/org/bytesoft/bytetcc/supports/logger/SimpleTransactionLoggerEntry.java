package org.bytesoft.bytetcc.supports.logger;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import org.bytesoft.transaction.archive.TransactionArchive;

public class SimpleTransactionLoggerEntry {

	private FileChannel fileChannel;
	private RandomAccessFile accessFile;
	private TransactionArchive archive;

	public FileChannel getFileChannel() {
		return fileChannel;
	}

	public void setFileChannel(FileChannel fileChannel) {
		this.fileChannel = fileChannel;
	}

	public RandomAccessFile getAccessFile() {
		return accessFile;
	}

	public void setAccessFile(RandomAccessFile accessFile) {
		this.accessFile = accessFile;
	}

	public TransactionArchive getArchive() {
		return archive;
	}

	public void setArchive(TransactionArchive archive) {
		this.archive = archive;
	}

}
