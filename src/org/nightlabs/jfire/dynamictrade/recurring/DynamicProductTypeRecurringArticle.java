package org.nightlabs.jfire.dynamictrade.recurring;

import javax.jdo.JDOHelper;

import org.nightlabs.jfire.accounting.Price;
import org.nightlabs.jfire.accounting.Tariff;
import org.nightlabs.jfire.dynamictrade.DynamicProductInfo;
import org.nightlabs.jfire.security.User;
import org.nightlabs.jfire.store.ProductType;
import org.nightlabs.jfire.store.Unit;
import org.nightlabs.jfire.store.id.UnitID;
import org.nightlabs.jfire.trade.Article;
import org.nightlabs.jfire.trade.Offer;
import org.nightlabs.jfire.trade.Segment;

/**
 *
 * @author Fitas Amine <fitas@nightlabs.de>
 *
 * @jdo.persistence-capable
 *		identity-type="application"
 *		persistence-capable-superclass="org.nightlabs.jfire.trade.Article"
 *		detachable="true"
 *		table="JFireDynamicTrade_DynamicProductTypeRecurringArticle"
 *
 * @jdo.inheritance strategy="new-table"
 *
 *
 * @jdo.fetch-group name="DynamicProductTypeRecurringArticle.name" fields="name"
 * @jdo.fetch-group name="FetchGroupsTrade.articleInOrderEditor" fields="quantity, unit, name, singlePrice"
 * @jdo.fetch-group name="FetchGroupsTrade.articleInOfferEditor" fields="quantity, unit, name, singlePrice"
 */
public class DynamicProductTypeRecurringArticle extends Article implements DynamicProductInfo {


	public static final String FETCH_GROUP_DYNAMIC_PRODUCT_TYPE_RECURRING_ARTICLE_NAME = "DynamicProductTypeRecurringArticle.name";

	private static final long serialVersionUID = 1L;

	/**
	 * @deprecated Only for JDO!
	 */
	@Deprecated
	protected DynamicProductTypeRecurringArticle() {}

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private long quantity;

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private Unit unit;

	/**
	 * @jdo.field persistence-modifier="persistent" mapped-by="dynamicProductTypeRecurringArticle"
	 */
	private DynamicProductTypeRecurringArticleName 	name;
	
	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private Price singlePrice;
	
	public DynamicProductTypeRecurringArticle(User user, Offer offer, Segment segment, long articleID, ProductType productType, Tariff tariff)
	{
		super(user, offer, segment, articleID, productType, null, tariff);
		this.name = new DynamicProductTypeRecurringArticleName(this);
	}


	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.dynamictrade.recurring.DynamicProductInfo#getQuantity()
	 */
	public long getQuantity() {
		return quantity;
	}


	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.dynamictrade.recurring.DynamicProductInfo#setQuantity(long)
	 */
	public void setQuantity(long quantity) {
		this.quantity = quantity;
	}


	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.dynamictrade.recurring.DynamicProductInfo#getUnit()
	 */
	public Unit getUnit() {
		return unit;
	}


	public UnitID getUnitID() {
		return (UnitID) JDOHelper.getObjectId(unit);
	}
	
	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.dynamictrade.recurring.DynamicProductInfo#setUnit(org.nightlabs.jfire.store.Unit)
	 */
	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public DynamicProductTypeRecurringArticleName getName() {
		return name;
	}


	public void setName(DynamicProductTypeRecurringArticleName name) {
		this.name = name;
	}


	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.dynamictrade.recurring.DynamicProductInfo#getSinglePrice()
	 */
	public Price getSinglePrice() {
		return singlePrice;
	}


	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.dynamictrade.recurring.DynamicProductInfo#setSinglePrice(org.nightlabs.jfire.accounting.Price)
	 */
	public void setSinglePrice(Price singlePrice) {
		this.singlePrice = singlePrice;
	}

	@Override
	public double getQuantityAsDouble()
	{
		return unit.toDouble(quantity);
	}


}
