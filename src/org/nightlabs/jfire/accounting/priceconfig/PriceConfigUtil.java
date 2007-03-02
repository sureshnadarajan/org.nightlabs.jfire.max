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

package org.nightlabs.jfire.accounting.priceconfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.nightlabs.jfire.accounting.Price;
import org.nightlabs.jfire.accounting.priceconfig.id.PriceConfigID;
import org.nightlabs.jfire.store.NestedProductType;
import org.nightlabs.jfire.store.Product;
import org.nightlabs.jfire.store.ProductLocal;
import org.nightlabs.jfire.store.ProductType;
import org.nightlabs.jfire.store.id.ProductTypeID;
import org.nightlabs.jfire.trade.Article;
import org.nightlabs.jfire.trade.ArticlePrice;

/**
 * @author Marco Schulze - marco at nightlabs dot de
 */
public class PriceConfigUtil
{

	protected PriceConfigUtil() { }

	/**
	 * The caller of this method (always the top-level-productType-packagePriceConfig)
	 * must look up or calculate a <tt>Price</tt> for the given
	 * <tt>article</tt>. This <tt>Price</tt> must be passed as <tt>origPrice</tt>
	 * to this method, which will copy it into a new
	 * instance of <tt>ArticlePrice</tt>. This new <tt>ArticlePrice</tt> stores the
	 * returned value as original price if priceID >= 0. Hence, if this price is
	 * calculated dynamically, it must have the priceID -1.
	 * <p>
	 * Note, that this method DOES NOT fill the top-level <code>ArticlePrice</code>
	 * with nested prices! This work is done by {@link #fillArticlePrice(IPackagePriceConfig, Article)}.
	 * </p>
	 */
	public static ArticlePrice createArticlePrice(
			IPackagePriceConfig packagePriceConfig, Article article, Price origPrice)
	{
		ArticlePrice packageArticlePrice = new ArticlePrice(
				article,
				origPrice,
				packagePriceConfig.getOrganisationID(),
				packagePriceConfig.getPriceConfigID(),
				packagePriceConfig.createPriceID(),
				false);

		return packageArticlePrice;
	}

	/**
	 * This method iterates all packagedProducts / packagedProductTypes of the first
	 * packaging level
	 * and nests an instance of <tt>ArticlePrice</tt> per packagedProductType / packagedProduct.
	 * To retrieve this nested <tt>ArticlePrice</tt>, the method
	 * {@link IPriceConfig#createNestedArticlePrice(IPackagePriceConfig, Article, LinkedList, ArticlePrice, ArticlePrice, LinkedList, NestedProductType, LinkedList, Product, LinkedList)
	 * is called, if the nested product is coming from the local organisation. If the product
	 * is coming from a foreign organisation, the ArticlePrice is copied from the corresponding
	 * ArticlePrice (from the Article which was used to buy this Product).
	 */
	public static void fillArticlePrice(IPackagePriceConfig packagePriceConfig, Article article)
	{
		ArticlePrice packageArticlePrice = article.getPrice();

		if (!packageArticlePrice.getNestedArticlePrices().isEmpty())
			throw new IllegalStateException("The ArticlePrice " + packageArticlePrice.getPrimaryKey() + " does already have nested prices!");

		LinkedList priceConfigStack = new LinkedList();
		LinkedList articlePriceStack = new LinkedList();
		LinkedList nestedProductTypeStack = new LinkedList();
		LinkedList productStack = new LinkedList();

		ProductType productType = article.getProductType();
		Product product = article.getProduct();
		if (product != null) {
			for (Iterator it = product.getProductLocal().getNestedProductLocals(true).iterator(); it.hasNext(); ) {
				ProductLocal nestedProductLocal = (ProductLocal) it.next();
				Product nestedProduct = nestedProductLocal.getProduct();
				NestedProductType nestedProductType = productType.getNestedProductType(nestedProduct.getProductType().getPrimaryKey(), true);
				ProductType innerProductType = nestedProductType.getInnerProductType();

//				Article nestedArticle = nestedProductLocal.getArticle();
//				if (nestedArticle != null) {
//					// The nestedArticle exists, hence it's a foreign product.
//					// nestedProductLocal.getArticle() is the article that was used to buy this product! It is NOT the same as the parameter article.
//
//					ArticlePrice origPrice = nestedArticle.getPrice().getNestedArticlePrice(product, true);
//
//					// We create a new ArticlePrice which will copy the nested prices recursively itself
//					new ArticlePrice(
//							article,
//							origPrice,
//							packagePriceConfig.getOrganisationID(),
//							packagePriceConfig.getPriceConfigID(),
//							packagePriceConfig.createPriceID(),
//							packageArticlePrice,
//							nestedProductType,
//							nestedProduct.getProductType(),
//							nestedProduct,
//							false, // virtualInner is impossible if it's a foreign product
//							false);
//				}
//				else {
//					// The nestedArticle doesn't exist, hence it's a local product.
//					innerProductType.getPriceConfigInPackage(productType.getPrimaryKey()).createNestedArticlePrice(
//							packagePriceConfig,
//							article,
//							priceConfigStack,
//							packageArticlePrice,
//							packageArticlePrice,
//							articlePriceStack,
//							nestedProductType,
//							nestedProductTypeStack,
//							nestedProduct,
//							productStack
//							);
//				}

				// Maybe it's wiser to always use the price configs to create the article prices and only check at the
				// end of allocation, whether it's correct.
				innerProductType.getPriceConfigInPackage(productType.getPrimaryKey()).createNestedArticlePrice(
						packagePriceConfig,
						article,
						priceConfigStack,
						packageArticlePrice,
						packageArticlePrice,
						articlePriceStack,
						nestedProductType,
						nestedProductTypeStack,
						nestedProduct,
						productStack
						);
			}
		}
		else {
			for (Iterator it = productType.getNestedProductTypes(true).iterator(); it.hasNext(); ) {
				NestedProductType nestedProductType = (NestedProductType) it.next();
				ProductType innerProductType = nestedProductType.getInnerProductType();

				innerProductType.getPriceConfigInPackage(productType.getPrimaryKey()).createNestedArticlePrice(
						packagePriceConfig,
						article,
						priceConfigStack,
						packageArticlePrice,
						packageArticlePrice,
						articlePriceStack,
						nestedProductType,
						nestedProductTypeStack
						);
			}
		}
	}

	/**
	 * Helper method used by
	 * {@link IPriceConfig#createNestedArticlePrice(IPackagePriceConfig, Article, LinkedList, ArticlePrice, ArticlePrice, LinkedList, NestedProductType, LinkedList, Product, LinkedList).
	 * <p>
	 * This method keeps track about adding and removing items to/from the stacks. Hence,
	 * you can simply call this method without touching the parameters in your
	 * <tt>IPriceConfig</tt>.
	 *
	 * @param topLevelPriceConfig This <tt>IPriceConfig</tt> will create the ID for the new <tt>ArticlePrice</tt>.
	 */
	public static ArticlePrice createNestedArticlePrice(
			IPackagePriceConfig topLevelPriceConfig,
			IPriceConfig priceConfig,
			Article article,
			LinkedList priceConfigStack,
			ArticlePrice topLevelArticlePrice, ArticlePrice nextLevelArticlePrice,
			LinkedList articlePriceStack,
			NestedProductType nestedProductType,
			LinkedList nestedProductTypeStack,
			Product nestedProduct,
			LinkedList productStack,
			Price origPrice)
	{
		boolean inner = nestedProduct.getPrimaryKey().equals(nextLevelArticlePrice.getProduct().getPrimaryKey());

		ArticlePrice articlePrice = new ArticlePrice(
				article,
				origPrice,
				topLevelPriceConfig.getOrganisationID(),
				topLevelPriceConfig.getPriceConfigID(),
				topLevelPriceConfig.createPriceID(),
				nextLevelArticlePrice,
				nestedProductType,
				nestedProduct.getProductType(),
				nestedProduct,
				inner,
				false);

		ProductType productType = nestedProductType.getInnerProductType();

		if (productType.isPackageOuter()) {

			// prevent eternal loop when self-packaging
			Product nextLevelProduct;
			if (productStack.isEmpty())
				nextLevelProduct = article.getProduct();
			else
				nextLevelProduct = (Product)productStack.peek();

			if (!nestedProduct.getPrimaryKey().equals(nextLevelProduct.getPrimaryKey())) {

				priceConfigStack.addFirst(priceConfig);
				articlePriceStack.addFirst(articlePrice);
				nestedProductTypeStack.addFirst(nestedProductType);
				productStack.addFirst(nestedProduct);
				try {
	
					for (Iterator it = nestedProduct.getProductLocal().getNestedProductLocals(true).iterator(); it.hasNext(); ) {
						ProductLocal innerProductLocal = (ProductLocal) it.next();
						NestedProductType innerNestedProductType = productType.getNestedProductType(
								nestedProduct.getProductType().getPrimaryKey(), true);
						ProductType innerProductType = innerNestedProductType.getInnerProductType();

						innerProductType.getPriceConfigInPackage(productType.getPrimaryKey()).createNestedArticlePrice(
								topLevelPriceConfig,
								article,
								priceConfigStack,
								topLevelArticlePrice,
								articlePrice,
								articlePriceStack,
								innerNestedProductType,
								nestedProductTypeStack,
								innerProductLocal.getProduct(),
								productStack
								);
					}

				} finally {
					priceConfigStack.removeFirst();
					articlePriceStack.removeFirst();
					nestedProductTypeStack.removeFirst();
					productStack.removeFirst();
				}

			} // if (!nestedProduct.getPrimaryKey().equals(nextLevelProduct.getPrimaryKey())) {

		} // if (productType.isPackage()) {

		return articlePrice;
	}
	
	public static ArticlePrice createNestedArticlePrice(
			IPackagePriceConfig topLevelPriceConfig,
			IPriceConfig priceConfig,
			Article article,
			LinkedList priceConfigStack,
			ArticlePrice topLevelArticlePrice, ArticlePrice nextLevelArticlePrice,
			LinkedList articlePriceStack,
			NestedProductType nestedProductType,
			LinkedList nestedProductTypeStack,
			Price origPrice)
	{
		boolean inner = nestedProductType.getInnerProductType().getPrimaryKey().equals(nextLevelArticlePrice.getProductType().getPrimaryKey());

		ArticlePrice articlePrice = new ArticlePrice(
				article,
				origPrice,
				topLevelPriceConfig.getOrganisationID(),
				topLevelPriceConfig.getPriceConfigID(),
				topLevelPriceConfig.createPriceID(),
				nextLevelArticlePrice,
				nestedProductType,
				nestedProductType.getInnerProductType(),
				(Product)null,
				inner,
				false);

		ProductType productType = nestedProductType.getInnerProductType();

		if (productType.isPackageOuter()) {
			
			// prevent eternal loop when self-packaging
			ProductType nextLevelProductType;
			if (nestedProductTypeStack.isEmpty())
				nextLevelProductType = article.getProductType();
			else
				nextLevelProductType = ((NestedProductType)nestedProductTypeStack.peek()).getInnerProductType();

			if (!productType.getPrimaryKey().equals(nextLevelProductType.getPrimaryKey())) {

				priceConfigStack.addFirst(priceConfig);
				articlePriceStack.addFirst(articlePrice);
				nestedProductTypeStack.addFirst(nestedProductType);
				try {
	
					for (Iterator it = productType.getNestedProductTypes(true).iterator(); it.hasNext(); ) {
						NestedProductType innerNestedProductType = (NestedProductType)it.next();
						ProductType innerProductType = innerNestedProductType.getInnerProductType();
	
						innerProductType.getPriceConfigInPackage(productType.getPrimaryKey()).createNestedArticlePrice(
								topLevelPriceConfig,
								article,
								priceConfigStack,
								topLevelArticlePrice,
								articlePrice,
								articlePriceStack,
								innerNestedProductType,
								nestedProductTypeStack
								);
					}
	
				} finally {
					priceConfigStack.removeFirst();
					articlePriceStack.removeFirst();
					nestedProductTypeStack.removeFirst();
				}

			} // if (!productType.getPrimaryKey().equals(nextLevelProductType.getPrimaryKey())) {

		} // if (productType.isPackage()) {

		return articlePrice;
	}

	/**
	 * This method finds out, which ProductTypes are affected by a price-change to the given <code>productType</code>.
	 * Therefore, it recursively goes through all nest-levels (by recursing up the package-ProductTypes) and collects
	 * the parents and the siblings. Because jfire currently supports only forward price configurations (a package price
	 * is dependent on its contents) and reverse price configs over one level (siblings dependent on each other), we
	 * do not need to go down the nesting-path - upward is sufficient.
	 *
	 * @param pm The PersistenceManager to access the datastore.
	 * @param productType The ProductType whose prices are about to be changed.
	 * @return Returns a List referencing the affected product types. This list starts with the given
	 *		<code>productType</code> having the nesting-level (see {@link AffectedProductType#getNestingLevel()}) 0. The
	 *		direct packages around this productType will follow with nesting-level 1 and so on. 
	 */
	public static ArrayList<AffectedProductType> getAffectedProductTypes(PersistenceManager pm, ProductType productType)
	{
		ArrayList<AffectedProductType> res = new ArrayList<AffectedProductType>();
		res.add(new AffectedProductType(null, AffectedProductType.CauseType.ROOT, (ProductTypeID) JDOHelper.getObjectId(productType)));
		populateAffectedProductTypeListWithSiblings(res, productType);
		populateProductTypesNestingThis(res, pm, productType, true);
		return res;
	}

	private static void populateAffectedProductTypeListWithSiblings(List<AffectedProductType> affectedProductTypes, ProductType productType)
	{
		ProductTypeID productTypeID = (ProductTypeID) JDOHelper.getObjectId(productType);
		for (NestedProductType nestedProductType : productType.getExtendedProductType().getNestedProductTypes()) {
			ProductTypeID nestedPTID = ProductTypeID.create(
					nestedProductType.getInnerProductTypeOrganisationID(), nestedProductType.getInnerProductTypeProductTypeID());

			if (!productTypeID.equals(nestedPTID))
				affectedProductTypes.add(new AffectedProductType(productTypeID, AffectedProductType.CauseType.SIBLING, nestedPTID));
		}
	}

	private static void populateProductTypesNestingThis(List<AffectedProductType> affectedProductTypes, PersistenceManager pm, ProductType productType, boolean includeSiblings)
	{
		List<ProductType> productTypes = ProductType.getProductTypesNestingThis(pm, productType);
		ArrayList<AffectedProductType> res = new ArrayList<AffectedProductType>(productTypes.size());
		for (ProductType pt : productTypes) {
			res.add(new AffectedProductType(
					(ProductTypeID) JDOHelper.getObjectId(productType),
					AffectedProductType.CauseType.NESTED,
					(ProductTypeID) JDOHelper.getObjectId(pt)));

			if (includeSiblings) {
				populateAffectedProductTypeListWithSiblings(affectedProductTypes, pt);
			}

			populateProductTypesNestingThis(affectedProductTypes, pm, pt, includeSiblings);
		}
//		ArrayList<AffectedProductType> res = new ArrayList<AffectedProductType>();
//		for (ProductType pt : (Collection<ProductType>)q.execute(productType)) {
//			res.add(new AffectedProductType((ProductTypeID) JDOHelper.getObjectId(pt), nestingLevel));
//			res.addAll(getProductTypesNestingThis(q, pt, nestingLevel + 1));
//		}
	}

	/**
	 * This method finds out which ProductTypes (identified by their ID) were affected, if the given <code>priceConfig</code>
	 * is changed. This means both, ProductTypes that use the given priceConfig (see {@link ProductType#getInnerPriceConfig()}
	 * and {@link ProductType#getPackagePriceConfig()})
	 */
	public static ArrayList<AffectedProductType> getAffectedProductTypes(PersistenceManager pm, IPriceConfig priceConfig, boolean includeSiblings)
	{
		ArrayList<AffectedProductType> res = new ArrayList<AffectedProductType>();

		Query q1 = pm.newQuery(ProductType.class);
		q1.setFilter("this.innerPriceConfig == :priceConfig || this.packagePriceConfig == :priceConfig");

//		Query q2 = pm.newQuery(ProductType.class);
//		q2.declareVariables(NestedProductType.class.getName() + " nestedProductType");
//		q2.setFilter("this.nestedProductTypes.containsValue(nestedProductType) && nestedProductType.innerProductType == :productType");

		for (ProductType pt : (Collection<ProductType>)q1.execute(priceConfig)) {
			ProductTypeID productTypeID = (ProductTypeID) JDOHelper.getObjectId(pt);
			res.add(new AffectedProductType(null, AffectedProductType.CauseType.ROOT, productTypeID));

			if (includeSiblings) {
				populateAffectedProductTypeListWithSiblings(res, pt);
			}

			populateProductTypesNestingThis(res, pm, pt, true);
		}

		return res;
	}

	/**
	 * @return The returned Map&lt;PriceConfigID, List&lt;AffectedProductType&gt;&gt; indicates which modified
	 *		price config would result in which products to have their prices recalculated.
	 */
	public static Map<PriceConfigID, List<AffectedProductType>> getAffectedProductTypes(PersistenceManager pm, Set<PriceConfigID> priceConfigIDs)
	{
		Map<PriceConfigID, List<AffectedProductType>> res = new HashMap<PriceConfigID, List<AffectedProductType>>(priceConfigIDs.size());
		for (PriceConfigID priceConfigID : priceConfigIDs) {
			IPriceConfig priceConfig = (IPriceConfig) pm.getObjectById(priceConfigID);
			ArrayList<AffectedProductType> affectedProductTypes = PriceConfigUtil.getAffectedProductTypes(pm, priceConfig, true);
			affectedProductTypes.trimToSize();
			res.put(priceConfigID, affectedProductTypes);
		}
		return res;
	}
}
