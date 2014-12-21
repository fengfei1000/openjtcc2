package org.bytesoft.transaction;

public interface TransactionListener {

	public int OPT_DEFAULT = 0;
	public int OPT_HEURCOM = 1;
	public int OPT_HEURRB = 2;
	public int OPT_HEURMIX = 3;

	public void prepareStart();

	public void prepareComplete(boolean success);

	public void commitStart();

	public void commitSuccess();

	public void commitFailure(int optcode);

	public void rollbackStart();

	public void rollbackSuccess();

	public void rollbackFailure(int optcode);

}
