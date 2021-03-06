package org.nightlabs.jfire.base.security.integration.ldap.connection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InsufficientResourcesException;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.ServiceUnavailableException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.event.EventContext;
import javax.naming.event.EventDirContext;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.naming.ldap.UnsolicitedNotificationEvent;
import javax.naming.ldap.UnsolicitedNotificationListener;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.security.auth.login.LoginException;

import org.nightlabs.jfire.base.security.integration.ldap.LDAPServer;
import org.nightlabs.jfire.base.security.integration.ldap.attributes.LDAPAttribute;
import org.nightlabs.jfire.base.security.integration.ldap.attributes.LDAPAttributeSet;
import org.nightlabs.jfire.base.security.integration.ldap.connection.ILDAPConnectionParamsProvider.AuthenticationMethod;
import org.nightlabs.jfire.base.security.integration.ldap.connection.ILDAPConnectionParamsProvider.EncryptionMethod;
import org.nightlabs.jfire.security.integration.UserManagementSystemCommunicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A connection wrapper that uses JNDI.
 * 
 * @author Denis Dudnik <deniska.dudnik[at]gmail{dot}com>
 */
public class JNDIConnectionWrapper implements LDAPConnectionWrapper{

	private static final Logger logger = LoggerFactory.getLogger(JNDIConnectionWrapper.class);
	
	/**
	 * This map holds attributes together with their known aliases, key is alias, value is full name of attribute
	 */
	private static final Map<String, String> attributeAliases = new HashMap<String, String>();
	static{
		attributeAliases.put("cn", "commonName");
		attributeAliases.put("sn", "surname");
		attributeAliases.put("fax", "facsimileTelephoneNumber");
		attributeAliases.put("gn", "givenName");
		attributeAliases.put("homePhone", "homeTelephoneNumber");
		attributeAliases.put("l", "locality");
		attributeAliases.put("mail", "rfc822Mailbox");
		attributeAliases.put("mobile", "mobileTelephoneNumber");
		attributeAliases.put("o", "organizationName");
		attributeAliases.put("ou", "organizationalUnitName");
		attributeAliases.put("pager", "pagerTelephoneNumber");
		attributeAliases.put("st", "stateOrProvinceName");
		attributeAliases.put("street", "streetAddress");
		attributeAliases.put("uid", "userid");
	}

	private static final String JAVA_NAMING_LDAP_VERSION = "java.naming.ldap.version"; //$NON-NLS-1$
	private static final String JAVA_NAMING_LDAP_FACTORY_SOCKET = "java.naming.ldap.factory.socket"; //$NON-NLS-1$
	private static final String JAVA_NAMING_SECURITY_SASL_REALM = "java.naming.security.sasl.realm"; //$NON-NLS-1$
	private static final String JAVAX_SECURITY_SASL_QOP = "javax.security.sasl.qop"; //$NON-NLS-1$
	private static final String COM_SUN_JNDI_DNS_TIMEOUT_RETRIES = "com.sun.jndi.dns.timeout.retries"; //$NON-NLS-1$
	private static final String COM_SUN_JNDI_DNS_TIMEOUT_INITIAL = "com.sun.jndi.dns.timeout.initial"; //$NON-NLS-1$
	private static final String COM_SUN_JNDI_LDAP_CONNECT_TIMEOUT = "com.sun.jndi.ldap.connect.timeout"; //$NON-NLS-1$
	private static final String COM_SUN_JNDI_LDAP_CONNECT_POOL = "com.sun.jndi.ldap.connect.pool"; //$NON-NLS-1$
	private static final String LDAP_SCHEME = "ldap://";
	private static final String LDAPS_SCHEME = "ldaps://";

	private ILDAPConnectionParamsProvider connectionParamsProvider;

	private InitialLdapContext context;

	private AtomicBoolean isConnected;

	/**
	 * Creates a new instance of JNDIConnectionContext.
	 * 
	 * @param connection
	 *            the connection
	 */
	public JNDIConnectionWrapper(ILDAPConnectionParamsProvider connectionParamsProvider) {
		if (connectionParamsProvider == null){
			throw new IllegalArgumentException("ILDAPConnectionParamsProvider can't be null!");
		}
		this.connectionParamsProvider = connectionParamsProvider;
		this.isConnected = new AtomicBoolean(false);
	}
	
	/**
	 * Get underlying {@link InitialLdapContext}.
	 * 
	 * @return
	 */
	public InitialLdapContext getContext() {
		return context;
	}

	/**
	 * {@inheritDoc}
	 * @throws CommunicationException 
	 */
	@Override
	public void connect() throws UserManagementSystemCommunicationException {

		context = null;

		String host = connectionParamsProvider.getHost();
		int port = connectionParamsProvider.getPort();
		boolean useLdaps = connectionParamsProvider.getEncryptionMethod() == EncryptionMethod.LDAPS;
        boolean useStartTLS = connectionParamsProvider.getEncryptionMethod() == EncryptionMethod.START_TLS;

		try{
			Hashtable<String, String> environment = new Hashtable<String, String>();
			synchronized (environment) {
				environment.put(JAVA_NAMING_LDAP_VERSION, "3"); //$NON-NLS-1$
				environment.put(Context.INITIAL_CONTEXT_FACTORY, getDefaultLdapContextFactory());
				environment.put(COM_SUN_JNDI_LDAP_CONNECT_POOL, "false"); //$NON-NLS-1$
				
		        // Don't use a timeout when using ldaps: JNDI throws a SocketException when setting a timeout on SSL connections.
		        if (!useLdaps){
		        	environment.put(COM_SUN_JNDI_LDAP_CONNECT_TIMEOUT, "30000"); //$NON-NLS-1$
		        }
				environment.put(COM_SUN_JNDI_DNS_TIMEOUT_INITIAL, "2000"); //$NON-NLS-1$
				environment.put(COM_SUN_JNDI_DNS_TIMEOUT_RETRIES, "3"); //$NON-NLS-1$
				
		        if (useLdaps) {
		            environment.put(Context.PROVIDER_URL, LDAPS_SCHEME + host + ':' + port);
		            environment.put(Context.SECURITY_PROTOCOL, "ssl"); //$NON-NLS-1$
		            environment.put(JAVA_NAMING_LDAP_FACTORY_SOCKET, DummySSLSocketFactory.class.getName());
		        }else{
		        	environment.put(Context.PROVIDER_URL, LDAP_SCHEME + host + ':' + port);
		        }

				if (logger.isDebugEnabled()){
					logger.debug("Connecting to LDAP server with params: " + environment.toString());
				}
				
				context = new InitialLdapContext(environment, null);
				
                if (useStartTLS){
                    try{
                        StartTlsResponse tls = (StartTlsResponse) context.extendedOperation(new StartTlsRequest());
                        tls.setHostnameVerifier(new HostnameVerifier(){
                            public boolean verify(String hostname, SSLSession session){
                                return true;
                            }
                        });
                        tls.negotiate(DummySSLSocketFactory.getDefault());
                    } catch (Exception e){
                    	NamingException namingException = new NamingException(e.getMessage() != null ? e.getMessage() : "Error while establishing TLS session"); //$NON-NLS-1$
                        namingException.setRootCause(e);
                        throw namingException;
                    }
                }

				isConnected.set(true);
				
				configureConnectionProblemsListener();
			}
			
		}catch(NamingException e){
			logger.error(String.format("Can't connect to LDAP server at %s:%s", host, port), e);
			
			disconnect();
			
			throw new UserManagementSystemCommunicationException(
					String.format(
							"Can't connect to LDAP server at %s:%s, see log for details. Cause: %s", host, port, e.getMessage()
							), e);
		}
		
	}

	/**
	 * {@inheritDoc}
	 * @throws CommunicationException 
	 * @throws AuthenticationException 
	 * 
	 * @throws NamingException
	 */
	@Override
	public void bind(
			String bindPrincipal, String bindCredentials
			) throws UserManagementSystemCommunicationException, LoginException {

		if (isConnected()) {
			
			String authMethod = AuthenticationMethod.NONE.stringValue();
			boolean useSASL = false;
			if (AuthenticationMethod.SIMPLE.equals(connectionParamsProvider.getAuthenticationMethod())) {
				authMethod = AuthenticationMethod.SIMPLE.stringValue();
			}else if (AuthenticationMethod.SASL_DIGEST_MD5.equals(connectionParamsProvider.getAuthenticationMethod())) {
				authMethod = AuthenticationMethod.SASL_DIGEST_MD5.stringValue();
				useSASL = true;
			}else if (AuthenticationMethod.SASL_CRAM_MD5.equals(connectionParamsProvider.getAuthenticationMethod())) {
				authMethod = AuthenticationMethod.SASL_CRAM_MD5.stringValue();
				useSASL = true;
			}

			// setup credentials
			try{
				synchronized (context) {
					context.removeFromEnvironment(Context.SECURITY_AUTHENTICATION);
					context.removeFromEnvironment(Context.SECURITY_PRINCIPAL);
					context.removeFromEnvironment(Context.SECURITY_CREDENTIALS);
                    context.removeFromEnvironment(JAVA_NAMING_SECURITY_SASL_REALM);

					context.addToEnvironment(Context.SECURITY_AUTHENTICATION, authMethod);

                    if (useSASL) {
                        // Request quality of protection
                    	context.addToEnvironment(JAVAX_SECURITY_SASL_QOP, "auth-conf,auth-int,auth");
                    	if (AuthenticationMethod.SASL_DIGEST_MD5.equals(authMethod)){
                    		String saslRealm = connectionParamsProvider.getSASLRealm(bindPrincipal);
                    		if (saslRealm != null && !saslRealm.isEmpty()){
                    			context.addToEnvironment(JAVA_NAMING_SECURITY_SASL_REALM, saslRealm);
                    		}
                    	}
                    }

					context.addToEnvironment(Context.SECURITY_PRINCIPAL, bindPrincipal);
					context.addToEnvironment(Context.SECURITY_CREDENTIALS, bindCredentials);
                    context.reconnect( context.getConnectControls() );
				}
			}catch(NamingException e){
				logger.error(String.format("Failed to bind against LDAP server at %s:%s", connectionParamsProvider.getHost(), connectionParamsProvider.getPort()), e);
				
				unbind();
				
				throw new LoginException(
						"LDAP login failed, see log for details. Cause: " + e.getMessage()
						);
			}
		}else{
			throw new UserManagementSystemCommunicationException(
					String.format("No connection to LDAP server at %s:%s", connectionParamsProvider.getHost(), connectionParamsProvider.getPort())
					);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void unbind() {
		if (isAuthenticated()) {
			try{
				synchronized(context){
					context.removeFromEnvironment(Context.SECURITY_AUTHENTICATION);
					context.removeFromEnvironment(Context.SECURITY_PRINCIPAL);
					context.removeFromEnvironment(Context.SECURITY_CREDENTIALS);
		
					context = new InitialLdapContext(context.getEnvironment(), context.getConnectControls());
				}
			}catch(NamingException e){
				logger.error(
						String.format("Failed to unbind on LDAP server at %s:%s", connectionParamsProvider.getHost(), connectionParamsProvider.getPort()), e
						);
				disconnect(); 
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public void createEntry(String dn, LDAPAttributeSet attributes) throws UserManagementSystemCommunicationException, LoginException{
        try {
        	synchronized (context) {
                context.createSubcontext(getSafeJndiName(dn), getJNDIAttributes(attributes));
			}
		} catch (NoPermissionException e){
			logger.error(e.getMessage(), e);
			throw new LoginException(
					String.format("Insufficient permissions! Failed to create an entry %s. Exception: %s", dn, e.getMessage()));
        }catch(NamingException e){
			throw new UserManagementSystemCommunicationException("Failed to create an entry! Entry DN: " + dn, e);
        }
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
    public void deleteEntry(String dn) throws UserManagementSystemCommunicationException, LoginException{
        try {
        	synchronized (context) {
                context.destroySubcontext(getSafeJndiName(dn));
			}
		} catch (NoPermissionException e){
			logger.error(e.getMessage(), e);
			throw new LoginException(
					String.format("Insufficient permissions! Failed to delete an entry %s. Exception: %s", dn, e.getMessage()));
        }catch(NamingException e){
			throw new UserManagementSystemCommunicationException("Failed to delete an entry! Entry DN: " + dn, e);
        }
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String modifyEntry(
			String dn, LDAPAttributeSet attributes, EntryModificationFlag modificationFlag
			) throws UserManagementSystemCommunicationException, LoginException {
		try{
	        // determine operation type
	        int operationType = DirContext.REPLACE_ATTRIBUTE;
	        switch(modificationFlag){
	        case MODIFY:
	        	operationType = DirContext.REPLACE_ATTRIBUTE;
	        	break;
	        case REMOVE:
	        	operationType = DirContext.REMOVE_ATTRIBUTE;
	        	break;
	        default: 
		        operationType = DirContext.REPLACE_ATTRIBUTE;
	        }
	        
	        Name entryName = getSafeJndiName(dn);
	        Name originalName = (Name) entryName.clone();
	        
	        // check if modified attributes contain RDN related attributes, 
	        // need to call context.rename() if found and remove such attributes from modification ones
	        Collection<String> attributesNames = attributes.getAllAttributesNames();
	        Enumeration<String> nameComponents = originalName.getAll();
	        int namePos = 0;
	        Collection<LDAPAttribute<Object>> attrsToDelete = new ArrayList<LDAPAttribute<Object>>();
	        while (nameComponents.hasMoreElements()) {
				String component = (String) nameComponents.nextElement();
		        for (String attrName : attributesNames) {
					if (component.matches("^"+attrName+"=(.+)$")
							|| (attributeAliases.containsKey(attrName) && component.matches("^"+attributeAliases.get(attrName)+"=(.+)$"))
							|| (attributeAliases.containsValue(attrName) && component.matches("^"+getAliasMapKeyByValue(attrName)+"=(.+)$"))){
						entryName.remove(namePos);
						entryName.add(namePos, component.replaceAll("^(.+)=(.+)$", "$1="+attributes.getAttributeValue(attrName)));
						attrsToDelete.add(attributes.getAttribute(attrName));
					}
				}
		        namePos++;
			}
	        attributes.removeAttributes(attrsToDelete);
	        String newName = null;
	        
	        synchronized (context) {
		        if (originalName.compareTo(entryName) != 0){
		        	context.rename(originalName, entryName);
		        	newName = entryName.toString();
		        }

		        // prepare modification items
		        Attributes translatedAttributes = getJNDIAttributes(attributes);
		        ModificationItem[] modificationItems = new ModificationItem[translatedAttributes.size()];
		        int i = 0;
		        for (Enumeration<? extends Attribute> attributesEnum = translatedAttributes.getAll(); attributesEnum.hasMoreElements();) {
		        	modificationItems[i] = new ModificationItem(operationType, attributesEnum.nextElement());
		        	i++;
				}

		        // perform modification
				context.modifyAttributes(entryName, modificationItems);
				
				return newName;
			}
		} catch (NoPermissionException e){
			logger.error(e.getMessage(), e);
			throw new LoginException(
					String.format("Insufficient permissions! Entry modification failed (%s). Exception: %s", dn, e.getMessage()));
		}catch(NamingException e){
			throw new UserManagementSystemCommunicationException("Entry modification failed! Entry: " + dn, e);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, LDAPAttributeSet> search(String baseDN, LDAPAttributeSet searchAttributes, String[] returnAttributes) 
			throws UserManagementSystemCommunicationException, LoginException {
		try{
			synchronized (context){
				return processResult(
						context.search(getSafeJndiName(getSafeSearchBaseDN(baseDN)), getJNDIAttributes(searchAttributes), returnAttributes));
			}
		} catch (NoPermissionException e){
			logger.error(e.getMessage(), e);
			throw new LoginException("Insufficient permissions! Search failed! Exception: " + e.getMessage());
		}catch(NamingException e){
			throw new UserManagementSystemCommunicationException("Search failed!" , e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, LDAPAttributeSet> search(String baseDN, String filterExpr, Object[] filterArgs, SearchScope searchScope)
			throws UserManagementSystemCommunicationException, LoginException {
		try{
			synchronized (context){
				int searchScopeValue = SearchControls.OBJECT_SCOPE;
				switch (searchScope) {
				case OBJECT:
					searchScopeValue = SearchControls.OBJECT_SCOPE;
					break;
				case ONELEVEL:
					searchScopeValue = SearchControls.ONELEVEL_SCOPE;
					break;
				case SUBTREE:
					searchScopeValue = SearchControls.SUBTREE_SCOPE;
					break;
				default:
					searchScopeValue = SearchControls.OBJECT_SCOPE;
					break;
				}
				
				// FIXME: temporary workaround for issue with searching when DIGEST-MD5 is used as authentication method,
				// see issue 2156 for details https://www.jfire.org/modules/bugs/view.php?id=2156
				// The same workaround could be implemented in another search method above if needed.
				boolean authMethodChanged = false;
				Object saslRealm = null;
				try{
					if (AuthenticationMethod.SASL_DIGEST_MD5.equals(connectionParamsProvider.getAuthenticationMethod())){
						context.removeFromEnvironment(Context.SECURITY_AUTHENTICATION);
		                saslRealm = context.removeFromEnvironment(JAVA_NAMING_SECURITY_SASL_REALM);
						context.addToEnvironment(Context.SECURITY_AUTHENTICATION, AuthenticationMethod.SASL_CRAM_MD5.stringValue());
		                authMethodChanged = true;
		                context.reconnect(context.getConnectControls());
					}
					
					SearchControls controls = new SearchControls();
					controls.setSearchScope(searchScopeValue);
					return processResult(
							context.search(getSafeJndiName(getSafeSearchBaseDN(baseDN)), filterExpr, filterArgs, controls));
					
				}finally{
					if (authMethodChanged){
						context.removeFromEnvironment(Context.SECURITY_AUTHENTICATION);
						context.addToEnvironment(Context.SECURITY_AUTHENTICATION, AuthenticationMethod.SASL_DIGEST_MD5.stringValue());
						if (saslRealm instanceof String && !((String) saslRealm).isEmpty()){
                			context.addToEnvironment(JAVA_NAMING_SECURITY_SASL_REALM, saslRealm);
						}
		                context.reconnect(context.getConnectControls());
					}
				}
			}
		} catch (NoPermissionException e){
			logger.error(e.getMessage(), e);
			throw new LoginException("Insufficient permissions! Search failed! Exception: " + e.getMessage());
		}catch(NamingException e){
			throw new UserManagementSystemCommunicationException("Search failed!" , e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LDAPAttributeSet getAttributesForEntry(String dn) throws UserManagementSystemCommunicationException, LoginException {
		return getAttributesForEntry(dn, null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LDAPAttributeSet getAttributesForEntry(String dn, String[] attributeNames) throws UserManagementSystemCommunicationException, LoginException {
		try {
			synchronized (context) {
				Attributes attributes = null;
				if (attributeNames != null && attributeNames.length > 0){
					attributes = context.getAttributes(getSafeJndiName(dn), attributeNames);
				}else{	// get all attributes
					attributes = context.getAttributes(getSafeJndiName(dn));
				}
				return getLDAPAttributeSet(attributes);
			}
		} catch (NoPermissionException e){
			throw new LoginException("Insufficient permissions! Bind against this LDAP server or enable anonymous access. Failed to get attributes for entry: " + dn);
		} catch (NamingException e) {
			throw new UserManagementSystemCommunicationException("Failed to get attributes from entry with DN: " + dn, e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<String> getChildEntries(String parentName) throws UserManagementSystemCommunicationException, LoginException{
		try {
			synchronized (context) {
				if (parentName == null || parentName.isEmpty()){
					
					return getRootEntries();
					
		 		}else{
					NamingEnumeration<NameClassPair> childEntries = context.list(getSafeJndiName(parentName));
					try{
						Collection<String> childNames = new ArrayList<String>();
						while (childEntries.hasMoreElements()) {
							NameClassPair pair = childEntries.nextElement();
							childNames.add(pair.getNameInNamespace());
						}
						
						return childNames;
						
					}finally{
						childEntries.close();
					}
		 		}
			}
		} catch (NoPermissionException e){
			throw new LoginException("Insufficient permissions! Bind against this LDAP server or enable anonymous access. Failed to get child entries for parent with DN: " + parentName);
		} catch (NamingException e) {
			throw new UserManagementSystemCommunicationException("Failed to get child entries for parent with DN: " + parentName, e);
		}
	}
    
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isConnected() {
		// It's not a very troubleproof strategy to check some internal flag. But since there's no way
		// to ping a LDAPServer via JNDI (we can just recreate a JNDI context, perform a search or lookup)
		// we need to maintain these flags carefully so they will reflect the real connection state.
		return context != null && isConnected.get();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAuthenticated() {
		if (isConnected()){
			synchronized (context) {
				Hashtable<?, ?> environment = null;
				try {
					 environment = context.getEnvironment();
				} catch (NamingException e) {
					logger.warn("Can't get environment properties!", e);
					return false;
				}
				return environment != null 
						&& environment.containsKey(Context.SECURITY_AUTHENTICATION)
						&& environment.containsKey(Context.SECURITY_PRINCIPAL)
						&& environment.containsKey(Context.SECURITY_CREDENTIALS);
			}
		}
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean entryExists(String entryName) throws LoginException{
		try{
			LDAPAttributeSet attributesForEntry = getAttributesForEntry(entryName, new String[]{LDAPServer.OBJECT_CLASS_ATTR_NAME});
			return attributesForEntry != null && !attributesForEntry.isEmpty();
		}catch(UserManagementSystemCommunicationException e){
			if (e.getCause() instanceof NoPermissionException){
				throw new LoginException("Authentication failed! Can't check for entry existance. See log for details.");
			}
			logger.info(
					String.format("Check for entry %s existence failed with exception which probably means that entry does not exist: %s", entryName, e.getMessage()));
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void disconnect() {
		if (context != null) {
			try {
				context.close();
			} catch (NamingException e) {
				logger.debug(e.getMessage(), e);
			}
			context = null;
		}
		isConnected.set(false);
		System.gc();
	}

	private static final int MAX_TRIES = 3;
	/**
	 * LDAP servers often have an idle timeout period after which they will close connections no longer being used.
	 * That's why we need this listener to catch connection exceptions from LDAP server. If such exception is caught
	 * it will be logged and we'll try (MAX_TRIES times) to establish a new connection immidiately.
	 * 
	 * @throws NamingException
	 */
	private void configureConnectionProblemsListener() throws NamingException{
		
		// we can't add a listener in InitialLdapContext since it does not implemet EventContext interface
		Object eventContext = context.lookup("");
		
		if (eventContext instanceof EventContext){
			
			UnsolicitedNotificationListener ldapPushNotificationsListener = new UnsolicitedNotificationListener() {
				
				@Override
				public void namingExceptionThrown(NamingExceptionEvent event) {
					if (event.getException() instanceof CommunicationException
							|| event.getException() instanceof ServiceUnavailableException
							|| event.getException() instanceof InsufficientResourcesException){
						logger.warn("Recieved communication exception from LDAP server, trying to reconnect.");
						int tries = 0;
						while (true){
							try {
								disconnect();
								connect();
								break;
							} catch (Exception e) {
								tries++;
								if (tries < MAX_TRIES){
									logger.warn("Faield to reconnect to LDAP server, retrying... Tries left: " + (MAX_TRIES - tries), e);
									continue;
								}else{
									logger.error("Faield to reconnect to LDAP server! Number of tries: " + MAX_TRIES, e);
									break;
								}
							}
						}
					}else{
						logger.warn("Recieved communication exception from LDAP server", event.getException());
					}
				}
				
				@Override
				public void notificationReceived(
						UnsolicitedNotificationEvent unsolicitednotificationevent
						) {
					// we are not inersted in any notifications, so do nothig
				}
			};

			// Name and scope parameters are not used when adding UnsolicitedNotificationListener,
			// so we just specify a blank string and OBJECT_SCOPE to avoid possible NullPointerExceptions.
			((EventContext) eventContext).addNamingListener(
					"", EventDirContext.OBJECT_SCOPE, ldapPushNotificationsListener
					);
		}
		
	}
	
	/**
	 * Translates {@link Attributes} to {@link LDAPAttributeSet} 
	 * 
	 * @param attributes if <code>null</code> is passed it returns and empty {@link HashMap}
	 * @return
	 * @throws NamingException 
	 */
	private static LDAPAttributeSet getLDAPAttributeSet(Attributes attributes) throws NamingException{
		
		LDAPAttributeSet attributeSet = new LDAPAttributeSet();
		
		if (attributes == null){
			return attributeSet;
		}
		
		for (Enumeration<? extends Attribute> attsEnum = attributes.getAll(); attsEnum.hasMoreElements();){
			Attribute attr = attsEnum.nextElement();
			attributeSet.createAttribute(attr.getID(), getAttributeValues(attr));
		}
		
		return attributeSet;
	}
	
	private static Collection<Object> getAttributeValues(Attribute attr) throws NamingException{
		
		if (attr == null){
			return null;
		}
		
		NamingEnumeration<?> allValues = attr.getAll();
		List<Object> valuesList = new ArrayList<Object>();
		for (Enumeration<?> valuesEnum = allValues; valuesEnum.hasMoreElements();) {
			valuesList.add(valuesEnum.nextElement());
		}
		return valuesList;
	}

	/**
	 * Translates {@link LDAPAttributeSet} into JNDI {@link Attributes}
	 * 
	 * @param attributeSet if <code>null</code> is passed it returs an empty {@link Attributes} object 
	 * @return
	 */
	private static Attributes getJNDIAttributes(LDAPAttributeSet attributeSet){
		Attributes atts = new BasicAttributes(true);	// "true" indicates that we consider attribute names case-insensitive as they are in LDAP

		if (attributeSet == null){
			return atts;
		}
		
		for (LDAPAttribute<Object> ldapAttribute : attributeSet){
			
        	Attribute attribute = new BasicAttribute(ldapAttribute.getName());
			if (ldapAttribute.hasSingleValue()){
        		attribute.add(ldapAttribute.getValue());
			}else{
				for (Object value : ldapAttribute.getValues()){
					attribute.add(value);
				}
			}
        	
        	atts.put(attribute);
        }
        
        return atts;
	}

	
	private static final String NAMING_CONTEXTS_ATTRIBUTE = "namingContexts";

	private Collection<String> getRootEntries() throws NamingException {
		
		SearchControls controls = new SearchControls();
		controls.setSearchScope(SearchControls.OBJECT_SCOPE);
		controls.setReturningAttributes(new String[]{NAMING_CONTEXTS_ATTRIBUTE});
		NamingEnumeration<SearchResult> childEntries = context.search("", "(objectclass=*)", controls);
		Attributes rootDSEAttributes = null;
		try{
			if (childEntries.hasMoreElements()) {
				rootDSEAttributes = childEntries.nextElement().getAttributes();
			}
		}finally{
			childEntries.close();
		}
		
		Collection<String> returnChildEntries = new ArrayList<String>();
		if (rootDSEAttributes != null){
			
            Set<String> namingContextSet = new HashSet<String>();
            Attribute namingAttribute = rootDSEAttributes.get(NAMING_CONTEXTS_ATTRIBUTE);
            if (namingAttribute != null) {
                NamingEnumeration<?> values = namingAttribute.getAll();
                while (values.hasMoreElements()) {
					Object value = (Object) values.nextElement();
					if (value instanceof String){
						namingContextSet.add((String) value);
					}
				}
            }

            if (!namingContextSet.isEmpty()) {
                for (String name : namingContextSet) {
                    
                	if (name.length() > 0 
                			&& name.charAt(name.length() - 1) == '\u0000') {
                        name = name.substring(0, name.length() - 1);
                    }

                    if (!name.isEmpty()) {
                    	
                    	try{
                    		context.lookup(name);
                        	returnChildEntries.add(name);
                    	}catch(NameNotFoundException e){
                    		// do nothing - it means that some of namingContexts values are not valid
                    	}
                    	
                    } else {
                        // special handling of empty namingContext (Novell eDirectory): 
                        // perform a one-level search and add all result DNs to the set
                        returnChildEntries.addAll(searchRootDseEntries());
                    }
                }
            } else {
                // special handling of non-existing namingContexts attribute (Oracle Internet Directory)
                // perform a one-level search and add all result DNs to the set
            	returnChildEntries.addAll(searchRootDseEntries());
            }
			
		}
		
		return returnChildEntries;
	}
	
    private Collection<String> searchRootDseEntries() throws NamingException {
		
    	SearchControls controls = new SearchControls();
		controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
		controls.setReturningAttributes(new String[0]);
		controls.setDerefLinkFlag(false);
		NamingEnumeration<SearchResult> childEntries = context.search("", "(objectclass=*)", controls);
		
		Collection<String> returnEntries = new ArrayList<String>();
		try{
			while (childEntries.hasMoreElements()) {
				String searchResultName = childEntries.nextElement().getNameInNamespace();
				if (searchResultName != null && !searchResultName.isEmpty()){
					returnEntries.add(searchResultName);
				}
			}
		}finally{
			childEntries.close();
		}
		
		return returnEntries;
    }
	
    /**
     * Gets the default LDAP context factory.
     * 
     * Right now the following context factories are supported (by Apache DS):
     * <ul>
     * <li>com.sun.jndi.ldap.LdapCtxFactory</li>
     * <li>org.apache.harmony.jndi.provider.ldap.LdapContextFactory</li>
     * </ul>
     * 
     * @return the default LDAP context factory
     * @throws NamingException 
     */
    public static String getDefaultLdapContextFactory() throws NamingException {

        try{
            
        	String sun = "com.sun.jndi.ldap.LdapCtxFactory"; //$NON-NLS-1$
            Class.forName(sun);
            return sun;
            
        }catch (ClassNotFoundException e){
        	logger.warn("com.sun.jndi.ldap.LdapCtxFactory class not found!");
        }
        
        try {
            
        	String apache = "org.apache.harmony.jndi.provider.ldap.LdapContextFactory"; //$NON-NLS-1$
            Class.forName(apache);
            return apache;
            
        }catch(ClassNotFoundException e){
        	logger.warn("org.apache.harmony.jndi.provider.ldap.LdapContextFactory class not found!");
        }

        throw new NamingException("No LDAP ContextFactory found!");
        
    }
    
	private static Map<String, LDAPAttributeSet> processResult(
			NamingEnumeration<SearchResult> result) throws NamingException {
		try{
			Map<String, LDAPAttributeSet> returnMap = new HashMap<String, LDAPAttributeSet>();
			while (result.hasMoreElements()) {
				SearchResult searchResult = result.nextElement();
				returnMap.put(searchResult.getNameInNamespace(), getLDAPAttributeSet(searchResult.getAttributes()));
			}
			return returnMap;
		}finally{
			result.close();
		}
	}
	
	private static final String DC_OBJECT_OBJECT_CLASS = "dcObject";
	
	private String getSafeSearchBaseDN(String baseDN) throws NamingException{
		if (baseDN == null || baseDN.isEmpty()){	// getting root entry
			Collection<String> rootEntries = getRootEntries();
			for (String entryName : rootEntries){
				Attributes attributes = context.getAttributes(entryName, new String[]{LDAPServer.OBJECT_CLASS_ATTR_NAME});
				if (attributes.get(LDAPServer.OBJECT_CLASS_ATTR_NAME).contains(DC_OBJECT_OBJECT_CLASS)){
					return entryName;
				}
			}
		}
		return baseDN;
	}

	private static String getAliasMapKeyByValue(String value){
		if (value == null){
			return null;
		}
		for (String key : attributeAliases.keySet()){
			if (value.equals(attributeAliases.get(key))){
				return key;
			}
		}
		return null;
	}
	
    /**
     * Gets a Name object that is save for JNDI operations.
     * <p>
     * In JNDI we have could use the following classes for names:
     * <ul>
     * <li>DN as String</li>
     * <li>javax.naming.CompositeName</li>
     * <li>javax.naming.ldap.LdapName (since Java5)</li>
     * <li>org.apache.directory.shared.ldap.name.LdapDN</li>
     * </ul>
     * <p>
     * There are some drawbacks when using this classes:
     * <ul>
     * <li>When passing DN as String, JNDI doesn't handle slashes '/' correctly.
     * So we must use a Name object here.</li>
     * <li>With CompositeName we have the same problem with slashes '/'.</li>
     * <li>When using LdapDN from shared-ldap, JNDI uses the toString() method
     * and LdapDN.toString() returns the normalized ATAV, but we need the
     * user provided ATAV.</li>
     * <li>When using LdapName for the empty DN (Root DSE) JNDI _sometimes_ throws
     * an Exception (java.lang.IndexOutOfBoundsException: Posn: -1, Size: 0
     * at javax.naming.ldap.LdapName.getPrefix(LdapName.java:240)).</li>
     * <li>Using LdapDN for the RootDSE doesn't work with Apache Harmony because
     * its JNDI provider only accepts intstances of CompositeName or LdapName.</li>
     * </ul>
     * <p>
     * So we use LdapName as default and the CompositeName for the empty DN.
     * 
     * @param name the DN
     * 
     * @return the save JNDI name
     * 
     * @throws InvalidNameException the invalid name exception
     */
    private static Name getSafeJndiName(String name) throws InvalidNameException{
        if (name == null || "".equals(name)){
            return new CompositeName();
        }else{
            return new LdapName(name);
        }
    }
}
