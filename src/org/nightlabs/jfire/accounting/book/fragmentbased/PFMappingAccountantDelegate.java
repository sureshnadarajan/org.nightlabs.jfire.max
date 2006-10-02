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

package org.nightlabs.jfire.accounting.book.fragmentbased;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jdo.JDOHelper;

import org.nightlabs.jfire.accounting.Invoice;
import org.nightlabs.jfire.accounting.PriceFragment;
import org.nightlabs.jfire.accounting.PriceFragmentType;
import org.nightlabs.jfire.accounting.book.BookMoneyTransfer;
import org.nightlabs.jfire.accounting.book.LocalAccountantDelegate;
import org.nightlabs.jfire.accounting.book.MoneyFlowMapping;
import org.nightlabs.jfire.accounting.id.PriceFragmentTypeID;
import org.nightlabs.jfire.security.User;
import org.nightlabs.jfire.store.ProductType;
import org.nightlabs.jfire.store.id.ProductTypeID;
import org.nightlabs.jfire.trade.Article;
import org.nightlabs.jfire.trade.ArticlePrice;
import org.nightlabs.jfire.trade.OrganisationLegalEntity;
import org.nightlabs.jfire.transfer.Anchor;
import org.nightlabs.util.Utils;

/**
 * @author Alexander Bieber <alex[AT]nightlabs[DOT]de>
 * 
 * @jdo.persistence-capable
 *		identity-type="application"
 *		persistence-capable-superclass="org.nightlabs.jfire.accounting.book.LocalAccountantDelegate"
 *		detachable="true"
 *		table="JFireTrade_PFMappingAccountantDelegate"
 *
 * @jdo.inheritance strategy = "new-table"
 *
 */
public class PFMappingAccountantDelegate extends
		LocalAccountantDelegate {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * LOG4J logger used by this class
	 */
	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger
			.getLogger(PFMappingAccountantDelegate.class);

	/**
	 * @deprecated Only for JDO
	 */
	public PFMappingAccountantDelegate() {
		super();
	}

	/**
	 * @param accountant
	 * @param productType
	 */
	public PFMappingAccountantDelegate(String organisationID, String localAccountantDelegateID) {
		super(organisationID, localAccountantDelegateID);
	}


	/**
	 * @param parent
	 * @param organisationID
	 * @param localAccountantDelegateID
	 */
	public PFMappingAccountantDelegate(LocalAccountantDelegate parent, String organisationID, String localAccountantDelegateID) {
		super(parent, organisationID, localAccountantDelegateID);
	}	

	/**
	 * key: Invoice invoice<br/>
	 * value: Map resolvedMappings
	 * 
	 * Used to store resolved mappings temporaly per invoice-book
	 * 
	 * TODO: Access to this map should be synchronized
	 * 
	 * @jdo.field persistence-modifier="none"
	 */
	private Map<Invoice, Map<ResolvedMapKey, ResolvedMapEntry>> resolvedPTypeMappings = new HashMap<Invoice, Map<ResolvedMapKey, ResolvedMapEntry>>();
	
	
	
	/**
	 * @see org.nightlabs.jfire.accounting.book.LocalAccountantDelegate#preBookArticles(org.nightlabs.jfire.trade.OrganisationLegalEntity, org.nightlabs.jfire.security.User, org.nightlabs.jfire.accounting.Invoice, BookMoneyTransfer, Map)
	 */
	public void preBookArticles(OrganisationLegalEntity mandator, User user, Invoice invoice, BookMoneyTransfer bookTransfer, Map<String, Anchor> involvedAnchors) {
		if (resolvedPTypeMappings.containsKey(invoice))
			return;
		resolvedPTypeMappings.put(invoice, resolveProductTypeMappings(invoice));
	}

	/**
	 * @see org.nightlabs.jfire.accounting.book.LocalAccountantDelegate#postBookArticles(org.nightlabs.jfire.trade.OrganisationLegalEntity, org.nightlabs.jfire.security.User, org.nightlabs.jfire.accounting.Invoice, BookMoneyTransfer, Map)
	 */
	public void postBookArticles(OrganisationLegalEntity mandator, User user, Invoice invoice, BookMoneyTransfer bookTransfer, Map<String, Anchor> involvedAnchors) {
		if (resolvedPTypeMappings.containsKey(invoice))
			resolvedPTypeMappings.remove(invoice);
	}

	/**
	 * @see org.nightlabs.jfire.accounting.book.LocalAccountantDelegate#bookArticle(org.nightlabs.jfire.trade.OrganisationLegalEntity, org.nightlabs.jfire.security.User, org.nightlabs.jfire.accounting.Invoice, org.nightlabs.jfire.trade.ArticlePrice, org.nightlabs.jfire.accounting.book.BookMoneyTransfer, Map)
	 */
	public void bookArticle(OrganisationLegalEntity mandator, User user,
			Invoice invoice, Article article , BookMoneyTransfer container,
			Map<String, Anchor> involvedAnchors) {
		Map<ResolvedMapKey, ResolvedMapEntry> resolvedMappings = resolvedPTypeMappings.get(invoice);
		if (resolvedMappings == null)
			throw new IllegalStateException("Could not find resolved mappings for invoice "+JDOHelper.getObjectId(invoice)+" can not book article. Was preBookInvoice() called?");
		LinkedList<ArticlePrice> articlePrices = new LinkedList<ArticlePrice>();
		articlePrices.add(article.getPrice());
		/*
		 * key: Anchor from
		 * value: Map toTransfers
		 *   key: Anchor to
		 *   value: Collection of BookInvoiceTransfer transfers
		 */
		Map<Anchor, Map<Anchor, Collection<BookInvoiceTransfer>>> bookInvoiceTransfers = new HashMap<Anchor, Map<Anchor, Collection<BookInvoiceTransfer>>>();

		bookProductTypeParts(mandator, user, resolvedMappings, articlePrices, bookInvoiceTransfers, 0, container, involvedAnchors);
		bookInvoiceTransfers(user, bookInvoiceTransfers, container, involvedAnchors);
	}
	
	

	/**
	 * @see org.nightlabs.jfire.accounting.book.LocalAccountantDelegate#getMoneyFlowMappingKey(org.nightlabs.jfire.store.ProductType, java.lang.String, Map, org.nightlabs.jfire.accounting.Currency)
	 */
	public String getMoneyFlowMappingKey(
			ProductTypeID productTypeID, 
			String packageType,
			Map<String, String> dimensionValues,
			String currencyID
		) 
	{
		return PFMoneyFlowMapping.getMappingKey(
				ProductType.getPrimaryKey(productTypeID.organisationID, productTypeID.productTypeID), 
				packageType, currencyID, 
				(String)dimensionValues.get(OwnerDimension.MONEY_FLOW_DIMENSION_ID),
				(String)dimensionValues.get(SourceOrganisationDimension.MONEY_FLOW_DIMENSION_ID),
				(String)dimensionValues.get(PriceFragmentDimension.MONEY_FLOW_DIMENSION_ID)
			);
	}
	
	

	public static List<String> DIMENSION_IDS = Utils.array2ArrayList(
			new String[] {
					OwnerDimension.MONEY_FLOW_DIMENSION_ID,
					SourceOrganisationDimension.MONEY_FLOW_DIMENSION_ID,
					PriceFragmentDimension.MONEY_FLOW_DIMENSION_ID
				}
		);
	
	/**
	 * @see org.nightlabs.jfire.accounting.book.LocalAccountantDelegate#getMoneyFlowDimensionIDs()
	 */
	public List<String> getMoneyFlowDimensionIDs() {
		return DIMENSION_IDS;
	}

	protected void internalBookProductTypeParts(
			OrganisationLegalEntity mandator, User user, Map<ResolvedMapKey, ResolvedMapEntry> resolvedMappings, 
			LinkedList<ArticlePrice> articlePriceStack, ArticlePrice articlePrice, 
			Map<Anchor, Map<Anchor, Collection<BookInvoiceTransfer>>> bookInvoiceTransfers,
			ProductType productType, String packageType,
			int delegationLevel, 
			BookMoneyTransfer container, Map<String, Anchor> involvedAnchors
		)
	{
		internalBookProductTypePartsByDimension(
				mandator, user, 
				resolvedMappings, articlePriceStack, articlePrice, 
				bookInvoiceTransfers,
				productType, packageType,
				delegationLevel,
				container, involvedAnchors
			);
	}
	
	/**
	 * @see org.nightlabs.jfire.accounting.book.LocalAccountantDelegate#getBookInvoiceTransfersForDimensionValues(org.nightlabs.jfire.trade.OrganisationLegalEntity, LinkedList, Map, org.nightlabs.jfire.accounting.book.MoneyFlowMapping, java.util.Map, BookMoneyTransfer)
	 */
	public Collection<BookInvoiceTransfer> getBookInvoiceTransfersForDimensionValues(
			OrganisationLegalEntity mandator, 
			LinkedList<ArticlePrice> articlePriceStack, 
			Map<String, String> dimensionValues, 
			MoneyFlowMapping resolvedMapping, 
			Map<ResolvedMapKey, ResolvedMapEntry> resolvedMappings, BookMoneyTransfer bookMoneyTransfer
		) 
	{
		// get the priceFragment of interest
		String priceFragmentTypePK = dimensionValues.get(PriceFragmentDimension.MONEY_FLOW_DIMENSION_ID);
		if (priceFragmentTypePK == null || "".equals(priceFragmentTypePK))
			throw new IllegalArgumentException("No value could be found in the dimensionValues Map for Dimension "+PriceFragmentDimension.MONEY_FLOW_DIMENSION_ID);
		
		PriceFragmentTypeID typeID = PriceFragmentType.primaryKeyToPriceFragmentTypeID(priceFragmentTypePK);		
		PriceFragmentType priceFragmentType = (PriceFragmentType) getPersistenceManager().getObjectById(typeID);
	
		// now if it is total then book all fragments contained in total
		// TODO: Check here if is container not only total
		if (priceFragmentType.getPriceFragmentTypeID().equals(PriceFragmentType.TOTAL_PRICEFRAGMENTTYPEID)) 
			return getBookInvoiceTransferForContainerFragmentType(
					mandator,
					priceFragmentType, articlePriceStack, dimensionValues,
					resolvedMapping, resolvedMappings, bookMoneyTransfer
				);
		else
			// else return the transfer for the single fragment
			return getBookInvoiceTransfersForSingleFragmentType(
					mandator,
					priceFragmentType, articlePriceStack, dimensionValues,
					resolvedMapping, resolvedMappings,
					false, bookMoneyTransfer
				);
		
	}

	/**
	 * Searches for transfers for all PriceFragments the given one is the container. 
	 * @param bookMoneyTransfer TODO
	 */
	protected Collection<BookInvoiceTransfer> getBookInvoiceTransferForContainerFragmentType(
			OrganisationLegalEntity mandator, 
			PriceFragmentType priceFragmentType,
			LinkedList articlePriceStack, 
			Map<String, String> dimensionValues, 
			MoneyFlowMapping resolvedMapping, 
			Map<ResolvedMapKey, ResolvedMapEntry> resolvedMappings, BookMoneyTransfer bookMoneyTransfer
		) 
	{
		ArticlePrice price = (ArticlePrice)articlePriceStack.getFirst();
		// Get the PriceFragementTypes involved in this price
		Set<PriceFragmentType> containedFragmentTypes = new HashSet<PriceFragmentType>();
		for (Iterator iter = price.getFragments().iterator(); iter.hasNext();) {
			PriceFragment fragment = (PriceFragment) iter.next();
			PriceFragmentType container = fragment.getPriceFragmentType().getContainerPriceFragmentType();
			if (container != null) {
				if (priceFragmentType.getPrimaryKey().equals(container.getPrimaryKey())) {
					containedFragmentTypes.add(fragment.getPriceFragmentType());
				}
			}
		}		
		
		Collection<BookInvoiceTransfer> result = new LinkedList<BookInvoiceTransfer>();
		
		// iterate all contained types 
		for (Iterator iter = containedFragmentTypes.iterator(); iter.hasNext();) {
			PriceFragmentType containedType = (PriceFragmentType) iter.next();
			Map<String, String> fakeDimValues = new HashMap<String, String>(dimensionValues);
			fakeDimValues.put(PriceFragmentDimension.MONEY_FLOW_DIMENSION_ID, containedType.getPrimaryKey());
			// find the transfer for the current type
			Collection<BookInvoiceTransfer> singleTransfers = getBookInvoiceTransfersForSingleFragmentType(
					mandator,
					containedType,
					articlePriceStack,
					fakeDimValues,
					resolvedMapping,
					resolvedMappings,
					true, bookMoneyTransfer
				);
			result.addAll(singleTransfers);
		}
		return result;
	}

	protected Collection<BookInvoiceTransfer> getBookInvoiceTransfersForSingleFragmentType(
			OrganisationLegalEntity mandator, 
			PriceFragmentType priceFragmentType, 
			LinkedList articlePriceStack, 
			Map<String, String> dimensionValues, 
			MoneyFlowMapping resolvedMapping, 
			Map<ResolvedMapKey, ResolvedMapEntry> resolvedMappings,
			boolean forContainer, BookMoneyTransfer bookMoneyTransfer
		) 
	{
		Collection<BookInvoiceTransfer> result = new ArrayList<BookInvoiceTransfer>();
		BookInvoiceTransfer transfer = getBookInvoiceTransferForSingleFragmentType(
				mandator, priceFragmentType, articlePriceStack,
				dimensionValues, resolvedMapping,
				resolvedMappings, forContainer, bookMoneyTransfer
			);
		if (transfer == null) {
			ArticlePrice articlePrice = (ArticlePrice)articlePriceStack.getFirst();
			Article article = articlePrice.getArticle();

			long amount = resolvedMapping.getArticlePriceDimensionAmount(
					dimensionValues, 
					articlePrice
				);

			Anchor from;
			Anchor to;
			if (amount >= 0) {
				from = mandator;
				to = resolvedMapping.getAccount();
			}
			else {
				to = mandator;
				from = resolvedMapping.getAccount();
				amount *= -1;
			}

			transfer = new BookInvoiceTransfer(
					from,
					to,
					amount
				); 
		}
		result.add(transfer);
		logBookInvoiceTransfers("getBookInvoiceTransferForSingleFragmentType ", result);
		
		return result;
	}
	
	protected BookInvoiceTransfer getBookInvoiceTransferForSingleFragmentType(
			OrganisationLegalEntity mandator, 
			PriceFragmentType priceFragmentType, 
			LinkedList<ArticlePrice> articlePriceStack, 
			Map<String, String> dimensionValues, 
			MoneyFlowMapping resolvedMapping,
			Map<ResolvedMapKey, ResolvedMapEntry> resolvedMappings, boolean forContainer, BookMoneyTransfer bookMoneyTransfer
		) 
	{
		List<PriceFragmentType> priceFragmentTypes = new LinkedList<PriceFragmentType>();		
		priceFragmentTypes.add(priceFragmentType);
		
		PriceFragmentType totalPType = PriceFragmentType.getTotalPriceFragmentType(getPersistenceManager());
		
		// if we look for the priceFragmentType of interest for total in all upper packages
//		if (!PriceFragmentType.TOTAL_PRICEFRAGMENTTYPEID.equals(priceFragmentType.getPriceFragmentTypeID()))
		priceFragmentTypes.add(totalPType);

		List<PriceFragmentType> totalPTypes = new ArrayList<PriceFragmentType>(1);
		totalPTypes.add(totalPType);
		
		Iterator iterator = articlePriceStack.iterator();
		logger.info("Search BookAnchors for single pricefragment: "+priceFragmentType.getName().getText("en"));
		int count = 0;
		while (iterator.hasNext()) {				
			ArticlePrice upperArticlePrice = (ArticlePrice)iterator.next();
			List<PriceFragmentType> pTypeParam = null;
			if (count == 0) {
				if (forContainer) {
					count++;
					continue;
				}
				else
					pTypeParam = totalPTypes; 
				// for our own product-type we only check if total was booked for this type already, 
				// but only of we are not a part of a container booking
			}
			else
				pTypeParam = priceFragmentTypes;			
			
			count++;
			
			logger.info("Search for ArticlePricePType: "+upperArticlePrice.getProductType().getName().getText("en"));
			BookInvoiceTransfer result = getBookInvoiceTransferForSingleFragmentType(
					mandator,
					upperArticlePrice,
					articlePriceStack,
					dimensionValues,
					pTypeParam,
					resolvedMapping, resolvedMappings, bookMoneyTransfer
				);
			logger.info("Found: "+result);
			if (result != null)
				return result;
			
		}
		
//		for (Iterator itPriceFragmentTypes = priceFragmentTypes.iterator(); itPriceFragmentTypes.hasNext(); ) {
//			PriceFragmentType searchPriceFragmentType = (PriceFragmentType) itPriceFragmentTypes.next();
//			
//			Iterator iterator = articlePriceStack.iterator();
//			if (iterator.hasNext()) // if (searchPriceFragmentType.getPrimaryKey().equals(priceFragmentType.getPrimaryKey()))
//				iterator.next(); // we skip the first entry if we are working with the current pricefragmenttype, because that's our current productType
//			while (iterator.hasNext()) {
//				ArticlePrice upperArticlePrice = (ArticlePrice)iterator.next();
//				String upperPackageType = getPackageType(upperArticlePrice);
//				Map<String, String> fakeDimValues = new HashMap<String, String>(dimensionValues);
//				fakeDimValues.put(PriceFragmentDimension.MONEY_FLOW_DIMENSION_ID, searchPriceFragmentType.getPrimaryKey());
//				PFMoneyFlowMapping packagingUpperMapping = (PFMoneyFlowMapping)getMoneyFlowMapping(
//						resolvedMappings, 
//						upperArticlePrice.getProductType(),
//						upperPackageType,
//						fakeDimValues,
//						upperArticlePrice.getCurrency()
//				);
//				
//				if (packagingUpperMapping != null) {
//					return
//							new LocalAccountantDelegate.BookInvoiceTransfer(
//									packagingUpperMapping.getAccount(),
//									resolvedMapping.getAccount(),
//									resolvedMapping.getArticlePriceDimensionAmount(
//											dimensionValues, 
//											(ArticlePrice)articlePriceStack.getFirst()
//										)
//								); 
//						
//				}				
//			}
//		} // while (iterator.hasNext()) {
		return null;
	}
	
	private BookInvoiceTransfer getBookInvoiceTransferForSingleFragmentType(
			OrganisationLegalEntity mandator, 
			ArticlePrice upperArticlePrice, 
			LinkedList<ArticlePrice> articlePriceStack, 
			Map<String, String> dimensionValues,
			List<PriceFragmentType> searchPriceFragmentTypes, 
			MoneyFlowMapping resolvedMapping, Map<ResolvedMapKey, ResolvedMapEntry> resolvedMappings, BookMoneyTransfer bookMoneyTransfer
		) 
	{
		String upperPackageType = getPackageType(upperArticlePrice);
		for (PriceFragmentType searchPriceFragmentType : searchPriceFragmentTypes) {
			Map<String, String> fakeDimValues = new HashMap<String, String>(dimensionValues);
			fakeDimValues.put(PriceFragmentDimension.MONEY_FLOW_DIMENSION_ID, searchPriceFragmentType.getPrimaryKey());
			PFMoneyFlowMapping packagingUpperMapping = (PFMoneyFlowMapping)getMoneyFlowMapping(
					resolvedMappings, 
					upperArticlePrice.getProductType(),
					upperPackageType,
					fakeDimValues,
					upperArticlePrice.getCurrency()
			);
			
			if (packagingUpperMapping != null) {
				Anchor from;
				Anchor to;
				
				long amount = resolvedMapping.getArticlePriceDimensionAmount(
						dimensionValues, 
						(ArticlePrice)articlePriceStack.getFirst()
					);

				if (amount >= 0) {
					from = packagingUpperMapping.getAccount();
					to = resolvedMapping.getAccount();
				}
				else {
					to = packagingUpperMapping.getAccount();
					from = resolvedMapping.getAccount();
					amount *= -1;
				}
				
				return
						new LocalAccountantDelegate.BookInvoiceTransfer(
								from,
								to,
								amount
							); 
					
			}
		}		
		return null;
		
	}
	
	private void logBookInvoiceTransfers(String prefix, Collection<BookInvoiceTransfer> transfers) {
		for (BookInvoiceTransfer transfer : transfers) {
			logger.info(prefix + transfers.toString());
		}
	}
}
