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

package org.nightlabs.jfire.reporting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.ReportEngine;
import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.nightlabs.jdo.NLJDOHelper;
import org.nightlabs.jdo.moduleregistry.ModuleMetaData;
import org.nightlabs.jfire.base.BaseSessionBeanImpl;
import org.nightlabs.jfire.config.ConfigSetup;
import org.nightlabs.jfire.config.UserConfigSetup;
import org.nightlabs.jfire.organisation.Organisation;
import org.nightlabs.jfire.reporting.config.ReportLayoutConfigModule;
import org.nightlabs.jfire.reporting.layout.ReportCategory;
import org.nightlabs.jfire.reporting.layout.ReportLayout;
import org.nightlabs.jfire.reporting.layout.ReportLayoutLocalisationData;
import org.nightlabs.jfire.reporting.layout.ReportRegistry;
import org.nightlabs.jfire.reporting.layout.ReportRegistryItem;
import org.nightlabs.jfire.reporting.layout.id.ReportRegistryItemID;
import org.nightlabs.jfire.reporting.layout.render.RenderManager;
import org.nightlabs.jfire.reporting.layout.render.RenderReportException;
import org.nightlabs.jfire.reporting.layout.render.RenderReportRequest;
import org.nightlabs.jfire.reporting.layout.render.RenderedReportLayout;
import org.nightlabs.jfire.reporting.layout.render.ReportLayoutRendererGeneric;
import org.nightlabs.jfire.reporting.layout.render.ReportLayoutRendererHTML;
import org.nightlabs.jfire.reporting.layout.render.ReportLayoutRendererPDF;
import org.nightlabs.jfire.reporting.layout.render.ReportLayoutRendererUtil;
import org.nightlabs.jfire.reporting.oda.JFireReportingOdaException;
import org.nightlabs.jfire.reporting.oda.jfs.IJFSQueryPropertySetMetaData;
import org.nightlabs.jfire.reporting.oda.jfs.JFSQueryPropertySet;
import org.nightlabs.jfire.reporting.oda.jfs.ScriptExecutorJavaClassReporting;
import org.nightlabs.jfire.reporting.oda.server.jfs.ServerJFSQueryProxy;
import org.nightlabs.jfire.reporting.platform.ServerPlatformContext;
import org.nightlabs.jfire.reporting.scripting.ScriptingInitialiser;
import org.nightlabs.jfire.scripting.ScriptException;
import org.nightlabs.jfire.scripting.ScriptRegistry;
import org.nightlabs.jfire.scripting.ScriptingIntialiserException;
import org.nightlabs.jfire.scripting.id.ScriptRegistryItemID;
import org.nightlabs.jfire.security.Authority;
import org.nightlabs.jfire.security.ResolveSecuringAuthorityStrategy;
import org.nightlabs.jfire.security.User;
import org.nightlabs.jfire.security.id.RoleID;
import org.nightlabs.jfire.servermanager.JFireServerManager;
import org.nightlabs.jfire.timer.Task;
import org.nightlabs.jfire.timer.id.TaskID;
import org.nightlabs.timepattern.TimePatternFormatException;
import org.nightlabs.version.MalformedVersionException;

/**
 * Manager that gives access to {@link ReportRegistryItem}s and other objects linked to them.
 * This is also the entry point for rendering report layouts.
 *
 * @author Alexander Bieber <alex[AT]nightlabs[DOT]de>
 *
 * @ejb.bean name="jfire/ejb/JFireReporting/ReportManager"
 *					 jndi-name="jfire/ejb/JFireReporting/ReportManager"
 *					 type="Stateless"
 *					 transaction-type="Container"
 *
 * @ejb.util generate="physical"
 * @ejb.transaction type="Required"
 */
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@TransactionManagement(TransactionManagementType.CONTAINER)
@Stateless
public class ReportManagerBean
extends BaseSessionBeanImpl
implements ReportManagerRemote
{
	private static final long serialVersionUID = 20080926L;

	private static final Logger logger = Logger.getLogger(ReportManagerBean.class);

	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.reporting.ReportManagerRemote#ping(java.lang.String)
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@RolesAllowed("_Guest_")
	@Override
	public String ping(String message) {
		return super.ping(message);
	}

	/**
	 * Called by {@link #initialise()} and registeres the reporting
	 * config-modules in their config-setup.
	 */
	private void initRegisterConfigModules(PersistenceManager pm)
	{
		// Register all Reporting-ConfigModules
		ConfigSetup configSetup = ConfigSetup.getConfigSetup(
				pm,
				getOrganisationID(),
				UserConfigSetup.CONFIG_SETUP_TYPE_USER
			);
		configSetup.getConfigModuleClasses().add(ReportLayoutConfigModule.class.getName());
		ConfigSetup.ensureAllPrerequisites(pm);
	}

	/**
	 *
	 * @param pm
	 * @param jfireServerManager
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private void initRegisterScripts(PersistenceManager pm, JFireServerManager jfireServerManager) throws InstantiationException, IllegalAccessException
	{
		ScriptRegistry.getScriptRegistry(pm).registerScriptExecutorClass(ScriptExecutorJavaClassReporting.class);
	}

	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.reporting.ReportManagerRemote#initialise()
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@RolesAllowed("_System_")
	public void initialise() throws InstantiationException, IllegalAccessException, MalformedVersionException, ScriptingIntialiserException
	{
		// TODO: Better check if platform initialized. Propose on birt forum.
		if (false) {
			JFireServerManager jfireServerManager = getJFireServerManager();
			try {
				try {

					ServerPlatformContext platformContext = new ServerPlatformContext();
					System.setProperty(Platform.PROPERTY_BIRT_HOME, platformContext.getPlatform());
					EngineConfig config = new EngineConfig();
					config.setEngineHome(platformContext.getPlatform());
					config.setLogConfig(platformContext.getPlatform(), java.util.logging.Level.ALL);

					config.setPlatformContext(platformContext);
					Platform.startup(config);
//					Platform.initialize(platformContext);
				} catch (Throwable t) {
					logger.log(Level.ERROR, "Initializing BIRT Platform failed!", t);
				}
			} finally {
				jfireServerManager.close();
			}
		}

		try {
			InitialContext initialContext = new InitialContext();
			try {
				new ReportingManagerFactory(initialContext, getOrganisationID()); // registers itself in JNDI
			} finally {
				initialContext.close();
			}
		} catch (NamingException x) {
			throw new RuntimeException(x); // local JNDI should always be available!
		}

		PersistenceManager pm;
		pm = getPersistenceManager();
		JFireServerManager jfireServerManager = getJFireServerManager();
		try {

			// init layout renderer
			ReportRegistry registry = ReportRegistry.getReportRegistry(pm);
			try {
				registry.registerReportRenderer(Birt.OutputFormat.html.toString(), ReportLayoutRendererHTML.class);
				registry.registerReportRenderer(Birt.OutputFormat.pdf.toString(), ReportLayoutRendererPDF.class);
				for (Birt.OutputFormat format : Birt.OutputFormat.values()) {
					if (format != Birt.OutputFormat.html && format != Birt.OutputFormat.pdf) {
						registry.registerReportRenderer(format.toString(), ReportLayoutRendererGeneric.class);
					}
				}
			} catch (Exception e) {
				logger.warn("Could not initially register HTML ReportLayoutRenderer when initializing ReportRegistry.", e);
			}

			// Init scripts before module metat data check
			initRegisterScripts(pm, jfireServerManager);

			ModuleMetaData moduleMetaData = ModuleMetaData.getModuleMetaData(pm, JFireReportingEAR.MODULE_NAME);
			if (moduleMetaData == null) {

				logger.info("Initialization of JFireReporting started ...");


				// version is {major}.{minor}.{release}-{patchlevel}-{suffix}
				moduleMetaData = new ModuleMetaData(
						JFireReportingEAR.MODULE_NAME, "0.9.7-0-beta", "0.9.7-0-beta");
				pm.makePersistent(moduleMetaData);
				logger.info("Persisted ModuleMetaData for JFireReporting with version 0.9.7-0-beta");

				initRegisterConfigModules(pm);
				logger.info("Initialized Reporting ConfigModules");

			}

			logger.info("Intializing JFireReporting basic scripts");
			ScriptingInitialiser.initialise(pm, jfireServerManager, Organisation.DEV_ORGANISATION_ID);

			// intialise the cleanup of the render folder
			initialiseCleanupRenderedReportLayoutFoldersTask(pm);

		} finally {
			pm.close();
			jfireServerManager.close();
		}

	}

	/**
	 * Create (if necessary) a task that will cleanup the temporary report folders
	 * using {@link #cleanupRenderedReportLayoutFolders(TaskID)}.
	 */
	protected void initialiseCleanupRenderedReportLayoutFoldersTask(PersistenceManager pm) {
		TaskID taskID = TaskID.create(
				getOrganisationID(),
				Task.TASK_TYPE_ID_SYSTEM,
				ReportLayoutRendererUtil.class.getSimpleName()+"#cleanupRenderedReportLayoutFolders"
		);
		Task task = null;
		try {
			task = (Task) pm.getObjectById(taskID);
		} catch (JDOObjectNotFoundException e) {
			task = null;
		}
		if (task != null) {
			logger.debug("Task already initialised");
			return;
		}
		task = new Task(
				taskID,
				User.getUser(pm, getOrganisationID(), User.USER_ID_SYSTEM),
				ReportManagerRemote.class,
				"cleanupRenderedReportLayoutFolders"
			);
		task.getName().setText(Locale.ENGLISH.getLanguage(), "Cleanup rendered report layout folders");
		task.getDescription().setText(Locale.ENGLISH.getLanguage(), "This task deletes old folder used for rendering reports");
		try {
			task.getTimePatternSet().createTimePattern(
				"*", // year
				"*", // month
				"*", // day
				"*", // dayOfWeek
				"*", //  hour
				"15"); // minute
		} catch (TimePatternFormatException e) {
			throw new RuntimeException(e);
		}

		task.setEnabled(true);
		pm.makePersistent(task);
	}

	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.reporting.ReportManagerRemote#cleanupRenderedReportLayoutFolders(org.nightlabs.jfire.timer.id.TaskID)
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@RolesAllowed("_System_")
	public void cleanupRenderedReportLayoutFolders(TaskID taskID)
	throws Exception
	{
		PersistenceManager pm = getPersistenceManager();
		try {
			ReportLayoutRendererUtil.cleanupRenderedReportLayoutFolders();
		} finally {
			pm.close();
		}
	}

	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.reporting.ReportManagerRemote#getJFSResultSetMetaData(org.nightlabs.jfire.reporting.oda.jfs.JFSQueryPropertySet)
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@RolesAllowed("org.nightlabs.jfire.reporting.editReport")
	public IResultSetMetaData getJFSResultSetMetaData(JFSQueryPropertySet queryPropertySet) throws ScriptException, InstantiationException
	{
		PersistenceManager pm = getPersistenceManager();
		try {
			return ServerJFSQueryProxy.getJFSResultSetMetaData(pm, queryPropertySet.getScriptRegistryItemID(), queryPropertySet);
		} finally {
			pm.close();
		}
	}

	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.reporting.ReportManagerRemote#getJFSQueryPropertySetMetaData(org.nightlabs.jfire.scripting.id.ScriptRegistryItemID)
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@RolesAllowed("org.nightlabs.jfire.reporting.editReport")
	public IJFSQueryPropertySetMetaData getJFSQueryPropertySetMetaData(ScriptRegistryItemID scriptID) throws ScriptException, InstantiationException
	{
		PersistenceManager pm = getPersistenceManager();
		try {
			return ServerJFSQueryProxy.getJFSQueryPropertySetMetaData(pm, scriptID);
		} finally {
			pm.close();
		}
	}

	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.reporting.ReportManagerRemote#getJFSResultSet(org.nightlabs.jfire.reporting.oda.jfs.JFSQueryPropertySet, java.util.Map)
	 */
	@TransactionAttribute(TransactionAttributeType.NEVER)
	@RolesAllowed("org.nightlabs.jfire.reporting.editReport")
	public IResultSet getJFSResultSet(
			JFSQueryPropertySet queryPropertySet,
			Map<String, Object> parameters
		) throws ScriptException, InstantiationException
	{
		PersistenceManager pm = getPersistenceManager();
		try {
			return ServerJFSQueryProxy.getJFSResultSet(
					pm,
					queryPropertySet.getScriptRegistryItemID(),
					queryPropertySet,
					parameters
			);
		} finally {
			pm.close();
		}
	}

	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.reporting.ReportManagerRemote#getJFSParameterMetaData(org.nightlabs.jfire.scripting.id.ScriptRegistryItemID)
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@RolesAllowed("org.nightlabs.jfire.reporting.editReport")
	public IParameterMetaData getJFSParameterMetaData(
			ScriptRegistryItemID scriptRegistryItemID
		) throws JFireReportingOdaException
	{
		PersistenceManager pm = getPersistenceManager();
		try {
			return ServerJFSQueryProxy.getScriptParameterMetaData(
					pm,
					scriptRegistryItemID
			);
		} finally {
			pm.close();
		}
	}

	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.reporting.ReportManagerRemote#getReportRegistryItems(java.util.List, org.nightlabs.jfire.security.id.RoleID, java.lang.String[], int)
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@RolesAllowed("org.nightlabs.jfire.reporting.renderReport")
	public List<ReportRegistryItem> getReportRegistryItems (
			List<ReportRegistryItemID> reportRegistryItemIDs, RoleID filterRoleID,
			String[] fetchGroups, int maxFetchDepth
		)
	{
		PersistenceManager pm;
		pm = getPersistenceManager();
		try {
			pm.getFetchPlan().setMaxFetchDepth(maxFetchDepth);
			if (fetchGroups != null)
				pm.getFetchPlan().setGroups(fetchGroups);

			List<ReportRegistryItem> result = new ArrayList<ReportRegistryItem>();
			for (ReportRegistryItemID itemID : reportRegistryItemIDs) {
				ReportRegistryItem item = (ReportRegistryItem)pm.getObjectById(itemID);
				result.add(pm.detachCopy(item));
			}
			return Authority.filterSecuredObjects(pm, result, getPrincipal(), filterRoleID, ResolveSecuringAuthorityStrategy.organisation);
		} finally {
			pm.close();
		}
	}

	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.reporting.ReportManagerRemote#getReportRegistryItems(java.util.List, java.lang.String[], int)
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@RolesAllowed("org.nightlabs.jfire.reporting.renderReport")
	public List<ReportRegistryItem> getReportRegistryItems (
			List<ReportRegistryItemID> reportRegistryItemIDs,
			String[] fetchGroups, int maxFetchDepth
		)
	{
		return getReportRegistryItems(reportRegistryItemIDs, RoleConstants.renderReport, fetchGroups, maxFetchDepth);
	}
	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.reporting.ReportManagerRemote#getReportRegistryItemIDsForParent(org.nightlabs.jfire.reporting.layout.id.ReportRegistryItemID, org.nightlabs.jfire.security.id.RoleID)
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@RolesAllowed("org.nightlabs.jfire.reporting.renderReport")
	public Collection<ReportRegistryItemID> getReportRegistryItemIDsForParent(ReportRegistryItemID reportRegistryItemID, RoleID filterRoleID) {
		PersistenceManager pm;
		pm = getPersistenceManager();
		try {
			ReportRegistryItem item = (ReportRegistryItem) pm.getObjectById(reportRegistryItemID);
			Collection<ReportRegistryItemID> result = Collections.emptyList();
			if (item instanceof ReportCategory) {
				result = new ArrayList<ReportRegistryItemID>(ReportRegistryItem.getReportRegistryItemIDsForParent(pm, (ReportCategory) item));
			}
			return Authority.filterSecuredObjectIDs(pm, result, getPrincipal(), filterRoleID, ResolveSecuringAuthorityStrategy.organisation);
		} finally {
			pm.close();
		}
	}

	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.reporting.ReportManagerRemote#getReportRegistryItemIDsForParent(org.nightlabs.jfire.reporting.layout.id.ReportRegistryItemID)
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@RolesAllowed("org.nightlabs.jfire.reporting.renderReport")
	public Collection<ReportRegistryItemID> getReportRegistryItemIDsForParent(ReportRegistryItemID reportRegistryItemID) {
		return getReportRegistryItemIDsForParent(reportRegistryItemID, RoleConstants.renderReport);
	}

	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.reporting.ReportManagerRemote#getTopLevelReportRegistryItemIDs(org.nightlabs.jfire.security.id.RoleID)
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@RolesAllowed("org.nightlabs.jfire.reporting.renderReport")
	public Collection<ReportRegistryItemID> getTopLevelReportRegistryItemIDs(RoleID filterRoleID) {
		PersistenceManager pm;
		pm = getPersistenceManager();
		try {
			Collection<ReportRegistryItem> topLevelItems = ReportRegistryItem.getTopReportRegistryItems(pm, getOrganisationID());
			Collection<ReportRegistryItemID> result = new HashSet<ReportRegistryItemID>(topLevelItems.size());
			for (ReportRegistryItem item : topLevelItems) {
				result.add((ReportRegistryItemID) JDOHelper.getObjectId(item));
			}
			Authority.filterSecuredObjectIDs(pm, result, getPrincipal(), filterRoleID, ResolveSecuringAuthorityStrategy.organisation);
			return result;
		} finally {
			pm.close();
		}
	}

	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.reporting.ReportManagerRemote#getTopLevelReportRegistryItemIDs()
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@RolesAllowed("org.nightlabs.jfire.reporting.renderReport")
	public Collection<ReportRegistryItemID> getTopLevelReportRegistryItemIDs() {
		return getTopLevelReportRegistryItemIDs(RoleConstants.renderReport);
	}


	/**
	 * Get the {@link ReportingManagerFactory} for the actual organisationID.
	 *
	 * @return The {@link ReportingManagerFactory} for the actual organisationID.
	 */
	protected ReportingManagerFactory getReportingManagerFactory() throws NamingException
	{
		return ReportingManagerFactory.getReportingManagerFactory(getInitialContext(getOrganisationID()), getOrganisationID());
	}

	/**
	 * Get the {@link ReportEnginePool} of this server.
	 *
	 * @return The {@link ReportEnginePool} of this server.
	 */
	protected ReportEnginePool getReportEnginePool() throws NamingException
	{
		return ReportEnginePool.getInstance(getInitialContext(getOrganisationID()));
	}

	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.reporting.ReportManagerRemote#storeRegistryItem(org.nightlabs.jfire.reporting.layout.ReportRegistryItem, boolean, java.lang.String[], int)
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@RolesAllowed("org.nightlabs.jfire.reporting.editReport")
	public ReportRegistryItem storeRegistryItem (
			ReportRegistryItem reportRegistryItemToStore,
			boolean get,
			String[] fetchGroups, int maxFetchDepth
		)
	{
		PersistenceManager pm;
		pm = getPersistenceManager();
		try {
			// first check if user is allowed to store the registry item
			// check if user is allowed to render
			if (JDOHelper.isDetached(reportRegistryItemToStore)) {
				// if the object is not new, it might have an authority assigned and
				// role checking on EJB method call level might not be sufficient
				ReportRegistryItem registryItemPersistent = (ReportRegistryItem) pm.getObjectById(JDOHelper.getObjectId(reportRegistryItemToStore));
				Authority.resolveSecuringAuthority(pm, registryItemPersistent, ResolveSecuringAuthorityStrategy.organisation)
					.assertContainsRoleRef(getPrincipal(), RoleConstants.renderReport);
			}

			ReportRegistryItem item = pm.makePersistent(reportRegistryItemToStore);

			item.applyInheritance();

			if (!get)
				return null;

			if (fetchGroups != null)
				pm.getFetchPlan().setGroups(fetchGroups);
			pm.getFetchPlan().setMaxFetchDepth(maxFetchDepth);

			return pm.detachCopy(item);
		} finally {
			pm.close();
		}
	}


	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.reporting.ReportManagerRemote#deleteRegistryItem(org.nightlabs.jfire.reporting.layout.id.ReportRegistryItemID)
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@RolesAllowed("org.nightlabs.jfire.reporting.editReport")
	public void deleteRegistryItem (ReportRegistryItemID reportRegistryItemID)
	{
		PersistenceManager pm;
		pm = getPersistenceManager();
		try {
			ReportRegistryItem item = null;
			try {
				item = (ReportRegistryItem) pm.getObjectById(reportRegistryItemID);
			} catch (JDOObjectNotFoundException e) {
				return;
			}
			// Do the object level access right check also before deleting
			Authority.resolveSecuringAuthority(pm, item, ResolveSecuringAuthorityStrategy.organisation)
				.assertContainsRoleRef(getPrincipal(), RoleConstants.renderReport);
			ReportCategory parent = item.getParentCategory();
			if (parent != null && (parent instanceof ReportCategory)) {
				parent.getChildItems().remove(item);
			}
			pm.deletePersistent(item);
		} finally {
			pm.close();
		}
	}


	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.reporting.ReportManagerRemote#renderReportLayout(org.nightlabs.jfire.reporting.layout.render.RenderReportRequest)
	 */
	@TransactionAttribute(TransactionAttributeType.NEVER)
	@RolesAllowed("org.nightlabs.jfire.reporting.renderReport")
	public RenderedReportLayout renderReportLayout(RenderReportRequest renderReportRequest)
	throws NamingException, RenderReportException
	{
		PersistenceManager pm;
		pm = getPersistenceManager();
		try {
			// check if user is allowed to render
			ReportRegistryItem registryItem = (ReportRegistryItem) pm.getObjectById(renderReportRequest.getReportRegistryItemID());
			Authority.resolveSecuringAuthority(pm, registryItem, ResolveSecuringAuthorityStrategy.organisation)
				.assertContainsRoleRef(getPrincipal(), RoleConstants.renderReport);

//			RenderManager rm = getReportingManagerFactory().createRenderManager();
			ReportEnginePool enginePool = getReportEnginePool();
			ReportEngine engine;
			try {
				engine = enginePool.borrowReportEngine();
			} catch (Exception e) {
				throw new RenderReportException("Could not borrow ReportEngine.", e);
			}
			try {
				RenderManager rm = new RenderManager(engine);
				return rm.renderReport(pm, renderReportRequest);
			} finally {
				try {
					enginePool.returnReportEngine(engine);
				} catch (Exception e) {
					throw new RenderReportException("Could not return ReportEngine", e);
				}
			}
		} finally {
			pm.close();
		}
	}

	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.reporting.ReportManagerRemote#execJDOQL(java.lang.String, java.util.Map, java.lang.String[])
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@RolesAllowed("org.nightlabs.jfire.reporting.editReport")
	public Collection execJDOQL (
			String jdoql,
			Map params,
			String[] fetchGroups
		)
	{
		PersistenceManager pm;
		pm = getPersistenceManager();
		try {
			Query q = pm.newQuery(jdoql);
			logger.info("Excecuting JDOQL : ");
			logger.info("");
			logger.info(jdoql);
			logger.info("");
			if (fetchGroups != null)
				pm.getFetchPlan().setGroups(fetchGroups);
			Object result = q.executeWithMap(params);
			if (result instanceof Collection)
				return NLJDOHelper.getDetachedQueryResultAsList(pm, (Collection<?>)result);
			else
				return NLJDOHelper.getDetachedQueryResultAsList(pm, result);
		} finally {
			pm.close();
		}
	}

	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.reporting.ReportManagerRemote#getReportLayoutLocalisationBundle(org.nightlabs.jfire.reporting.layout.id.ReportRegistryItemID, java.lang.String[], int)
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@RolesAllowed("org.nightlabs.jfire.reporting.editReport")
	public Collection<ReportLayoutLocalisationData> getReportLayoutLocalisationBundle(
			ReportRegistryItemID reportLayoutID,
			String[] fetchGroups, int maxFetchDepth
	) {
		PersistenceManager pm;
		pm = getPersistenceManager();
		try {
			pm.getFetchPlan().setMaxFetchDepth(maxFetchDepth);
			if (fetchGroups != null)
				pm.getFetchPlan().setGroups(fetchGroups);

			ReportLayout reportLayout = (ReportLayout) pm.getObjectById(reportLayoutID);

			// now do the access right check also on object level
			Authority.resolveSecuringAuthority(pm, reportLayout, ResolveSecuringAuthorityStrategy.organisation)
				.assertContainsRoleRef(getPrincipal(), RoleConstants.editReport);

			Collection<ReportLayoutLocalisationData> bundle = ReportLayoutLocalisationData.getReportLayoutLocalisationBundle(pm, reportLayout);
			return pm.detachCopyAll(bundle);
		} finally {
			pm.close();
		}
	}

	/* (non-Javadoc)
	 * @see org.nightlabs.jfire.reporting.ReportManagerRemote#storeReportLayoutLocalisationBundle(java.util.Collection, boolean, java.lang.String[], int)
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@RolesAllowed("org.nightlabs.jfire.reporting.editReport")
	public Collection<ReportLayoutLocalisationData> storeReportLayoutLocalisationBundle(
			Collection<ReportLayoutLocalisationData> bundle, boolean get, String[] fetchGroups, int maxFetchDepth
	) {
		PersistenceManager pm;
		pm = getPersistenceManager();
		try {
			pm.getFetchPlan().setMaxFetchDepth(maxFetchDepth);
			if (fetchGroups != null)
				pm.getFetchPlan().setGroups(fetchGroups);

			Set<ReportRegistryItemID> checkItemIDs = new HashSet<ReportRegistryItemID>();
			// collect all item IDs to check because its possible that there are several referenced in the collection of bundles
			for (ReportLayoutLocalisationData data : bundle) {
				ReportRegistryItemID itemID = ReportRegistryItemID.create(
						data.getOrganisationID(), data.getReportRegistryItemType(), data.getReportRegistryItemID());
				checkItemIDs.add(itemID);
			}
			// now check the access rights for all referenced
			for (ReportRegistryItemID itemID : checkItemIDs) {
				ReportRegistryItem item = (ReportRegistryItem) pm.getObjectById(itemID);
				Authority.resolveSecuringAuthority(pm, item, ResolveSecuringAuthorityStrategy.organisation)
					.assertContainsRoleRef(getPrincipal(), RoleConstants.editReport);
			}
			if (get) {
				Collection<ReportLayoutLocalisationData> result = new ArrayList<ReportLayoutLocalisationData>(bundle.size());
				for (ReportLayoutLocalisationData data : bundle) {
					result.add(NLJDOHelper.storeJDO(pm, data, get, fetchGroups, maxFetchDepth));
				}
				return result;
			}
			else {
				for (ReportLayoutLocalisationData data : bundle) {
					pm.makePersistent(data);
				}
				return null;
			}
		} finally {
			pm.close();
		}
	}
}
