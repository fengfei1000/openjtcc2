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

import java.io.Serializable;

import org.bytesoft.bytejta.common.TransactionXid;
import org.bytesoft.utils.CommonUtils;

public class CompensableArchive {
	public boolean launchSvc;
	public TransactionXid branchXid;
	// public Compensable<Serializable> service;
	public Serializable variable;
	public boolean tryCommitted;
	public boolean confirmed;
	public boolean cancelled;
	public boolean committed;
	public boolean rolledback;

	public int hashCode() {
		int hash = 23;
		hash += 29 * (this.branchXid == null ? 0 : this.branchXid.hashCode());
		return hash;
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		} else if (this.getClass().equals(obj.getClass()) == false) {
			return false;
		}
		CompensableArchive that = (CompensableArchive) obj;
		return CommonUtils.equals(this.branchXid, that.branchXid);
	}
}
