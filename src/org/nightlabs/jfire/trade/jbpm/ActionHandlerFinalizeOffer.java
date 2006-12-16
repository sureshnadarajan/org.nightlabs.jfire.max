package org.nightlabs.jfire.trade.jbpm;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;

import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.instantiation.Delegation;
import org.nightlabs.annotation.Implement;
import org.nightlabs.jfire.asyncinvoke.AsyncInvoke;
import org.nightlabs.jfire.jbpm.graph.def.AbstractActionHandler;
import org.nightlabs.jfire.security.SecurityReflector;
import org.nightlabs.jfire.security.User;
import org.nightlabs.jfire.trade.Offer;
import org.nightlabs.jfire.trade.OfferActionHandler;
import org.nightlabs.jfire.trade.id.OfferID;

public class ActionHandlerFinalizeOffer
extends AbstractActionHandler
{
	private static final long serialVersionUID = 1L;

	public static void register(org.jbpm.graph.def.ProcessDefinition jbpmProcessDefinition)
	{
		Action action = new Action(new Delegation(ActionHandlerFinalizeOffer.class.getName()));
		action.setName(ActionHandlerFinalizeOffer.class.getName());

		Event event = new Event("node-enter");
		event.addAction(action);

		Node finalized = jbpmProcessDefinition.getNode(JbpmConstantsOffer.Vendor.NODE_NAME_FINALIZED);
		if (finalized == null)
			throw new IllegalArgumentException("The node \""+ JbpmConstantsOffer.Vendor.NODE_NAME_FINALIZED +"\" does not exist in the ProcessDefinition \"" + jbpmProcessDefinition.getName() + "\"!");

		finalized.addEvent(event);
	}

	@Implement
	protected void doExecute(ExecutionContext executionContext)
	throws Exception
	{
		PersistenceManager pm = getPersistenceManager();
		Offer offer = (Offer) getStatable();

		doExecute(pm, offer);
	}

	/**
	 * This method is called by {@link #doExecute(ExecutionContext)} and by {@link ActionHandlerAcceptOfferImplicitelyVendor#doExecute(ExecutionContext)}.
	 */
	protected static void doExecute(PersistenceManager pm, Offer offer)
	throws Exception
	{
		if (offer.isFinalized())
			return;

		User user = SecurityReflector.getUserDescriptor().getUser(pm);

		offer.setFinalized(user);
		for (OfferActionHandler offerActionHandler : offer.getOfferLocal().getOfferActionHandlers()) {
			offerActionHandler.onFinalizeOffer(user, offer);
		}

		AsyncInvoke.exec(new SendOfferInvocation((OfferID) JDOHelper.getObjectId(offer)), true);
	}
}
