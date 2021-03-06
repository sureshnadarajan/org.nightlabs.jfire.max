package org.nightlabs.jfire.store;

import org.nightlabs.jfire.editlock.EditLockType;
import org.nightlabs.jfire.editlock.id.EditLockTypeID;
import org.nightlabs.jfire.organisation.Organisation;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.Inheritance;

/**
 * @author Marco Schulze - Marco at NightLabs dot de
 *
 * @jdo.persistence-capable
 *		identity-type="application"
 *		detachable="true"
 *		persistence-capable-superclass="org.nightlabs.jfire.editlock.EditLockType"
 *
 * @jdo.inheritance strategy="superclass-table"
 */
@PersistenceCapable(
	identityType=IdentityType.APPLICATION,
	detachable="true")
@Inheritance(strategy=InheritanceStrategy.SUPERCLASS_TABLE)
public class EditLockTypeDeliveryNote
extends EditLockType
{
	private static final long serialVersionUID = 1L;

	public static final EditLockTypeID EDIT_LOCK_TYPE_ID = EditLockTypeID.create(Organisation.DEV_ORGANISATION_ID, EditLockTypeDeliveryNote.class.getName());

	/**
	 * @deprecated Only for JDO!
	 */
	@Deprecated
	protected EditLockTypeDeliveryNote() { }

	public EditLockTypeDeliveryNote(EditLockTypeID editLockTypeID)
	{
		super(editLockTypeID);
	}

	public EditLockTypeDeliveryNote(String organisationID, String editLockTypeID)
	{
		super(organisationID, editLockTypeID);
	}
}
