/*
 * Created on Apr 20, 2005
 */
package org.nightlabs.ipanema.trade;

import java.util.HashMap;
import java.util.Map;

import org.nightlabs.i18n.I18nText;

/**
 * @author Marco Schulze - marco at nightlabs dot de
 *
 * @jdo.persistence-capable
 *		identity-type="application"
 *		objectid-class="org.nightlabs.ipanema.trade.id.CustomerGroupNameID"
 *		detachable="true"
 *		table="JFireTrade_CustomerGroupName"
 *
 * @jdo.inheritance strategy="new-table"
 *
 * @jdo.create-objectid-class field-order="organisationID, customerGroupID"
 */
public class CustomerGroupName extends I18nText
{
	/**
	 * @jdo.field primary-key="true"
	 * @jdo.column length="100"
	 */
	private String organisationID;

	/**
	 * @jdo.field primary-key="true"
	 * @jdo.column length="100"
	 */
	private String customerGroupID;

	/**
	 * key: String languageID<br/>
	 * value: String name
	 * 
	 * @jdo.field
	 *		persistence-modifier="persistent"
	 *		collection-type="map"
	 *		key-type="java.lang.String"
	 *		value-type="java.lang.String"
	 *		dependent="true"
	 *		default-fetch-group="true"
	 *		table="JFireTrade_CustomerGroupName_names"
	 *
	 * @jdo.join
	 */
	protected Map names = new HashMap();

	/**
	 * This variable contains the name in a certain language after localization.
	 *
	 * @see #localize(String)
	 * @see #detachCopyLocalized(String, javax.jdo.PersistenceManager)
	 *
	 * @jdo.field persistence-modifier="transactional" default-fetch-group="false"
	 */
	protected String name;

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private CustomerGroup customerGroup;

	/**
	 * @deprecated Only for JDO!
	 */
	protected CustomerGroupName()
	{
	}

	public CustomerGroupName(CustomerGroup customerGroup)
	{
		this.organisationID = customerGroup.getOrganisationID();
		this.customerGroupID = customerGroup.getCustomerGroupID();
		this.customerGroup = customerGroup;
	}

	/**
	 * @see org.nightlabs.i18n.I18nText#getI18nMap()
	 */
	protected Map getI18nMap()
	{
		return names;
	}

	/**
	 * @see org.nightlabs.i18n.I18nText#setText(java.lang.String)
	 */
	protected void setText(String localizedValue)
	{
		this.name = localizedValue;
	}

	/**
	 * @see org.nightlabs.i18n.I18nText#getText()
	 */
	public String getText()
	{
		return name;
	}

	/**
	 * @see org.nightlabs.i18n.I18nText#getFallBackValue(java.lang.String)
	 */
	protected String getFallBackValue(String languageID)
	{
		return CustomerGroup.getPrimaryKey(organisationID, customerGroupID);
	}

	public String getOrganisationID()
	{
		return organisationID;
	}
	public String getCustomerGroupID()
	{
		return customerGroupID;
	}

	public CustomerGroup getCustomerGroup()
	{
		return customerGroup;
	}

}
