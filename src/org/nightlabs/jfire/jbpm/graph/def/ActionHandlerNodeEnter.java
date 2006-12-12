package org.nightlabs.jfire.jbpm.graph.def;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;

import org.apache.log4j.Logger;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.GraphElement;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.instantiation.Delegation;
import org.nightlabs.annotation.Implement;
import org.nightlabs.jdo.ObjectID;
import org.nightlabs.jdo.ObjectIDUtil;
import org.nightlabs.jfire.security.SecurityReflector;
import org.nightlabs.jfire.security.User;

public class ActionHandlerNodeEnter
extends AbstractActionHandler
{
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(ActionHandlerNodeEnter.class);

	public static void register(org.jbpm.graph.def.ProcessDefinition jbpmProcessDefinition)
	{
		Action action = new Action(new Delegation(ActionHandlerNodeEnter.class.getName()));
		action.setName(ActionHandlerNodeEnter.class.getName());

		Event event = new Event("node-enter");
		event.addAction(action);

		jbpmProcessDefinition.addEvent(event);
	}

	/**
	 * This variable name references the toString()-representation of the {@link ObjectID} which
	 * references the instance of {@link Statable} for which the {@link ProcessInstance} has been
	 * created.
	 */
	public static final String VARIABLE_NAME_STATABLE_ID = "statableID";
//	/**
//	 * This variable name references the fully qualified name of the class extending {@link StateDefinition}.
//	 * It is used in the {@link ContextInstance}. Usually, this will be the name of one of the following classes:
//	 * {@link OfferStateDefinition},
//	 * {@link org.nightlabs.jfire.accounting.state.InvoiceStateDefinition},
//	 * {@link org.nightlabs.jfire.store.state.DeliveryNoteStateDefinition}.
//	 */
//	public static final String VARIABLE_NAME_STATE_DEFINITION_CLASS = "stateDefinitionClass";

	public static State createStartState(PersistenceManager pm, User user, Statable statable,
			org.jbpm.graph.def.ProcessDefinition jbpmProcessDefinition)
	{
		if (logger.isDebugEnabled())
			logger.debug("createStartState: user=" + JDOHelper.getObjectId(user) + " statable=" + JDOHelper.getObjectId(statable) + " jbpmProcessDefinition=" + JDOHelper.getObjectId(jbpmProcessDefinition));

		StateDefinition stateDefinition = (StateDefinition) pm.getObjectById(StateDefinition.getStateDefinitionID(jbpmProcessDefinition.getStartState()));

		if (logger.isDebugEnabled())
			logger.debug("createStartState: stateDefinition=" + JDOHelper.getObjectId(stateDefinition));

		return stateDefinition.createState(user, statable);
//		return (State) pm.makePersistent(
//				new State(
//						IDGenerator.getOrganisationID(), IDGenerator.nextID(State.class),
//						user, statable, stateDefinition));
//		return stateDefinition.createState(user, statable);
	}

	@Implement
	protected void doExecute(ExecutionContext executionContext)
			throws Exception
	{
		GraphElement graphElement = executionContext.getEventSource();

		if (logger.isDebugEnabled())
			logger.debug("doExecute: graphElement.class=" + (graphElement == null ? null : graphElement.getClass().getName()) + " graphElement=" + graphElement);

//		if (!(graphElement instanceof org.jbpm.graph.node.State || graphElement instanceof org.jbpm.graph.node.EndState)) {
		if (!(graphElement instanceof org.jbpm.graph.def.Node)) {
			if (logger.isDebugEnabled())
				logger.debug("doExecute: graphElement is not an instance of an interesting type => return without action!");

			return;
		}

		org.jbpm.graph.def.Node jbpmNode = (org.jbpm.graph.def.Node) graphElement;

		PersistenceManager pm = getPersistenceManager();
		Object statableID = ObjectIDUtil.createObjectID((String) executionContext.getVariable(VARIABLE_NAME_STATABLE_ID));
		Statable statable = (Statable) pm.getObjectById(statableID);

		User user = SecurityReflector.getUserDescriptor().getUser(pm);

//		StateDefinition stateDefinition = (StateDefinition) pm.getObjectById(getStateDefinitionID(
//				(String) executionContext.getVariable(VARIABLE_NAME_STATE_DEFINITION_CLASS), jbpmState));
//		stateDefinition.createState(user, statable);
		StateDefinition stateDefinition = (StateDefinition) pm.getObjectById(StateDefinition.getStateDefinitionID(jbpmNode));

		if (logger.isDebugEnabled())
			logger.debug("doExecute: statable=" + statableID + " user=" + JDOHelper.getObjectId(user) + " stateDefinition=" + JDOHelper.getObjectId(stateDefinition));

		stateDefinition.createState(user, statable);
	}
}
