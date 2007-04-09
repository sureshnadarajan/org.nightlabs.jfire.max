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

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.naming.InitialContext;

import org.apache.log4j.Logger;
import org.nightlabs.jfire.geography.id.CityID;
import org.nightlabs.jfire.geography.id.CountryID;
import org.nightlabs.jfire.geography.id.LocationID;
import org.nightlabs.jfire.geography.id.RegionID;
import org.nightlabs.jfire.idgenerator.IDGenerator;
import org.nightlabs.jfire.organisation.Organisation;
import org.nightlabs.jfire.security.SecurityReflector;
import org.nightlabs.util.FulltextMap;

/**
 * @author Marco Schulze - marco at nightlabs dot de
 */
public abstract class Geography
{
	/**
	 * LOG4J logger used by this class
	 */
	private static final Logger logger = Logger.getLogger(Geography.class);

	private static Map<String, Geography> organisationID2sharedInstance = new HashMap<String, Geography>();

	public static final String PROPERTY_KEY_GEOGRAPHY_CLASS = "org.nightlabs.jfire.geography.Geography";

	/**
	 * This method finds out the current organisation by consulting the {@link SecurityReflector}
	 * and looks up the shared instance for this organisation. If there is none existing, a new
	 * instance will be created based on the system property with the key {@link #PROPERTY_KEY_GEOGRAPHY_CLASS}.
	 *
	 * @return The shared instance of Geography.
	 */
	public static Geography sharedInstance()
	{
		String organisationID = SecurityReflector.getUserDescriptor().getOrganisationID();

		Geography sharedInstance = null;
		createLocalVMSharedInstance:
			synchronized (organisationID2sharedInstance) {
				sharedInstance = organisationID2sharedInstance.get(organisationID);
				if (sharedInstance == null) {
					String className = System.getProperty(PROPERTY_KEY_GEOGRAPHY_CLASS);
					if (className == null)
						break createLocalVMSharedInstance;

					Class clazz;
					try {
						clazz = Class.forName(className);
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}

					try {
						sharedInstance = (Geography) clazz.newInstance();
					} catch (InstantiationException e) {
						throw new RuntimeException(e);
					} catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					}
					sharedInstance.setOrganisationID(organisationID);
					organisationID2sharedInstance.put(organisationID, sharedInstance);
				}
			} // synchronized (organisationID2sharedInstance) {

		if (sharedInstance == null)
			throw new IllegalStateException("Neither does a shared instance of Geography exist, nor is the property '" + PROPERTY_KEY_GEOGRAPHY_CLASS + "' set!");
	
		return sharedInstance;
	}

	public Geography()
	{
		InitialContext initialContext = SecurityReflector.createInitialContext();
		try {
			rootOrganisationID = Organisation.getRootOrganisationID(initialContext);
		} finally {
			try {
				initialContext.close();
			} catch (Exception e) {
				logger.error("Closing InitialContext failed!", e);
			}
		}
	}

	private String organisationID;

	public String getOrganisationID()
	{
		return organisationID;
	}

	private String rootOrganisationID;

	public String getRootOrganisationID()
	{
		return rootOrganisationID;
	}

	/**
	 * This method is called by {@link #sharedInstance()}, after a new instance of <code>Geography</code>
	 * was created and by {@link #setSharedInstance(Geography)}, if {@link #getOrganisationID()} returns <code>null</code>.
	 * <p>
	 * If this method is called, after an organisationID has already been set, an exception is thrown.
	 * </p>
	 *
	 * @param organisationID The organisationID to set.
	 */
	protected void setOrganisationID(String organisationID)
	{
		if (this.organisationID != null && !this.organisationID.equals(organisationID))
			throw new IllegalStateException("This Geography's organisationID is already initialized to the value \""+this.organisationID+"\" - cannot change it to \""+organisationID+"\"!!!");

		this.organisationID = organisationID;
	}

	protected static void setSharedInstance(Geography sharedInstance)
	{
		String organisationID = sharedInstance.getOrganisationID();
		if (organisationID == null)
			organisationID = SecurityReflector.getUserDescriptor().getOrganisationID();

		sharedInstance.setOrganisationID(organisationID);
		synchronized (organisationID2sharedInstance) {
			organisationID2sharedInstance.put(organisationID, sharedInstance);
		}
	}

	public static final int FIND_MODE_BEGINS_WITH = FulltextMap.FIND_MODE_BEGINS_WITH;
	public static final int FIND_MODE_CONTAINS = FulltextMap.FIND_MODE_CONTAINS;
	public static final int FIND_MODE_ENDS_WITH = FulltextMap.FIND_MODE_ENDS_WITH;

	/**
	 * key: String countryID <br/>
	 * value: Country country
	 */
	protected Map<String, Country> countries = new HashMap<String, Country>();

	/**
	 * key: String regionPK <br/>
	 * value: Region region
	 */
	protected Map<String, Region> regions = new HashMap<String, Region>();

	/**
	 * key: String cityPK <br/>
	 * value: City city
	 */
	protected Map<String, City> cities = new HashMap<String, City>();

	/**
	 * key: String districtPK <br/>
	 * value: District district
	 */
	protected Map<String, District> districts = new HashMap<String, District>();

	/**
	 * key: String locationPK<br/>
	 * value: Location location
	 */
	protected Map<String, Location> locations = new HashMap<String, Location>();


	public Collection<Country> getCountries()
	{
		needCountries();

		return Collections.unmodifiableCollection(countries.values());
	}

	/**
	 * key: String languageID (this is important for sorting)<br/>
	 * value: List countries
	 */
	private transient Map<String, List<Country>> countriesSortedByLanguageID = null;

	public List<Country> getCountriesSorted(final Locale locale)
	{
		if (countriesSortedByLanguageID == null)
			countriesSortedByLanguageID = new HashMap<String, List<Country>>();

		final String languageID = locale.getLanguage();
		List<Country> countriesSorted = countriesSortedByLanguageID.get(languageID);

		if (countriesSorted == null) {
			countriesSorted = new ArrayList<Country>(getCountries());
			Collections.sort(countriesSorted, getCountryComparator(locale));

			countriesSortedByLanguageID.put(languageID, countriesSorted);
		}

		return countriesSorted;
	}
	
	/**
	 * key: String countryID<br/>
	 * value: Map {<br/>
	 *		key: String languageID<br/>
	 *		value: List locations<br/>
	 * }
	 */
	private transient Map<String, Map<String, List<Location>>> locationsSortedByLanguageIDByCountryID = null;

	/**
	 * key: String countryID<br/>
	 * value: Map {<br/>
	 *		key: String languageID<br/>
	 *		value: List regions<br/>
	 * }
	 */
	private transient Map<String, Map<String, List<Region>>> regionsSortedByLanguageIDByCountryID = null;

	/**
	 * key: RegionID regionID<br/>
	 * value: Map {<br/>
	 *		key: String languageID<br/>
	 *		value: List cities<br/>
	 * }
	 */
	private transient Map<RegionID, Map<String, List<City>>> citiesSortedByLanguageIDByRegionID = null;

	public Country getCountry(CountryID countryID, boolean throwExceptionIfNotFound)
	{
		needCountries();

		Country res = (Country) countries.get(countryID.countryID);

		if (res == null && throwExceptionIfNotFound)
			throw new IllegalArgumentException("No Country registered with countryID=\""+countryID+"\"!!!");

		return res;
	}

	public Region getRegion(RegionID regionID, boolean throwExceptionIfNotFound)
	{
		needRegions(regionID.countryID);

		Region res = (Region) regions.get(Region.getPrimaryKey(regionID));

		if (res == null && throwExceptionIfNotFound)
			throw new IllegalArgumentException("No Region registered with regionID=\""+regionID+"\"!!!");

		return res;
	}

	public City getCity(CityID cityID, boolean throwExceptionIfNotFound)
	{
		needCities(cityID.countryID);

		City res = (City) cities.get(City.getPrimaryKey(cityID));

		if (res == null && throwExceptionIfNotFound)
			throw new IllegalArgumentException("No City registered with cityID=\""+cityID+"\"!!!");

		return res;
	}

	protected static final Collection EMPTY_COLLECTION = Collections.unmodifiableCollection(new LinkedList());

	/**
	 * @param countryID
	 * @param throwExceptionIfNotFound
	 * @return Returns an empty collection if <tt>throwExceptionIfNotFound == false</tt> and
	 *		no <tt>Country</tt> exists for the given <tt>countryID</tt>.
	 */
	public Collection<Region> getRegions(CountryID countryID, boolean throwExceptionIfNotFound)
	{
		Country country = getCountry(countryID, throwExceptionIfNotFound);
		if (country == null)
			return EMPTY_COLLECTION;

		return Collections.unmodifiableCollection(country.getRegions());
	}

	public List<Region> getRegionsSorted(final CountryID countryID, final Locale locale)
	{
		if (regionsSortedByLanguageIDByCountryID == null)
			regionsSortedByLanguageIDByCountryID = new HashMap<String, Map<String,List<Region>>>();

		Map<String,List<Region>> regionsSortedByLanguageID = regionsSortedByLanguageIDByCountryID.get(countryID.countryID);
		if (regionsSortedByLanguageID == null) {
			regionsSortedByLanguageID = new HashMap<String, List<Region>>();
			regionsSortedByLanguageIDByCountryID.put(countryID.countryID, regionsSortedByLanguageID);
		}

		final String languageID = locale.getLanguage();

		List<Region> regionsSorted = regionsSortedByLanguageID.get(languageID);

		if (regionsSorted == null) {
			regionsSorted = new ArrayList<Region>(getRegions(countryID, false));
			Collections.sort(regionsSorted, getRegionComparator(locale));

			regionsSortedByLanguageID.put(languageID, regionsSorted);
		}

		return regionsSorted;
	}

	public Location getLocation(LocationID locationID, boolean throwExceptionIfNotFound)
	{
		needLocations(locationID.countryID);

		Location res = (Location) locations.get(Location.getPrimaryKey(locationID));

		if (res == null && throwExceptionIfNotFound)
			throw new IllegalArgumentException("No Location registered with locationID=\""+locationID+"\"!!!");

		return res;
	}

	/**
	 * @param countryID
	 * @param throwExceptionIfNotFound
	 * @return Returns an empty collection if <tt>throwExceptionIfNotFound == false</tt> and
	 *		no <tt>Country</tt> exists for the given <tt>countryID</tt>.
	 */
	public Collection<Location> getLocations(CityID cityID, boolean throwExceptionIfNotFound)
	{
		City city = getCity(cityID, throwExceptionIfNotFound);
		if (city == null)
			return EMPTY_COLLECTION;

		return Collections.unmodifiableCollection(city.getLocations());
	}

	public List<Location> getLocationsSorted(final CityID cityID, final Locale locale)
	{
		if (locationsSortedByLanguageIDByCountryID == null)
			locationsSortedByLanguageIDByCountryID = new HashMap<String, Map<String,List<Location>>>();

		Map<String,List<Location>> locationsSortedByLanguageID = locationsSortedByLanguageIDByCountryID.get(cityID.countryID);
		if (locationsSortedByLanguageID == null) {
			locationsSortedByLanguageID = new HashMap<String, List<Location>>();
			locationsSortedByLanguageIDByCountryID.put(cityID.countryID, locationsSortedByLanguageID);
		}

		final String languageID = locale.getLanguage();

		List<Location> locationsSorted = locationsSortedByLanguageID.get(languageID);

		if (locationsSorted == null) {
			locationsSorted = new ArrayList<Location>(getLocations(cityID, false));
			Collections.sort(locationsSorted, getLocationComparator(locale));

			locationsSortedByLanguageID.put(languageID, locationsSorted);
		}

		return locationsSorted;
	}

	/**
	 * @param regionID
	 * @param throwExceptionIfNotFound
	 * @return Returns an empty collection if <tt>throwExceptionIfNotFound == false</tt> and
	 *		no <tt>Country</tt> exists for the given <tt>countryID</tt>.
	 */
	public Collection<City> getCities(RegionID regionID, boolean throwExceptionIfNotFound)
	{
		needCities(regionID.countryID);

		Region region = getRegion(regionID, throwExceptionIfNotFound);
		if (region == null)
			return EMPTY_COLLECTION;

		return Collections.unmodifiableCollection(region.getCities());
	}

	protected Comparator<Country> getCountryComparator(final Locale locale)
	{
		return new Comparator<Country>() {
			private Collator collator = Collator.getInstance(locale);
			private String languageID = locale.getLanguage();

			public int compare(Country c0, Country c1)
			{
				String n0 = c0.getName().getText(languageID);
				String n1 = c1.getName().getText(languageID);

				return collator.compare(n0, n1);
			}
		};
	}

	protected Comparator<Region> getRegionComparator(final Locale locale)
	{
		return new Comparator<Region>() {
			private Collator collator = Collator.getInstance(locale);
			private String languageID = locale.getLanguage();

			public int compare(Region r0, Region r1)
			{
				String n0 = r0.getName().getText(languageID);
				String n1 = r1.getName().getText(languageID);

				return collator.compare(n0, n1);
			}
		};
	}

	protected Comparator<City> getCityComparator(final Locale locale)
	{
		return new Comparator<City>() {
			private Collator collator = Collator.getInstance(locale);
			private String languageID = locale.getLanguage();

			public int compare(City c0, City c1)
			{
				String n0 = c0.getName().getText(languageID);
				String n1 = c1.getName().getText(languageID);

				return collator.compare(n0, n1);
			}
		};
	}

	protected Comparator<Location> getLocationComparator(final Locale locale)
	{
		return new Comparator<Location>() {
			private Collator collator = Collator.getInstance(locale);
			private String languageID = locale.getLanguage();

			public int compare(Location l0, Location l1)
			{
				String n0 = l0.getName().getText(languageID);
				String n1 = l1.getName().getText(languageID);

				return collator.compare(n0, n1);
			}
		};
	}

	public List getCitiesSorted(final RegionID regionID, Locale locale)
	{
		if (citiesSortedByLanguageIDByRegionID == null)
			citiesSortedByLanguageIDByRegionID = new HashMap<RegionID, Map<String, List<City>>>();

		Map<String, List<City>> citiesSortedByLanguageID = citiesSortedByLanguageIDByRegionID.get(regionID);
		if (citiesSortedByLanguageID == null) {
			citiesSortedByLanguageID = new HashMap<String, List<City>>();
			citiesSortedByLanguageIDByRegionID.put(regionID, citiesSortedByLanguageID);
		}

		String languageID = locale.getLanguage();

		List<City> citiesSorted = citiesSortedByLanguageID.get(languageID);

		if (citiesSorted == null) {
			citiesSorted = new ArrayList<City>(getCities(regionID, false));
			Collections.sort(citiesSorted, getCityComparator(locale));

			citiesSortedByLanguageID.put(languageID, citiesSorted);
		}

		return citiesSorted;
	}

	/**
	 * key: String languageID<br/>
	 * value: FulltextMap countriesByCountryName {<br/>
	 *		key: String countryName
	 *		value: List countries (instances of {@link Country}
	 * }
	 */
	protected transient Map<String, FulltextMap> countriesByCountryNameByLanguageID = null;
	protected FulltextMap getCountriesByCountryNameMap(String languageID)
	{
		logger.debug("getCountriesByCountryNameMap(languageID=\""+languageID+"\") entered.");

		if (countriesByCountryNameByLanguageID == null)
			countriesByCountryNameByLanguageID = new HashMap<String, FulltextMap>();

		FulltextMap countriesByCountryName = countriesByCountryNameByLanguageID.get(languageID);
		if (countriesByCountryName == null) {
			countriesByCountryName = new FulltextMap(FULLTEXT_MAP_FEATURES);
			countriesByCountryNameByLanguageID.put(languageID, countriesByCountryName);
			for (Iterator it = getCountries().iterator(); it.hasNext(); ) {
				Country country = (Country) it.next();
				String countryName = country.getName().getText(languageID);
				List<Country> countryList = (List) countriesByCountryName.get(countryName);
				if (countryList == null) {
					countryList = new LinkedList<Country>();
					countriesByCountryName.put(countryName, countryList);
				}
				countryList.add(country);
			}
		}

		logger.debug("getCountriesByCountryNameMap(languageID=\""+languageID+"\") about to exit.");
		return countriesByCountryName;
	}

	/**
	 * key: CityID cityID<br/>
	 * value: Map {<br/>
	 *		key: String languageID<br/>
	 *		value: FulltextMap locationsByLocationName {<br/>
	 *			key: String locationName
	 *			value: List locations (instances of {@link Location}
	 *		}<br/>
	 * }
	 */
	protected transient Map locationsByLocationNameByLanguageIDByCityID = null;

	protected FulltextMap getLocationsByLocationNameMap(CityID cityID, String languageID)
	{
		logger.debug("getLocationsByLocationNameMap(cityID=\""+cityID+"\", languageID=\""+languageID+"\") entered.");

		needLocations(cityID.countryID);

		if (locationsByLocationNameByLanguageIDByCityID == null)
			locationsByLocationNameByLanguageIDByCityID = new HashMap();

		Map locationsByLocationNameByLanguageID = (Map) locationsByLocationNameByLanguageIDByCityID.get(cityID);
		if (locationsByLocationNameByLanguageID == null) {
			locationsByLocationNameByLanguageID = new HashMap();
			locationsByLocationNameByLanguageIDByCityID.put(cityID, locationsByLocationNameByLanguageID);
		}

		FulltextMap locationsByLocationName = (FulltextMap) locationsByLocationNameByLanguageID.get(languageID);
		if (locationsByLocationName == null) {
			locationsByLocationName = new FulltextMap(FULLTEXT_MAP_FEATURES);
			locationsByLocationNameByLanguageID.put(languageID, locationsByLocationName);

			City city = getCity(cityID, false);
			if (city != null) {
				for (Iterator it = city.getLocations().iterator(); it.hasNext(); ) {
					Location location = (Location) it.next();
					String locationName = location.getName().getText(languageID);
					List locationList = (List) locationsByLocationName.get(locationName);
					if (locationList == null) {
						locationList = new LinkedList();
						locationsByLocationName.put(locationName, locationList);
					}
					locationList.add(location);
				}
			}
		}

		logger.debug("getLocationsByLocationNameMap(cityID=\""+cityID+"\", languageID=\""+languageID+"\") about to exit.");
		return locationsByLocationName;
	}

	/**
	 * key: CountryID countryID<br/>
	 * value: Map {<br/>
	 *		key: String languageID<br/>
	 *		value: FulltextMap regionsByRegionName {<br/>
	 *			key: String regionName
	 *			value: List regions (instances of {@link Region}
	 *		}<br/>
	 * }
	 */
	protected transient Map regionsByRegionNameByLanguageIDByCountryID = null;

	protected FulltextMap getRegionsByRegionNameMap(CountryID countryID, String languageID)
	{
		logger.debug("getRegionsByRegionNameMap(countryID=\""+countryID+"\", languageID=\""+languageID+"\") entered.");

		needRegions(countryID.countryID);

		if (regionsByRegionNameByLanguageIDByCountryID == null)
			regionsByRegionNameByLanguageIDByCountryID = new HashMap();

		Map regionsByRegionNameByLanguageID = (Map) regionsByRegionNameByLanguageIDByCountryID.get(countryID);
		if (regionsByRegionNameByLanguageID == null) {
			regionsByRegionNameByLanguageID = new HashMap();
			regionsByRegionNameByLanguageIDByCountryID.put(countryID, regionsByRegionNameByLanguageID);
		}

		FulltextMap regionsByRegionName = (FulltextMap) regionsByRegionNameByLanguageID.get(languageID);
		if (regionsByRegionName == null) {
			regionsByRegionName = new FulltextMap(FULLTEXT_MAP_FEATURES);
			regionsByRegionNameByLanguageID.put(languageID, regionsByRegionName);

			Country country = getCountry(countryID, false);
			if (country != null) {
				for (Iterator it = country.getRegions().iterator(); it.hasNext(); ) {
					Region region = (Region) it.next();
					String regionName = region.getName().getText(languageID);
					List regionList = (List) regionsByRegionName.get(regionName);
					if (regionList == null) {
						regionList = new LinkedList();
						regionsByRegionName.put(regionName, regionList);
					}
					regionList.add(region);
				}
			}
		}

		logger.debug("getRegionsByRegionNameMap(countryID=\""+countryID+"\", languageID=\""+languageID+"\") about to exit.");
		return regionsByRegionName;
	}

	/**
	 * key: RegionID regionID<br/>
	 * value: FulltextMap districtsByZip {<br/>
	 *		key: String zip
	 *		value: List districts (instances of {@link District})
	 * }
	 */
	protected Map districtsByZipByRegionID = null;

	protected FulltextMap getDistrictsByZipMap(RegionID regionID)
	{
		logger.debug("getDistrictsByZipMap(regionID=\""+regionID+"\") entered.");

		needZips(regionID.countryID);

		if (districtsByZipByRegionID == null)
			districtsByZipByRegionID = new HashMap();

		FulltextMap districtsByZip = (FulltextMap) districtsByZipByRegionID.get(regionID);
		if (districtsByZip == null) {
			districtsByZip = new FulltextMap(FULLTEXT_MAP_FEATURES);
			districtsByZipByRegionID.put(regionID, districtsByZip);

			Region region = getRegion(regionID, false);
			if (region != null) {
				for (Iterator itC = region.getCities().iterator(); itC.hasNext(); ) {
					City city = (City) itC.next();
					for (Iterator itD = city.getDistricts().iterator(); itD.hasNext(); ) {
						District district = (District) itD.next();
						for (Iterator itZ = district.getZips().iterator(); itZ.hasNext(); ) {
							String zip = (String) itZ.next();

							List districtList = (List) districtsByZip.get(zip);
							if (districtList == null) {
								districtList = new LinkedList();
								districtsByZip.put(zip, districtList);
							}
							districtList.add(district);
						}
					}
				}
			}
		}

		logger.debug("getDistrictsByZipMap(regionID=\""+regionID+"\") about to exit.");
		return districtsByZip;
	}

	/**
	 * key: RegionID regionID<br/>
	 * value: Map {<br/>
	 *		key: String languageID<br/>
	 *		value: FulltextMap citiesByCityName {<br/>
	 *			key: String cityName<br/>
	 *			value: List cities (instances of {@link City})<br/>
	 *		}<br/>
	 * }
	 */
	protected Map citiesByCityNameByLanguageIDByRegionID = null;

	protected FulltextMap getCitiesByCityNameMap(RegionID regionID, String languageID)
	{
		logger.debug("getCitiesByCityNameMap(regionID=\""+regionID+"\", languageID=\""+languageID+"\") entered.");

		needCities(regionID.countryID);

		if (citiesByCityNameByLanguageIDByRegionID == null)
			citiesByCityNameByLanguageIDByRegionID = new HashMap();

		Map citiesByCityNameByLanguageID = (Map) citiesByCityNameByLanguageIDByRegionID.get(regionID);
		if (citiesByCityNameByLanguageID == null) {
			citiesByCityNameByLanguageID = new HashMap();
			citiesByCityNameByLanguageIDByRegionID.put(regionID, citiesByCityNameByLanguageID);
		}

		FulltextMap citiesByCityName = (FulltextMap) citiesByCityNameByLanguageID.get(languageID);
		if (citiesByCityName == null) {
			citiesByCityName = new FulltextMap(FULLTEXT_MAP_FEATURES);
			citiesByCityNameByLanguageID.put(languageID, citiesByCityName);

			Region region = getRegion(regionID, false);
			if (region != null) {
				for (Iterator it = region.getCities().iterator(); it.hasNext(); ) {
					City city = (City) it.next();
					String cityName = city.getName().getText(languageID);
					List cityList = (List) citiesByCityName.get(cityName);
					if (cityList == null) {
						cityList = new LinkedList();
						citiesByCityName.put(cityName, cityList);
					}
					cityList.add(city);
				}
			}
		}

		logger.debug("getCitiesByCityNameMap(regionID=\""+regionID+"\", languageID=\""+languageID+"\") about to exit.");
		return citiesByCityName;
	}

	public List<Country> findCountriesByCountryNameSorted(String countryNamePart, Locale locale, int findMode)
	{
		if ("".equals(countryNamePart))
			return getCountriesSorted(locale);

		List countriesSorted = (List) findCountriesByCountryName(countryNamePart, locale.getLanguage(), findMode);
		Collections.sort(countriesSorted, getCountryComparator(locale));
		return countriesSorted;
	}

	public Collection<Country> findCountriesByCountryName(String countryNamePart, Locale locale, int findMode)
	{
		return findCountriesByCountryName(countryNamePart, locale.getLanguage(), findMode);
	}

	protected Collection findCountriesByCountryName(String countryNamePart, String languageID, int findMode)
	{
		if ("".equals(countryNamePart))
			return getCountries();

		Collection res = new ArrayList();
		FulltextMap countriesByCountryName = getCountriesByCountryNameMap(languageID);
		for (Iterator it = countriesByCountryName.find(countryNamePart, findMode).iterator(); it.hasNext(); ) {
			List countries = (List) it.next();
			res.addAll(countries);
		}
		return res;
	}

	public Collection<Region> findRegionsByRegionName(CountryID countryID, String regionNamePart, Locale locale, int findMode)
	{
		return findRegionsByRegionName(countryID, regionNamePart, locale.getLanguage(), findMode);
	}

	protected Collection<Region> findRegionsByRegionName(CountryID countryID, String regionNamePart, String languageID, int findMode)
	{
		if ("".equals(regionNamePart))
			return getRegions(countryID, false);

		Collection res = new ArrayList();
		FulltextMap regionNames2regions = getRegionsByRegionNameMap(countryID, languageID);
		for (Iterator it = regionNames2regions.find(regionNamePart, findMode).iterator(); it.hasNext(); ) {
			List regions = (List) it.next();
			res.addAll(regions);
		}
		return res;
	}

	public List<Region> findRegionsByRegionNameSorted(CountryID countryID, String regionNamePart, Locale locale, int findMode)
	{
		if ("".equals(regionNamePart))
			return getRegionsSorted(countryID, locale);

		List regionsSorted = (List) findRegionsByRegionName(countryID, regionNamePart, locale.getLanguage(), findMode);
		Collections.sort(regionsSorted, getRegionComparator(locale));
		return regionsSorted;
	}

	public Collection<City> findCitiesByZip(RegionID regionID, String zipPart, int findMode)
	{
		if ("".equals(zipPart))
			return getCities(regionID, false);

		// key: String cityPK
		// value: City city
		Map cities = new HashMap();
		FulltextMap zips2districts = getDistrictsByZipMap(regionID);
		for (Iterator itL = zips2districts.find(zipPart, findMode).iterator(); itL.hasNext(); ) {
			List districts = (List) itL.next();
			for (Iterator itD = districts.iterator(); itD.hasNext(); ) {
				District district = (District) itD.next();
				cities.put(district.getCity().getPrimaryKey(), district.getCity());
			}
		}
		return cities.values();
	}

	public List<City> findCitiesByZipSorted(RegionID regionID, String zipPart, Locale locale, int findMode)
	{
		if ("".equals(zipPart))
			return getCitiesSorted(regionID, locale);

		List citiesSorted = new ArrayList(findCitiesByZip(regionID, zipPart, findMode));
		Collections.sort(citiesSorted, getCityComparator(locale));
		return citiesSorted;
	}

	public Collection<City> findCitiesByCityName(RegionID regionID, String cityNamePart, Locale locale, int findMode)
	{
		return findCitiesByCityName(regionID, cityNamePart, locale.getLanguage(), findMode);
	}

	protected Collection findCitiesByCityName(RegionID regionID, String cityNamePart, String languageID, int findMode)
	{
		if ("".equals(cityNamePart))
			return getCities(regionID, false);

		Collection res = new ArrayList();
		FulltextMap cityNames2cities = getCitiesByCityNameMap(regionID, languageID); // (FulltextMap) cityNames2citiesByLanguageID.get(languageID);
		for (Iterator it = cityNames2cities.find(cityNamePart, findMode).iterator(); it.hasNext(); ) {
			List cities = (List) it.next();
			res.addAll(cities);
		}
		return res;
	}

	public List findCitiesByCityNameSorted(RegionID regionID, String cityNamePart, Locale locale, int findMode)
	{
		if ("".equals(cityNamePart))
			return getCitiesSorted(regionID, locale);

		List citiesSorted = (List) findCitiesByCityName(regionID, cityNamePart, locale.getLanguage(), findMode);
		Collections.sort(citiesSorted, getCityComparator(locale));
		return citiesSorted;
	}


	public Collection findLocationsByLocationName(CityID cityID, String locationNamePart, Locale locale, int findMode)
	{
		return findLocationsByLocationName(cityID, locationNamePart, locale.getLanguage(), findMode);
	}

	protected Collection findLocationsByLocationName(CityID cityID, String locationNamePart, String languageID, int findMode)
	{
		if ("".equals(locationNamePart))
			return getLocations(cityID, false);

		Collection res = new ArrayList();
		FulltextMap locationNames2locations = getLocationsByLocationNameMap(cityID, languageID); // (FulltextMap) locationNames2locationsByLanguageID.get(languageID);
		for (Iterator it = locationNames2locations.find(locationNamePart, findMode).iterator(); it.hasNext(); ) {
			List locations = (List) it.next();
			res.addAll(locations);
		}
		return res;
	}

	public List findLocationsByLocationNameSorted(CityID cityID, String locationNamePart, Locale locale, int findMode)
	{
		if ("".equals(locationNamePart))
			return getLocationsSorted(cityID, locale);

		List locationsSorted = (List) findLocationsByLocationName(cityID, locationNamePart, locale.getLanguage(), findMode);
		Collections.sort(locationsSorted, getLocationComparator(locale));
		return locationsSorted;
	}

	protected static final int FULLTEXT_MAP_FEATURES =
			FulltextMap.FEATURE_BEGINS_WITH | FulltextMap.FEATURE_CONTAINS | FulltextMap.FEATURE_ENDS_WITH;
//	protected static final int FULLTEXT_MAP_FIND_MODE = FulltextMap.FIND_MODE_CONTAINS;

	public static final String IMPORTCHARSET = "UTF-8";


	protected boolean loadedCountries = false;

	protected abstract void loadCountries();

	/**
	 * This method loads the countries, if they're not yet loaded. It uses the file
	 * "resource/Data-Country.csv" relative to this class. Additionally, it adds all
	 * the countries from {@link Locale} that are missing in the csv file.
	 */
	protected synchronized void needCountries()
	{
		if (loadedCountries)
			return;

		loadCountries();

		loadedCountries = true;
	}

	/**
	 * Contains instances of {@link String} representing the <tt>countryID</tt> of all countries
	 * for which the regions have already been loaded.
	 */
	protected Set loadedRegionsCountryIDSet = new HashSet();

	protected abstract void loadRegions(String countryID);

	/**
	 * This method loads all the regions for the given country, if they have not yet been loaded.
	 *
	 * @param countryID
	 * @throws IOException
	 */
	protected synchronized void needRegions(String countryID)
	{
		if (loadedRegionsCountryIDSet.contains(countryID))
			return;

		needCountries();

		loadRegions(countryID);

		loadedRegionsCountryIDSet.add(countryID);
	}

	/**
	 * Contains instances of {@link String} representing the <tt>countryID</tt> of all countries
	 * for which the locations have already been loaded.
	 */
	protected Set loadedLocationsCountryIDSet = new HashSet();

	protected abstract void loadLocations(String countryID);

	/**
	 * This method loads all the locations for the given country, if they have not yet been loaded.
	 *
	 * @param countryID
	 * @throws IOException
	 */
	protected synchronized void needLocations(String countryID)
	{
		if (loadedLocationsCountryIDSet.contains(countryID))
			return;

		needCities(countryID);

		loadLocations(countryID);

		loadedLocationsCountryIDSet.add(countryID);
	}


	/**
	 * Contains instances of {@link String} representing the <tt>countryID</tt> of all countries
	 * for which the cities have already been loaded.
	 */
	protected Set loadedCitiesCountryIDSet = new HashSet();

	protected abstract void loadCities(String countryID);

	/**
	 * This method loads all the cities for the given country, if they have not yet been loaded.
	 *
	 * @param countryID
	 * @throws IOException
	 */
	protected synchronized void needCities(String countryID)
	{
		if (loadedCitiesCountryIDSet.contains(countryID))
			return;

		needRegions(countryID);

		loadCities(countryID);

		loadedCitiesCountryIDSet.add(countryID);
	}


	/**
	 * Contains instances of {@link String} representing the <tt>countryID</tt> of all countries
	 * for which the districts have already been loaded.
	 */
	protected Set loadedDistrictsCountryIDSet = new HashSet();

	protected abstract void loadDistricts(String countryID);

	/**
	 * This method loads all the cities for the given country, if they have not yet been loaded.
	 *
	 * @param countryID
	 * @throws IOException
	 */
	protected synchronized void needDistricts(String countryID)
	{
		if (loadedDistrictsCountryIDSet.contains(countryID))
			return;

		needCities(countryID);

		loadDistricts(countryID);

		loadedDistrictsCountryIDSet.add(countryID);
	}


	/**
	 * Contains instances of {@link String} representing the <tt>countryID</tt> of all countries
	 * for which the zips have already been loaded.
	 */
	protected Set loadedZipsCountryIDSet = new HashSet();

	protected abstract void loadZips(String countryID);

	/**
	 * This method loads all the cities for the given country, if they have not yet been loaded.
	 *
	 * @param countryID
	 * @throws IOException
	 */
	protected synchronized void needZips(String countryID)
	{
		if (loadedZipsCountryIDSet.contains(countryID))
			return;

		needDistricts(countryID);

		loadZips(countryID);

 		loadedZipsCountryIDSet.add(countryID);
	}

	/**
	 * This method clears the complete cache in order to reload all data again. Call this method after you
	 * modified data.
	 */
	public synchronized void clearCache()
	{
		loadedCountries = false;
		loadedCitiesCountryIDSet.clear();
		loadedDistrictsCountryIDSet.clear();
		loadedLocationsCountryIDSet.clear();
		loadedRegionsCountryIDSet.clear();
		loadedZipsCountryIDSet.clear();

		countries.clear();
		regions.clear();
		cities.clear();
		districts.clear();
		locations.clear();

		countriesByCountryNameByLanguageID = null;
		countriesSortedByLanguageID = null;
		regionsByRegionNameByLanguageIDByCountryID = null;
		regionsSortedByLanguageIDByCountryID = null;
		citiesByCityNameByLanguageIDByRegionID = null;
		citiesSortedByLanguageIDByRegionID = null;
		districtsByZipByRegionID = null;
		locationsByLocationNameByLanguageIDByCityID = null;
		locationsSortedByLanguageIDByCountryID = null;
	}

	// countryIDs are ISO standard Strings - we don't need an ID generator method for them!

	public static String nextRegionID(String countryID, String organisationID)
	{
		if (!IDGenerator.getOrganisationID().equals(organisationID))
			throw new IllegalArgumentException("Can only generate an ID for the organisation '"+IDGenerator.getOrganisationID()+"' - the argument is invalid: " + organisationID);

		return String.valueOf(IDGenerator.nextID(Region.class.getName() + "#" + countryID));
//		throw new UnsupportedOperationException("NYI");
	}

	public static String nextCityID(String countryID, String organisationID)
	{
		if (!IDGenerator.getOrganisationID().equals(organisationID))
			throw new IllegalArgumentException("Can only generate an ID for the organisation '"+IDGenerator.getOrganisationID()+"' - the argument is invalid: " + organisationID);

		return String.valueOf(IDGenerator.nextID(City.class.getName() + "#" + countryID));
	}

	// TODO implement the other ID generator methods - don't forget to initialise the namespace correctly!
	public static String nextLocationID(String countryID, String organisationID)
	{
		if (!IDGenerator.getOrganisationID().equals(organisationID))
			throw new IllegalArgumentException("Can only generate an ID for the organisation '"+IDGenerator.getOrganisationID()+"' - the argument is invalid: " + organisationID);

		return String.valueOf(IDGenerator.nextID(Location.class.getName() + "#" + countryID));
	}
}
