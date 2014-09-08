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

import org.bytesoft.bytejta.common.TransactionXid;
import org.bytesoft.bytetcc.jta.JtaTransaction;

public interface TransactionRepository {

	public void putTransaction(TransactionXid globalXid, JtaTransaction transaction);

	public JtaTransaction getTransaction(TransactionXid globalXid);

	public JtaTransaction removeTransaction(TransactionXid globalXid);

	public void putErrorTransaction(TransactionXid globalXid, JtaTransaction transaction);

	public JtaTransaction getErrorTransaction(TransactionXid globalXid);

	public JtaTransaction removeErrorTransaction(TransactionXid globalXid);

	public TransactionLogger getTransactionLogger();

	public Set<JtaTransaction> getActiveTransactionSet();

	public Set<JtaTransaction> getErrorTransactionSet();

}
