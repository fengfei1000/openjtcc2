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

import java.util.Map;

import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.TransactionStatus;
import org.bytesoft.transaction.xa.TransactionXid;

public interface TransactionArchive {

	public TransactionStatus getTransactionStatus();

	public Map<TransactionXid, CompensableArchive> getCompensableArchiveMap();

	public TransactionContext getTransactionContext();

	public void setTransactionStatus(TransactionStatus status);

	public void setTransactionContext(TransactionContext context);

}
