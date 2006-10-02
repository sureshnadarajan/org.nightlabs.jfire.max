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

package org.nightlabs.jfire.accounting.tariffpriceconfig;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.jdo.JDODetachedFieldAccessException;
import javax.jdo.PersistenceManager;

import org.apache.log4j.Logger;
import org.nightlabs.ModuleException;
import org.nightlabs.jdo.NLJDOHelper;
import org.nightlabs.jfire.accounting.AccountingManagerBean;
import org.nightlabs.jfire.accounting.Currency;
import org.nightlabs.jfire.accounting.Price;
import org.nightlabs.jfire.accounting.PriceFragmentType;
import org.nightlabs.jfire.accounting.Tariff;
import org.nightlabs.jfire.accounting.id.CurrencyID;
import org.nightlabs.jfire.accounting.id.TariffID;
import org.nightlabs.jfire.accounting.priceconfig.id.PriceConfigID;
import org.nightlabs.jfire.base.BaseSessionBeanImpl;
import org.nightlabs.jfire.trade.CustomerGroup;
import org.nightlabs.jfire.trade.id.CustomerGroupID;

/**
 * @ejb.bean name="jfire/ejb/JFireTrade/TariffPriceConfigManager"	
 *           jndi-name="jfire/ejb/JFireTrade/TariffPriceConfigManager"
 *           type="Stateless" 
 *           transaction-type="Container"
 *
 * @ejb.util generate = "physical"
 */
public abstract class TariffPriceConfigManagerBean
extends BaseSessionBeanImpl
implements SessionBean
{
	/**
	 * LOG4J logger used by this class
	 */
	private static final Logger logger = Logger.getLogger(AccountingManagerBean.class);

	/**
	 * @see org.nightlabs.jfire.base.BaseSessionBeanImpl#setSessionContext(javax.ejb.SessionContext)
	 */
	public void setSessionContext(SessionContext sessionContext)
			throws EJBException, RemoteException
	{
		logger.debug(this.getClass().getName() + ".setSessionContext("+sessionContext+")");
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
	public void ejbCreate()
	throws CreateException
	{
		logger.debug(this.getClass().getName() + ".ejbCreate()");
	}
	/**
	 * @see javax.ejb.SessionBean#ejbRemove()
	 * 
	 * @ejb.permission unchecked="true"
	 */
	public void ejbRemove() throws EJBException, RemoteException
	{
		logger.debug(this.getClass().getName() + ".ejbRemove()");
	}
	
	/**
	 * @see javax.ejb.SessionBean#ejbActivate()
	 */
	public void ejbActivate() throws EJBException, RemoteException
	{
		logger.debug(this.getClass().getName() + ".ejbActivate()");
	}
	/**
	 * @see javax.ejb.SessionBean#ejbPassivate()
	 */
	public void ejbPassivate() throws EJBException, RemoteException
	{
		logger.debug(this.getClass().getName() + ".ejbPassivate()");
	}

	public static void logTariffPriceConfig(TariffPriceConfig priceConfig)
	{
		if (!logger.isDebugEnabled())
			return;

		try {
			logger.debug("priceConfig="+priceConfig.getPrimaryKey() + " priceConfig.class=" + priceConfig.getClass().getName());
			for (Currency currency : priceConfig.getCurrencies()) {
				logger.debug("  currency=" + currency.getCurrencyID() + " ("+currency.getCurrencySymbol()+")");
				for (CustomerGroup customerGroup : priceConfig.getCustomerGroups()) {
					logger.debug("    customerGroup=" + customerGroup.getPrimaryKey() + " ("+customerGroup.getName().getText(Locale.ENGLISH.getLanguage())+")");
					for (Tariff tariff : priceConfig.getTariffs()) {
						logger.debug("      tariff=" + tariff.getPrimaryKey() + " ("+tariff.getName().getText(Locale.ENGLISH.getLanguage())+")");
						PriceCoordinate priceCoordinate = new PriceCoordinate(customerGroup.getPrimaryKey(), tariff.getPrimaryKey(), currency.getCurrencyID());
						if (priceConfig instanceof IFormulaPriceConfig) {
							FormulaCell formulaCell = ((IFormulaPriceConfig)priceConfig).getFormulaCell(priceCoordinate, false);
							for (PriceFragmentType priceFragmentType : priceConfig.getPriceFragmentTypes()) {
								String formula = formulaCell == null ? null : formulaCell.getFormula(priceFragmentType);
								logger.debug("        priceFragmentType=" + priceFragmentType.getPrimaryKey() + " ("+priceFragmentType.getName().getText(Locale.ENGLISH.getLanguage())+") formula=" + formula);
							}
						}
						else if (priceConfig instanceof IResultPriceConfig) {
							PriceCell priceCell = ((IResultPriceConfig)priceConfig).getPriceCell(priceCoordinate, false);
							Price price = priceCell == null ? null : priceCell.getPrice();
							for (PriceFragmentType priceFragmentType : priceConfig.getPriceFragmentTypes()) {
								String amount = price == null ? null : String.valueOf(price.getAmount(priceFragmentType));
								logger.debug("        priceFragmentType=" + priceFragmentType.getPrimaryKey() + " ("+priceFragmentType.getName().getText(Locale.ENGLISH.getLanguage())+") amount=" + amount);
							}
						}
					}
				}
			}
			
			if (priceConfig instanceof IFormulaPriceConfig) {
				FormulaCell formulaCell = ((IFormulaPriceConfig)priceConfig).getFallbackFormulaCell(false);
				if (formulaCell == null)
					logger.debug("  fallbackFormulaCell is null");
				else {
					logger.debug("  fallbackFormulaCell:");
					for (PriceFragmentType priceFragmentType : priceConfig.getPriceFragmentTypes()) {
						String formula = formulaCell == null ? null : formulaCell.getFormula(priceFragmentType);
						logger.debug("    priceFragmentType=" + priceFragmentType.getPrimaryKey() + " ("+priceFragmentType.getName().getText(Locale.ENGLISH.getLanguage())+") formula=" + formula);
					}
				}
			}
		} catch (JDODetachedFieldAccessException x) {
			logger.warn("Could not traverse PriceConfig", x);
		}
	}

	/**
	 * @throws ModuleException
	 *
	 * @ejb.interface-method
	 * @ejb.transaction type = "Required"
	 * @ejb.permission role-name="_Guest_"
	 */
	public TariffPriceConfig storePriceConfig(TariffPriceConfig priceConfig, boolean get, String[] fetchGroups, int maxFetchDepth)
	throws ModuleException
	{
		PersistenceManager pm = getPersistenceManager();
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("storePriceConfig: PriceConfig BEFORE attach:");
				logTariffPriceConfig(priceConfig);
			}

			return (TariffPriceConfig) NLJDOHelper.storeJDO(pm, priceConfig, get, fetchGroups, maxFetchDepth);
		} finally {
			pm.close();
		}
	}

	protected static Pattern tariffPKSplitPattern = null;

	/**
	 * @return a <tt>Collection</tt> of {@link TariffPricePair}
	 * 
	 * @throws ModuleException
	 *
	 * @ejb.interface-method
	 * @ejb.transaction type = "Required"
	 * @ejb.permission role-name="_Guest_"
	 */
	public Collection getTariffPricePairs(
			PriceConfigID priceConfigID, CustomerGroupID customerGroupID, CurrencyID currencyID,
			String[] tariffFetchGroups, String[] priceFetchGroups)
	throws ModuleException
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

			Collection res = new ArrayList();

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
}
