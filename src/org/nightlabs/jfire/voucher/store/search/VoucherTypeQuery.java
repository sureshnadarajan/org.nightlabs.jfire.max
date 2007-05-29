package org.nightlabs.jfire.voucher.store.search;

import javax.jdo.Query;

import org.nightlabs.jfire.store.search.ProductTypeQuery;
import org.nightlabs.jfire.voucher.store.VoucherType;

/**
 * @author Daniel.Mazurek [at] NightLabs [dot] de
 *
 */
public class VoucherTypeQuery 
extends ProductTypeQuery 
{

	public VoucherTypeQuery() {
	}

	@Override
	protected Query prepareQuery() 
	{
		super.prepareQuery();
		// FIXME: Query also subclasses when JPOX problem is solved
		Query q = getPersistenceManager().newQuery(getPersistenceManager().getExtent(
				VoucherType.class, false));
		
		q.setFilter(getFilter().toString());
		q.declareVariables(getVars().toString());
		
		return q;		
	}

	
}
