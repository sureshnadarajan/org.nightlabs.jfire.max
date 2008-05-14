/**
 * 
 */
package org.nightlabs.jfire.issue.jbpm;

import java.util.Locale;

import javax.jdo.JDOObjectNotFoundException;

import org.nightlabs.jfire.jbpm.graph.def.ProcessDefinition;
import org.nightlabs.jfire.jbpm.graph.def.StateDefinition;
import org.nightlabs.jfire.organisation.Organisation;

/**
 * @author Alexander Bieber <!-- alex [AT] nightlabs [DOT] de -->
 *
 */
public class JbpmConstants
{
	private JbpmConstants() { }

	public static final String NODE_NAME_NEW = Organisation.DEV_ORGANISATION_ID + ":new";
	public static final String NODE_NAME_OPEN = Organisation.DEV_ORGANISATION_ID + ":open";
	public static final String NODE_NAME_ACKNOWLEDGED = Organisation.DEV_ORGANISATION_ID + ":acknowledged";
	public static final String NODE_NAME_ACKNOWLEDGED_IMPLICITELY = Organisation.DEV_ORGANISATION_ID + ":acknowledgedImplicitely";
	public static final String NODE_NAME_CONFIRMED = Organisation.DEV_ORGANISATION_ID + ":confirmed";
	public static final String NODE_NAME_CONFIRMED_IMPLICITELY = Organisation.DEV_ORGANISATION_ID + ":confirmedImplicitely";
	public static final String NODE_NAME_ASSIGNED = Organisation.DEV_ORGANISATION_ID + ":assigned";
	public static final String NODE_NAME_RESOLVED = Organisation.DEV_ORGANISATION_ID + ":resolved";
	public static final String NODE_NAME_RESOLVED_IMPLICITELY = Organisation.DEV_ORGANISATION_ID + ":resolvedImplicitely";
	public static final String NODE_NAME_CLOSED = Organisation.DEV_ORGANISATION_ID + ":closed";
	public static final String NODE_NAME_REOPENED = Organisation.DEV_ORGANISATION_ID + ":reopened";
	public static final String NODE_NAME_REJECTED = Organisation.DEV_ORGANISATION_ID + ":rejected";

	public static final String TRANSITION_NAME_NEW = "new";
	public static final String TRANSITION_NAME_OPEN = "open";
	public static final String TRANSITION_NAME_ACKNOWLEDGE = "acknowledge";
	public static final String TRANSITION_NAME_CONFIRM = "confirm";
	public static final String TRANSITION_NAME_ASSIGN = "assign";
	public static final String TRANSITION_NAME_RESOLVE = "resolve";
	public static final String TRANSITION_NAME_CLOSE = "close";
	public static final String TRANSITION_NAME_REOPEN = "reopen";
	public static final String TRANSITION_NAME_REJECT = "reject";
	/**
	 * Sets the State properties for the standard IssueTracking workflow.
	 * @param processDefinition The {@link ProcessDefinition} to use.
	 */
	public static void initStandardProcessDefinition(ProcessDefinition processDefinition) {
		setStateDefinitionProperties(processDefinition, NODE_NAME_NEW, 
				"New", "Issue has been created", true);
		setStateDefinitionProperties(processDefinition, NODE_NAME_OPEN, 
				"Open", "Issue has been opened", true);
		setStateDefinitionProperties(processDefinition, NODE_NAME_ACKNOWLEDGED, 
				"Acknowledged", "Issue has been acknowledged", true);
		setStateDefinitionProperties(processDefinition, NODE_NAME_ACKNOWLEDGED_IMPLICITELY, 
				"Acknowledged implicitely", "Issue has been acknowledged while setting a differen state.", true);
		setStateDefinitionProperties(processDefinition, NODE_NAME_CONFIRMED, 
				"Confirmed", "Issue has been confirmed", true);
		setStateDefinitionProperties(processDefinition, NODE_NAME_CONFIRMED_IMPLICITELY, 
				"Confirmed implicitely", "Issue has been confirmed while setting a different state", true);
		setStateDefinitionProperties(processDefinition, NODE_NAME_ASSIGNED, 
				"Assigned", "Issue has been assigned", true);
		setStateDefinitionProperties(processDefinition, NODE_NAME_RESOLVED, 
				"Resolved", "Issue has been resolved", true);
		setStateDefinitionProperties(processDefinition, NODE_NAME_RESOLVED_IMPLICITELY, 
				"Resolved implicitely", "Issue has been resolved while closing", true);
		setStateDefinitionProperties(processDefinition, NODE_NAME_CLOSED, 
				"Closed", "Issue has been closed", true);
		setStateDefinitionProperties(processDefinition, NODE_NAME_REOPENED, 
				"Reopened", "Issue has been reopened", true);
		setStateDefinitionProperties(processDefinition, NODE_NAME_REJECTED, 
				"Rejected", "Issue has been rejected", true);
	}
	
	private static void setStateDefinitionProperties(
			ProcessDefinition processDefinition, String jbpmNodeName,
			String name, String description, boolean publicState)
	{
		StateDefinition stateDefinition;
		try {
			stateDefinition = StateDefinition.getStateDefinition(processDefinition, jbpmNodeName);
		} catch (JDOObjectNotFoundException x) {
			return;
		}
		stateDefinition.getName().setText(Locale.ENGLISH.getLanguage(), name);
		stateDefinition.getDescription().setText(Locale.ENGLISH.getLanguage(), description);
		stateDefinition.setPublicState(publicState);
	}
}
