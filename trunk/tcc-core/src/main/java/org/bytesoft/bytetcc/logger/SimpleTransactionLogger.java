package org.bytesoft.bytetcc.logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.xa.Xid;

import org.bytesoft.bytejta.utils.ByteUtils;
import org.bytesoft.bytetcc.archive.CompensableArchive;
import org.bytesoft.bytetcc.archive.CompensableTransactionArchive;
import org.bytesoft.bytetcc.supports.CompensableTransactionLogger;
import org.bytesoft.transaction.archive.TransactionArchive;
import org.bytesoft.transaction.archive.XAResourceArchive;
import org.bytesoft.transaction.serialize.TransactionArchiveSerializer;
import org.bytesoft.transaction.serialize.XAResourceSerializer;
import org.bytesoft.transaction.xa.XAResourceDescriptor;

public class SimpleTransactionLogger implements CompensableTransactionLogger, TransactionArchiveSerializer {
	private File directory;
	private XAResourceSerializer resourceSerializer;
	private final Map<Xid, SimpleTransactionLoggerEntry> transactions = new ConcurrentHashMap<Xid, SimpleTransactionLoggerEntry>();

	public void initialize() {
		if (this.directory == null) {
			this.directory = new File("bytetcc/simple/");
		}

		if (directory.exists() == false) {
			directory.mkdirs();
		}
	}

	public void createTransaction(TransactionArchive transactionArchive) {
		this.updateTransaction(transactionArchive);
	}

	public void deleteTransaction(TransactionArchive transactionArchive) {
		if (CompensableTransactionArchive.class.isInstance(transactionArchive) == false) {
			throw new IllegalArgumentException();
		}
		CompensableTransactionArchive archive = (CompensableTransactionArchive) transactionArchive;

		Xid xid = archive.getXid();
		SimpleTransactionLoggerEntry entry = this.transactions.remove(xid);
		String txid = ByteUtils.byteArrayToString(xid.getGlobalTransactionId());
		File storageFile = new File(this.directory, txid);
		if (entry != null) {
			RandomAccessFile raf = entry.getAccessFile();
			MappedByteBuffer buf = entry.getByteBuffer();
			buf.position(0);
			buf.put((byte) 0);
			try {
				raf.close();
			} catch (IOException ex) {
				// ignore
			}
		}

		try {
			if (storageFile.delete()) {
				storageFile.deleteOnExit();
			}
		} catch (RuntimeException ex) {
			// ignore
		}

	}

	public List<TransactionArchive> getTransactionArchiveList() {
		return new ArrayList<TransactionArchive>();
	}

	public void updateResource(XAResourceArchive archive) {
	}

	public void updateTransaction(TransactionArchive transactionArchive) {
		if (CompensableTransactionArchive.class.isInstance(transactionArchive) == false) {
			throw new IllegalArgumentException();
		}
		CompensableTransactionArchive archive = (CompensableTransactionArchive) transactionArchive;

		byte[] bytes = null;
		try {
			bytes = this.serialize(archive);
		} catch (IOException ex) {
			return;// TODO
		}

		Xid xid = archive.getXid();
		SimpleTransactionLoggerEntry entry = this.transactions.get(xid);
		if (entry == null) {
			String txid = ByteUtils.byteArrayToString(xid.getGlobalTransactionId());
			RandomAccessFile raf = null;
			MappedByteBuffer buf = null;
			try {
				File storageFile = new File(this.directory, txid);
				raf = new RandomAccessFile(storageFile, "rw");
				buf = raf.getChannel().map(MapMode.READ_WRITE, 0, raf.length());
				entry = new SimpleTransactionLoggerEntry();
				entry.setAccessFile(raf);
				entry.setByteBuffer(buf);
				this.transactions.put(xid, entry);
			} catch (IOException ex) {
				if (raf != null) {
					try {
						raf.close();
					} catch (IOException otherEx) {
						// ignore
					}
				} // if (raf != null)
				return;
			}
		}

		MappedByteBuffer buf = entry.getByteBuffer();
		buf.position(0);
		buf.put((byte) 1);
		buf.putInt(bytes.length);
		buf.put(bytes);
	}

	public CompensableTransactionArchive deserialize(byte[] bytes) throws IOException {
		return null;
	}

	public byte[] serialize(TransactionArchive transactionArchive) throws IOException {
		if (CompensableTransactionArchive.class.isInstance(transactionArchive) == false) {
			throw new IllegalArgumentException();
		}
		CompensableTransactionArchive archive = (CompensableTransactionArchive) transactionArchive;

		ByteBuffer buffer = ByteBuffer.allocate(4096);
		Xid xid = archive.getXid();
		byte[] transactionId = xid.getGlobalTransactionId();
		buffer.put(transactionId);
		int status = archive.getStatus();
		buffer.put((byte) status);

		int compensableStatus = archive.getCompensableStatus();
		buffer.put((byte) compensableStatus);

		byte coordinator = archive.isCoordinator() ? (byte) 1 : (byte) 0;
		buffer.put((byte) coordinator);

		List<CompensableArchive> compensables = archive.getCompensables();
		int compensableNumber = compensables.size();
		buffer.put((byte) compensableNumber);
		for (int i = 0; i < compensableNumber; i++) {
			CompensableArchive compensableArchive = compensables.get(i);
			this.serializeCompensableArchive(buffer, compensableArchive);
		}

		List<XAResourceArchive> resources = archive.getRemoteResources();
		int resourceNumber = resources.size();
		buffer.put((byte) resourceNumber);

		for (int i = 0; i < resourceNumber; i++) {
			XAResourceArchive resourceArchive = resources.get(i);
			this.serializeXAResourceArchive(buffer, resourceArchive);
		}

		int pos = buffer.position();
		byte[] byteArray = new byte[pos];
		buffer.flip();
		buffer.get(byteArray);

		return byteArray;
	}

	private void serializeCompensableArchive(ByteBuffer buffer, CompensableArchive compensableArchive) {
		compensableArchive.getCompensable();
		compensableArchive.getXid();
	}

	private void serializeXAResourceArchive(ByteBuffer buffer, XAResourceArchive resourceArchive) {
		XAResourceDescriptor descriptor = resourceArchive.getDescriptor();
		byte descriptorId = (byte) descriptor.getDescriptorId();
		Xid branchXid = resourceArchive.getXid();

		byte[] branchQualifier = branchXid.getBranchQualifier();
		byte branchVote = (byte) resourceArchive.getVote();
		byte rolledback = resourceArchive.isRolledback() ? (byte) 1 : (byte) 0;
		byte committed = resourceArchive.isCommitted() ? (byte) 1 : (byte) 0;
		byte heuristic = resourceArchive.isHeuristic() ? (byte) 1 : (byte) 0;

		buffer.put(branchQualifier);
		buffer.put(descriptorId);
		buffer.put(branchVote);
		buffer.put(committed);
		buffer.put(rolledback);
		buffer.put(heuristic);
	}

	public void updateCompensable(CompensableArchive archive) {
	}

	public File getDirectory() {
		return directory;
	}

	public void setDirectory(File directory) {
		this.directory = directory;
	}

	public XAResourceSerializer getResourceSerializer() {
		return resourceSerializer;
	}

	public void setResourceSerializer(XAResourceSerializer resourceSerializer) {
		this.resourceSerializer = resourceSerializer;
	}

}
