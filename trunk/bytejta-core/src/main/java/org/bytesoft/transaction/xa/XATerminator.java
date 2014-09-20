package org.bytesoft.transaction.xa;

import java.util.List;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.bytesoft.transaction.RemoteSystemException;
import org.bytesoft.transaction.archive.XAResourceArchive;

public interface XATerminator extends javax.transaction.xa.XAResource {

	public boolean xaSupports() throws RemoteSystemException;

	public boolean delistResource(XAResourceDescriptor xaRes, int flag) throws IllegalStateException, SystemException;

	public boolean enlistResource(XAResourceDescriptor xaRes) throws RollbackException, IllegalStateException,
			SystemException;

	public void resumeAllResource() throws IllegalStateException, SystemException;

	public void suspendAllResource() throws IllegalStateException, SystemException;

	public void delistAllResource() throws IllegalStateException, SystemException;

	public List<XAResourceArchive> getResourceArchives();

}
