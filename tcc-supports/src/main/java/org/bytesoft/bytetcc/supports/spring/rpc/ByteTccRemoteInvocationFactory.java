package org.bytesoft.bytetcc.supports.spring.rpc;

import org.bytesoft.byterpc.RemoteInvocation;
import org.bytesoft.byterpc.RemoteInvocationResult;
import org.bytesoft.byterpc.supports.RemoteInvocationFactory;

public class ByteTccRemoteInvocationFactory implements RemoteInvocationFactory {

	public RemoteInvocation createRemoteInvocation() {
		return new ByteTccRemoteInvocation();
	}

	public RemoteInvocationResult createRemoteInvocationResult(RemoteInvocation invocation) {
		if (ByteTccRemoteInvocation.class.isInstance(invocation)) {
			ByteTccRemoteInvocation tccInvocation = (ByteTccRemoteInvocation) invocation;
			return new ByteTccRemoteInvocationResult(tccInvocation);
		} else {
			return new RemoteInvocationResult(invocation);
		}
	}

}
