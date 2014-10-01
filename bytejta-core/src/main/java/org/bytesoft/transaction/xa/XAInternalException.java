package org.bytesoft.transaction.xa;

import javax.transaction.xa.XAException;

public class XAInternalException extends XAException {
	private static final long serialVersionUID = 1L;

	public XAInternalException() {
		super();
	}

	public XAInternalException(String s) {
		super(s);
	}

	public XAInternalException(int errcode) {
		super(errcode);
	}

}
