package org.bytesoft.transaction.store;

import java.util.List;

public interface TransactionStorageManager {

	public int registerResource(String identifier) throws IllegalArgumentException;

	public List<String> getRegisteredResources();

	public int getRegisteredResource(String identifier) throws IllegalStateException;

	public String getRegisteredResource(int index) throws IllegalStateException;

	public List<TransactionStorageKey> getStorageKeyList();

	public TransactionStorageObject locateStorageObject(TransactionStorageKey storageKey);

	public void createStorageObject(TransactionStorageObject storageObject);

	public void modifyStorageObject(TransactionStorageObject storageObject);

	public void deleteStorageObject(TransactionStorageObject storageObject);

}
