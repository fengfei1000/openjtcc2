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
package org.bytesoft.bytetcc.supports;

import java.util.Set;

import org.bytesoft.bytetcc.jta.JtaTransaction;
import org.bytesoft.bytetcc.xa.XidImpl;

public interface TransactionRepository {

	public void putTransaction(XidImpl globalXid, JtaTransaction transaction);

	public JtaTransaction getTransaction(XidImpl globalXid);

	public JtaTransaction removeTransaction(XidImpl globalXid);

	public void putErrorTransaction(XidImpl globalXid, JtaTransaction transaction);

	public JtaTransaction getErrorTransaction(XidImpl globalXid);

	public JtaTransaction removeErrorTransaction(XidImpl globalXid);

	public TransactionLogger getTransactionLogger();

	public Set<JtaTransaction> getActiveTransactionSet();

	public Set<JtaTransaction> getErrorTransactionSet();

}
