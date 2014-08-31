package org.bytesoft.transaction.archive;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.bytesoft.transaction.xa.XAResourceDescriptor;

public class XAResourceArchive implements XAResource {
	private XAResourceDescriptor descriptor;
	private transient boolean delisted;
	private Xid xid;
	private int vote = -1;
	private transient boolean completed;
	private transient boolean readonly;
	private boolean committed;
	private boolean rolledback;

	public void commit(Xid xid, boolean onePhase) throws XAException {
		if (this.readonly) {
			throw new XAException(XAException.XAER_NOTA);
		} else if (this.committed) {
			// ignore
		} else if (this.rolledback) {
			throw new XAException(XAException.XA_HEURRB);
		} else {
			descriptor.commit(xid, onePhase);
		}
	}

	public void end(Xid xid, int flags) throws XAException {
		descriptor.end(xid, flags);
	}

	public void forget(Xid xid) throws XAException {
		descriptor.forget(xid);
	}

	public int getTransactionTimeout() throws XAException {
		return descriptor.getTransactionTimeout();
	}

	public boolean isSameRM(XAResource xares) throws XAException {
		if (XAResourceArchive.class.isInstance(xares)) {
			XAResourceArchive archive = (XAResourceArchive) xares;
			return descriptor.isSameRM(archive.getDescriptor());
		} else {
			return descriptor.isSameRM(xares);
		}
	}

	public int prepare(Xid xid) throws XAException {

		if (this.readonly) {
			return XAResource.XA_RDONLY;
		} else if (this.vote != -1) {
			return this.vote;
		} else {
			return descriptor.prepare(xid);
		}

	}

	public Xid[] recover(int flag) throws XAException {
		return descriptor.recover(flag);
	}

	public void rollback(Xid xid) throws XAException {

		if (this.readonly) {
			throw new XAException(XAException.XAER_NOTA);
		} else if (this.committed) {
			throw new XAException(XAException.XA_HEURCOM);
		} else if (this.rolledback) {
			// ignore
		} else {
			descriptor.rollback(xid);
		}

	}

	public boolean setTransactionTimeout(int seconds) throws XAException {
		return descriptor.setTransactionTimeout(seconds);
	}

	public void start(Xid xid, int flags) throws XAException {
		descriptor.start(xid, flags);
	}

	public XAResourceDescriptor getDescriptor() {
		return descriptor;
	}

	public void setDescriptor(XAResourceDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	public boolean isDelisted() {
		return delisted;
	}

	public void setDelisted(boolean delisted) {
		this.delisted = delisted;
	}

	public Xid getXid() {
		return xid;
	}

	public void setXid(Xid xid) {
		this.xid = xid;
	}

	public int getVote() {
		return vote;
	}

	public void setVote(int vote) {
		this.vote = vote;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public boolean isCommitted() {
		return committed;
	}

	public void setCommitted(boolean committed) {
		this.committed = committed;
	}

	public boolean isRolledback() {
		return rolledback;
	}

	public void setRolledback(boolean rolledback) {
		this.rolledback = rolledback;
	}

	public boolean isReadonly() {
		return readonly;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

}
