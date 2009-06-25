package org.nightlabs.jfire.personrelation;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.FetchGroup;
import javax.jdo.annotations.FetchGroups;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.PersistenceModifier;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Queries;
import javax.jdo.listener.AttachCallback;
import javax.jdo.listener.DeleteCallback;
import javax.jdo.listener.DeleteLifecycleListener;
import javax.jdo.listener.DetachCallback;
import javax.jdo.listener.InstanceLifecycleEvent;
import javax.jdo.listener.StoreCallback;
import javax.jdo.listener.StoreLifecycleListener;

import org.nightlabs.jdo.ObjectIDUtil;
import org.nightlabs.jfire.idgenerator.IDGenerator;
import org.nightlabs.jfire.organisation.Organisation;
import org.nightlabs.jfire.person.Person;
import org.nightlabs.jfire.personrelation.id.PersonRelationID;
import org.nightlabs.jfire.prop.id.PropertySetID;
import org.nightlabs.util.Util;

@PersistenceCapable(
		objectIdClass=PersonRelationID.class,
		identityType=IdentityType.APPLICATION,
		detachable="true",
		table="JFirePersonRelation_PersonRelation"
)
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.CLASS_NAME)
@Queries({
	@javax.jdo.annotations.Query(
			name="getPersonRelationsFromTo",
			value="SELECT WHERE this.from == :from && this.to == :to"
	),
	@javax.jdo.annotations.Query(
			name="getPersonRelationsFromToWithType",
			value="SELECT WHERE this.personRelationType == :personRelationType && this.from == :from && this.to == :to"
	),
	@javax.jdo.annotations.Query(
			name="getPersonRelationsFrom",
			value="SELECT WHERE this.from == :from"
	),
	@javax.jdo.annotations.Query(
			name="getPersonRelationsTo",
			value="SELECT WHERE this.to == :to"
	),
})
@FetchGroups({
	@FetchGroup(
			name=PersonRelation.FETCH_GROUP_PERSON_RELATION_TYPE,
			members=@Persistent(name=PersonRelation.FieldName.personRelationType)
	),
	@FetchGroup(
			name=PersonRelation.FETCH_GROUP_FROM,
			members=@Persistent(name=PersonRelation.FieldName.from)
	),
	@FetchGroup(
			name=PersonRelation.FETCH_GROUP_TO,
			members=@Persistent(name=PersonRelation.FieldName.to)
	),
})
public class PersonRelation
implements Serializable, AttachCallback, DetachCallback, StoreCallback, DeleteCallback
{
	private static final long serialVersionUID = 1L;

	public static Collection<? extends PersonRelation> getPersonRelations(PersistenceManager pm, PersonRelationType personRelationType, Person fromPerson, Person toPerson)
	{
		if (personRelationType == null)
			return getPersonRelations(pm, fromPerson, toPerson);

		Query q = pm.newNamedQuery(PersonRelation.class, "getPersonRelationsFromToWithType");
		@SuppressWarnings("unchecked")
		Collection<? extends PersonRelation> c = (Collection<? extends PersonRelation>) q.execute(personRelationType, fromPerson, toPerson);
		return c;
	}

	public static Collection<? extends PersonRelation> getPersonRelations(PersistenceManager pm, Person fromPerson, Person toPerson)
	{
		if (fromPerson == null)
			throw new IllegalArgumentException("fromPerson must not be null!");

		if (toPerson == null)
			throw new IllegalArgumentException("toPerson must not be null!");

		Query q = pm.newNamedQuery(PersonRelation.class, "getPersonRelationsFromTo");
		@SuppressWarnings("unchecked")
		Collection<? extends PersonRelation> c = (Collection<? extends PersonRelation>) q.execute(fromPerson, toPerson);
		return c;
	}

	public static Collection<? extends PersonRelation> getPersonRelationsFrom(PersistenceManager pm, Person fromPerson)
	{
		if (fromPerson == null)
			throw new IllegalArgumentException("fromPerson must not be null!");

		Query q = pm.newNamedQuery(PersonRelation.class, "getPersonRelationsFrom");
		@SuppressWarnings("unchecked")
		Collection<? extends PersonRelation> c = (Collection<? extends PersonRelation>) q.execute(fromPerson);
		return c;
	}

	public static Collection<? extends PersonRelation> getPersonRelationsTo(PersistenceManager pm, Person toPerson)
	{
		if (toPerson == null)
			throw new IllegalArgumentException("toPerson must not be null!");

		Query q = pm.newNamedQuery(PersonRelation.class, "getPersonRelationsTo");
		@SuppressWarnings("unchecked")
		Collection<? extends PersonRelation> c = (Collection<? extends PersonRelation>) q.execute(toPerson);
		return c;
	}

	public static final String FETCH_GROUP_PERSON_RELATION_TYPE = "PersonRelation.personRelationType";
	public static final String FETCH_GROUP_FROM = "PersonRelation.from";
	public static final String FETCH_GROUP_TO = "PersonRelation.to";
	public static final String FETCH_GROUP_FROM_ID = "PersonRelation.fromID";
	public static final String FETCH_GROUP_TO_ID = "PersonRelation.toID";

	public static final class FieldName {
		public static final String personRelationType = "personRelationType";
		public static final String from = "from";
		public static final String fromID = "fromID";
		public static final String to = "to";
		public static final String toID = "toID";
	}

	@PrimaryKey
	@Column(length=100)
	private String organisationID;

	@PrimaryKey
	private long personRelationID;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private PersonRelationType personRelationType;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private Person from;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private Person to;

	@Persistent(persistenceModifier=PersistenceModifier.NONE)
	private PropertySetID fromID;

	@Persistent(persistenceModifier=PersistenceModifier.NONE)
	private PropertySetID toID;

	/**
	 * @deprecated Only for JDO!
	 */
	@Deprecated
	protected PersonRelation() { }

	protected PersonRelation(String organisationID, long personRelationID, PersonRelationType personRelationType, Person from, Person to)
	{
		Organisation.assertValidOrganisationID(organisationID);
		if (personRelationID < 0)
			throw new IllegalArgumentException("personRelationID < 0");

		if (personRelationType == null)
			throw new IllegalArgumentException("personRelationType must not be null!");

		if (from == null)
			throw new IllegalArgumentException("from must not be null!");

		if (to == null)
			throw new IllegalArgumentException("to must not be null!");

		this.organisationID = organisationID;
		this.personRelationID = personRelationID;
		this.from = from;
		this.to = to;
	}

	protected PersonRelation(PersonRelationType personRelationType, Person from, Person to)
	{
		this(
				IDGenerator.getOrganisationID(),
				IDGenerator.nextID(PersonRelation.class),
				personRelationType,
				from,
				to
		);
	}

	public String getOrganisationID() {
		return organisationID;
	}
	public long getPersonRelationID() {
		return personRelationID;
	}

	public PersonRelationType getPersonRelationType() {
		return personRelationType;
	}

	public Person getFrom() {
		return from;
	}
	public Person getTo() {
		return to;
	}

	public PropertySetID getFromID() {
		if (fromID == null)
			fromID = (PropertySetID) JDOHelper.getObjectId(from);

		return fromID;
	}
	public PropertySetID getToID() {
		if (toID == null)
			toID = (PropertySetID) JDOHelper.getObjectId(to);

		return toID;
	}

	@Override
	public void jdoPostDetach(Object o) {
		PersonRelation detached = this;
		PersonRelation attached = (PersonRelation) o;

		PersistenceManager pm = JDOHelper.getPersistenceManager(attached);
		Set<?> fetchGroups = pm.getFetchPlan().getGroups();
		if (fetchGroups.contains(FETCH_GROUP_FROM_ID)) {
			detached.fromID = attached.getFromID();
		}
		if (fetchGroups.contains(FETCH_GROUP_TO_ID)) {
			detached.toID = attached.getToID();
		}
	}

	@Override
	public void jdoPreDetach() { }

	@Override
	public void jdoPostAttach(Object o) { }

	@Override
	public void jdoPreAttach() { }

	@Override
	public void jdoPreStore() {
		final PersistenceManager pm = JDOHelper.getPersistenceManager(this);

		// Prevent duplicate relations. A relation is considered duplicate, if it
		// is between the same two persons *AND* in the same direction *AND* of the
		// same type (PersonRelationType).
		Collection<? extends PersonRelation> reverseRelations = PersonRelation.getPersonRelations(
				pm,
				getPersonRelationType(),
				getFrom(),
				getTo()
		);
		for (PersonRelation r : reverseRelations) {
			if (JDOHelper.isDeleted(r))
				continue;

			throw new DuplicatePersonRelationException(r);
		}

		pm.addInstanceLifecycleListener(
				new StoreLifecycleListener() {
					@Override
					public void preStore(InstanceLifecycleEvent event) { }

					@Override
					public void postStore(InstanceLifecycleEvent event) {
						PersonRelation personRelation = (PersonRelation) event.getPersistentInstance();
						if (!PersonRelation.this.equals(personRelation))
							return;

						pm.removeInstanceLifecycleListener(this);
						getPersonRelationType().postPersonRelationCreated(personRelation);
					}
				},
				PersonRelation.class
		);
	}

	@Override
	public void jdoPreDelete() {
		getPersonRelationType(); // it's not possible to read data in postDelete() - forcing fields to be read here.
		getFrom();
		getTo();
		getFromID();
		getToID();

		final PersistenceManager pm = JDOHelper.getPersistenceManager(this);
		pm.addInstanceLifecycleListener(
				new DeleteLifecycleListener() {
					@Override
					public void preDelete(InstanceLifecycleEvent event) {
					}

					@Override
					public void postDelete(InstanceLifecycleEvent event) {
						PersonRelation personRelation = (PersonRelation) event.getPersistentInstance();
						if (!PersonRelation.this.equals(personRelation))
							return;

						pm.removeInstanceLifecycleListener(this);
						getPersonRelationType().postPersonRelationDeleted(personRelation);
					}
				},
				PersonRelation.class
		);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((organisationID == null) ? 0 : organisationID.hashCode());
		result = prime * result + (int) (personRelationID ^ (personRelationID >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;

		PersonRelation other = (PersonRelation) obj;
		return (
				Util.equals(this.personRelationID, other.personRelationID) &&
				Util.equals(this.organisationID, other.organisationID)
		);
	}

	@Override
	public String toString() {
		return getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this)) + '[' + organisationID + ',' + ObjectIDUtil.longObjectIDFieldToString(personRelationID) + ']';
	}
}
