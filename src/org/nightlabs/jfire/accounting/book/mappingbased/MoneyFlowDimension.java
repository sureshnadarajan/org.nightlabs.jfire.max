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

package org.nightlabs.jfire.accounting.book.mappingbased;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.nightlabs.jfire.store.ProductType;
import org.nightlabs.jfire.trade.Article;

/**
 * Abstract Dimension for MoneyFlowMappings.
 * 
 * @author Alexander Bieber <alex[AT]nightlabs[DOT]de>
 *
 * @jdo.persistence-capable
 *		identity-type="application"
 *		objectid-class="org.nightlabs.jfire.accounting.book.id.MoneyFlowDimensionID"
 *		detachable="true"
 *		table="JFireTrade_MoneyFlowDimension"
 *
 * @jdo.inheritance strategy="new-table"
 * 
 * @jdo.create-objectid-class
 *
 * @jdo.query
 *		name="getMoneyFlowDimension"
 *		query="SELECT UNIQUE this
 *			WHERE moneyFlowDimensionID == paramMoneyFlowDimensionID
 *			PARAMETERS String paramMoneyFlowDimensionID
 *			import java.lang.String"
 */
public abstract class MoneyFlowDimension {

	private static final String FETCH_GROUP_GET_MONEY_FLOW_DIMENSION = "getMoneyFlowDimension";
	
	/**
	 * @jdo.field persistence-modifier="persistent" primary-key="true"
	 * @jdo.column length="200"
	 */
	private String moneyFlowDimensionID;
	
	public MoneyFlowDimension() {
		this.moneyFlowDimensionID = getMoneyFlowDimensionID();
	}
	
	/**
	 * Returns the ID of this MoneyFlowDimension.
	 */
	public abstract String getMoneyFlowDimensionID();

	/**
	 * Returns all possible values this dimension might have.
	 * @param bookArticle TODO
	 */
	public abstract String[] getValues(ProductType productType, Article bookArticle);
	
	
	/**
	 * Returns the MoneyFlowDimension with the given organisationID and
	 * moneyFlowDimensionID.
	 * 
	 * @param pm The PersistenceManager to use.
	 * @param organisationID The organisationID of the dimension.
	 * @param moneyFlowDimensionID The moneyFlowDimensionID of the dimension.
	 * @return The MoneyFlowDimension with for the given keys.
	 */
	public static MoneyFlowDimension getMoneyFlowDimension(PersistenceManager pm, String moneyFlowDimensionID) {
		Query q = pm.newNamedQuery(MoneyFlowDimension.class, FETCH_GROUP_GET_MONEY_FLOW_DIMENSION);
		return (MoneyFlowDimension)q.execute(moneyFlowDimensionID);
	}
	
}
