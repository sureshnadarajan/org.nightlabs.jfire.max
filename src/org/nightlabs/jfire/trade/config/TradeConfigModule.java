package org.nightlabs.jfire.trade.config;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.nightlabs.annotation.Implement;
import org.nightlabs.jfire.accounting.Currency;
import org.nightlabs.jfire.accounting.Invoice;
import org.nightlabs.jfire.accounting.id.CurrencyID;
import org.nightlabs.jfire.config.ConfigModule;
import org.nightlabs.jfire.idgenerator.IDGenerator;
import org.nightlabs.jfire.store.DeliveryNote;
import org.nightlabs.jfire.trade.Offer;
import org.nightlabs.jfire.trade.Order;

/**
 * @author Marco Schulze - marco at nightlabs dot de
 *
 *  @jdo.persistence-capable
 *		identity-type="application"
 *		persistence-capable-superclass="org.nightlabs.jfire.config.ConfigModule"
 *		detachable="true"
 *		table="JFireTrade_TradeConfigModule"
 *
 * @jdo.inheritance strategy = "new-table"
 *
 * @jdo.fetch-group name="TradeConfigModule.currency" fields="currency"
 * @jdo.fetch-group name="TradeConfigModule.idPrefixCfs" fields="idPrefixCfs"
 */
public class TradeConfigModule
		extends ConfigModule
{
	private static final long serialVersionUID = 1L;

	public static final String FETCH_GROUP_CURRENCY = "TradeConfigModule.currency";
	public static final String FETCH_GROUP_ID_PREFIX_CFS = "TradeConfigModule.idPrefixCfs";

	/**
	 * Which currency is used by the user group / user.
	 * TODO Maybe we should store the Currency per Organisation, because usually all accounting activities are
	 * performed in ONE currency?!
	 * IMHO we need one Currency per Organisation specifying which currency is used for the accounting and then
	 * we still need somehow different currencies for the users and even maybe the possibility to change the currency
	 * while being in a sale process.
	 */
	private Currency currency;

	/**
	 * key: String articleContainerClassName <br/>
	 * value: IDPrefixCf
	 *
	 * @jdo.field
	 *		persistence-modifier="persistent"
	 *		collection-type="map"
	 *		key-type="java.lang.String"
	 *		value-type="IDPrefixCf"
	 *		table="JFireTrade_TradeConfigModule_idPrefixCfs"
	 *		null-value="exception"
	 *		@!mapped-by="tradeConfigModule" // not possible because of inheritance - we don't want to copy IDPrefixCfs!
	 *
	 * @jdo.join
	 */
	private Map<String, IDPrefixCf> idPrefixCfs = null;

	public TradeConfigModule() { }

	@Override
	@Implement
	public void init() {
		PersistenceManager pm = JDOHelper.getPersistenceManager(this);
		if (pm == null)
			throw new IllegalStateException("JDOHelper.getPersistenceManager(this) returned null!");

		idPrefixCfs = new HashMap<String, IDPrefixCf>();

		try {
			this.currency = (Currency) pm.getObjectById(CurrencyID.create("EUR"));
		} catch (JDOObjectNotFoundException x) {
			// EUR does not exist - initialise below with the first one that exists
		}

		if (this.currency == null) {
			Query q = pm.newQuery(Currency.class);
			q.setOrdering("this.currencyID ASCENDING"); // we sort it to ensure that it's always the same
			Collection<?> c = (Collection<?>) q.execute();
			Iterator<?> it = c.iterator();
			if (it.hasNext())
				this.currency = (Currency) it.next();
			else
				throw new IllegalStateException("There is no Currency in the datastore! Cannot initialise TradeConfigModule!");
		}
	}

	public CurrencyID getCurrencyID()
	{
		if (this.currency == null)
			return null;

		CurrencyID currencyID = (CurrencyID) JDOHelper.getObjectId(this.currency);
		if (currencyID == null)
			throw new IllegalStateException("JDOHelper.getObjectId(this.currency) returned null!");

		return currencyID;
	}

	public Currency getCurrency()
	{
		return currency;
	}
	public void setCurrency(Currency currency)
	{
		this.currency = currency;
	}

	/**
	 * Creates only if necessary - if it already exists, the previously created IDPrefixCf will be returned.
	 *
	 * @param articleContainerClassName The class name of the articleContainer for which to fetch the
	 *		settings or {@link IDPrefixCf#ARTICLE_CONTAINER_CLASS_NAME_GLOBAL } for the global ones (if you don't want different settings for order/offer/invoice/deliveryNote).
	 * @return Returns the desired IDPrefixCf assigned to the given <code>articleContainerClassName</code>.
	 */
	public IDPrefixCf createIDPrefixCf(String articleContainerClassName)
	{
		IDPrefixCf res = getIDPrefixCf(articleContainerClassName, false);
		if (res == null) {
			res = new IDPrefixCf(
					IDGenerator.getOrganisationID(),
					IDGenerator.nextID(IDPrefixCf.class),
					this, articleContainerClassName);
			idPrefixCfs.put(res.getArticleContainerClassName(), res);
		}
		return res;
	}

	/**
	 * This method first looks, whether an IDPrefixCf exists for the given articleContainerClassName.
	 * If not, it will return the one for {@link IDPrefixCf#ARTICLE_CONTAINER_CLASS_NAME_GLOBAL } (which is
	 * created if necessary).
	 *
	 * @param articleContainerClassName
	 * @param throwExceptionIfNotFound
	 * @return Returns an instance of {@link IDPrefixCf} - never returns <code>null</code>.
	 */
	public IDPrefixCf getActiveIDPrefixCf(String articleContainerClassName)
	{
		IDPrefixCf cf = getIDPrefixCf(articleContainerClassName, false);
		if (cf == null) {
			cf = createIDPrefixCf(IDPrefixCf.ARTICLE_CONTAINER_CLASS_NAME_GLOBAL);
		}
		return cf;
	}

	public IDPrefixCf getIDPrefixCf(String articleContainerClassName, boolean throwExceptionIfNotFound)
	{
		if (!IDPrefixCf.ARTICLE_CONTAINER_CLASS_NAME_GLOBAL.equals(articleContainerClassName) &&
				!Order.class.getName().equals(articleContainerClassName) &&
				!Offer.class.getName().equals(articleContainerClassName) &&
				!Invoice.class.getName().equals(articleContainerClassName) &&
				!DeliveryNote.class.getName().equals(articleContainerClassName))
			throw new IllegalArgumentException("articleContainerClassName invalid!");

		IDPrefixCf res = idPrefixCfs.get(articleContainerClassName);
		if (res == null && throwExceptionIfNotFound)
			throw new IllegalStateException("No IDPrefixCf registered for articleContainerClassName=\"" + articleContainerClassName + "\"");

		return res;
	}

}
