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

package org.nightlabs.jfire.geography;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;

import org.nightlabs.jfire.geography.id.RegionID;

/**
 * @author Marco Schulze - marco at nightlabs dot de
 *
 * @jdo.persistence-capable
 *		identity-type = "application"
 *		objectid-class = "org.nightlabs.jfire.geography.id.RegionID"
 *		detachable = "true"
 *		table = "JFireGeography_Region"
 *
 * @jdo.inheritance strategy = "new-table"
 *
 * @jdo.fetch-group name="Region.country" fields="country"
 * @jdo.fetch-group name="Region.name" fields="name"
 * @jdo.fetch-group name="Region.cities" fields="cities"
 */
public class Region implements Serializable
{
	public static final String FETCH_GROUP_COUNTRY = "Region.country";
	public static final String FETCH_GROUP_NAME = "Region.name";
	public static final String FETCH_GROUP_CITIES = "Region.cities";

	/////// begin primary key ///////
	/**
	 * @jdo.field primary-key="true"
	 * @jdo.column length="100"
	 */
	private String countryID;

	/**
	 * @jdo.field primary-key="true"
	 * @jdo.column length="100"
	 */
	private String organisationID;

	/**
	 * @jdo.field primary-key="true"
	 * @jdo.column length="100"
	 */
	private String regionID;
	/////// end primary key ///////
	
	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private String primaryKey;

	/////// begin normal fields ///////

	/**
	 * @jdo.field persistence-modifier="none"
	 */
	protected transient GeographySystem geographySystem;

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private Country country;

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private RegionName name;

	/**
	 * key: String cityPK<br/>
	 * value: City city
	 *
	 * @jdo.field
	 *		persistence-modifier="persistent"
	 *		collection-type="map"
	 *		key-type="java.lang.String"
	 *		value-type="City"
	 *		dependent="true"
	 *		mapped-by="region"
	 *
	 * @jdo.key mapped-by="primaryKey"
	 *
	 * @!jdo.map-vendor-extension vendor-name="jpox" key="key-field" value="primaryKey"
	 */
	protected Map cities = new HashMap();	
	/////// end normal fields ///////
	
	
	/////// begin constructors ///////

	/**
	 * @deprecated Only for JDO!
	 */
	protected Region() { }
	
	public Region(String organisationID, String regionID, Country country)
	{
		this(null, organisationID, regionID, country);
	}
	public Region(GeographySystem geographySystem, String organisationID, String regionID, Country country)
	{
		if (organisationID == null)
			throw new NullPointerException("organisationID");

		if (regionID == null)
			throw new NullPointerException("regionID");

		this.geographySystem = geographySystem;
		this.countryID = country.getCountryID();
		this.organisationID = organisationID;
		this.regionID = regionID;
		this.country = country;
		this.primaryKey = getPrimaryKey(countryID, organisationID, regionID);
		this.name = new RegionName(this);
	}

	/////// end constructors ///////
	
	
	/////// begin methods ///////
	/**
	 * @return Returns the countryID.
	 */
	public String getCountryID()
	{
		return countryID;
	}
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
		this.primaryKey = getPrimaryKey(countryID, organisationID, regionID);
	}
	/**
	 * @return Returns the regionID.
	 */
	public String getRegionID()
	{
		return regionID;
	}
	/**
	 * @param regionID The regionID to set.
	 */
	protected void setRegionID(String regionID)
	{
		this.regionID = regionID;
		this.primaryKey = getPrimaryKey(countryID, organisationID, regionID);
	}
	public static String getPrimaryKey(
			String countryID,
			String organisationID, String regionID)
	{
		return countryID + '/' + organisationID + '/' + regionID;
	}

	public static String getPrimaryKey(RegionID regionID)
	{
		return getPrimaryKey(regionID.countryID, regionID.organisationID, regionID.regionID);
	}

	public String getPrimaryKey()
	{
		return primaryKey;
	}
	
	/**
	 * @return Returns the country.
	 */
	public Country getCountry()
	{
		return country;
	}
	/**
	 * @return Returns the name.
	 */
	public RegionName getName()
	{
		return name;
	}
	public void addCity(City city)
	{
		if (city.getPrimaryKey() == null) {
			PersistenceManager pm = JDOHelper.getPersistenceManager(this);
			if (pm == null)			
				throw new IllegalStateException("city does not have a primary key and this instance of Region is not persistent! Cannot assign a primary key!");
			
			pm.makePersistent(city);
		}
		cities.put(city.getPrimaryKey(), city);
	}
	public Collection getCities()
	{
		if (geographySystem != null)
			geographySystem.needCities(countryID);

		return Collections.unmodifiableCollection(cities.values());
	}

	/**
	 * This method creates a copy of this Region. It exchanges the country by the
	 * <tt>persistentCountry</tt> and does not copy the cities.
	 *
	 * @param persistentCountry An instance of <tt>Country</tt> which either is
	 *		currently persistent or has been detached from detastore. The countryID
	 *		must match the currently assigned country.
	 * @return Returns a partial copy of this instance.
	 */
	public Region copyForJDOStorage(Country persistentCountry)
	{
		if (persistentCountry == null)
			throw new NullPointerException("persistentCountry");

		if (!persistentCountry.getCountryID().equals(country.getCountryID()))
			throw new IllegalArgumentException("persistentCountry.countryID != this.country.countryID!!!");

		if (JDOHelper.getObjectId(persistentCountry) == null)
			throw new IllegalArgumentException("persistentCountry is neither persistent nor detached! Could not obtain an object-id!");

		Region n = new Region(organisationID, regionID, persistentCountry);
		n.name.load(this.name);
		return n;
	}

	/////// end methods ///////
}
