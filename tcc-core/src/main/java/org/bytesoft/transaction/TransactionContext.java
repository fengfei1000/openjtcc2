/**
 * Copyright 2014 yangming.liu<liuyangming@gmail.com>.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, see <http://www.gnu.org/licenses/>.
 */
package org.bytesoft.transaction;

import java.io.Serializable;

import org.bytesoft.bytejta.common.XidImpl;

public class TransactionContext implements Serializable, Cloneable {
	private static final long serialVersionUID = 1L;

	private boolean optimized;
	private transient boolean coordinator;
	private transient boolean recovery;
	// private transient XidImpl creationXid;
	// private transient Object terminalKey;
	// private transient final Stack<XidImpl> stack;

	private XidImpl currentXid;
	// private PropagationKey instanceKey;
	private long createdTime;
	private long expiredTime;
	private boolean compensable;

	public TransactionContext() {
		// this.stack = new Stack<XidImpl>();
	}

	// public synchronized void propagateTransactionContext(TransactionContext that) {
	// XidImpl xid = that.getCurrentXid();
	// if (xid.equals(this.creationXid)) {
	// // ignore
	// } else {
	// this.stack.push(this.currentXid);
	// this.currentXid = xid;
	// }
	// }

	// public synchronized void revertTransactionContext() {
	// if (this.currentXid.equals(this.creationXid)) {
	// // ignore
	// } else {
	// this.currentXid = this.stack.pop();
	// }
	// }

	public TransactionContext clone() {
		TransactionContext that = new TransactionContext();
		that.currentXid = this.currentXid;
		// that.instanceKey = this.instanceKey;
		that.createdTime = System.currentTimeMillis();
		that.expiredTime = this.getExpiredTime();
		that.compensable = this.compensable;
		return that;
	}

	public boolean isCoordinator() {
		// if (this.coordinator) {
		// return this.currentXid.equals(this.creationXid);
		// } else {
		// return false;
		// }
		return this.coordinator;
		// throw new RuntimeException();
	}

	public XidImpl getCurrentXid() {
		return currentXid;
	}

	public void setCurrentXid(XidImpl branchXid) {
		this.currentXid = branchXid;
	}

	public XidImpl getGlobalXid() {
		return this.currentXid.getGlobalXid();
	}

	public void setCoordinator(boolean coordinator) {
		this.coordinator = coordinator;
	}

	public long getExpiredTime() {
		return expiredTime;
	}

	public void setExpiredTime(long expiredTime) {
		this.expiredTime = expiredTime;
	}

	public long getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(long createdTime) {
		this.createdTime = createdTime;
	}

	public boolean isCompensable() {
		return compensable;
	}

	public void setCompensable(boolean compensable) {
		this.compensable = compensable;
	}

	public boolean isFresh() {
		return this.recovery == false;
	}

	public boolean isRecovery() {
		return recovery;
	}

	public void setRecovery(boolean recovery) {
		this.recovery = recovery;
	}

	public boolean isOptimized() {
		return optimized;
	}

	public void setOptimized(boolean optimized) {
		this.optimized = optimized;
	}

}
