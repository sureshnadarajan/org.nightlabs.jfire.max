package org.nightlabs.jfire.jbpm.dao;

import java.util.Collection;
import java.util.Set;

import org.nightlabs.jfire.base.jdo.BaseJDOObjectDAO;
import org.nightlabs.jfire.jbpm.JbpmManagerRemote;
import org.nightlabs.jfire.jbpm.graph.def.StateDefinition;
import org.nightlabs.jfire.jbpm.graph.def.id.StateDefinitionID;
import org.nightlabs.progress.ProgressMonitor;

/**
 * @author Daniel.Mazurek [at] NightLabs [dot] de
 *
 */
public class StateDefinitionDAO
extends BaseJDOObjectDAO<StateDefinitionID, StateDefinition>
{
	private static StateDefinitionDAO sharedInstance;
	public static StateDefinitionDAO sharedInstance() {
		if (sharedInstance == null) {
			synchronized (StateDefinitionDAO.class) {
				if (sharedInstance == null)
					sharedInstance = new StateDefinitionDAO();
			}
		}
		return sharedInstance;
	}

	protected StateDefinitionDAO() {
		super();
	}

	@Override
	protected Collection<StateDefinition> retrieveJDOObjects(
			Set<StateDefinitionID> objectIDs, String[] fetchGroups,
			int maxFetchDepth, ProgressMonitor monitor)
	throws Exception
	{
		monitor.beginTask("Loading StateDefintions", 2);
		monitor.worked(1);
		JbpmManagerRemote jbpmManager = getEjbProvider().getRemoteBean(JbpmManagerRemote.class);
		Collection<StateDefinition> stateDefintions = jbpmManager.getStateDefinitions(objectIDs, fetchGroups, maxFetchDepth);
		monitor.worked(1);
		return stateDefintions;
	}

	public Collection<StateDefinition> getStateDefintions(
			Set<StateDefinitionID> objectIDs, String[] fetchGroups,
			int maxFetchDepth, ProgressMonitor monitor)
	{
		return getJDOObjects(null, objectIDs, fetchGroups, maxFetchDepth, monitor);
	}
	
	public StateDefinition getStateDefintion(
			StateDefinitionID objectID, String[] fetchGroups,
			int maxFetchDepth, ProgressMonitor monitor)
	{
		return getJDOObject(null, objectID, fetchGroups, maxFetchDepth, monitor);
	}
	
}
