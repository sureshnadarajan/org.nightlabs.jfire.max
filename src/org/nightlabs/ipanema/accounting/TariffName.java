/*
 * Created on Jan 5, 2005
 */
package org.nightlabs.ipanema.accounting;

import java.util.HashMap;
import java.util.Map;

import javax.jdo.listener.StoreCallback;

import org.nightlabs.i18n.I18nText;

/**
 * @author Marco Schulze - marco at nightlabs dot de
 *
 * @jdo.persistence-capable 
 *		identity-type="application"
 *		objectid-class="org.nightlabs.ipanema.accounting.id.TariffNameID"
 *		detachable="true"
 *		table="JFireTrade_TariffName"
 *
 * @jdo.inheritance strategy = "new-table"
 * 
 * @jdo.create-objectid-class
 *		field-order="organisationID, tariffID"
 *
 * @jdo.fetch-group name="Tariff.name" fields="tariff, names"
 * @jdo.fetch-group name="PriceCell.this" fetch-groups="default" fields="price, priceConfig, priceCoordinate"
 *
 * @jdo.fetch-group name="FetchGroupsTrade.articleInOrderEditor" fields="tariff, names"
 * @jdo.fetch-group name="FetchGroupsTrade.articleInOfferEditor" fields="tariff, names"
 * @jdo.fetch-group name="FetchGroupsTrade.articleInInvoiceEditor" fields="tariff, names"
 * @jdo.fetch-group name="FetchGroupsTrade.articleInDeliveryNoteEditor" fields="tariff, names"
 *
 * @!
 * TODO What is that fetch-group "PriceCell.this" doing here? Marco ;-) Well, not mine, maybe just delete it. Alex
 */
public class TariffName extends I18nText
implements StoreCallback
{
	/**
	 * @jdo.field primary-key="true"
	 * @jdo.column length="100"
	 */
	private String organisationID;

	/**
	 * @jdo.field primary-key="true"
	 */
	private long tariffID = -1;

	public TariffName()
	{
	}

	public TariffName(Tariff tariff)
	{
		this.tariff = tariff;
		this.organisationID = tariff.getOrganisationID();
		this.tariffID = tariff.getTariffID();
	}

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private Tariff tariff;

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
	 *		table="JFireTrade_TariffName_names"
	 *
	 * @jdo.join
	 */
	protected Map names = new HashMap();

	/**
	 * This variable contains the name in a certain language after localization.
	 *
	 * @see I18nText#localize(java.lang.String)
	 * @see org.nightlabs.jdo.LocalizedDetachable#detachCopyLocalized(java.lang.String, javax.jdo.PersistenceManager)
	 *
	 * @jdo.field persistence-modifier="transactional" default-fetch-group="false"
	 */
	protected String name;

	/**
	 * @return Returns the organisationID.
	 */
	public String getOrganisationID()
	{
		return organisationID;
	}
	/**
	 * @param organisationID The organisationID to set.
	 */
	protected void setOrganisationID(String organisationID)
	{
		this.organisationID = organisationID;
	}
	/**
	 * @return Returns the tariff.
	 */
	public Tariff getTariff()
	{
		return tariff;
	}
	/**
	 * @return Returns the tariffID.
	 */
	public long getTariffID()
	{
		return tariffID;
	}
	/**
	 * @param tariffID The tariffID to set.
	 */
	protected void setTariffID(long tariffID)
	{
		this.tariffID = tariffID;
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
	public void setText(String localizedValue)
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
		return Tariff.getPrimaryKey(organisationID, tariffID);
	}

	/**
	 * @see javax.jdo.listener.StoreCallback#jdoPreStore()
	 */
	public void jdoPreStore()
	{
		if (tariffID < 0)
			throw new IllegalStateException("tariffID < 0!!!");
	}

}
