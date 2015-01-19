package org.bytesoft.transaction.supports.rpc;

import java.lang.reflect.Method;

import javax.transaction.xa.XAResource;

import org.bytesoft.byterpc.common.RemoteMethodKey;
import org.bytesoft.byterpc.supports.RemoteMethodFactory;
import org.bytesoft.byterpc.supports.RemoteMethodFactoryImpl;

public class TransactionMethodFactory extends RemoteMethodFactoryImpl implements RemoteMethodFactory {

	public void initialize() {
		try {
			Method[] methods = XAResource.class.getMethods();
			for (int i = 0; i < methods.length; i++) {
				Method method = methods[i];
				this.registerRemoteMethod(method);
			}
		} catch (RuntimeException rex) {
			Method[] methods = XAResource.class.getMethods();
			for (int i = 0; i < methods.length; i++) {
				Method method = methods[i];
				RemoteMethodKey methodKey = this.getRemoteMethodKey(method);
				if (methodKey != null) {
					this.removeRemoteMethod(methodKey);
				} // end-if (methodKey != null)
			} // end-for (int i = 0; i < methods.length; i++)
			throw rex;
		}
	}

}
