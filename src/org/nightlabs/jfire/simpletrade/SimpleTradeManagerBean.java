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

package org.nightlabs.jfire.simpletrade;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.jdo.FetchPlan;
import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.apache.log4j.Logger;
import org.nightlabs.ModuleException;
import org.nightlabs.jdo.NLJDOHelper;
import org.nightlabs.jdo.moduleregistry.ModuleMetaData;
import org.nightlabs.jfire.accounting.Price;
import org.nightlabs.jfire.accounting.Tariff;
import org.nightlabs.jfire.accounting.gridpriceconfig.FormulaPriceConfig;
import org.nightlabs.jfire.accounting.gridpriceconfig.GridPriceConfig;
import org.nightlabs.jfire.accounting.gridpriceconfig.GridPriceConfigUtil;
import org.nightlabs.jfire.accounting.gridpriceconfig.IResultPriceConfig;
import org.nightlabs.jfire.accounting.gridpriceconfig.PriceCalculationException;
import org.nightlabs.jfire.accounting.gridpriceconfig.PriceCalculator;
import org.nightlabs.jfire.accounting.gridpriceconfig.PriceCell;
import org.nightlabs.jfire.accounting.gridpriceconfig.StablePriceConfig;
import org.nightlabs.jfire.accounting.gridpriceconfig.TariffPricePair;
import org.nightlabs.jfire.accounting.id.CurrencyID;
import org.nightlabs.jfire.accounting.id.TariffID;
import org.nightlabs.jfire.accounting.priceconfig.FetchGroupsPriceConfig;
import org.nightlabs.jfire.accounting.priceconfig.id.PriceConfigID;
import org.nightlabs.jfire.base.BaseSessionBeanImpl;
import org.nightlabs.jfire.base.JFireException;
import org.nightlabs.jfire.jdo.notification.persistent.PersistentNotificationEJB;
import org.nightlabs.jfire.jdo.notification.persistent.PersistentNotificationEJBUtil;
import org.nightlabs.jfire.jdo.notification.persistent.SubscriptionUtil;
import org.nightlabs.jfire.organisation.Organisation;
import org.nightlabs.jfire.security.User;
import org.nightlabs.jfire.simpletrade.notification.SimpleProductTypeNotificationFilter;
import org.nightlabs.jfire.simpletrade.notification.SimpleProductTypeNotificationReceiver;
import org.nightlabs.jfire.simpletrade.store.SimpleProductType;
import org.nightlabs.jfire.simpletrade.store.SimpleProductTypeActionHandler;
import org.nightlabs.jfire.store.NestedProductType;
import org.nightlabs.jfire.store.ProductType;
import org.nightlabs.jfire.store.Store;
import org.nightlabs.jfire.store.deliver.DeliveryConfiguration;
import org.nightlabs.jfire.store.deliver.ModeOfDelivery;
import org.nightlabs.jfire.store.deliver.id.ModeOfDeliveryID;
import org.nightlabs.jfire.store.id.ProductTypeID;
import org.nightlabs.jfire.trade.ArticleCreator;
import org.nightlabs.jfire.trade.CustomerGroup;
import org.nightlabs.jfire.trade.Offer;
import org.nightlabs.jfire.trade.Order;
import org.nightlabs.jfire.trade.Segment;
import org.nightlabs.jfire.trade.Trader;
import org.nightlabs.jfire.trade.id.CustomerGroupID;
import org.nightlabs.jfire.trade.id.SegmentID;


/**
 * @ejb.bean name="jfire/ejb/JFireSimpleTrade/SimpleTradeManager"	
 *					 jndi-name="jfire/ejb/JFireSimpleTrade/SimpleTradeManager"
 *					 type="Stateless" 
 *					 transaction-type="Container"
 *
 * @ejb.util generate = "physical"
 */
public abstract class SimpleTradeManagerBean
extends BaseSessionBeanImpl
implements SessionBean
{
	/**
	 * LOG4J logger used by this class
	 */
	private static final Logger logger = Logger.getLogger(SimpleTradeManagerBean.class);

	/**
	 * @see org.nightlabs.jfire.base.BaseSessionBeanImpl#setSessionContext(javax.ejb.SessionContext)
	 */
	public void setSessionContext(SessionContext sessionContext)
	throws EJBException, RemoteException
	{
		super.setSessionContext(sessionContext);
	}
	/**
	 * @see org.nightlabs.jfire.base.BaseSessionBeanImpl#unsetSessionContext()
	 */
	public void unsetSessionContext() {
		super.unsetSessionContext();
	}
	
	/**
	 * @ejb.create-method
	 * @ejb.permission role-name="_Guest_"	
	 */
	public void ejbCreate() throws CreateException
	{
	}
	/**
	 * @see javax.ejb.SessionBean#ejbRemove()
	 *
	 * @ejb.permission unchecked="true"
	 */
	public void ejbRemove() throws EJBException, RemoteException { }
	
	/**
	 * This method is called by the datastore initialisation mechanism.
	 * It creates the root simple product for the organisation itself.
	 * Simple products of other organisations must be imported.
	 * 
	 * @throws ModuleException
	 *
	 * @ejb.interface-method
	 * @ejb.permission role-name="_System_"
	 * @ejb.transaction type = "Required"
	 */
	public void initialise() 
	throws ModuleException 
	{
		PersistenceManager pm = this.getPersistenceManager();
		try {
			String organisationID = getOrganisationID();

			ModuleMetaData moduleMetaData = ModuleMetaData.getModuleMetaData(pm, "JFireSimpleTrade");
			if (moduleMetaData != null)
				return;

			logger.info("Initialization of JFireSimpleTrade started...");

			// version is {major}.{minor}.{release}-{patchlevel}-{suffix}
			moduleMetaData = new ModuleMetaData(
					"JFireSimpleTrade", "1.0.0-0-beta", "1.0.0-0-beta");
			pm.makePersistent(moduleMetaData);

			SimpleProductTypeActionHandler simpleProductTypeActionHandler = new SimpleProductTypeActionHandler(
					Organisation.DEVIL_ORGANISATION_ID, SimpleProductTypeActionHandler.class.getName(), SimpleProductType.class);
			pm.makePersistent(simpleProductTypeActionHandler);

			Store store = Store.getStore(pm);
//			Accounting accounting = Accounting.getAccounting(pm);

			// create a default DeliveryConfiguration with all default ModeOfDelivery s
			DeliveryConfiguration deliveryConfiguration = new DeliveryConfiguration(
					organisationID, "JFireSimpleTrade.default");
			deliveryConfiguration.getName().setText(Locale.ENGLISH.getLanguage(), "Default Delivery Configuration");
			deliveryConfiguration.getName().setText(Locale.GERMAN.getLanguage(), "Standard-Liefer-Konfiguration");
			pm.getExtent(ModeOfDelivery.class);

			try {
				ModeOfDelivery modeOfDelivery;
				
				modeOfDelivery = (ModeOfDelivery) pm.getObjectById(ModeOfDeliveryID.create(
						Organisation.DEVIL_ORGANISATION_ID, ModeOfDelivery.MODE_OF_DELIVERY_ID_MANUAL));
				deliveryConfiguration.addModeOfDelivery(modeOfDelivery);
	
				modeOfDelivery = (ModeOfDelivery) pm.getObjectById(ModeOfDeliveryID.create(
						Organisation.DEVIL_ORGANISATION_ID, ModeOfDelivery.MODE_OF_DELIVERY_ID_MAILING_VIRTUAL));
				deliveryConfiguration.addModeOfDelivery(modeOfDelivery);

				modeOfDelivery = (ModeOfDelivery) pm.getObjectById(ModeOfDeliveryID.create(
						Organisation.DEVIL_ORGANISATION_ID, ModeOfDelivery.MODE_OF_DELIVERY_ID_MAILING_PHYSICAL));
				deliveryConfiguration.addModeOfDelivery(modeOfDelivery);

				pm.makePersistent(deliveryConfiguration);
			} catch (JDOObjectNotFoundException x) {
				logger.warn("Could not populate default DeliveryConfiguration for JFireSimpleTrade with ModeOfDelivery s!", x);
			}

			User user = User.getUser(pm, getPrincipal());
			SimpleProductType rootSimpleProductType = new SimpleProductType(
					organisationID, SimpleProductType.class.getName(),
					null, store.getMandator(),
					ProductType.INHERITANCE_NATURE_BRANCH, ProductType.PACKAGE_NATURE_OUTER);
			rootSimpleProductType.setDeliveryConfiguration(deliveryConfiguration);
			store.addProductType(user, rootSimpleProductType, SimpleProductTypeActionHandler.getDefaultHome(pm, rootSimpleProductType));
			store.setProductTypeStatus_published(user, rootSimpleProductType);


//			// TEST add test products
//			// TODO remove this test stuff
//			{
//				String langID = Locale.ENGLISH.getLanguage();
//
////				pm.getExtent(CustomerGroup.class);
////				CustomerGroup customerGroup = (CustomerGroup) pm.getObjectById(CustomerGroupID.create(organisationID, "default"));
//
//				pm.getExtent(Currency.class);
//				Currency euro = (Currency) pm.getObjectById(CurrencyID.create("EUR"));
//
//				pm.getExtent(Tariff.class);
//				Tariff tariff;
//				try {
//					tariff = (Tariff) pm.getObjectById(TariffID.create(organisationID, 0));
//				} catch (JDOObjectNotFoundException x) {
//					tariff = new Tariff(organisationID);
//					tariff.getName().setText(langID, "Normal Price");
//					pm.makePersistent(tariff);
//				}
//
//				// create the category "car"
//				SimpleProductType car = new SimpleProductType(
//						organisationID, "car", rootSimpleProductType, null, 
//						ProductType.INHERITANCE_NATURE_BRANCH, ProductType.PACKAGE_NATURE_OUTER);
//				car.getName().setText(langID, "Car");
////				car.setDeliveryConfiguration(deliveryConfiguration);
//				store.addProductType(user, car, SimpleProductType.getDefaultHome(pm, car));
//				store.setProductTypeStatus_published(user, car);
//
//				// create the price config "Car - Middle Class"
//				PriceFragmentType totalPriceFragmentType = PriceFragmentType.getTotalPriceFragmentType(pm);
//				PriceFragmentType vatNet = (PriceFragmentType) pm.getObjectById(PriceFragmentTypeID.create(getRootOrganisationID(), "vat-de-16-net"));
//				PriceFragmentType vatVal = (PriceFragmentType) pm.getObjectById(PriceFragmentTypeID.create(getRootOrganisationID(), "vat-de-16-val"));
//
//				Accounting accounting = Accounting.getAccounting(pm);
//				Trader trader = Trader.getTrader(pm);
//				StablePriceConfig stablePriceConfig = new StablePriceConfig(organisationID, accounting.createPriceConfigID());
//				FormulaPriceConfig formulaPriceConfig = new FormulaPriceConfig(organisationID, accounting.createPriceConfigID());
//				formulaPriceConfig.getName().setText(langID, "Car - Middle Class");
//				
//				CustomerGroup customerGroupDefault = trader.getDefaultCustomerGroupForKnownCustomer();
//				CustomerGroup customerGroupAnonymous = LegalEntity.getAnonymousCustomer(pm).getDefaultCustomerGroup();
//				formulaPriceConfig.addCustomerGroup(customerGroupDefault);
//				formulaPriceConfig.addCustomerGroup(customerGroupAnonymous);
//				formulaPriceConfig.addCurrency(euro);
//				formulaPriceConfig.addTariff(tariff);
////				formulaPriceConfig.addProductType(rootSimpleProductType);
//				formulaPriceConfig.addPriceFragmentType(totalPriceFragmentType);
//				formulaPriceConfig.addPriceFragmentType(vatNet);
//				formulaPriceConfig.addPriceFragmentType(vatVal);
//				stablePriceConfig.adoptParameters(formulaPriceConfig);
//
//				FormulaCell fallbackFormulaCell = formulaPriceConfig.createFallbackFormulaCell();
//				fallbackFormulaCell.setFormula(totalPriceFragmentType,
//						"cell.resolvePriceCellsAmount(\n" +
//						"	new AbsolutePriceCoordinate(\n" +
//						"		\""+organisationID+"/"+CustomerGroup.CUSTOMER_GROUP_ID_DEFAULT+"\",\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		null\n" +
//						"	)\n" +
//						");");
//				fallbackFormulaCell.setFormula(vatNet, "cell.resolvePriceCellsAmount(\n" +
//						"	new AbsolutePriceCoordinate(\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		\""+Organisation.DEVIL_ORGANISATION_ID+"/_Total_\"\n" +
//						"	)\n" +
//						") / 1.16;");
//				fallbackFormulaCell.setFormula(vatVal, "cell.resolvePriceCellsAmount(\n" +
//						"	new AbsolutePriceCoordinate(\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		\""+Organisation.DEVIL_ORGANISATION_ID+"/_Total_\"\n" +
//						"	)\n" +
//						")\n" +
//
////						"/ 1.16 * 0.16");
//
//						"\n" +
//						"-\n" +
//						"\n" +
//						"cell.resolvePriceCellsAmount(\n" +
//						"	new AbsolutePriceCoordinate(\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		\""+getRootOrganisationID()+"/vat-de-16-net\"\n" +
//						"	)\n" +
//						");");
//
//				FormulaCell cell = formulaPriceConfig.createFormulaCell(customerGroupDefault, tariff, euro);
//				cell.setFormula(totalPriceFragmentType, "5000");
//
//				// create the car "BMW 320i" and assign the "Car - Middle Class" price config
//				SimpleProductType bmw320i = new SimpleProductType(
//						organisationID, "bmw320i", car, null, ProductType.INHERITANCE_NATURE_LEAF, ProductType.PACKAGE_NATURE_OUTER);
//				bmw320i.getName().setText(Locale.ENGLISH.getLanguage(), "BMW 320i");
//				bmw320i.setPackagePriceConfig(stablePriceConfig);
//				bmw320i.setInnerPriceConfig(formulaPriceConfig);
//				bmw320i.setDeliveryConfiguration(deliveryConfiguration);
//				store.addProductType(user, bmw320i, SimpleProductType.getDefaultHome(pm, bmw320i));
//
//				store.setProductTypeStatus_published(user, bmw320i);
//				store.setProductTypeStatus_confirmed(user, bmw320i);
//				store.setProductTypeStatus_saleable(user, bmw320i, true);
//
//				// create the category "Car Part"
//				SimpleProductType carPart = new SimpleProductType(
//						organisationID, "carPart", rootSimpleProductType, null, ProductType.INHERITANCE_NATURE_BRANCH, ProductType.PACKAGE_NATURE_INNER);
//				carPart.getName().setText(Locale.ENGLISH.getLanguage(), "Car Part");
//				carPart.setDeliveryConfiguration(deliveryConfiguration);
//				store.addProductType(user, carPart, SimpleProductType.getDefaultHome(pm, carPart));
//
//				// create the part "Wheel"
//				SimpleProductType wheel = new SimpleProductType(
//						organisationID, "wheel", carPart, null, ProductType.INHERITANCE_NATURE_LEAF, ProductType.PACKAGE_NATURE_INNER);
//				wheel.getName().setText(Locale.ENGLISH.getLanguage(), "Wheel");
//				wheel.setDeliveryConfiguration(deliveryConfiguration);
//				store.addProductType(user, wheel, SimpleProductType.getDefaultHome(pm, wheel));
//
//				// create the priceConfig "Car Part - Wheel" and assign it to "Wheel"
//				formulaPriceConfig = new FormulaPriceConfig(organisationID, accounting.createPriceConfigID());
//				formulaPriceConfig.addProductType(car);
//				formulaPriceConfig.addPriceFragmentType(vatVal);
//				formulaPriceConfig.addPriceFragmentType(vatNet);
//				formulaPriceConfig.getName().setText(langID, "Car Part - Wheel");
//				fallbackFormulaCell = formulaPriceConfig.createFallbackFormulaCell();
//				fallbackFormulaCell.setFormula(
//						Organisation.DEVIL_ORGANISATION_ID,
//						PriceFragmentType.TOTAL_PRICEFRAGMENTTYPEID,
//						"cell.resolvePriceCellsAmount(\n" +
//						"	new AbsolutePriceCoordinate(\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		\""+car.getPrimaryKey()+"\",\n" +
//						"		null\n" +
//						"	)\n" +
//						") * 0.1;");
//
//				fallbackFormulaCell.setFormula(vatNet, "cell.resolvePriceCellsAmount(\n" +
//						"	new AbsolutePriceCoordinate(\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		\""+Organisation.DEVIL_ORGANISATION_ID+"/_Total_\"\n" +
//						"	)\n"+
//						") / 1.16;");
//				fallbackFormulaCell.setFormula(vatVal, "cell.resolvePriceCellsAmount(\n" +
//						"	new AbsolutePriceCoordinate(\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		\""+Organisation.DEVIL_ORGANISATION_ID+"/_Total_\"\n" +
//						"	)\n"+
//						")\n" +
//						
////						"/ 1.16 * 0.16;");
//						
//						"\n" +
//						"-\n" +
//						"\n" +
//						"cell.resolvePriceCellsAmount(\n" +
//						"	new AbsolutePriceCoordinate(\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		null,\n" +
//						"		\""+getRootOrganisationID()+"/vat-de-16-net\"\n" +
//						"	)\n"+
//						");");
//
//				wheel.setInnerPriceConfig(formulaPriceConfig);
//
//				// package 4 wheels inside the bmw320i
//				NestedProductType wheelInsideBMW = bmw320i.createNestedProductType(wheel);
//				wheelInsideBMW.setQuantity(4);
//
//				// calculate prices
//				PriceCalculator priceCalculator = new PriceCalculator(bmw320i);
//				priceCalculator.preparePriceCalculation(accounting);
//				priceCalculator.calculatePrices();
//			}
//			// TEST END

			logger.info("Initialization of JFireSimpleTrade complete!");
		} finally {
			pm.close();
		}
	}

	/**
	 * @return Returns a <tt>Collection</tt> of <tt>SimpleProductType</tt>.
	 *
	 * @ejb.interface-method
	 * @ejb.permission role-name="SimpleTradeManager-user"
	 * @ejb.transaction type = "Required"
	 */
	public Collection<SimpleProductType> test(Map<String, SimpleProductType> m)
		throws ModuleException
	{
		return new ArrayList<SimpleProductType>(m.values());
	}

	/**
	 * @return Returns a <tt>Collection</tt> of <tt>SimpleProductType</tt>.
	 *
	 * @ejb.interface-method
	 * @ejb.permission role-name="SimpleTradeManager-user"
	 * @ejb.transaction type = "Required"
	 */
	public Collection getChildProductTypes(ProductTypeID parentProductTypeID, String[] fetchGroups, int maxFetchDepth)
		throws ModuleException
	{
		PersistenceManager pm = getPersistenceManager();
		try {
			Collection res = SimpleProductType.getChildProductTypes(pm, parentProductTypeID);

			FetchPlan fetchPlan = pm.getFetchPlan();
			fetchPlan.setMaxFetchDepth(maxFetchDepth);
			if (fetchGroups != null)
				fetchPlan.setGroups(fetchGroups);

			return pm.detachCopyAll(res);
		} finally {
			pm.close();
		}
	}

	/**
	 * @return Returns a newly detached instance of <tt>SimpleProductType</tt> if <tt>get</tt> is true - otherwise <tt>null</tt>.
	 *
	 * @ejb.interface-method
	 * @ejb.permission role-name="SimpleTradeManager.Admin"
	 * @ejb.transaction type = "Required"
	 */
	public SimpleProductType storeProductType(SimpleProductType productType, boolean get, String[] fetchGroups, int maxFetchDepth)
	throws ModuleException
	{
		if (productType == null)
			throw new NullPointerException("productType");

		PersistenceManager pm = getPersistenceManager();
		try {
			pm.getFetchPlan().setGroup(FetchPlan.DEFAULT);

			SimpleProductType result = null;
			if (NLJDOHelper.exists(pm, productType)) {
				result = (SimpleProductType) NLJDOHelper.storeJDO(pm, productType, get, fetchGroups, maxFetchDepth);
				productType = (SimpleProductType) pm.getObjectById(JDOHelper.getObjectId(productType));
			}
			else {
				Store.getStore(pm).addProductType(
						User.getUser(pm, getPrincipal()),
						productType,
						SimpleProductTypeActionHandler.getDefaultHome(pm, productType));

				// make sure the prices are correct
				((IResultPriceConfig)productType.getPackagePriceConfig()).adoptParameters(
						productType.getInnerPriceConfig());
				PriceCalculator priceCalculator = new PriceCalculator(productType);
				priceCalculator.preparePriceCalculation();
				priceCalculator.calculatePrices();
			}
			// now, productType is attached to the datastore in any case

			// take care about the inheritance
			productType.applyInheritance();

			if (!get)
				return null;

			if (result == null)
				result = (SimpleProductType) pm.detachCopy(pm.getObjectById(JDOHelper.getObjectId(productType)));

			return result;
		} finally {
			pm.close();
		}
	}

//	/**
//	 * @return Returns a newly detached instance of <tt>SimpleProductType</tt> if <tt>get</tt> is true - otherwise <tt>null</tt>.
//	 *
//	 * @ejb.interface-method
//	 * @ejb.permission role-name="SimpleTradeManager.Admin"
//	 * @ejb.transaction type = "Required"
//	 */
//	public SimpleProductType updateProductType(SimpleProductType productType, boolean get, String[] fetchGroups, int maxFetchDepth)
//		throws ModuleException
//	{
//		if (productType == null)
//			throw new NullPointerException("productType");
//
//		PersistenceManager pm = getPersistenceManager();
//		try {
////			// WORKAROUND
////			pm.getExtent(SimpleProductType.class);
////			if (productType.getExtendedProductType() != null) {
////				Object oid = JDOHelper.getObjectId(productType.getExtendedProductType());
////				productType.setExtendedProductType((ProductType)pm.getObjectById(oid));
////			}
////
////			IInnerPriceConfig packagePriceConfig = productType.getInnerPriceConfig();
////			if (packagePriceConfig != null) {
////				pm.getExtent(packagePriceConfig.getClass());
////				Object oid = JDOHelper.getObjectId(packagePriceConfig);
////				if (oid != null) {
////					try {
////						packagePriceConfig = (IInnerPriceConfig) pm.getObjectById(oid);
////						productType.setInnerPriceConfig(packagePriceConfig);
////					} catch (JDOObjectNotFoundException x) {
////						// Object is not in datastore, so try to store it as it is.
////					}
////				}
////			}
////
////			IInnerPriceConfig innerPriceConfig = productType.getInnerPriceConfig();
////			if (innerPriceConfig != null) {
////				pm.getExtent(innerPriceConfig.getClass());
////				Object oid = JDOHelper.getObjectId(innerPriceConfig);
////				if (oid != null) {
////					try {
////						innerPriceConfig = (IInnerPriceConfig) pm.getObjectById(oid);
////						productType.setInnerPriceConfig(innerPriceConfig);
////					} catch (JDOObjectNotFoundException x) {
////						// Object is not in datastore, so try to store it as it is.
////					}
////				}
////			}
////			// END WORK AROUND
////			Object productTypeID = JDOHelper.getObjectId(productType);
////			if (productTypeID != null)
////				cache_addChangedObjectID(productTypeID);
//			if (!Store.getStore(pm).containsProductType(productType))
//				throw new IllegalStateException("The productType \""+productType.getPrimaryKey()+"\" is not yet known! Use addProductType(...) instead!");
//
//			return (SimpleProductType) NLJDOHelper.storeJDO(pm, productType, get, fetchGroups);
////			return (SimpleProductType)NLJDOHelper.storeJDO(pm, product);
//		} finally {
//			pm.close();
//		}
//	}

	/**
	 * @return Returns a <tt>Collection</tt> of {@link FormulaPriceConfig}</tt>.
	 *
	 * @ejb.interface-method
	 * @ejb.permission role-name="SimpleTradeManager.Admin"
	 * @ejb.transaction type = "Required"
	 */
	public Collection getFormulaPriceConfigs(String[] fetchGroups, int maxFetchDepth)
		throws ModuleException
	{
		PersistenceManager pm = getPersistenceManager();
		try {
			pm.getFetchPlan().setMaxFetchDepth(maxFetchDepth);
			if (fetchGroups != null)
				pm.getFetchPlan().setGroups(fetchGroups);

			Query q = pm.newQuery(FormulaPriceConfig.class);
			return pm.detachCopyAll((Collection)q.execute());
		} finally {
			pm.close();
		}
	}

	/**
	 * @return <tt>Collection</tt> of {@link org.nightlabs.jfire.trade.Article}
	 * @throws org.nightlabs.jfire.store.NotAvailableException in case there are not enough <tt>Product</tt>s available and the <tt>Product</tt>s cannot be created (because of a limit). 
	 *
	 * @ejb.interface-method
	 * @ejb.permission role-name="_Guest_"
	 * @ejb.transaction type = "Required"
	 */
	public Collection createArticles(
			SegmentID segmentID,
			ProductTypeID productTypeID,
			int quantity,
			TariffID tariffID, String[] fetchGroups, int maxFetchDepth)
	throws ModuleException
	{
		PersistenceManager pm = getPersistenceManager();
		try {
			Trader trader = Trader.getTrader(pm);
			Store store = Store.getStore(pm);
			Segment segment = (Segment) pm.getObjectById(segmentID);
			Order order = segment.getOrder();

			User user = User.getUser(pm, getPrincipal());

			pm.getExtent(SimpleProductType.class);
			ProductType pt = (ProductType) pm.getObjectById(productTypeID);
			if (!(pt instanceof SimpleProductType))
				throw new IllegalArgumentException("productTypeID \""+productTypeID+"\" specifies a ProductType of type \""+pt.getClass().getName()+"\", but must be \""+SimpleProductType.class.getName()+"\"!");
			
			SimpleProductType productType = (SimpleProductType)pt;

			Tariff tariff = (Tariff) pm.getObjectById(tariffID);

			// find an Offer within the Order which is not finalized - or create one
			Collection offers = Offer.getNonFinalizedOffers(pm, order);
			Offer offer;
			if (!offers.isEmpty()) {
				offer = (Offer) offers.iterator().next();
			}
			else {
				offer = trader.createOffer(user, order, null); // TODO offerIDPrefix ???
			}

			// find / create Products
			NestedProductType pseudoNestedPT = null;
			if (quantity != 1)
				pseudoNestedPT = new NestedProductType(null, productType, quantity);

			Collection products = store.findProducts(user, productType, pseudoNestedPT, null);

			Collection articles = trader.createArticles(
					user, offer, segment,
					products,
					new ArticleCreator(tariff),
					true, false, true);
//			Collection articles = new ArrayList();
//			for (Iterator it = products.iterator(); it.hasNext(); ) {
//				SimpleProduct product = (SimpleProduct) it.next();
//				Article article = trader.createArticle(
//						user, offer, segment, product,
//						new ArticleCreator(tariff),
//						true, false);
//// auto-release must be controlled via the offer (the whole offer has an expiry time
////						new Date(System.currentTimeMillis() + 3600 * 1000 * 10)); // TODO the autoReleaseTimeout must come from the config
//				articles.add(article);
//			}

			pm.getFetchPlan().setMaxFetchDepth(maxFetchDepth);
			if (fetchGroups != null)
				pm.getFetchPlan().setGroups(fetchGroups);

			return pm.detachCopyAll(articles);
		} finally {
			pm.close();
		}
	}

	/**
	 * @ejb.interface-method
	 * @ejb.permission role-name="_Guest_"
	 */
	public List<SimpleProductType> getSimpleProductTypesForReseller(Collection<ProductTypeID> productTypeIDs, boolean includeChildrenRecursively)
	{
		PersistenceManager pm = getPersistenceManager();
		try {
			pm.getExtent(SimpleProductType.class);
			pm.getFetchPlan().setGroups(new String[] { FetchPlan.DEFAULT, FetchGroupsPriceConfig.FETCH_GROUP_EDIT });
			pm.getFetchPlan().setMaxFetchDepth(NLJDOHelper.MAX_FETCH_DEPTH_NO_LIMIT);
			pm.getFetchPlan().setDetachmentOptions(FetchPlan.DETACH_LOAD_FIELDS);

			List<SimpleProductType> res = new ArrayList<SimpleProductType>(productTypeIDs.size());
			for (ProductTypeID productTypeID : productTypeIDs) {
				SimpleProductType simpleProductType = (SimpleProductType) pm.getObjectById(productTypeID);

				// we need to strip off the nested product types (they're out of business ;-)
				// and we need to replace the price config - actually it should be sufficient to simply omit the inner price config
				// as the package price config contains only stable prices

				// we simply touch every field we need - the others should not be loaded and thus not detached then.
				simpleProductType.getName().getTexts();
//				simpleProductType.getPackagePriceConfig();
				simpleProductType.getOwner();
				simpleProductType.getExtendedProductType();

				// and detach
				simpleProductType = (SimpleProductType) pm.detachCopy(simpleProductType);

//				// TODO load CustomerGroups of the other customer-organisation
//				// and remove all prices from the package price config that are for
//				// different customer groups (not available to the client)
//				if (simpleProductType.getPackagePriceConfig() == null) {
//					// nothing to do
//				}
//				else if (simpleProductType.getPackagePriceConfig() instanceof GridPriceConfig) {
//					Set<CustomerGroupID> unavailableCustomerGroupIDs = new HashSet<CustomerGroupID>();
//					GridPriceConfig gridPriceConfig = (GridPriceConfig) simpleProductType.getPackagePriceConfig();
//					for (CustomerGroup customerGroup : gridPriceConfig.getCustomerGroups()) {
//					}
//	
//					for (CustomerGroupID customerGroupID : unavailableCustomerGroupIDs)
//						gridPriceConfig.removeCustomerGroup(customerGroupID.organisationID, customerGroupID.customerGroupID);
//				}
//				else
//					throw new IllegalStateException("SimpleProductType.packagePriceConfig unsupported! " + productTypeID);

				res.add(simpleProductType);
			}
			return res;
		} finally {
			pm.close();
		}
	}

	/**
	 * @ejb.interface-method
	 * @ejb.permission role-name="_Guest_"
	 * @ejb.transaction type="Required"
	 */
	public SimpleProductType importSimpleProductTypeForReselling(ProductTypeID productTypeID, String[] fetchGroups, int maxFetchDepth)
	throws JFireException
	{
		try {
			PersistenceManager pm = getPersistenceManager();
			try {
				Hashtable initialContextProperties = getInitialContextProperties(productTypeID.organisationID);

				PersistentNotificationEJB persistentNotificationEJB = PersistentNotificationEJBUtil.getHome(initialContextProperties).create();
				SimpleProductTypeNotificationFilter notificationFilter = new SimpleProductTypeNotificationFilter(
						productTypeID.organisationID, SubscriptionUtil.SUBSCRIBER_TYPE_ORGANISATION, getOrganisationID(),
						SimpleProductTypeNotificationFilter.class.getName());
				SimpleProductTypeNotificationReceiver notificationReceiver = new SimpleProductTypeNotificationReceiver(notificationFilter);
				notificationReceiver = (SimpleProductTypeNotificationReceiver) pm.makePersistent(notificationReceiver);
				persistentNotificationEJB.storeNotificationFilter(notificationFilter, false, null, 1);

				ArrayList<ProductTypeID> productTypeIDs = new ArrayList<ProductTypeID>(1);
				productTypeIDs.add(productTypeID);

				SimpleTradeManager simpleTradeManager = SimpleTradeManagerUtil.getHome(initialContextProperties).create();

				Collection<SimpleProductType> productTypes = simpleTradeManager.getSimpleProductTypesForReseller(productTypeIDs, true);
				if (productTypes.size() != 1)
					throw new IllegalStateException("productTypes.size() != 1");

				// currently we only support subscribing root-producttypes
				for (SimpleProductType productType : productTypes) {
					if (productType.getExtendedProductType() != null)
						throw new UnsupportedOperationException("The given SimpleProductType is not a root node (not yet supported!): " + productTypeID);
				}

				productTypes = pm.makePersistentAll(productTypes);
				return productTypes.iterator().next();
			} finally {
				pm.close();
			}
		} catch (Exception x) {
			logger.error("Import of SimpleProductType failed!", x);
			throw new JFireException(x);
		}
	}

//	/**
//	 * @ejb.interface-method
//	 * @ejb.permission role-name="_Guest_"
//	 * @ejb.transaction type="Required"
//	 */
//	public SimpleProductType backend_subscribe(ProductTypeID productTypeID, String[] fetchGroups, int maxFetchDepth)
//	{
//		PersistenceManager pm = getPersistenceManager();
//		try {
//			
//		} finally {
//			pm.close();
//		}
//	}

	private static Pattern tariffPKSplitPattern = null;

	/**
	 * @return a <tt>Collection</tt> of {@link TariffPricePair}
	 *
	 * @ejb.interface-method
	 * @ejb.transaction type="Supports"
	 * @ejb.permission role-name="_Guest_"
	 */
	public Collection<TariffPricePair> getTariffPricePairs(
			PriceConfigID priceConfigID, CustomerGroupID customerGroupID, CurrencyID currencyID,
			String[] tariffFetchGroups, String[] priceFetchGroups)
	{
		PersistenceManager pm = getPersistenceManager();
		try {
			if (tariffPKSplitPattern == null)
				tariffPKSplitPattern = Pattern.compile("/");

			// TODO use setResult and put all this logic into the JDO query!
			StablePriceConfig priceConfig = (StablePriceConfig) pm.getObjectById(priceConfigID);
			Collection priceCells = priceConfig.getPriceCells(
					CustomerGroup.getPrimaryKey(customerGroupID.organisationID, customerGroupID.customerGroupID),
					currencyID.currencyID);

			Collection<TariffPricePair> res = new ArrayList<TariffPricePair>();

			for (Iterator it = priceCells.iterator(); it.hasNext(); ) {
				PriceCell priceCell = (PriceCell) it.next();
				String tariffPK = priceCell.getPriceCoordinate().getTariffPK();
				String[] tariffPKParts = tariffPKSplitPattern.split(tariffPK);
				if (tariffPKParts.length != 2)
					throw new IllegalStateException("How the hell can it happen that the tariffPK does not consist out of two parts?");

				String tariffOrganisationID = tariffPKParts[0];
				long tariffID = Long.parseLong(tariffPKParts[1]);

				if (tariffFetchGroups != null)
					pm.getFetchPlan().setGroups(tariffFetchGroups);

				Tariff tariff = (Tariff) pm.getObjectById(TariffID.create(tariffOrganisationID, tariffID));
				tariff = (Tariff) pm.detachCopy(tariff);

				if (priceFetchGroups != null)
					pm.getFetchPlan().setGroups(priceFetchGroups);

				Price price = (Price) pm.detachCopy(priceCell.getPrice());

				res.add(new TariffPricePair(tariff, price));
			}

			return res;
		} finally {
			pm.close();
		}
	}

	/**
	 * @ejb.interface-method
	 * @ejb.transaction type="Required"
	 * @ejb.permission role-name="_Guest_"
	 */
	public Collection<GridPriceConfig> storePriceConfigs(Collection<GridPriceConfig> priceConfigs, boolean get) // , String[] fetchGroups, int maxFetchDepth)
	throws PriceCalculationException
	{
		PersistenceManager pm = getPersistenceManager();
		try {
			return GridPriceConfigUtil.storePriceConfigs(pm, priceConfigs, PriceCalculator.class, get); // , fetchGroups, maxFetchDepth);
		} finally {
			pm.close();
		}
	}

}
