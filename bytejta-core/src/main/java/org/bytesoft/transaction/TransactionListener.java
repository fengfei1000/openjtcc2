package org.bytesoft.transaction;

public interface TransactionListener {

	public int OPT_SUCCESS = 0;
	public int OPT_FAILURE = 1;
	public int OPT_HEURMIX = 2;

	public void prepareStart();

	public void prepareComplete(boolean success);

	public void commitStart();

	public void commitComplete(int optcode);

	public void rollbackStart();

	public void rollbackComplete(int optcode);

}
