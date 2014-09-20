package org.bytesoft.bytejta.xa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.bytesoft.bytejta.common.TransactionXid;
import org.bytesoft.bytejta.utils.ByteUtils;
import org.bytesoft.bytejta.utils.CommonUtils;
import org.bytesoft.transaction.RemoteSystemException;
import org.bytesoft.transaction.RollbackRequiredException;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.archive.XAResourceArchive;
import org.bytesoft.transaction.xa.XAResourceDescriptor;
import org.bytesoft.transaction.xa.XATerminator;

public class XATerminatorImpl implements XATerminator {
	static final Logger logger = Logger.getLogger(XATerminatorImpl.class.getSimpleName());
	private TransactionContext transactionContext;
	private int transactionTimeout;
	private final List<XAResourceArchive> resources = new ArrayList<XAResourceArchive>();

	public XATerminatorImpl(TransactionContext txContext) {
		this.transactionContext = txContext;
	}

	public int prepare(Xid xid) throws XAException {
		return this.invokePrepare(false);
	}

	private int invokePrepare(boolean optimizeEnabled) throws XAException {
		int globalVote = XAResource.XA_RDONLY;
		int length = this.resources.size();
		int lastResourceIdx = this.chooseLastResourceIndex();
		for (int i = 0; i < length; i++) {
			boolean currentLastResource = (i == lastResourceIdx);
			if (optimizeEnabled && currentLastResource) {
				// ignore
			} else {
				XAResourceArchive archive = this.resources.get(i);
				Xid branchXid = archive.getXid();
				int branchVote = archive.prepare(branchXid);
				archive.setVote(branchVote);
				if (branchVote == XAResource.XA_RDONLY) {
					archive.setReadonly(true);
					archive.setCompleted(true);
				} else {
					globalVote = XAResource.XA_OK;
				}
			}
		}
		return globalVote;
	}

	private int chooseLastResourceIndex() {
		int length = this.resources.size();
		int lastResourceIdx = length - 1;
		for (int i = 0; i < length; i++) {
			XAResourceArchive archive = this.resources.get(i);
			XAResourceDescriptor descriptor = archive.getDescriptor();
			if (descriptor.isSupportsXA() == false) {
				lastResourceIdx = i;
			}
		}
		return lastResourceIdx;
	}

	public void commit(Xid xid, boolean onePhase) throws XAException {
		if (onePhase) {
			this.invokeOnePhaseCommit(xid);
		} else {
			this.invokeTwoPhaseCommit(xid);
		}
	}

	private void invokeOnePhaseCommit(Xid xid) throws XAException {

		try {
			this.invokePrepare(true);
		} catch (XAException xaex) {
			this.rollbackInCommitPhase(xid);
			return;
		}

		int length = this.resources.size();
		int lastResourceIdx = this.chooseLastResourceIndex();
		boolean commitExists = false;
		boolean rollbackExists = false;
		boolean errorExists = false;
		for (int i = 0; i < length; i++) {
			boolean currentLastResource = (i == lastResourceIdx);
			XAResourceArchive archive = this.resources.get(i);
			Xid branchXid = archive.getXid();
			try {
				if (archive.isCompleted()) {
					if (archive.isCommitted()) {
						commitExists = true;
					} else if (archive.isRolledback()) {
						rollbackExists = true;
					} else {
						// read-only, ignore.
					}
				} else if (currentLastResource) {
					archive.commit(branchXid, true);
					archive.setCommitted(true);
					archive.setCompleted(true);
				} else {
					archive.commit(branchXid, false);
					archive.setCommitted(true);
					archive.setCompleted(true);
				}
			} catch (XAException xaex) {
				if (commitExists) {
					// * @exception XAException An error has occurred. Possible XAExceptions
					// * are XA_HEURHAZ, XA_HEURCOM, XA_HEURRB, XA_HEURMIX, XAER_RMERR,
					// * XAER_RMFAIL, XAER_NOTA, XAER_INVAL, or XAER_PROTO.
					// * <P>If the resource manager did not commit the transaction and the
					// * parameter onePhase is set to true, the resource manager may throw
					// * one of the XA_RB* exceptions. Upon return, the resource manager has
					// * rolled back the branch's work and has released all held resources.
					switch (xaex.errorCode) {
					case XAException.XA_HEURMIX:
					case XAException.XA_HEURHAZ:
						commitExists = true;
						rollbackExists = true;
						break;
					case XAException.XA_HEURCOM:
						commitExists = true;
						break;
					case XAException.XAER_RMERR:
					case XAException.XAER_RMFAIL:
						errorExists = true;
						break;
					case XAException.XAER_NOTA:
					case XAException.XAER_INVAL:
					case XAException.XAER_PROTO:
						errorExists = true;
						break;
					case XAException.XA_HEURRB:
					default:
						rollbackExists = true;
						break;
					}
				} else {
					this.rollbackInCommitPhase(xid);
					return;
				}
			}
		}
		this.throwCommitExceptionIfRequired(commitExists, rollbackExists, errorExists);
	}

	private void rollbackInCommitPhase(Xid xid) throws XAException {
		boolean committed = false;
		boolean rolledback = false;
		try {
			this.rollback(xid);
			rolledback = true;
		} catch (XAException rbex) {
			switch (rbex.errorCode) {
			case XAException.XA_HEURCOM:
				committed = true;
				break;
			case XAException.XA_HEURHAZ:
			case XAException.XA_HEURMIX:
				throw rbex;
			case XAException.XAER_RMERR:
			case XAException.XAER_RMFAIL:
				throw new XAException(XAException.XAER_RMERR);
			case XAException.XAER_NOTA:
			case XAException.XAER_INVAL:
			case XAException.XAER_PROTO:
				// TODO
				throw new XAException(XAException.XAER_RMERR);
			case XAException.XA_HEURRB:
			default:
				rolledback = true;
			}
		}
		if (committed) {
			return;
		} else if (rolledback) {
			throw new XAException(XAException.XA_HEURRB);
		} else {
			// never happen
			throw new XAException(XAException.XAER_RMERR);
		}
	}

	private void throwCommitExceptionIfRequired(boolean commitExists, boolean rollbackExists, boolean errorExists)
			throws XAException {
		if (commitExists && rollbackExists) {
			throw new XAException(XAException.XA_HEURMIX);
		} else if (commitExists) {
			if (errorExists) {
				throw new XAException(XAException.XA_HEURHAZ);
			} else {
				// ignore
			}
		} else if (rollbackExists) {
			if (errorExists) {
				throw new XAException(XAException.XA_HEURHAZ);
			} else {
				throw new XAException(XAException.XA_HEURRB);
			}
		} else {
			if (errorExists) {
				throw new XAException(XAException.XAER_RMERR);
			} else {
				// ignore
			}
		}
	}

	private void invokeTwoPhaseCommit(Xid xid) throws XAException {
		int length = this.resources.size();
		boolean commitExists = false;
		boolean rollbackExists = false;
		boolean errorExists = false;
		for (int i = 0; i < length; i++) {
			XAResourceArchive archive = this.resources.get(i);
			Xid branchXid = archive.getXid();
			try {
				if (archive.isCompleted()) {
					if (archive.isCommitted()) {
						commitExists = true;
					} else if (archive.isRolledback()) {
						rollbackExists = true;
					} else {
						// read-only, ignore.
					}
				} else {
					archive.commit(branchXid, false);
					archive.setCommitted(true);
					archive.setCompleted(true);
				}
			} catch (XAException xaex) {
				if (commitExists) {
					// * @exception XAException An error has occurred. Possible XAExceptions
					// * are XA_HEURHAZ, XA_HEURCOM, XA_HEURRB, XA_HEURMIX, XAER_RMERR,
					// * XAER_RMFAIL, XAER_NOTA, XAER_INVAL, or XAER_PROTO.
					// * <P>If the resource manager did not commit the transaction and the
					// * parameter onePhase is set to true, the resource manager may throw
					// * one of the XA_RB* exceptions. Upon return, the resource manager has
					// * rolled back the branch's work and has released all held resources.

					switch (xaex.errorCode) {
					case XAException.XA_HEURMIX:
					case XAException.XA_HEURHAZ:
						commitExists = true;
						rollbackExists = true;
						break;
					case XAException.XA_HEURCOM:
						commitExists = true;
						break;
					case XAException.XAER_RMERR:
					case XAException.XAER_RMFAIL:
						errorExists = true;
						break;
					case XAException.XAER_NOTA:
					case XAException.XAER_PROTO:
					case XAException.XAER_INVAL:
						// errorExists = true;
						commitExists = true;
						break;
					case XAException.XA_HEURRB:
					default:
						rollbackExists = true;
						break;
					}
				} else {
					this.rollbackInCommitPhase(xid);
					return;
				}
			}
		}// end-for
		this.throwCommitExceptionIfRequired(commitExists, rollbackExists, errorExists);
	}

	public void rollback(Xid xid) throws XAException {
		int length = this.resources.size();
		boolean commitExists = false;
		boolean rollbackExists = false;
		boolean errorExists = false;
		for (int i = 0; i < length; i++) {
			XAResourceArchive archive = this.resources.get(i);
			Xid branchXid = archive.getXid();
			try {
				if (archive.isCompleted()) {
					if (archive.isCommitted()) {
						commitExists = true;
					} else if (archive.isRolledback()) {
						rollbackExists = true;
					} else {
						// read-only, ignore.
					}
				} else {
					archive.rollback(branchXid);
				}
			} catch (XAException xaex) {
				// * @exception XAException An error has occurred. Possible XAExceptions are
				// * XA_HEURHAZ, XA_HEURCOM, XA_HEURRB, XA_HEURMIX, XAER_RMERR, XAER_RMFAIL,
				// * XAER_NOTA, XAER_INVAL, or XAER_PROTO.
				// * <p>If the transaction branch is already marked rollback-only the
				// * resource manager may throw one of the XA_RB* exceptions. Upon return,
				// * the resource manager has rolled back the branch's work and has released
				// * all held resources.
				switch (xaex.errorCode) {
				case XAException.XA_HEURMIX:
				case XAException.XA_HEURHAZ:
					commitExists = true;
					rollbackExists = true;
					break;
				case XAException.XA_HEURCOM:
					commitExists = true;
					break;
				case XAException.XAER_RMERR:
				case XAException.XAER_RMFAIL:
				case XAException.XAER_NOTA:
				case XAException.XAER_INVAL:
				case XAException.XAER_PROTO:
					errorExists = true;
					break;
				case XAException.XA_HEURRB:
				default:
					rollbackExists = true;
					break;
				}
				errorExists = true;
			}
		}
		this.throwRollbackExceptionIfRequired(commitExists, rollbackExists, errorExists);
	}

	private void throwRollbackExceptionIfRequired(boolean commitExists, boolean rollbackExists, boolean errorExists)
			throws XAException {
		if (commitExists && rollbackExists) {
			throw new XAException(XAException.XA_HEURMIX);
		} else if (commitExists) {
			if (errorExists) {
				throw new XAException(XAException.XA_HEURHAZ);
			} else {
				throw new XAException(XAException.XA_HEURCOM);
			}
		} else if (rollbackExists) {
			if (errorExists) {
				throw new XAException(XAException.XA_HEURHAZ);
			} else {
				// ignore
			}
		} else {
			if (errorExists) {
				throw new XAException(XAException.XAER_RMERR);
			} else {
				// ignore
			}
		}
	}

	public void end(Xid xid, int flags) throws XAException {
		throw new IllegalStateException("Not supported yet!");
	}

	public void forget(Xid xid) throws XAException {
		throw new IllegalStateException("Not supported yet!");
	}

	public int getTransactionTimeout() throws XAException {
		return this.transactionTimeout;
	}

	public boolean isSameRM(XAResource xares) throws XAException {
		throw new IllegalStateException("Not supported yet!");
	}

	public Xid[] recover(int flag) throws XAException {
		throw new IllegalStateException("Not supported yet!");
	}

	public boolean setTransactionTimeout(int seconds) throws XAException {
		this.transactionTimeout = seconds;
		return true;
	}

	public void start(Xid xid, int flags) throws XAException {
		throw new IllegalStateException("Not supported yet!");
	}

	public boolean xaSupports() throws RemoteSystemException {
		int length = this.resources.size();
		for (int i = 0; i < length; i++) {
			XAResourceArchive archive = this.resources.get(i);
			XAResourceDescriptor descriptor = archive.getDescriptor();
			if (descriptor.isSupportsXA() == false) {
				return false;
			}
		}
		return true;
	}

	public boolean delistResource(XAResourceDescriptor descriptor, int flag) throws IllegalStateException,
			SystemException {

		XAResourceArchive archive = this.locateExisted(descriptor);
		if (archive == null) {
			throw new SystemException();
		}

		return this.delistResource(archive, flag);
	}

	private boolean delistResource(XAResourceArchive archive, int flag) throws SystemException,
			RollbackRequiredException {
		try {
			Xid branchXid = archive.getXid();
			logger.info(String.format("\t[%s] delist: xares= %s, flags= %s",
					ByteUtils.byteArrayToString(branchXid.getBranchQualifier()), archive, flag));

			archive.end(branchXid, flag);
			archive.setDelisted(true);
		} catch (XAException xae) {
			xae.printStackTrace();

			// Possible XAException values are XAER_RMERR, XAER_RMFAIL,
			// XAER_NOTA, XAER_INVAL, XAER_PROTO, or XA_RB*.
			switch (xae.errorCode) {
			case XAException.XAER_RMFAIL:
			case XAException.XAER_NOTA:
			case XAException.XAER_INVAL:
			case XAException.XAER_PROTO:
				return false;
			case XAException.XAER_RMERR:
				SystemException sysex = new SystemException();
				sysex.initCause(xae);
				throw sysex;
			default:
				RollbackRequiredException rrex = new RollbackRequiredException();
				rrex.initCause(xae);
				throw rrex;
			}
		} catch (RuntimeException ex) {
			SystemException sysex = new SystemException();
			sysex.initCause(ex);
			throw sysex;
		}

		return true;
	}

	public boolean enlistResource(XAResourceDescriptor descriptor) throws RollbackException, IllegalStateException,
			SystemException {

		XAResourceArchive archive = this.locateExisted(descriptor);
		int flags = XAResource.TMNOFLAGS;
		if (archive == null) {
			boolean resourceSupportsXA = descriptor.isSupportsXA();
			boolean currentSupportXA = this.xaSupports();
			if (resourceSupportsXA || currentSupportXA) {
				archive = new XAResourceArchive();
				archive.setDescriptor(descriptor);
				TransactionXid globalXid = this.transactionContext.getCurrentXid().getGlobalXid();
				archive.setXid(globalXid.createBranchXid());
			} else {
				throw new SystemException("There already has a non-xa resource exists.");
			}
		} else {
			flags = XAResource.TMJOIN;
		}

		return this.enlistResource(archive, flags);
	}

	private boolean enlistResource(XAResourceArchive archive, int flags) throws SystemException, RollbackException {
		try {
			Xid branchXid = archive.getXid();
			logger.info(String.format("\t[%s] enlist: xares= %s, flags: %s",
					ByteUtils.byteArrayToString(branchXid.getBranchQualifier()), archive, flags));

			if (flags == XAResource.TMNOFLAGS) {
				long expired = this.transactionContext.getExpiredTime();
				long current = System.currentTimeMillis();
				long remains = expired - current;
				int timeout = (int) (remains / 1000L);
				archive.setTransactionTimeout(timeout);
				archive.start(branchXid, flags);
				this.resources.add(archive);
			} else if (flags == XAResource.TMJOIN) {
				archive.start(branchXid, flags);
				archive.setDelisted(false);
			} else if (flags == XAResource.TMRESUME) {
				archive.start(branchXid, flags);
				archive.setDelisted(false);
			} else {
				throw new SystemException();
			}
		} catch (XAException xae) {
			xae.printStackTrace();

			// Possible exceptions are XA_RB*, XAER_RMERR, XAER_RMFAIL,
			// XAER_DUPID, XAER_OUTSIDE, XAER_NOTA, XAER_INVAL, or XAER_PROTO.
			switch (xae.errorCode) {
			case XAException.XAER_RMFAIL:
			case XAException.XAER_DUPID:
			case XAException.XAER_OUTSIDE:
			case XAException.XAER_NOTA:
			case XAException.XAER_INVAL:
			case XAException.XAER_PROTO:
				return false;
			case XAException.XAER_RMERR:
				SystemException sysex = new SystemException();
				sysex.initCause(xae);
				throw sysex;
			default:
				throw new RollbackException();
			}
		} catch (RuntimeException ex) {
			throw new RollbackException();
		}

		return true;
	}

	private XAResourceArchive locateExisted(XAResourceDescriptor descriptor) {
		Iterator<XAResourceArchive> itr = this.resources.iterator();
		while (itr.hasNext()) {
			XAResourceArchive existed = itr.next();
			XAResourceDescriptor existedDescriptor = existed.getDescriptor();
			String identifier = descriptor.getIdentifier();
			String existedIdentifirer = existedDescriptor.getIdentifier();

			if (CommonUtils.equals(identifier, existedIdentifirer)) {
				try {
					if (existedDescriptor.isSameRM(descriptor)) {
						return existed;
					}
				} catch (XAException ex) {
					continue;
				} catch (RuntimeException ex) {
					continue;
				}
			}// end-if
		}// end-while
		return null;

	}

	public void resumeAllResource() throws IllegalStateException, SystemException {
		boolean rollbackRequired = false;
		boolean errorExists = false;
		for (int i = 0; i < this.resources.size(); i++) {
			XAResourceArchive xares = this.resources.get(i);
			if (xares.isDelisted()) {
				try {
					this.enlistResource(xares, XAResource.TMRESUME);
				} catch (RollbackException rex) {
					rollbackRequired = true;
				} catch (SystemException rex) {
					errorExists = true;
				} catch (RuntimeException rex) {
					errorExists = true;
				}
			}
		}

		if (rollbackRequired) {
			throw new SystemException(XAException.XA_RBBASE);
		} else if (errorExists) {
			throw new SystemException(XAException.XAER_RMERR);
		}

	}

	public void suspendAllResource() throws IllegalStateException, SystemException {
		boolean rollbackRequired = false;
		boolean errorExists = false;
		for (int i = 0; i < this.resources.size(); i++) {
			XAResourceArchive xares = this.resources.get(i);
			if (xares.isDelisted() == false) {
				try {
					this.delistResource(xares, XAResource.TMSUSPEND);
				} catch (RollbackRequiredException ex) {
					rollbackRequired = true;
				} catch (SystemException ex) {
					errorExists = true;
				} catch (RuntimeException ex) {
					errorExists = true;
				}
			}
		}

		if (rollbackRequired) {
			throw new SystemException(XAException.XA_RBBASE);
		} else if (errorExists) {
			throw new SystemException(XAException.XAER_RMERR);
		}
	}

	public void delistAllResource() throws IllegalStateException, SystemException {
		boolean rollbackRequired = false;
		boolean errorExists = false;
		for (int i = 0; i < this.resources.size(); i++) {
			XAResourceArchive xares = this.resources.get(i);
			if (xares.isDelisted() == false) {
				try {
					this.delistResource(xares, XAResource.TMSUCCESS);
				} catch (RollbackRequiredException ex) {
					rollbackRequired = true;
				} catch (SystemException ex) {
					errorExists = true;
				} catch (RuntimeException ex) {
					errorExists = true;
				}
			}
		}// end-for

		if (rollbackRequired) {
			throw new SystemException(XAException.XA_RBBASE);
		} else if (errorExists) {
			throw new SystemException(XAException.XAER_RMERR);
		}

	}

	public List<XAResourceArchive> getResourceArchives() {
		return this.resources;
	}

}
