package org.bytesoft.bytetcc.xa;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public class CompensableJtaTransactionSkeleton implements XAResource {

	public void commit(Xid xid, boolean opc) throws XAException {
	}

	public void rollback(Xid xid) throws XAException {
	}

	public Xid[] recover(int arg0) throws XAException {
		return new Xid[0];
	}

	public void forget(Xid arg0) throws XAException {
	}

	public int getTransactionTimeout() throws XAException {
		throw new XAException(XAException.XAER_PROTO);
	}

	public boolean isSameRM(XAResource arg0) throws XAException {
		throw new XAException(XAException.XAER_PROTO);
	}

	public int prepare(Xid arg0) throws XAException {
		throw new XAException(XAException.XAER_PROTO);
	}

	public boolean setTransactionTimeout(int arg0) throws XAException {
		throw new XAException(XAException.XAER_PROTO);
	}

	public void start(Xid arg0, int arg1) throws XAException {
		throw new XAException(XAException.XAER_PROTO);
	}

	public void end(Xid arg0, int arg1) throws XAException {
		throw new XAException(XAException.XAER_PROTO);
	}

}
