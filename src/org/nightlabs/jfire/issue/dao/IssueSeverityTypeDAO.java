package org.nightlabs.jfire.issue.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.nightlabs.jdo.NLJDOHelper;
import org.nightlabs.jfire.base.jdo.BaseJDOObjectDAO;
import org.nightlabs.jfire.issue.IssueManagerRemote;
import org.nightlabs.jfire.issue.IssueSeverityType;
import org.nightlabs.jfire.issue.id.IssueSeverityTypeID;
import org.nightlabs.progress.ProgressMonitor;

/**
 * Data access object for {@link IssueSeverityType}s.
 *
 * @author Chairat Kongarayawetchakun - chairat [AT] nightlabs [DOT] de
 */
public class IssueSeverityTypeDAO
		extends BaseJDOObjectDAO<IssueSeverityTypeID, IssueSeverityType>
{
	private IssueSeverityTypeDAO() {}

	private static IssueSeverityTypeDAO sharedInstance = null;

	public static IssueSeverityTypeDAO sharedInstance()
	{
		if (sharedInstance == null) {
			synchronized (IssueSeverityTypeDAO.class) {
				if (sharedInstance == null)
					sharedInstance = new IssueSeverityTypeDAO();
			}
		}
		return sharedInstance;
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	protected Collection<IssueSeverityType> retrieveJDOObjects(Set<IssueSeverityTypeID> objectIDs,
			String[] fetchGroups, int maxFetchDepth, ProgressMonitor monitor)
			throws Exception
	{
		monitor.beginTask("Fetching severity types information", 1);
		Collection<IssueSeverityType> issueSeverityTypes;
		try {			
			IssueManagerRemote im = getEjbProvider().getRemoteBean(IssueManagerRemote.class);
			if (objectIDs == null) {
				objectIDs = im.getIssueSeverityTypeIDs();	
			}
			issueSeverityTypes = im.getIssueSeverityTypes(objectIDs, fetchGroups, maxFetchDepth);
			monitor.worked(1);
		} catch (Exception e) {
			monitor.done();
			throw new RuntimeException("Failed downloading severity types information!", e);
		}

		monitor.done();
		return issueSeverityTypes;
	}

	public synchronized IssueSeverityType getIssueSeverityType(IssueSeverityTypeID issueSeverityTypeID, String[] fetchGroups, int maxFetchDepth, ProgressMonitor monitor)
	{
		return getJDOObject(null, issueSeverityTypeID, fetchGroups, maxFetchDepth, monitor);
	}

	public List<IssueSeverityType> getIssueSeverityTypes(Set<IssueSeverityTypeID> issueSeverityTypeIDs, String[] fetchGroups, int maxFetchDepth, ProgressMonitor monitor)
	{
		return getJDOObjects(null, issueSeverityTypeIDs, fetchGroups, maxFetchDepth, monitor);
	}

	public List<IssueSeverityType> getIssueSeverityTypes(String[] fetchGroups, int maxFetchDepth, ProgressMonitor monitor)
	{
		try {
			return new ArrayList<IssueSeverityType>(retrieveJDOObjects(null, fetchGroups, NLJDOHelper.MAX_FETCH_DEPTH_NO_LIMIT, monitor));
		} catch (Exception e) {
			throw new RuntimeException("Error while fetching issue severity type: " + e.getMessage(), e); //$NON-NLS-1$
		}
	}

	public IssueSeverityType storeIssueSeverityType(IssueSeverityType issueSeverityType, boolean get, String[] fetchGroups, int maxFetchDepth, ProgressMonitor monitor)
	{
		if(issueSeverityType == null)
			throw new NullPointerException("Issue severity type to save must not be null");
		monitor.beginTask("Storing issueSeverityType: "+ issueSeverityType.getIssueSeverityTypeID(), 3);
		try {
			IssueManagerRemote im = getEjbProvider().getRemoteBean(IssueManagerRemote.class);
			IssueSeverityType result = im.storeIssueSeverityType(issueSeverityType, get, fetchGroups, maxFetchDepth);
			monitor.worked(1);
			monitor.done();
			return result;
		} catch (Exception e) {
			monitor.done();
			throw new RuntimeException("Error while storing IssueSeverityType!\n" ,e);
		}
	}
}