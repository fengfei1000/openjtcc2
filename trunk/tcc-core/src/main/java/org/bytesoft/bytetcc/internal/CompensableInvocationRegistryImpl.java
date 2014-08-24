package org.bytesoft.bytetcc.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bytesoft.bytetcc.CompensableInvocation;
import org.bytesoft.bytetcc.CompensableInvocationRegistry;

public class CompensableInvocationRegistryImpl implements CompensableInvocationRegistry {
	private static final CompensableInvocationRegistryImpl instance = new CompensableInvocationRegistryImpl();
	private final Map<Thread, CompensableInvocationImpl> invocations = new ConcurrentHashMap<Thread, CompensableInvocationImpl>();

	private CompensableInvocationRegistryImpl() {
		if (instance != null) {
			throw new IllegalStateException();
		}
	}

	public void registerCompensableInvocation(CompensableInvocation invocation) throws IllegalStateException {
		if (this.invocations.containsKey(Thread.currentThread())) {
			throw new IllegalStateException("There is a compensable service exists.");
		} else {
			this.invocations.put(Thread.currentThread(), (CompensableInvocationImpl) invocation);
		}
	}

	public void unregisterCompensableInvocation(CompensableInvocation invocation) {
		this.invocations.remove(Thread.currentThread());
	}

	public CompensableInvocationImpl getCompensableInvocation() {
		return this.invocations.get(Thread.currentThread());
	}

	public static CompensableInvocationRegistryImpl getInstance() {
		return instance;
	}

}
