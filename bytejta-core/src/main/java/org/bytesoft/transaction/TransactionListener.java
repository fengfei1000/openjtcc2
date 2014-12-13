package org.bytesoft.transaction;

public interface TransactionListener {

	public void prepareStart();

	public void prepareComplete(boolean success);

	public void commitStart();

	public void commitComplete(boolean success);

	public void rollbackStart();

	public void rollbackComplete(boolean success);

}
