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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import org.bytesoft.bytetcc.archive.CompensableArchive;
import org.bytesoft.bytetcc.archive.TransactionArchive;
import org.bytesoft.transaction.TransactionContext;

public interface TransactionLogger {
	public static final String NULL = "(null)";

	/* service */
	public void enlistService(TransactionContext transactionContext, CompensableArchive holder);

	public void delistService(TransactionContext transactionContext, CompensableArchive holder);

	public void updateService(TransactionContext transactionContext, CompensableArchive holder);

	public void confirmService(TransactionContext transactionContext, CompensableArchive holder);

	public void cancelService(TransactionContext transactionContext, CompensableArchive holder);

	public void commitService(TransactionContext transactionContext, CompensableArchive holder);

	public void rollbackService(TransactionContext transactionContext, CompensableArchive holder);

	/* transaction */
	public void beginTransaction(TransactionArchive transaction);

	public void prepareTransaction(TransactionArchive transaction);

	public void updateTransaction(TransactionArchive transaction);

	public void completeTransaction(TransactionArchive transaction);

	public void cleanupTransaction(TransactionArchive transaction);

	public Set<TransactionArchive> getLoggedTransactionSet();

	/* default transaction logger */
	public static TransactionLogger defaultTransactionLogger = NullTransactionLoggerHanlder.getNullTransactionLogger();

	public static class NullTransactionLoggerHanlder implements InvocationHandler {

		// private static final Logger logger = Logger.getLogger("bytetcc");
		private static final NullTransactionLoggerHanlder instance = new NullTransactionLoggerHanlder();

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Class<?> clazz = method.getReturnType();
			if (Void.TYPE.equals(clazz)) {
				return null;
			} else if (Set.class.equals(clazz)) {
				return this.newInstance(HashSet.class);
			} else {
				return null;
			}
		}

		private Object newInstance(Class<?> clazz) {
			try {
				return clazz.newInstance();
			} catch (Exception ex) {
				return null;
			}
		}

		public static TransactionLogger getNullTransactionLogger() {
			return (TransactionLogger) Proxy.newProxyInstance(TransactionLogger.class.getClassLoader(),
					new Class[] { TransactionLogger.class }, instance);
		}
	}
}