package org.bytesoft.bytejta.logger.store;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bytesoft.transaction.store.TransactionStorageKey;
import org.bytesoft.transaction.store.TransactionStorageManager;
import org.bytesoft.transaction.store.TransactionStorageObject;

public class SimpleTransactionStorageManager implements TransactionStorageManager {

	protected static final int SIZE_STORAGE_FILE = 1024 * 1024 * 2;// 2M
	protected static final int SIZE_SECTION_HEADER = 1024;
	protected static final int SIZE_SECTION_RESOURCES = 1024 * 3;
	protected static final int SIZE_UNIT_TRANSACTION = 256;
	protected static final int SIZE_UNIT_RESOURCE = 32;

	protected static final byte[] IDENTIFIER = "org.bytesoft.bytejta.storage.simple".getBytes();
	protected static final int VERSION_MAJOR = 0;
	protected static final int VERSION_MINOR = 1;

	protected final File storageFile;
	protected RandomAccessFile raf;
	protected MappedByteBuffer resourceBuf;
	protected final List<SimpleResourcePosition> resources = new ArrayList<SimpleResourcePosition>();
	protected final Map<String, SimpleResourcePosition> resourceMap = new ConcurrentHashMap<String, SimpleResourcePosition>();

	protected final List<SimpleStoragePosition> positions = new ArrayList<SimpleStoragePosition>();
	protected final Map<TransactionStorageKey, SimpleStoragePosition> positionMap = new ConcurrentHashMap<TransactionStorageKey, SimpleStoragePosition>();

	protected transient long maxPosition;

	public SimpleTransactionStorageManager(File storageFile) {
		this.storageFile = storageFile;
	}

	public void initialize() throws IOException {
		if (this.storageFile.exists()) {
			this.raf = new RandomAccessFile(this.storageFile, "rw");
		} else {
			this.mkdirDirectoryIfRequired();
			this.createStorageFile();
		}

		try {
			this.validation();
		} catch (Exception ex) {
			this.raf.setLength(SIZE_STORAGE_FILE);
			this.truncateStorageFile();
			this.updateStorageMetaData();
		}

		FileChannel channel = this.raf.getChannel();
		this.resourceBuf = channel.map(MapMode.READ_WRITE, SIZE_SECTION_HEADER, SIZE_SECTION_RESOURCES);
		this.initializeResourceSection();
		this.scanMaxPositionIfRequired();
		this.initializeStorageSection();
	}

	protected synchronized void initializeResourceSection() throws IllegalStateException {
		int index = 0;
		while (this.resourceBuf.remaining() > 0) {
			int position = this.resourceBuf.position();
			int idx = 0;
			boolean enabled = false;
			byte[] byteArray = new byte[SIZE_UNIT_RESOURCE - 2];//
			try {
				idx = this.resourceBuf.get();
				byte enabledByte = this.resourceBuf.get();
				enabled = (enabledByte > 0);
				this.resourceBuf.get(byteArray);
			} catch (BufferUnderflowException bfex) {
				throw new IllegalStateException(bfex);
			}

			index++;
			SimpleResourcePosition rp = new SimpleResourcePosition();
			rp.setIndex(index);
			rp.setPosition(position);
			if (enabled) {
				String identifier = new String(byteArray).trim();
				if (index != idx) {
					throw new IllegalStateException();
				}
				rp.setEnabled(true);
				rp.setIdentifier(identifier);
				this.resourceMap.put(identifier, rp);
			} else {
				rp.setEnabled(false);
			}
			this.resources.add(rp);
		}
	}

	protected synchronized void scanMaxPositionIfRequired() throws IOException {
		if (this.maxPosition <= 0) {
			FileChannel channel = this.raf.getChannel();
			long eofpos = channel.size();
			long startIdx = SIZE_SECTION_HEADER + SIZE_SECTION_RESOURCES;
			channel.position(startIdx);
			while (channel.position() < eofpos) {
				long position = channel.position();
				long remain = eofpos - position;
				if (remain >= SIZE_UNIT_TRANSACTION) {
					continue;
				} else {
					this.maxPosition = position;
					this.updateStorageMetaData();
					break;
				}
			}
		}
	}

	protected synchronized void initializeStorageSection() throws IllegalStateException, IOException {
		FileChannel channel = this.raf.getChannel();
		long startIdx = SIZE_SECTION_HEADER + SIZE_SECTION_RESOURCES;
		channel.position(startIdx);
		while (channel.position() < this.maxPosition) {
			long position = channel.position();
			SimpleStoragePosition tp = new SimpleStoragePosition();
			MappedByteBuffer buffer = channel.map(MapMode.READ_WRITE, position, SIZE_UNIT_TRANSACTION);
			tp.setBuffer(buffer);
			byte[] contentByteArray = new byte[SIZE_UNIT_TRANSACTION];
			buffer.get(contentByteArray);
			SimpleTransactionStorageObject object = new SimpleTransactionStorageObject(contentByteArray);
			if (object.isEnabled()) {
				TransactionStorageKey key = object.getStorageKey();
				tp.setKey(key);
				tp.setEnabled(true);
				this.positionMap.put(key, tp);
			}
			this.positions.add(tp);

			channel.position(position + SIZE_UNIT_TRANSACTION);
		}
	}

	private void truncateStorageFile() throws IOException {
		FileChannel channel = this.raf.getChannel();
		channel.position(0);
		channel.truncate(channel.size());
	}

	private synchronized void updateStorageMetaData() throws IOException {
		FileChannel channel = this.raf.getChannel();
		ByteBuffer buffer = ByteBuffer.allocate(SIZE_SECTION_HEADER);
		buffer.put(IDENTIFIER);
		buffer.put((byte) VERSION_MAJOR);
		buffer.put((byte) VERSION_MINOR);
		buffer.putLong(this.maxPosition);

		channel.position(0);
		buffer.flip();
		channel.write(buffer);

		this.forceRaf();
	}

	private void validation() throws IllegalStateException, IOException {
		FileChannel channel = this.raf.getChannel();

		if (this.raf.length() != SIZE_STORAGE_FILE) {
			throw new IllegalStateException();
		}

		ByteBuffer buffer = ByteBuffer.allocate(SIZE_SECTION_HEADER);
		channel.read(buffer);
		buffer.flip();

		byte[] bytes = new byte[IDENTIFIER.length];
		buffer.get(bytes);
		if (Arrays.equals(bytes, IDENTIFIER) == false) {
			throw new IllegalStateException();
		}

		int major = buffer.get();
		int minor = buffer.get();
		if (major != VERSION_MAJOR || minor != VERSION_MINOR) {
			throw new IllegalStateException();
		}

		long maxPos = buffer.getLong();
		if (maxPos < (SIZE_SECTION_HEADER + SIZE_SECTION_RESOURCES) || maxPos > SIZE_STORAGE_FILE) {
			throw new IllegalStateException();
		}

		this.maxPosition = maxPos;
	}

	private void createStorageFile() throws IOException {
		try {
			this.raf = new RandomAccessFile(this.storageFile, "rw");
			this.raf.setLength(SIZE_STORAGE_FILE);
		} catch (FileNotFoundException fnfex) {
			throw fnfex;
		} catch (IOException ioex) {
			this.closeRaf();
			this.storageFile.delete();
			throw ioex;
		}
	}

	protected synchronized void mkdirDirectoryIfRequired() {
		File directory = this.storageFile.getParentFile();
		if (directory.exists() == false) {
			directory.mkdirs();
		}
	}

	protected synchronized void closeRaf() {
		if (this.raf != null) {
			try {
				this.raf.close();
			} catch (IOException ioex) {
				// ignore
			}
		}
	}

	protected synchronized void forceRaf() {
		if (this.raf != null) {
			try {
				this.raf.getChannel().force(false);
			} catch (IOException ex) {
				// ignore
			}
		}
	}

	public int registerResource(String identifier) throws IllegalArgumentException {
		try {
			return this.registerResource(identifier);
		} catch (IllegalStateException rex) {
			SimpleResourcePosition position = locateAvailableResourcePosition();
			int pos = position.getPosition();
			try {
				synchronized (this.resourceBuf) {
					this.resourceBuf.position(pos);
					this.resourceBuf.put((byte) position.getIndex());
					this.resourceBuf.put((byte) 1);
					try {
						this.resourceBuf.put(identifier.getBytes());
					} catch (BufferOverflowException bfex) {
						this.resourceBuf.position(pos);
						this.resourceBuf.put((byte) position.getIndex());
						this.resourceBuf.put((byte) 0);
						throw new IllegalArgumentException();
					} finally {
						this.resourceBuf.force();
					}
					this.resourceMap.put(identifier, position);
					return position.getIndex();
				}
			} catch (IllegalArgumentException iaex) {
				synchronized (position) {
					position.setEnabled(false);
				}
				throw iaex;
			}

		}
	}

	public List<String> getRegisteredResources() {
		return new ArrayList<String>(this.resourceMap.keySet());
	}

	public int getRegisteredResource(String identifier) throws IllegalStateException {
		SimpleResourcePosition position = this.resourceMap.get(identifier);
		if (position == null) {
			throw new IllegalStateException("Resource is not registered.");
		}
		return position.getIndex();
	}

	public String getRegisteredResource(int index) throws IllegalStateException {
		IllegalStateException thrown = null;
		try {
			SimpleResourcePosition position = this.resources.get(index);
			if (position.getIndex() == index) {
				return position.getIdentifier();
			}
		} catch (IndexOutOfBoundsException ioobex) {
			thrown = new IllegalStateException(ioobex);
		}

		for (int i = 0; i < this.resources.size(); i++) {
			SimpleResourcePosition position = this.resources.get(index);
			if (position.getIndex() == index) {
				return position.getIdentifier();
			}
		}

		if (thrown == null) {
			throw new IllegalStateException();
		} else {
			throw thrown;
		}
	}

	public List<TransactionStorageKey> getStorageKeyList() {
		return new ArrayList<TransactionStorageKey>(this.positionMap.keySet());
	}

	public TransactionStorageObject locateStorageObject(TransactionStorageKey storageKey) {
		byte[] instanceKey = storageKey.getInstanceKey();
		TransactionStorageKey key = new SimpleTransactionStorageKey(instanceKey);
		SimpleStoragePosition position = this.positionMap.get(key);
		if (position == null) {
			return null;
		} else {
			SimpleTransactionStorageObject storageObject = null;
			synchronized (position) {
				MappedByteBuffer buffer = position.getBuffer();
				byte[] bytes = new byte[SIZE_UNIT_TRANSACTION];
				buffer.position(0);
				buffer.get(bytes);
				storageObject = new SimpleTransactionStorageObject(bytes);
			}// end-synchronized
			return storageObject;
		}
	}

	public void createStorageObject(TransactionStorageObject storageObject) {
		TransactionStorageKey storageKey = storageObject.getStorageKey();
		SimpleStoragePosition position = this.positionMap.get(storageKey);
		if (position == null) {
			position = this.locateAvailableTransactionPosition();
			this.positionMap.put(storageKey, position);
			MappedByteBuffer buffer = position.getBuffer();
			synchronized (position) {
				position.setKey(storageKey);

				buffer.position(0);
				buffer.put(storageObject.getContentByteArray());
				buffer.force();
			}
		} else {
			synchronized (position) {
				this.modifyStorageObject(storageObject);
			}
		}
	}

	public void modifyStorageObject(TransactionStorageObject storageObject) {
		TransactionStorageKey storageKey = storageObject.getStorageKey();
		SimpleStoragePosition position = this.positionMap.get(storageKey);
		if (position == null) {
			this.createStorageObject(storageObject);
		} else {
			MappedByteBuffer buffer = position.getBuffer();
			synchronized (position) {
				buffer.position(0);
				buffer.put(storageObject.getContentByteArray());
				buffer.force();
			}
		}
	}

	public void deleteStorageObject(TransactionStorageObject storageObject) {
		TransactionStorageKey storageKey = storageObject.getStorageKey();
		SimpleStoragePosition position = this.positionMap.remove(storageKey);
		if (position == null) {
			// ignore
		} else {
			MappedByteBuffer buffer = position.getBuffer();
			synchronized (position) {
				position.setEnabled(false);
				position.setKey(null);

				buffer.position(0);
				buffer.put(storageObject.getContentByteArray());
				buffer.force();
			}
		}
	}

	private SimpleStoragePosition locateAvailableTransactionPosition() throws IllegalStateException {
		for (int i = 0; i < this.positions.size(); i++) {
			SimpleStoragePosition position = this.positions.get(i);
			if (position.isEnabled() == false) {
				synchronized (position) {
					if (position.isEnabled() == false) {
						position.setEnabled(true);
						return position;
					}
				}
			}
		}
		throw new IllegalStateException();
	}

	private SimpleResourcePosition locateAvailableResourcePosition() throws IllegalStateException {
		for (int i = 0; i < this.resources.size(); i++) {
			SimpleResourcePosition position = this.resources.get(i);
			if (position.isEnabled() == false) {
				synchronized (position) {
					if (position.isEnabled() == false) {
						position.setEnabled(true);
						return position;
					}
				}
			}
		}
		throw new IllegalStateException();
	}

}
