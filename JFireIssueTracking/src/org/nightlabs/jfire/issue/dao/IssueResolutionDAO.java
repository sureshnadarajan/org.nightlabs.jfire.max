package org.nightlabs.jfire.issue.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.nightlabs.jfire.base.jdo.BaseJDOObjectDAO;
import org.nightlabs.jfire.issue.IssueManagerRemote;
import org.nightlabs.jfire.issue.IssueResolution;
import org.nightlabs.jfire.issue.id.IssueResolutionID;
import org.nightlabs.progress.ProgressMonitor;
import org.nightlabs.progress.SubProgressMonitor;

/**
 * Data access object for {@link IssuResolution}s.
 *
 * @author Chairat Kongarayawetchakun - chairat [AT] nightlabs [DOT] de
 */
public class IssueResolutionDAO
extends BaseJDOObjectDAO<IssueResolutionID, IssueResolution>
{
	private IssueResolutionDAO() {}

	private static IssueResolutionDAO sharedInstance = null;

	public static IssueResolutionDAO sharedInstance() {
		if (sharedInstance == null) {
			synchronized (IssueResolutionDAO.class) {
				if (sharedInstance == null)
					sharedInstance = new IssueResolutionDAO();
			}
		}
		return sharedInstance;
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	protected Collection<IssueResolution> retrieveJDOObjects(Set<IssueResolutionID> objectIDs,
			String[] fetchGroups, int maxFetchDepth, ProgressMonitor monitor)
			throws Exception {
		monitor.beginTask("Fetching IssueResolution...", 1); //$NON-NLS-1$
		try {
			IssueManagerRemote im = getEjbProvider().getRemoteBean(IssueManagerRemote.class);
			monitor.worked(1);
			return im.getIssueResolutions(objectIDs, fetchGroups, maxFetchDepth);
		} catch (Exception e) {
			monitor.setCanceled(true);
			throw e;
		} finally {
			monitor.done();
		}
	}

	public synchronized IssueResolution getIssueResolution(IssueResolutionID issueResolutionID, String[] fetchGroups, int maxFetchDepth, ProgressMonitor monitor)
	{
		monitor.beginTask("Loading issueResolution "+issueResolutionID.issueResolutionID, 1);
		IssueResolution issueResolution = getJDOObject(null, issueResolutionID, fetchGroups, maxFetchDepth, new SubProgressMonitor(monitor, 1));
		monitor.done();
		return issueResolution;
	}

	public List<IssueResolution> getIssueResolutions(String[] fetchGroups, int maxFetchDepth, ProgressMonitor monitor) {
		try {
			return new ArrayList<IssueResolution>(retrieveJDOObjects(null, fetchGroups, maxFetchDepth, monitor));
		} catch (Exception e) {
			throw new RuntimeException("Error while fetching issue resolutions: " + e.getMessage(), e); //$NON-NLS-1$
		}
	}

	public IssueResolution storeIssueResolution(IssueResolution issueResolution, boolean get, String[] fetchGroups, int maxFetchDepth, ProgressMonitor monitor) {
		if(issueResolution == null)
			throw new NullPointerException("Issue Resolution to save must not be null");
		monitor.beginTask("Storing issuePriority: "+ issueResolution.getIssueResolutionID(), 3);
		try {
			IssueManagerRemote im = getEjbProvider().getRemoteBean(IssueManagerRemote.class);
			IssueResolution result = im.storeIssueResolution(issueResolution, get, fetchGroups, maxFetchDepth);
			monitor.worked(1);
			monitor.done();
			return result;
		} catch (Exception e) {
			monitor.done();
			throw new RuntimeException("Error while storing IssuePriority!\n" ,e);
		}
	}
}