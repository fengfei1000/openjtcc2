/**
 * Copyright 2014-2015 yangming.liu<liuyangming@gmail.com>.
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
package org.bytesoft.bytetcc.supports;

import javax.transaction.xa.XAResource;

public final class RecoverableXAResource {

	private boolean supportXA;
	private XAResource delegate;
	private String recoveryURI;

	public boolean isSupportXA() {
		return supportXA;
	}

	public void setSupportXA(boolean supportXA) {
		this.supportXA = supportXA;
	}

	public XAResource getDelegate() {
		return delegate;
	}

	public void setDelegate(XAResource delegate) {
		this.delegate = delegate;
	}

	public String getDelegateURI() {
		return recoveryURI;
	}

	public void setDelegateURI(String delegateURI) {
		this.recoveryURI = delegateURI;
	}

}
