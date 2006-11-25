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

package org.nightlabs.jfire.chezfrancois;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.nightlabs.ModuleException;
import org.nightlabs.i18n.I18nText;
import org.nightlabs.jfire.accounting.Account;
import org.nightlabs.jfire.accounting.Accounting;
import org.nightlabs.jfire.accounting.Currency;
import org.nightlabs.jfire.accounting.PriceFragmentType;
import org.nightlabs.jfire.accounting.Tariff;
import org.nightlabs.jfire.accounting.book.fragmentbased.PFMoneyFlowMapping;
import org.nightlabs.jfire.accounting.id.CurrencyID;
import org.nightlabs.jfire.accounting.id.PriceFragmentTypeID;
import org.nightlabs.jfire.accounting.priceconfig.IInnerPriceConfig;
import org.nightlabs.jfire.accounting.priceconfig.PriceConfig;
import org.nightlabs.jfire.accounting.tariffpriceconfig.FormulaCell;
import org.nightlabs.jfire.accounting.tariffpriceconfig.FormulaPriceConfig;
import org.nightlabs.jfire.accounting.tariffpriceconfig.PriceCalculator;
import org.nightlabs.jfire.accounting.tariffpriceconfig.StablePriceConfig;
import org.nightlabs.jfire.idgenerator.IDGenerator;
import org.nightlabs.jfire.organisation.Organisation;
import org.nightlabs.jfire.person.Person;
import org.nightlabs.jfire.person.PersonStruct;
import org.nightlabs.jfire.prop.IStruct;
import org.nightlabs.jfire.prop.Struct;
import org.nightlabs.jfire.prop.TextDataField;
import org.nightlabs.jfire.prop.exception.DataBlockGroupNotFoundException;
import org.nightlabs.jfire.prop.exception.DataBlockNotFoundException;
import org.nightlabs.jfire.prop.exception.DataFieldNotFoundException;
import org.nightlabs.jfire.security.SecurityException;
import org.nightlabs.jfire.security.User;
import org.nightlabs.jfire.security.UserLocal;
import org.nightlabs.jfire.simpletrade.store.SimpleProductType;
import org.nightlabs.jfire.simpletrade.store.SimpleProductTypeActionHandler;
import org.nightlabs.jfire.store.ProductType;
import org.nightlabs.jfire.store.Store;
import org.nightlabs.jfire.store.id.ProductTypeID;
import org.nightlabs.jfire.trade.CustomerGroup;
import org.nightlabs.jfire.trade.LegalEntity;
import org.nightlabs.jfire.trade.Order;
import org.nightlabs.jfire.trade.OrganisationLegalEntity;
import org.nightlabs.jfire.trade.SegmentType;
import org.nightlabs.jfire.trade.Trader;

public class DataCreator
{
	private String languageID = "en";
	private String[] languages = new String[] {
			"en",
			Locale.GERMAN.getLanguage(),
			Locale.FRENCH.getLanguage()
	};

	private PersistenceManager pm;
	private String organisationID;
	private User user;
	private SimpleProductType rootSimpleProductType;
	private Store store;
	private Accounting accounting;
	private String rootOrganisationID;

	public SimpleProductType getRootSimpleProductType()
	{
		return rootSimpleProductType;
	}

	public DataCreator(User user)
	{
		this.user = user;
		this.organisationID = user.getOrganisationID();
		this.pm = JDOHelper.getPersistenceManager(user);
		
		pm.getExtent(SimpleProductType.class);
		rootSimpleProductType = (SimpleProductType) pm.getObjectById(
				ProductTypeID.create(organisationID, SimpleProductType.class.getName()));

		store = Store.getStore(pm);
		accounting = Accounting.getAccounting(pm);

		try {
			InitialContext ctx = new InitialContext();
			try {
				rootOrganisationID = Organisation.getRootOrganisationID(ctx);
			} finally {
				ctx.close();
			}
		} catch (NamingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Set names for different languages. The order for language
	 * entries is defined by {@link #languages}.
	 * @param names Names in different languages.
	 * @param name The i18n text object to set.
	 */
	protected void setNames(I18nText name, String[] names)
	{
		int langIdx = 0;
		for (String string : names) {
			if(langIdx >= languages.length)
				break;
			name.setText(languages[langIdx], string);
			langIdx++;
		}
	}
	
	protected SimpleProductType createCategory(SimpleProductType parent, String productTypeID, String ... names)
	{
		if (parent == null)
			parent = rootSimpleProductType;

		try {
			return (SimpleProductType) pm.getObjectById(ProductTypeID.create(organisationID, productTypeID));
		} catch (JDOObjectNotFoundException x) {
			// not yet existent => create the object!
		}

		SimpleProductType pt = new SimpleProductType(
				organisationID, productTypeID, parent, null, 
				ProductType.INHERITANCE_NATURE_BRANCH, ProductType.PACKAGE_NATURE_OUTER);
		setNames(pt.getName(), names);

		store.addProductType(user, pt, SimpleProductTypeActionHandler.getDefaultHome(pm, pt));
		store.setProductTypeStatus_published(user, pt);

		return pt;
	}

	private List<SimpleProductType> createdLeafs = new ArrayList<SimpleProductType>(); 

	public SimpleProductType createLeaf(SimpleProductType category, String productTypeID, IInnerPriceConfig innerPriceConfig, String ... names)
	{
		SimpleProductType pt = new SimpleProductType(
				organisationID, productTypeID, category, null, ProductType.INHERITANCE_NATURE_LEAF, ProductType.PACKAGE_NATURE_OUTER);
		setNames(pt.getName(), names);
		pt.setPackagePriceConfig(new StablePriceConfig(organisationID, PriceConfig.createPriceConfigID()));
		pt.setInnerPriceConfig(innerPriceConfig);
		store.addProductType(user, pt, SimpleProductTypeActionHandler.getDefaultHome(pm, pt));

		store.setProductTypeStatus_published(user, pt);
		store.setProductTypeStatus_confirmed(user, pt);
		store.setProductTypeStatus_saleable(user, pt, true);
		
		createdLeafs.add(pt);

		return pt;
	}
	
	public void calculatePrices()
	throws ModuleException
	{
		for (SimpleProductType pt : createdLeafs) {
			if (pt.getInnerPriceConfig() != null && pt.getPackagePriceConfig() != null)
				((StablePriceConfig)pt.getPackagePriceConfig()).adoptParameters(pt.getInnerPriceConfig());

			PriceCalculator priceCalculator = new PriceCalculator(pt);
			priceCalculator.preparePriceCalculation();
			priceCalculator.calculatePrices();
		}
	}

	public IInnerPriceConfig createInnerPercentagePriceConfig(String name, int percentage, ProductType packageProductType)
	{
		FormulaPriceConfig formulaPriceConfig = new FormulaPriceConfig(organisationID, PriceConfig.createPriceConfigID());
		PriceFragmentType vatNet = (PriceFragmentType) pm.getObjectById(PriceFragmentTypeID.create(rootOrganisationID, "vat-de-16-net"));
		PriceFragmentType vatVal = (PriceFragmentType) pm.getObjectById(PriceFragmentTypeID.create(rootOrganisationID, "vat-de-16-val"));
		formulaPriceConfig.addProductType(packageProductType);
		formulaPriceConfig.addPriceFragmentType(vatVal);
		formulaPriceConfig.addPriceFragmentType(vatNet);
		formulaPriceConfig.getName().setText(languageID, name);
		FormulaCell fallbackFormulaCell = formulaPriceConfig.createFallbackFormulaCell();
		fallbackFormulaCell.setFormula(
				Organisation.DEVIL_ORGANISATION_ID,
				PriceFragmentType.TOTAL_PRICEFRAGMENTTYPEID,
				"cell.resolvePriceCellsAmount(\n" +
				"	new AbsolutePriceCoordinate(\n" +
				"		null,\n" +
				"		null,\n" +
				"		null,\n" +
				"		\""+packageProductType.getPrimaryKey()+"\",\n" +
				"		null\n" +
				"	)\n" +
				") * 0.1;");

		fallbackFormulaCell.setFormula(vatNet, "cell.resolvePriceCellsAmount(\n" +
				"	new AbsolutePriceCoordinate(\n" +
				"		null,\n" +
				"		null,\n" +
				"		null,\n" +
				"		null,\n" +
				"		\""+Organisation.DEVIL_ORGANISATION_ID+"/_Total_\"\n" +
				"	)\n"+
				") / 1.16;");
		fallbackFormulaCell.setFormula(vatVal, "cell.resolvePriceCellsAmount(\n" +
				"	new AbsolutePriceCoordinate(\n" +
				"		null,\n" +
				"		null,\n" +
				"		null,\n" +
				"		null,\n" +
				"		\""+Organisation.DEVIL_ORGANISATION_ID+"/_Total_\"\n" +
				"	)\n"+
				")\n" +
				
//					"/ 1.16 * 0.16;");
				
				"\n" +
				"-\n" +
				"\n" +
				"cell.resolvePriceCellsAmount(\n" +
				"	new AbsolutePriceCoordinate(\n" +
				"		null,\n" +
				"		null,\n" +
				"		null,\n" +
				"		null,\n" +
				"		\""+rootOrganisationID+"/vat-de-16-net\"\n" +
				"	)\n"+
				");");

		return formulaPriceConfig;
	}

	public IInnerPriceConfig createFixPriceConfig(Tariff[] tariffs, long[] prices, String ... names)
	{
		String[] formulas = new String[prices.length];
		for (int i = 0; i < prices.length; i++) {
			long price = prices[i];
			formulas[i] = String.valueOf(price);
		}
		return createFormulaPriceConfig(tariffs, formulas, names);
	}

	private Currency euro = null;	
	protected Currency getCurrencyEUR()
	{
		if (euro == null) {
			pm.getExtent(Currency.class);
			euro = (Currency) pm.getObjectById(CurrencyID.create("EUR"));
		}

		return euro;
	}

	private PriceFragmentType priceFragmentTypeTotal = null;
	public PriceFragmentType getPriceFragmentTypeTotal()
	{
		if (priceFragmentTypeTotal == null)
			priceFragmentTypeTotal = PriceFragmentType.getTotalPriceFragmentType(pm);

		return priceFragmentTypeTotal;
	}

	private PriceFragmentType priceFragmentTypeVatNet = null;
	public PriceFragmentType getPriceFragmentTypeVatNet() {
		if (priceFragmentTypeVatNet == null)
			priceFragmentTypeVatNet = (PriceFragmentType) pm.getObjectById(PriceFragmentTypeID.create(rootOrganisationID, "vat-de-16-net"));

		return priceFragmentTypeVatNet;			
	}

	private PriceFragmentType priceFragmentTypeVatVal = null;
	public PriceFragmentType getPriceFragmentTypeVatVal() {
		if (priceFragmentTypeVatVal == null)
			priceFragmentTypeVatVal = (PriceFragmentType) pm.getObjectById(PriceFragmentTypeID.create(rootOrganisationID, "vat-de-16-val"));

		return priceFragmentTypeVatVal;
	}

	public IInnerPriceConfig createFormulaPriceConfig(Tariff[] tariffs, String[] formulas, String ... names)
	{
		Currency euro = getCurrencyEUR();

//	 create the price config "Car - Middle Class"
		PriceFragmentType totalPriceFragmentType = getPriceFragmentTypeTotal();
		PriceFragmentType vatNet = getPriceFragmentTypeVatNet();
		PriceFragmentType vatVal = getPriceFragmentTypeVatVal();

		Accounting accounting = Accounting.getAccounting(pm);
		Trader trader = Trader.getTrader(pm);
		StablePriceConfig stablePriceConfig = new StablePriceConfig(organisationID, PriceConfig.createPriceConfigID());
		FormulaPriceConfig formulaPriceConfig = new FormulaPriceConfig(organisationID, PriceConfig.createPriceConfigID());
		setNames(formulaPriceConfig.getName(), names);
		
		CustomerGroup customerGroupDefault = trader.getDefaultCustomerGroupForKnownCustomer();
		CustomerGroup customerGroupAnonymous = LegalEntity.getAnonymousCustomer(pm).getDefaultCustomerGroup();
		formulaPriceConfig.addCustomerGroup(customerGroupDefault);
		formulaPriceConfig.addCustomerGroup(customerGroupAnonymous);
		formulaPriceConfig.addCurrency(euro);
		for (int i = 0; i < tariffs.length; i++) {
			Tariff tariff = tariffs[i];
			formulaPriceConfig.addTariff(tariff);			
		}
//			formulaPriceConfig.addProductType(rootSimpleProductType);
		formulaPriceConfig.addPriceFragmentType(totalPriceFragmentType);
		formulaPriceConfig.addPriceFragmentType(vatNet);
		formulaPriceConfig.addPriceFragmentType(vatVal);
		stablePriceConfig.adoptParameters(formulaPriceConfig);

		FormulaCell fallbackFormulaCell = formulaPriceConfig.createFallbackFormulaCell();
		fallbackFormulaCell.setFormula(totalPriceFragmentType,
				"cell.resolvePriceCellsAmount(\n" +
				"	new AbsolutePriceCoordinate(\n" +
				"		\""+organisationID+"/"+CustomerGroup.CUSTOMER_GROUP_ID_DEFAULT+"\",\n" +
				"		null,\n" +
				"		null,\n" +
				"		null,\n" +
				"		null\n" +
				"	)\n" +
				");");
		fallbackFormulaCell.setFormula(vatNet, "cell.resolvePriceCellsAmount(\n" +
				"	new AbsolutePriceCoordinate(\n" +
				"		null,\n" +
				"		null,\n" +
				"		null,\n" +
				"		null,\n" +
				"		\""+Organisation.DEVIL_ORGANISATION_ID+"/_Total_\"\n" +
				"	)\n" +
				") / 1.16;");
		fallbackFormulaCell.setFormula(vatVal, "cell.resolvePriceCellsAmount(\n" +
				"	new AbsolutePriceCoordinate(\n" +
				"		null,\n" +
				"		null,\n" +
				"		null,\n" +
				"		null,\n" +
				"		\""+Organisation.DEVIL_ORGANISATION_ID+"/_Total_\"\n" +
				"	)\n" +
				")\n" +

//					"/ 1.16 * 0.16");

				"\n" +
				"-\n" +
				"\n" +
				"cell.resolvePriceCellsAmount(\n" +
				"	new AbsolutePriceCoordinate(\n" +
				"		null,\n" +
				"		null,\n" +
				"		null,\n" +
				"		null,\n" +
				"		\""+rootOrganisationID+"/vat-de-16-net\"\n" +
				"	)\n" +
				");");

		for (int i = 0; i < formulas.length; i++) {
			String formula = formulas[i];
			Tariff tariff = tariffs[i];
		
			FormulaCell cell = formulaPriceConfig.createFormulaCell(customerGroupDefault, tariff, euro);
			cell.setFormula(totalPriceFragmentType, formula);
		}

		return formulaPriceConfig;
	}

	private OrganisationLegalEntity organisationLegalEntity = null;
	protected OrganisationLegalEntity getOrganisationLegalEntity()
	{
		if (organisationLegalEntity == null)
			organisationLegalEntity = OrganisationLegalEntity.getOrganisationLegalEntity(
				pm, organisationID, OrganisationLegalEntity.ANCHOR_TYPE_ID_ORGANISATION, true);

		return organisationLegalEntity;
	}

	public Account createLocalAccount(String anchorID, String name)
	{
		Currency euro = getCurrencyEUR();

		Account account = new Account(
				organisationID, Account.ANCHOR_TYPE_ID_LOCAL_NORMAL, anchorID, organisationLegalEntity, euro);
		account.getName().setText(languageID, name);

		pm.makePersistent(account);

		return account;
	}

	public PFMoneyFlowMapping createPFMoneyFlowMapping(
			ProductType productType, PriceFragmentType priceFragmentType, Account account)
	{
		Currency euro = getCurrencyEUR();
		PFMoneyFlowMapping mapping = new PFMoneyFlowMapping(
				organisationID,
				accounting.createMoneyFlowMappingID(),
				productType.getPrimaryKey(),
				PFMoneyFlowMapping.PACKAGE_TYPE_PACKAGE,
				priceFragmentType.getPrimaryKey(),
				euro.getCurrencyID()
		);
		mapping.setOwnerPK(getOrganisationLegalEntity().getPrimaryKey());
		mapping.setSourceOrganisationID(organisationID);
		mapping.setAccountPK(account.getPrimaryKey());
//
//		pm.makePersistent(mapping);
		return mapping;
	}

	public User createUser(
			String userID, String password,
			String personCompany, String personName, String personFirstName, String personEMail)
	throws
		SecurityException, DataBlockNotFoundException, DataBlockGroupNotFoundException, DataFieldNotFoundException
	{
		User user = new User(organisationID, userID);
		UserLocal userLocal = new UserLocal(user);
		userLocal.setPasswordPlain(password);

		Person person = createPerson(personCompany, personName, personFirstName, personEMail);
		user.setPerson(person);
//		personStruct.implodePerson(person);
		pm.makePersistent(user);
		return user;
	}

	private IStruct personStruct = null;
	protected IStruct getPersonStruct()
	{
		if (personStruct == null)
			personStruct = Struct.getStruct(getOrganisationLegalEntity().getOrganisationID(), Person.class, pm);

		return personStruct;
	}

	public Person createPerson(
			String company, String name, String firstName, String eMail)
	throws DataBlockNotFoundException, DataBlockGroupNotFoundException, DataFieldNotFoundException
	{
		IStruct personStruct = getPersonStruct();
		Person person = new Person(IDGenerator.getOrganisationID(), IDGenerator.nextID(Person.class));
		personStruct.explodeProperty(person);
		((TextDataField)person.getDataField(PersonStruct.PERSONALDATA_COMPANY)).setText(company);
		((TextDataField)person.getDataField(PersonStruct.PERSONALDATA_NAME)).setText(name);
		((TextDataField)person.getDataField(PersonStruct.PERSONALDATA_FIRSTNAME)).setText(firstName);
		((TextDataField)person.getDataField(PersonStruct.INTERNET_EMAIL)).setText(eMail);
		person.setAutoGenerateDisplayName(true);
		person.setDisplayName(null, personStruct);
		personStruct.implodeProperty(person);
		pm.makePersistent(person);
		return person;
	}

	public LegalEntity createLegalEntity(Person person)
	{
		LegalEntity legalEntity = new LegalEntity(
				person.getOrganisationID(), LegalEntity.ANCHOR_TYPE_ID_PARTNER, Long.toHexString(person.getPropertyID()));
		legalEntity.setPerson(person);
		pm.makePersistent(legalEntity);
		return legalEntity;
	}

	public Order createOrderForEndcustomer(LegalEntity customer)
	throws ModuleException
	{
		Trader trader = Trader.getTrader(pm);
		Order order = trader.createOrder(trader.getMandator(), customer, null, getCurrencyEUR());
		trader.createSegment(order, SegmentType.getDefaultSegmentType(pm));
		return order;
	}

}
