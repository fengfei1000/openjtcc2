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
package org.bytesoft.bytetcc.archive;

import javax.transaction.xa.Xid;

import org.bytesoft.bytejta.utils.CommonUtils;
import org.bytesoft.bytetcc.CompensableInvocation;

public class CompensableArchive {
	private Xid xid;
	private CompensableInvocation compensable;
	private boolean confirmed;
	private boolean cancelled;
	private boolean committed;
	private boolean rolledback;

	public int hashCode() {
		int hash = 23;
		hash += 29 * (this.xid == null ? 0 : this.xid.hashCode());
		return hash;
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		} else if (this.getClass().equals(obj.getClass()) == false) {
			return false;
		}
		CompensableArchive that = (CompensableArchive) obj;
		return CommonUtils.equals(this.xid, that.xid);
	}

	public Xid getXid() {
		return xid;
	}

	public void setXid(Xid xid) {
		this.xid = xid;
	}

	public CompensableInvocation getCompensable() {
		return compensable;
	}

	public void setCompensable(CompensableInvocation compensable) {
		this.compensable = compensable;
	}

	public boolean isConfirmed() {
		return confirmed;
	}

	public void setConfirmed(boolean confirmed) {
		this.confirmed = confirmed;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
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

}
