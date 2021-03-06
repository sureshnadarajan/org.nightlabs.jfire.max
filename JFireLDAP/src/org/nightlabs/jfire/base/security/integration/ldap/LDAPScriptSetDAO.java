package org.nightlabs.jfire.base.security.integration.ldap;

import java.util.Collection;
import java.util.Set;

import org.nightlabs.jdo.NLJDOHelper;
import org.nightlabs.jfire.base.jdo.BaseJDOObjectDAO;
import org.nightlabs.jfire.base.security.integration.ldap.id.LDAPScriptSetID;
import org.nightlabs.jfire.base.security.integration.ldap.scripts.ILDAPScriptProvider;
import org.nightlabs.jfire.security.User;
import org.nightlabs.jfire.security.id.UserID;
import org.nightlabs.jfire.security.integration.id.UserManagementSystemID;
import org.nightlabs.progress.ProgressMonitor;
import org.nightlabs.progress.SubProgressMonitor;

/**
 * Get {@link LDAPScriptSet} JDO objects using the JFire client cache.
 *
 * @author Denis Dudnik <deniska.dudnik[at]gmail{dot}com>
 * 
 */
public class LDAPScriptSetDAO extends BaseJDOObjectDAO<LDAPScriptSetID, LDAPScriptSet> {

	private static LDAPScriptSetDAO sharedInstance = null;

	/**
	 * Get the lazily created shared instance.
	 * @return The shared instance
	 */
	public static LDAPScriptSetDAO sharedInstance(){
		if (sharedInstance == null) {
			synchronized (LDAPScriptSetDAO.class) {
				if (sharedInstance == null)
					sharedInstance = new LDAPScriptSetDAO();
			}
		}
		return sharedInstance;
	}

	private LDAPScriptSetDAO(){
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Collection<LDAPScriptSet> retrieveJDOObjects(Set<LDAPScriptSetID> objectIDs, String[] fetchGroups, int maxFetchDepth, ProgressMonitor monitor) throws Exception {
		monitor.beginTask("Fetching "+objectIDs.size()+" ldap script set(s)", 1);
		try{
			LDAPManagerRemote remoteBean = getEjbProvider().getRemoteBean(LDAPManagerRemote.class);
			Collection<LDAPScriptSet> ldapScriptSets = remoteBean.getLDAPScriptSets(objectIDs, fetchGroups, maxFetchDepth);
			monitor.worked(1);
			return ldapScriptSets;
		}catch(Exception e) {
			monitor.setCanceled(true);
			throw new RuntimeException("Failed fetching ldap script set(s) data!", e);
		}finally{
			monitor.done();
		}
	}
	
	/**
	 * Get {@link LDAPScriptSet} by corresponding {@link LDAPServer} ID.
	 * 
	 * @param ldapServerID ID of {@link LDAPServer} object
	 * @param fetchGroups Wich fetch groups to use
	 * @param maxFetchDepth Fetch depth or {@link NLJDOHelper#MAX_FETCH_DEPTH_NO_LIMIT}
	 * @param monitor The progress monitor for this action
	 * @return found {@link LDAPScriptSet}
	 */
	public synchronized LDAPScriptSet getLDAPScriptSetByLDAPServerID(UserManagementSystemID ldapServerID, String[] fetchgroups, int maxFetchDepth, ProgressMonitor monitor){
		monitor.beginTask("Load LDAP script set", 1);
		try{
			LDAPManagerRemote remoteBean = getEjbProvider().getRemoteBean(LDAPManagerRemote.class);
			LDAPScriptSetID id = remoteBean.getLDAPScriptSetID(ldapServerID);
			monitor.worked(1);
			return getJDOObject(null, id, fetchgroups, maxFetchDepth, new SubProgressMonitor(monitor, 1));
		}catch(Exception e){
			monitor.setCanceled(true);
			throw new RuntimeException("Failed to load LDAP scrip set!", e);
		}finally{
			monitor.done();
		}
	}

	/**
	 * Stores a {@link LDAPScriptSet} on the server.
	 * 
	 * @param ldapScriptSet The {@link LDAPScriptSet} to store, will throw {@link IllegalArgumentException} if <code>null</code>
	 * @param get If stored object should be detached and returned
	 * @param fetchGroups Which fetch groups to use
	 * @param maxFetchDepth Fetch depth or {@link NLJDOHelper#MAX_FETCH_DEPTH_NO_LIMIT}
	 * @param monitor The progress monitor for this action
	 * @return Stored detached {@link LDAPScriptSet} object if <code>get</code> is <code>true</code> or <code>null</code> otherwise
	 */
	public synchronized LDAPScriptSet storeLDAPScriptSet(LDAPScriptSet ldapScriptSet, boolean get, String[] fetchGroups, int maxFetchDepth, ProgressMonitor monitor){
		if(ldapScriptSet == null){
			throw new IllegalArgumentException("LDAPScriptSet to store must not be null!");
		}

		monitor.beginTask("Storing LDAPScriptSet", 3);
		try{
			LDAPManagerRemote remoteBean = getEjbProvider().getRemoteBean(LDAPManagerRemote.class);
			monitor.worked(1);

			LDAPScriptSet result = remoteBean.storeLDAPScriptSet(ldapScriptSet, get, fetchGroups, maxFetchDepth);
			if (result != null){
				getCache().put(null, result, fetchGroups, maxFetchDepth);
			}

			monitor.worked(1);

			return result;
		}catch (Exception e){
			monitor.setCanceled(true);
			throw new RuntimeException("Failed to store LDAPScriptSet!", e);
		}finally{
			monitor.done();
		}
	}

	/**
	 * Get initial script content by specified scriptID (one of values from {@link ILDAPScriptProvider})
	 * 
	 * @param ldapScriptSetID ID of the {@link LDAPScriptSet} where initial script content will be set, can't be <code>null</code>
	 * @param scriptID ID of script wich will be rolled back to initial content, can't be <code>null</code>
	 * @param monitor The progress monitor to use
	 * @return initial script content
	 */
	public synchronized String getInitialScriptContent(LDAPScriptSetID ldapScriptSetID, String scriptID, ProgressMonitor monitor){
		if (ldapScriptSetID == null){
			throw new IllegalArgumentException("LDAPScriptSetID can't be null!");
		}
		if (scriptID == null){
			throw new IllegalArgumentException("scriptID can't be null!");
		}
		
		monitor.beginTask("Getting initial content for script " + scriptID, 3);
		try{
			LDAPManagerRemote remoteBean = getEjbProvider().getRemoteBean(LDAPManagerRemote.class);
			monitor.worked(1);
			String inititalScriptContent = remoteBean.getInitialScriptContent(ldapScriptSetID, scriptID);
			monitor.worked(1);
			return inititalScriptContent;
		}catch (Exception e){
			monitor.setCanceled(true);
			throw new RuntimeException("Failed to get initial script content for scriptID " + scriptID, e);
		}finally{
			monitor.done();
		}
	}
	
	/**
	 * Get LDAP entry name for given {@link UserID} using {@link LDAPServer} by given {@link UserManagementSystemID}.
	 * 
	 * @param ldapServerID The ID of {@link LDAPServer} which will generate LDAP entry name
	 * @param userID The ID of a {@link User} to generate LDAP entry name for
	 * @return full LDAP entry name as a {@link String}
	 */
	public synchronized String getLDAPEntryName(UserManagementSystemID ldapServerID, UserID userID, ProgressMonitor monitor){
		if (ldapServerID == null){
			throw new IllegalArgumentException("LDAPServer ID can't be null!");
		}
		if (userID == null){
			throw new IllegalArgumentException("userID can't be null!");
		}

		monitor.beginTask("Getting LDAP entry name for user " + userID.toString(), 3);
		try{
			LDAPManagerRemote remoteBean = getEjbProvider().getRemoteBean(LDAPManagerRemote.class);
			monitor.worked(1);
			String ldapEntryName = remoteBean.getLDAPEntryName(ldapServerID, userID);
			monitor.worked(1);
			return ldapEntryName;
		}catch (Exception e){
			monitor.setCanceled(true);
			throw new RuntimeException("Failed to get LDAP entry name for user " + userID.toString(), e);
		}finally{
			monitor.done();
		}
	}
}
