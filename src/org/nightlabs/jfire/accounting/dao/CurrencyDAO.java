package org.nightlabs.jfire.accounting.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.jdo.FetchPlan;

import org.nightlabs.jdo.NLJDOHelper;
import org.nightlabs.jfire.accounting.AccountingManagerRemote;
import org.nightlabs.jfire.accounting.Currency;
import org.nightlabs.jfire.accounting.id.CurrencyID;
import org.nightlabs.jfire.base.JFireEjb3Factory;
import org.nightlabs.jfire.base.jdo.BaseJDOObjectDAO;
import org.nightlabs.jfire.security.SecurityReflector;
import org.nightlabs.progress.ProgressMonitor;
import org.nightlabs.progress.SubProgressMonitor;

public class CurrencyDAO extends BaseJDOObjectDAO<CurrencyID, Currency>
{
	private CurrencyDAO() {}

	private static CurrencyDAO sharedInstance = null;

	public static CurrencyDAO sharedInstance() {
		if (sharedInstance == null) {
			synchronized (CurrencyDAO.class) {
				if (sharedInstance == null)
					sharedInstance = new CurrencyDAO();
			}
		}
		return sharedInstance;
	}

	@Override
	protected Collection<Currency> retrieveJDOObjects(Set<CurrencyID> objectIDs,
			String[] fetchGroups, int maxFetchDepth, ProgressMonitor monitor)
			throws Exception
	{
		monitor.beginTask("Fetching Currencies...", 1); //$NON-NLS-1$
		try {
			AccountingManagerRemote am = JFireEjb3Factory.getRemoteBean(AccountingManagerRemote.class, SecurityReflector.getInitialContextProperties());
			monitor.worked(1);
			return am.getCurrencies(fetchGroups, maxFetchDepth);
		} catch (Exception e) {
			monitor.setCanceled(true);
			throw e;
		} finally {
			monitor.done();
		}
	}

	private static final String[] FETCH_GROUPS = { FetchPlan.DEFAULT };

	public List<Currency> getCurrencies(ProgressMonitor monitor)
	{
		try {
			return new ArrayList<Currency>(retrieveJDOObjects(null, FETCH_GROUPS, NLJDOHelper.MAX_FETCH_DEPTH_NO_LIMIT, monitor));
		} catch (Exception e) {
			throw new RuntimeException("Error while fetching Currencies: " + e.getMessage(), e); //$NON-NLS-1$
		}
	}

	public Currency getCurrency(CurrencyID currencyID, ProgressMonitor monitor) {
		monitor.beginTask("Loading currency "+ currencyID.currencyID, 1);
		Currency currency = getJDOObject(null, currencyID, FETCH_GROUPS, NLJDOHelper.MAX_FETCH_DEPTH_NO_LIMIT, new SubProgressMonitor(monitor, 1));
		monitor.done();
		return currency;
	}
}
