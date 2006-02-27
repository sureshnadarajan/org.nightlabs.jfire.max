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
import java.util.Locale;
import java.util.Map;

import javax.jdo.JDOHelper;

import org.nightlabs.jfire.geography.id.CityID;

/**
 * @author Marco Schulze - marco at nightlabs dot de
 *
 * @jdo.persistence-capable
 *		identity-type = "application"
 *		objectid-class = "org.nightlabs.jfire.geography.id.CityID"
 *		detachable = "true"
 *		table = "JFireGeography_City"
 *
 * @jdo.inheritance strategy = "new-table"
 *
 * @jdo.create-objectid-class field-order="countryID, organisationID, cityID"
 *
 * @jdo.fetch-group name="City.name" fields="name"
 * @jdo.fetch-group name="City.locations" fields="locations"
 * @jdo.fetch-group name="City.region" fields="region"
 */
public class City implements Serializable
{
	public static final String FETCH_GROUP_NAME = "City.name";
	public static final String FETCH_GROUP_LOCATIONS = "City.locations";
	public static final String FETCH_GROUP_REGION = "City.region";

	/////// begin primary key ///////
	/**
	 * 2-char-iso-code
	 *
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
	 */
	private String cityID;
	/////// end primary key ///////

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private String primaryKey;

	public static final String DEFAULT_LANGUAGEID = Locale.ENGLISH.getLanguage();
	
	/////// begin normal fields ///////

	/**
	 * @jdo.field persistence-modifier="none"
	 */
	protected transient GeographySystem geographySystem;

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private Region region;

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private CityName name;

	/**
	 * key: String locationPK<br/>
	 * value: Location location
	 *
	 * @jdo.field
	 *		persistence-modifier="persistent"
	 *		collection-type="map"
	 *		key-type="java.lang.String"
	 *		value-type="Location"
	 *		dependent-value="true"
	 *		mapped-by="city"
	 *
	 * @jdo.key mapped-by="primaryKey"
	 *
	 * @!jdo.map-vendor-extension vendor-name="jpox" key="key-field" value="primaryKey"
	 */
	private Map locations;

	/**
	 * key: String districtPK<br/>
	 * value: District district
	 *
	 * @jdo.field
	 *		persistence-modifier="persistent"
	 *		collection-type="map"
	 *		key-type="java.lang.String"
	 *		value-type="District"
	 *		dependent-value="true"
	 *		mapped-by="city"
	 *
	 * @jdo.key mapped-by="primaryKey"
	 *
	 * @!jdo.map-vendor-extension vendor-name="jpox" key="key-field" value="primaryKey"
	 */
	private Map districts = new HashMap();
	/////// end normal fields ///////


	/////// begin constructors ///////
	/**
	 * @deprecated Only for JDO!
	 */
	protected City() { }

	public City(String organisationID, String cityID, Region region)
	{
		this(null, organisationID, cityID, region);
	}
	public City(GeographySystem geographySystem, String organisationID, String cityID, Region region)
	{
		if (organisationID == null)
			throw new NullPointerException("organisationID");

		if (cityID == null)
			throw new NullPointerException("cityID");

		this.geographySystem = geographySystem;
		this.countryID = region.getCountryID();
		this.organisationID = organisationID;
		this.cityID = cityID;
		this.region = region;
		this.primaryKey = getPrimaryKey(countryID, organisationID, cityID);
		this.name = new CityName(this);

		locations = new HashMap();
	}

	/////// end constructors ///////
	
	
	/////// begin methods ///////

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
	 * @return Returns the cityID.
	 */
	public String getCityID()
	{
		return cityID;
	}
	/**
	 * @return Returns the region.
	 */
	public Region getRegion()
	{
		return region;
	}
	
	public static String getPrimaryKey(String countryID, String organisationID, String cityID)
	{
		return countryID + '/' + organisationID + '/' + cityID;
	}
	public static String getPrimaryKey(CityID cityID)
	{
		return getPrimaryKey(cityID.countryID, cityID.organisationID, cityID.cityID);
	}

	public String getPrimaryKey()
	{
		return primaryKey;
	}

	/**
	 * @return Returns the name.
	 */
	public CityName getName()
	{
		return name;
	}

	public Collection getLocations()
	{
		if (geographySystem != null)
			geographySystem.needLocations(countryID);

		return Collections.unmodifiableCollection(locations.values());
	}

	public Collection getDistricts()
	{
		if (geographySystem != null)
			geographySystem.needDistricts(countryID);

		return Collections.unmodifiableCollection(districts.values());
	}

	public City copyForJDOStorage(Region persistentRegion)
	{
		if (persistentRegion == null)
			throw new NullPointerException("persistentRegion");

		if (!persistentRegion.getPrimaryKey().equals(region.getPrimaryKey()))
			throw new IllegalArgumentException("persistentRegion.primaryKey != region.primaryKey!!!");

		if (JDOHelper.getObjectId(persistentRegion) == null)
			throw new IllegalArgumentException("persistentRegion is neither persistent nor detached! Could not obtain an object-id!");

		City n = new City(organisationID, cityID, persistentRegion);
		n.name.load(this.name);
		// do NOT copy locations
		// do NOT copy districts
		return n;
	}

	public void addDistrict(District district)
	{
		if (!this.getPrimaryKey().equals(district.getCity().getPrimaryKey())) 
			throw new IllegalArgumentException("district has wrong city (!= this)!!!");

		districts.put(district.getPrimaryKey(), district);
	}

	public void addLocation(Location location)
	{
		if (!this.getPrimaryKey().equals(location.getCity().getPrimaryKey())) 
			throw new IllegalArgumentException("location has wrong city (!= this)!!!");

		locations.put(location.getPrimaryKey(), location);
	}
}
