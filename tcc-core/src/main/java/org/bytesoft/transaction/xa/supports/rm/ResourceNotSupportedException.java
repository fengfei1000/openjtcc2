package org.bytesoft.transaction.xa.supports.rm;

public class ResourceNotSupportedException extends Exception {
	private static final long serialVersionUID = 1L;

	public ResourceNotSupportedException() {
		super();
	}

	public ResourceNotSupportedException(String message) {
		super(message);
	}

	public ResourceNotSupportedException(String message, Throwable cause) {
		super(message, cause);
	}

}
