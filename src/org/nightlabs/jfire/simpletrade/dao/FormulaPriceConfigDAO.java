package org.nightlabs.jfire.simpletrade.dao;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.nightlabs.annotation.Implement;
import org.nightlabs.jfire.accounting.gridpriceconfig.FormulaPriceConfig;
import org.nightlabs.jfire.accounting.priceconfig.id.PriceConfigID;
import org.nightlabs.jfire.base.jdo.BaseJDOObjectDAO;
import org.nightlabs.jfire.security.SecurityReflector;
import org.nightlabs.jfire.simpletrade.SimpleTradeManager;
import org.nightlabs.jfire.simpletrade.SimpleTradeManagerUtil;
import org.nightlabs.progress.ProgressMonitor;

public class FormulaPriceConfigDAO
extends BaseJDOObjectDAO<PriceConfigID, FormulaPriceConfig>
{
	private static FormulaPriceConfigDAO sharedInstance = null;

	public static FormulaPriceConfigDAO sharedInstance()
	{
		if (sharedInstance == null) {
			synchronized (FormulaPriceConfigDAO.class) {
				if (sharedInstance == null)
					sharedInstance = new FormulaPriceConfigDAO();
			}
		}
		return sharedInstance;
	}

	@Override
	@SuppressWarnings("unchecked")
	@Implement
	protected Collection<FormulaPriceConfig> retrieveJDOObjects(
			Set<PriceConfigID> formulaPriceConfigIDs, String[] fetchGroups, int maxFetchDepth,
			ProgressMonitor monitor)
			throws Exception
	{
		monitor.beginTask("Loading FormulaPriceConfigs", 1);
		try {
			SimpleTradeManager vm = simpleTradeManager;
			if (vm == null)
				vm = SimpleTradeManagerUtil.getHome(SecurityReflector.getInitialContextProperties()).create();

			return vm.getFormulaPriceConfigs(formulaPriceConfigIDs, fetchGroups, maxFetchDepth);
		} finally {
			monitor.worked(1);
		}
	}

	private SimpleTradeManager simpleTradeManager;

	@SuppressWarnings("unchecked")
	public List<FormulaPriceConfig> getFormulaPriceConfigs(String[] fetchGroups, int maxFetchDepth,
			ProgressMonitor monitor)
	{
		try {
			simpleTradeManager = SimpleTradeManagerUtil.getHome(SecurityReflector.getInitialContextProperties()).create();
			try {
				Collection<PriceConfigID> formulaPriceConfigIDs = simpleTradeManager.getFormulaPriceConfigIDs();
				return getJDOObjects(null, formulaPriceConfigIDs, fetchGroups, maxFetchDepth, monitor);
			} finally {
				simpleTradeManager = null;
			}
		} catch (Exception x) {
			throw new RuntimeException(x);
		}
	}

	public List<FormulaPriceConfig> getFormulaPriceConfigs(Collection<PriceConfigID> formulaPriceConfigIDs,
			String[] fetchGroups, int maxFetchDepth,
			ProgressMonitor monitor)
	{
		if (formulaPriceConfigIDs == null)
			throw new IllegalArgumentException("formulaPriceConfigIDs must not be null!");

		return getJDOObjects(null, formulaPriceConfigIDs, fetchGroups, maxFetchDepth, monitor);
	}

	public FormulaPriceConfig getFormulaPriceConfig(PriceConfigID formulaPriceConfigID,
			String[] fetchGroups, int maxFetchDepth,
			ProgressMonitor monitor)
	{
		if (formulaPriceConfigID == null)
			throw new IllegalArgumentException("formulaPriceConfigID must not be null!");

		return getJDOObject(null, formulaPriceConfigID, fetchGroups, maxFetchDepth, monitor);
	}
}
