/* *****************************************************************************
 * JFire - it's hot - Free ERP System - http://jfire.org                       *
 * Copyright (C) 2004-2005 NightLabs - http://NightLabs.org                    *
 *                                                                             *
 * This library is free software; you can redistribute it and/or               *
 * modify it under the terms of the GNU Lesser General Public                  *
 * License as published by the Free Software Foundation; either                *
 * version 2.1 of the License, or (at your option) any later version.          *
 *                                                                             *
 * This library is distributed in the hope that it will be useful,             *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of              *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU           *
 * Lesser General Public License for more details.                             *
 *                                                                             *
 * You should have received a copy of the GNU Lesser General Public            *
 * License along with this library; if not, write to the                       *
 *     Free Software Foundation, Inc.,                                         *
 *     51 Franklin St, Fifth Floor,                                            *
 *     Boston, MA  02110-1301  USA                                             *
 *                                                                             *
 * Or get it online :                                                          *
 *     http://opensource.org/licenses/lgpl-license.php                         *
 *                                                                             *
 *                                                                             *
 ******************************************************************************/

package org.nightlabs.jfire.accounting.pay;

import java.util.HashMap;
import java.util.Map;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.FetchGroup;
import javax.jdo.annotations.FetchGroups;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.PersistenceModifier;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.nightlabs.i18n.I18nText;
import org.nightlabs.jfire.accounting.pay.id.ModeOfPaymentFlavourNameID;

/**
 * @author Marco Schulze - marco at nightlabs dot de
 *
 * @jdo.persistence-capable
 *		identity-type="application"
 *		objectid-class="org.nightlabs.jfire.accounting.pay.id.ModeOfPaymentFlavourNameID"
 *		detachable="true"
 *		table="JFireTrade_ModeOfPaymentFlavourName"
 *
 * @jdo.inheritance strategy="new-table"
 *
 * @jdo.create-objectid-class field-order="organisationID, modeOfPaymentFlavourID"
 *
 * @jdo.fetch-group name="ModeOfPaymentFlavourName.names" fields="names"
 * @jdo.fetch-group name="ModeOfPaymentFlavourName.this" fetch-groups="default" fields="names"
 */
@PersistenceCapable(
	objectIdClass=ModeOfPaymentFlavourNameID.class,
	identityType=IdentityType.APPLICATION,
	detachable="true",
	table="JFireTrade_ModeOfPaymentFlavourName")
@FetchGroups({
	@FetchGroup(
		name=ModeOfPaymentFlavourName.FETCH_GROUP_NAMES,
		members=@Persistent(name="names")),
	@FetchGroup(
		fetchGroups={"default"},
		name=ModeOfPaymentFlavourName.FETCH_GROUP_THIS_MODE_OF_PAYMENT_FLAVOUR_NAME,
		members=@Persistent(name="names"))
})
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
public class ModeOfPaymentFlavourName extends I18nText
{
	private static final long serialVersionUID = 1L;
	public static final String FETCH_GROUP_NAMES = "ModeOfPaymentFlavourName.names";
	/**
	 * @deprecated The *.this-FetchGroups lead to bad programming style and are therefore deprecated, now. They should be removed soon!
	 */
	@Deprecated
	public static final String FETCH_GROUP_THIS_MODE_OF_PAYMENT_FLAVOUR_NAME = "ModeOfPaymentFlavourName.this";

	/**
	 * @jdo.field primary-key="true"
	 * @jdo.column length="100"
	 */
	@PrimaryKey
	@Column(length=100)
	private String organisationID;

	/**
	 * @jdo.field primary-key="true"
	 * @jdo.column length="100"
	 */
	@PrimaryKey
	@Column(length=100)
	private String modeOfPaymentFlavourID;

	/**
	 * key: String languageID<br/>
	 * value: String name
	 *
	 * @jdo.field
	 *		persistence-modifier="persistent"
	 *		collection-type="map"
	 *		key-type="java.lang.String"
	 *		value-type="java.lang.String"
	 *		default-fetch-group="true"
	 *		table="JFireTrade_ModeOfPaymentFlavourName_names"
	 *		null-value="exception"
	 *
	 * @jdo.join
	 */
	@Join
	@Persistent(
		nullValue=NullValue.EXCEPTION,
		table="JFireTrade_ModeOfPaymentFlavourName_names",
		defaultFetchGroup="true",
		persistenceModifier=PersistenceModifier.PERSISTENT)
	protected Map<String, String> names;

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	@Persistent(persistenceModifier=PersistenceModifier.PERSISTENT)
	private ModeOfPaymentFlavour modeOfPaymentFlavour;

	/**
	 * @deprecated Only for JDO!
	 */
	@Deprecated
	protected ModeOfPaymentFlavourName() { }

	public ModeOfPaymentFlavourName(ModeOfPaymentFlavour modeOfPaymentFlavour)
	{
		this.modeOfPaymentFlavour = modeOfPaymentFlavour;
		this.organisationID = modeOfPaymentFlavour.getOrganisationID();
		this.modeOfPaymentFlavourID = modeOfPaymentFlavour.getModeOfPaymentFlavourID();
		names = new HashMap<String, String>();
	}

	public ModeOfPaymentFlavour getModeOfPaymentFlavour()
	{
		return modeOfPaymentFlavour;
	}

	@Override
	protected Map<String, String> getI18nMap()
	{
		return names;
	}

	/**
	 * @see org.nightlabs.i18n.I18nText#getFallBackValue(java.lang.String)
	 */
	@Override
	protected String getFallBackValue(String languageID)
	{
		return ModeOfPaymentFlavour.getPrimaryKey(organisationID, modeOfPaymentFlavourID);
	}

}
