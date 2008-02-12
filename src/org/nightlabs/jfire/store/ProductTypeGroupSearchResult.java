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

package org.nightlabs.jfire.store;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jdo.JDOHelper;

import org.nightlabs.jfire.store.id.ProductTypeGroupID;
import org.nightlabs.jfire.store.id.ProductTypeID;

public class ProductTypeGroupSearchResult implements Serializable {
	private static final long serialVersionUID = 1L;

	public static class Entry implements Serializable {
		private static final long serialVersionUID = 1L;
		private ProductTypeGroupID productTypeGroup;
		private List productTypes = new LinkedList();
		
		public Entry(ProductTypeGroupID productTypeGroupID) {
			this.productTypeGroup = productTypeGroupID;			
		}
		
		public Entry(ProductTypeGroup productTypeGroup) {
			this.productTypeGroup = (ProductTypeGroupID)JDOHelper.getObjectId(productTypeGroup);			
		}

		public void addProductType(ProductType productType) {
			productTypes.add(JDOHelper.getObjectId(productType));
		}
		
		public void addProductType(ProductTypeID productType) {
			productTypes.add(productType);
		}
		
		public List getProductTypes() {
			return productTypes;
		}
		
		public ProductTypeGroupID getProductTypeGroup() {
			return productTypeGroup;
		}
	}
	
	
	private Map entries = new HashMap();

	public ProductTypeGroupSearchResult() {
		super();
	}
	
	public Collection getEntries() {
		return entries.values();
	}
	
	public Set getEventGroupIDs() {
		return entries.keySet();
	}
	
	public Entry getEntry(ProductTypeGroup productTypeGroup) {
		return (Entry)entries.get(JDOHelper.getObjectId(productTypeGroup));
	}
	
	public Entry getEntry(ProductTypeGroupID productTypeGroup) {
		return (Entry)entries.get(productTypeGroup);
	}
	
	public void addEntry(Entry entry) {
		entries.put(entry.productTypeGroup, entry);
	}
	
	public void addEntry(ProductTypeGroupID productTypeGroup) {
		entries.put(productTypeGroup, new Entry(productTypeGroup));
	}
	
	public void addEntry(ProductTypeGroup productTypeGroup) {
		entries.put(JDOHelper.getObjectId(productTypeGroup), new Entry(productTypeGroup));
	}
	
	public void addType(ProductTypeGroup productTypeGroup, ProductType productType) {
		Entry entry = getEntry(productTypeGroup);
		if (entry != null)
			entry.addProductType(productType);
	}
	
	public void addType(ProductTypeGroupID productTypeGroup, ProductTypeID productType) {
		Entry entry = getEntry(productTypeGroup);
		if (entry != null)
			entry.addProductType(productType);
	}
	
	public int getSize() {
		return entries.size();
	}
}
