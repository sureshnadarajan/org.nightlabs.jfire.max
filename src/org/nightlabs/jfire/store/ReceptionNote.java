package org.nightlabs.jfire.store;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.listener.AttachCallback;
import javax.jdo.listener.DetachCallback;

import org.nightlabs.jfire.jbpm.graph.def.ActionHandlerNodeEnter;
import org.nightlabs.jfire.jbpm.graph.def.Statable;
import org.nightlabs.jfire.jbpm.graph.def.StatableLocal;
import org.nightlabs.jfire.jbpm.graph.def.State;
import org.nightlabs.jfire.security.User;
import org.nightlabs.jfire.trade.Article;
import org.nightlabs.jfire.trade.ArticleContainer;
import org.nightlabs.jfire.trade.ArticleContainerException;
import org.nightlabs.jfire.transfer.id.AnchorID;
import org.nightlabs.util.CollectionUtil;
import org.nightlabs.util.Utils;

/**
 * @author Marco Schulze - Marco at NightLabs dot de
 *
 * @jdo.persistence-capable
 *		identity-type="application"
 *		objectid-class="org.nightlabs.jfire.store.id.ReceptionNoteID"
 *		detachable="true"
 *		table="JFireTrade_ReceptionNote"
 *
 * @jdo.implements name="org.nightlabs.jfire.trade.ArticleContainer"
 * @jdo.implements name="org.nightlabs.jfire.jbpm.graph.def.Statable"
 *
 * @jdo.inheritance strategy="new-table"
 *
 * @jdo.create-objectid-class
 *		field-order="organisationID, deliveryNoteIDPrefix, deliveryNoteID, receptionNoteID"
 *		add-interfaces="org.nightlabs.jfire.trade.id.ArticleContainerID"
 *
 * TODO other fetch-groups
 *
 * @jdo.fetch-group name="Statable.state" fields="state"
 * @jdo.fetch-group name="Statable.states" fields="states"
 */
public class ReceptionNote
implements
		Serializable,
		ArticleContainer,
		AttachCallback,
		DetachCallback,
		Statable
{
	private static final long serialVersionUID = 1L;

	// the following fetch-groups are virtual and processed in the detach callback
	public static final String FETCH_GROUP_VENDOR_ID = "ReceptionNote.vendorID";
	public static final String FETCH_GROUP_CUSTOMER_ID = "ReceptionNote.customerID";

	/**
	 * @jdo.field primary-key="true"
	 * @jdo.column length="100"
	 */
	private String organisationID;
	/**
	 * @jdo.field primary-key="true"
	 * @jdo.column length="50"
	 */
	private String deliveryNoteIDPrefix;
	/**
	 * @jdo.field primary-key="true"
	 */
	private long deliveryNoteID;
	/**
	 * @jdo.field primary-key="true"
	 */
	private int receptionNoteID;

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private DeliveryNote deliveryNote;

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private Date createDT;

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private User createUser;

	/**
	 * @jdo.field
	 *		persistence-modifier="persistent"
	 *		collection-type="collection"
	 *		element-type="org.nightlabs.jfire.trade.Article"
	 *		mapped-by="receptionNote"
	 */
	private Set<Article> articles;

	/**
	 * @jdo.field persistence-modifier="none"
	 */
	private AnchorID vendorID = null;
	/**
	 * @jdo.field persistence-modifier="none"
	 */
	private boolean vendorID_detached = false;

	/**
	 * @jdo.field persistence-modifier="none"
	 */
	private AnchorID customerID = null;
	/**
	 * @jdo.field persistence-modifier="none"
	 */
	private boolean customerID_detached = false;

	/**
	 * @jdo.field persistence-modifier="persistent" mapped-by="receptionNote"
	 */
	private ReceptionNoteLocal receptionNoteLocal;

	/**
	 * @deprecated Only for JDO! 
	 */
	protected ReceptionNote() { }

	public ReceptionNote(User createUser, DeliveryNote deliveryNote)
	{
		this.createUser = createUser;
		this.deliveryNote = deliveryNote;
		this.organisationID = deliveryNote.getOrganisationID();
		this.deliveryNoteIDPrefix = deliveryNote.getDeliveryNoteIDPrefix();
		this.deliveryNoteID = deliveryNote.getDeliveryNoteID();
		this.receptionNoteID = deliveryNote.createReceptionNoteID();

		createDT = new Date();
		articles = new HashSet<Article>();
	}

	public String getOrganisationID()
	{
		return organisationID;
	}
	public String getDeliveryNoteIDPrefix()
	{
		return deliveryNoteIDPrefix;
	}
	public long getDeliveryNoteID()
	{
		return deliveryNoteID;
	}
	public int getReceptionNoteID()
	{
		return receptionNoteID;
	}
	public DeliveryNote getDeliveryNote()
	{
		return deliveryNote;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (!(obj instanceof ReceptionNote)) return false;
		ReceptionNote o = (ReceptionNote) obj;
		return
				Utils.equals(this.organisationID,       o.organisationID) &&
				Utils.equals(this.deliveryNoteIDPrefix, o.deliveryNoteIDPrefix) &&
				Utils.equals(this.deliveryNoteID,       o.deliveryNoteID) &&
				Utils.equals(this.receptionNoteID,      o.receptionNoteID);
	}
	@Override
	public int hashCode()
	{
		return Utils.hashCode(organisationID) + Utils.hashCode(deliveryNoteIDPrefix) + Utils.hashCode(deliveryNoteID) + receptionNoteID;
	}

	public void addArticle(Article article)
	throws ArticleContainerException
	{
		// TODO check, whether we're allowed to add or whether we're maybe finalized (or other problems exist - i.e. the Article being already in another ReceptionNote)
		
		articles.add(article);
	}

	public void removeArticle(Article article)
	throws ArticleContainerException
	{
		articles.remove(article);
	}

	/**
	 * @jdo.field persistence-modifier="none"
	 */
	private transient Set<Article> _articles = null;

	public Collection getArticles()
	{
		if (_articles == null)
			_articles = Collections.unmodifiableSet(articles);
		return _articles;
	}

	public Date getCreateDT()
	{
		return createDT;
	}

	public User getCreateUser()
	{
		return createUser;
	}

	public AnchorID getVendorID()
	{
		if (vendorID == null && !vendorID_detached)
			vendorID = deliveryNote.getVendorID();

		return vendorID;
	}

	public AnchorID getCustomerID()
	{
		if (customerID == null && !customerID_detached)
			customerID = deliveryNote.getCustomerID();

		return customerID;
	}

	public ReceptionNoteLocal getReceptionNoteLocal()
	{
		return receptionNoteLocal;
	}
	protected void setReceptionNoteLocal(ReceptionNoteLocal receptionNoteLocal)
	{
		this.receptionNoteLocal = receptionNoteLocal;
	}

//	/**
//	 * @jdo.field persistence-modifier="none"
//	 */
//	private boolean attachable = true;

	protected PersistenceManager getPersistenceManager()
	{
		PersistenceManager pm = JDOHelper.getPersistenceManager(this);
		if (pm == null)
			throw new IllegalStateException("This instance of ReceptionNote is currently not attached to the datastore. Cannot obtain PersistenceManager!");
		return pm;
	}

	public void jdoPreDetach()
	{
	}

	public void jdoPostDetach(Object _attached)
	{
		ReceptionNote attached = (ReceptionNote)_attached;
		ReceptionNote detached = this;
		Collection fetchGroups = attached.getPersistenceManager().getFetchPlan().getGroups();

		if (fetchGroups.contains(FETCH_GROUP_VENDOR_ID)) {
			detached.vendorID = attached.getVendorID();
			detached.vendorID_detached = true;
		}

		if (fetchGroups.contains(FETCH_GROUP_CUSTOMER_ID)) {
			detached.customerID = attached.getCustomerID();
			detached.customerID_detached = true;
		}
//		detached.attachable = true;
	}

	public void jdoPreAttach()
	{
//		if (!attachable)
//			throw new IllegalStateException("This offer became non-attachable!");
	}

	public void jdoPostAttach(Object arg0)
	{
	}

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private State state;

	/**
	 * This is the history of <b>public</b> {@link State}s with the newest last and the oldest first.
	 *
	 * @jdo.field
	 *		persistence-modifier="persistent"
	 *		collection-type="collection"
	 *		element-type="State"
	 *		table="JFireTrade_ReceptionNote_states"
	 *
	 * @jdo.join
	 */
	private List<State> states;

	/**
	 * This method is <b>not</b> intended to be called directly. It is called by
	 * {@link State#State(String, long, User, Statable, org.nightlabs.jfire.jbpm.graph.def.StateDefinition)}
	 * which is called automatically by {@link ActionHandlerNodeEnter}, if this <code>ActionHandler</code> is registered.
	 */
	public void setState(State currentState)
	{
		if (currentState == null)
			throw new IllegalArgumentException("state must not be null!");

		if (!currentState.getStateDefinition().isPublicState())
			throw new IllegalArgumentException("state.stateDefinition.publicState is false!");

		this.state = (State)currentState;
		this.states.add((State)currentState);
	}

	public State getState()
	{
		return state;
	}

	/**
	 * @jdo.field persistence-modifier="none"
	 */
	private transient List<State> _states = null;

	public List<State> getStates()
	{
		if (_states == null)
			_states = CollectionUtil.castList(Collections.unmodifiableList(states));

		return _states;
	}

	public StatableLocal getStatableLocal()
	{
		return receptionNoteLocal;
	}
}
