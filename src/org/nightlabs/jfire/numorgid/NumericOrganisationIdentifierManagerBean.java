package org.nightlabs.jfire.numorgid;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.naming.InitialContext;

import org.nightlabs.jfire.base.BaseSessionBeanImpl;
import org.nightlabs.jfire.idgenerator.IDGenerator;
import org.nightlabs.jfire.idgenerator.IDNamespace;
import org.nightlabs.jfire.idgenerator.id.IDNamespaceID;
import org.nightlabs.jfire.numorgid.id.NumericOrganisationIdentifierID;
import org.nightlabs.jfire.organisation.Organisation;
import org.nightlabs.jfire.security.User;

/**
 * @ejb.bean
 * 		name="jfire/ejb/JFireNumericOrganisationID/NumericOrganisationIdentifierManager"
 *    jndi-name="jfire/ejb/JFireNumericOrganisationID/NumericOrganisationIdentifierManager"
 *    type="Stateless"
 *    transaction-type="Container"
 * 
 * @ejb.util
 * 		generate="physical"
 */
public abstract class NumericOrganisationIdentifierManagerBean
extends BaseSessionBeanImpl
implements SessionBean
{
	/**
	 * @ejb.interface-method
	 * @ejb.permission role-name="_Guest_"
	 * @ejb.transaction type="Required"
	 **/
	public int getNumericOrganisationID() {
		PersistenceManager pm = getPersistenceManager();
		try {
			String clientOrganisationID = getUserID();
			if (!clientOrganisationID.startsWith(User.USERID_PREFIX_TYPE_ORGANISATION))
				throw new IllegalStateException("Sorry, only organisations are allowed to query this information!");

			clientOrganisationID = clientOrganisationID.substring(User.USERID_PREFIX_TYPE_ORGANISATION.length());

			String localOrganisationID = getOrganisationID();
			String rootOrganisationID;
			try {
				InitialContext initialContext = new InitialContext();
				try {
					rootOrganisationID = Organisation.getRootOrganisationID(initialContext);
				} finally {
					initialContext.close();
				}
			} catch (Exception x) {
				throw new RuntimeException("Acquiring the rootOrganisationID failed.");
			}

			if (! localOrganisationID.equals(rootOrganisationID))
				throw new IllegalStateException("You must not call this method for any other organisation than the root organisation. I am " + localOrganisationID + " - ask " + rootOrganisationID);

			NumericOrganisationIdentifierID numericOrganisationIdentifierID = NumericOrganisationIdentifierID.create(clientOrganisationID);
			try {
				NumericOrganisationIdentifier numericOrganisationIdentifier = (NumericOrganisationIdentifier) pm.getObjectById(numericOrganisationIdentifierID);
				return numericOrganisationIdentifier.getNumericOrganisationID();
			} catch (JDOObjectNotFoundException x) {
				// fine - ask the ID-generator and create the record
			}

			long id = IDGenerator.nextID(NumericOrganisationIdentifier.class);
			if (id > NumericOrganisationIdentifier.MAX_NUMERIC_ORGANISATION_ID)
				throw new IllegalStateException("Out of range! The id generated by the IDGenerator exceeds NumericOrganisationIdentifier.MAX_NUMERIC_ORGANISATION_ID!");

			NumericOrganisationIdentifier numericOrganisationIdentifier = new NumericOrganisationIdentifier(clientOrganisationID, (int) id);
			numericOrganisationIdentifier = pm.makePersistent(numericOrganisationIdentifier);
			return numericOrganisationIdentifier.getNumericOrganisationID();
			
		} finally {
			pm.close();
		}
	}
	
	/**
	 * @ejb.interface-method
	 * @ejb.permission role-name="_System_"
	 * @ejb.transaction type = "Required"
	 */
	public void initialise() {
		PersistenceManager pm = getPersistenceManager();
		try {
			String localOrganisationID = getOrganisationID();
			String rootOrganisationID;
			try {
				InitialContext initialContext = new InitialContext();
				try {
					rootOrganisationID = Organisation.getRootOrganisationID(initialContext);
				} finally {
					initialContext.close();
				}
			} catch (Exception x) {
				throw new RuntimeException("Acquiring the rootOrganisationID failed.");
			}

			if (! localOrganisationID.equals(rootOrganisationID))
				return; // We only want to assign a numeric organisation ID for the root organisation
			
			try {
				NumericOrganisationIdentifierID numericOrganisationIdentifierID = NumericOrganisationIdentifierID.create(rootOrganisationID);
				pm.getObjectById(numericOrganisationIdentifierID);
				
				// if the id already exists, we simply return since the init has already happened once.
				return;
			} catch (JDOObjectNotFoundException x) {
				// otherwise we create it
				NumericOrganisationIdentifier numericOrganisationIdentifier = new NumericOrganisationIdentifier(rootOrganisationID, NumericOrganisationIdentifier.ROOT_ORGANISATION_NUMERIC_ORGANISATION_ID);
				numericOrganisationIdentifier = pm.makePersistent(numericOrganisationIdentifier);
			}
			
			IDNamespace idNamespace;
			try {
				idNamespace = (IDNamespace) pm.getObjectById(IDNamespaceID.create(rootOrganisationID, NumericOrganisationIdentifier.class.getName()));
			} catch (JDOObjectNotFoundException e) {
				idNamespace = new IDNamespace(rootOrganisationID, NumericOrganisationIdentifier.class.getName(), null);
				idNamespace.setCacheSizeClient(0);
				idNamespace.setCacheSizeServer(0);
				idNamespace.setNextID(1);
				
				idNamespace = pm.makePersistent(idNamespace);
			}
		} finally {
			pm.close();
		}
	}
	
	/**
	 * @see org.nightlabs.jfire.base.BaseSessionBeanImpl#setSessionContext(javax.ejb.SessionContext)
	 */
	public void setSessionContext(SessionContext sessionContext)
	throws EJBException, RemoteException
	{
		super.setSessionContext(sessionContext);
	}
	/**
	 * @see org.nightlabs.jfire.base.BaseSessionBeanImpl#unsetSessionContext()
	 */
	public void unsetSessionContext() {
		super.unsetSessionContext();
	}
	
	/**
	 * @ejb.create-method
	 * @ejb.permission role-name="_Guest_"  
	 */
	public void ejbCreate() throws CreateException { }
	
	/**
	 * @ejb.permission unchecked="true"
	 */
	public void ejbRemove() throws EJBException, RemoteException { }
}
