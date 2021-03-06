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

package org.nightlabs.jfire.trade.dao;

import java.util.Collection;
import java.util.Set;

import javax.jdo.FetchPlan;
import javax.jdo.JDODetachedFieldAccessException;

import org.nightlabs.jdo.NLJDOHelper;
import org.nightlabs.jfire.base.JFireEjb3Factory;
import org.nightlabs.jfire.base.jdo.BaseJDOObjectDAO;
import org.nightlabs.jfire.base.jdo.IJDOObjectDAO;
import org.nightlabs.jfire.person.Person;
import org.nightlabs.jfire.prop.IStruct;
import org.nightlabs.jfire.prop.PropertySet;
import org.nightlabs.jfire.prop.dao.StructLocalDAO;
import org.nightlabs.jfire.prop.id.PropertySetID;
import org.nightlabs.jfire.security.GlobalSecurityReflector;
import org.nightlabs.jfire.trade.LegalEntity;
import org.nightlabs.jfire.trade.OrganisationLegalEntity;
import org.nightlabs.jfire.trade.TradeManagerRemote;
import org.nightlabs.jfire.transfer.id.AnchorID;
import org.nightlabs.progress.NullProgressMonitor;
import org.nightlabs.progress.ProgressMonitor;

/**
 * JDOObjectDAO for LegalEntities.
 *
 * @author Alexander Bieber <!-- alex [AT] nightlabs[DOT] de -->
 * @author Daniel Mazurek
 */
public class LegalEntityDAO
extends BaseJDOObjectDAO<AnchorID, LegalEntity>
implements IJDOObjectDAO<LegalEntity>
{
	private static String[] DEFAULT_FETCH_GROUP_ANONYMOUS = new String[] {FetchPlan.DEFAULT, LegalEntity.FETCH_GROUP_PERSON, PropertySet.FETCH_GROUP_FULL_DATA};

	private AnchorID _anonymousAnchorID;

	private LegalEntityDAO() {}

	/** The shared instance */
	private static LegalEntityDAO sharedInstance = null;
	/**
	 * Returns (and lazily creates) the static shared instance of {@link LegalEntityDAO}.
	 * @return The static shared instance of {@link LegalEntityDAO}.
	 */
	public static LegalEntityDAO sharedInstance()
	{
		if (sharedInstance == null) {
			synchronized (LegalEntityDAO.class) {
				if (sharedInstance == null)
					sharedInstance = new LegalEntityDAO();
			}
		}
		return sharedInstance;
	}
	
	public AnchorID getAnonymousLegalEntityID()
	{
		String organisationID = GlobalSecurityReflector.sharedInstance().getUserDescriptor().getOrganisationID();
		if (_anonymousAnchorID != null) {
			if (!organisationID.equals(_anonymousAnchorID.organisationID))
				_anonymousAnchorID = null;
		}

		if (_anonymousAnchorID == null) {
			_anonymousAnchorID = AnchorID.create(
					organisationID,
					LegalEntity.ANCHOR_TYPE_ID_LEGAL_ENTITY,
					LegalEntity.ANCHOR_ID_ANONYMOUS
			);
		}

		return _anonymousAnchorID;
	}

	/**
	 * {@inheritDoc}
	 * @see org.nightlabs.jfire.base.jdo.BaseJDOObjectDAO#retrieveJDOObjects(java.util.Set, java.lang.String[], int, org.nightlabs.progress.ProgressMonitor)
	 */
	@Override
	protected Collection<LegalEntity> retrieveJDOObjects(
			Set<AnchorID> objectIDs, String[] fetchGroups, int maxFetchDepth,
			ProgressMonitor monitor) throws Exception {
		TradeManagerRemote tradeManager = getEjbProvider().getRemoteBean(TradeManagerRemote.class);
		Collection<LegalEntity> legalEntities = tradeManager.getLegalEntities(objectIDs, fetchGroups, maxFetchDepth);

		// TODO: Really need this ?!? Better not to explode here I think, Alex.
		for (LegalEntity le : legalEntities) {
			inflatePerson(le);
		}
		return legalEntities;
	}

	private void inflatePerson(LegalEntity le)
	{
		if (le == null)
			return;

		Person person;
		try {
			person = le.getPerson();
		} catch (JDODetachedFieldAccessException e) {
			return; // le.person was not detached -> return
		}

		if (person == null)
			return; // LegalEntity doesn't have a Person assigned => return

		IStruct struct = StructLocalDAO.sharedInstance().getStructLocal(
				person.getStructLocalObjectID(),
				new NullProgressMonitor()
		);

		try {
			person.inflate(struct);
		} catch (JDODetachedFieldAccessException e) {
			// le.person was detached INCOMPLETELY -> no explosion, break
//			break; // why break? what about the others?
		}
	}

	/**
	 * Returns the LegalEntity with the given leAnchorID detached with the given
	 * fetchGroups out of Cache if possible.
	 * @param leAnchorID The LegalEntity's AnchorID
	 * @param fetchGroups The fetch-groups to detach the LegalEntity with.
	 * @param maxFetchDepth The maximum fetch-depth to use while detaching.
	 * @param monitor The monitor to report progress.
	 */
	public LegalEntity getLegalEntity(AnchorID leAnchorID, String[] fetchGroups, int maxFetchDepth, ProgressMonitor monitor) {
		return getJDOObject(null, leAnchorID, fetchGroups, maxFetchDepth, monitor);
	}

	/**
	 * Returns the anonymous LegalEntity for the organisation of the currently
	 * logged in user. It will be detached with the given fetchGroups.
	 * @param fetchGroups The fetch-groups to detach the LegalEntity with.
	 * @param maxFetchDepth The maximum fetch-depth to use while detaching.
	 * @param monitor The monitor to report progress to.
	 */
	public LegalEntity getAnonymousLegalEntity(String[] fetchGroups, int maxFetchDepth, ProgressMonitor monitor) {
		return getJDOObject(null, getAnonymousLegalEntityID(), fetchGroups, maxFetchDepth, monitor);
	}

	/**
	 * Returns the anonymous LegalEntity for the organisation of the currently
	 * logged in user. It will be detached with the {@link #DEFAULT_FETCH_GROUP_ANONYMOUS}.
	 * @param monitor The monitor to report progress.
	 */
	public LegalEntity getAnonymousLegalEntity(ProgressMonitor monitor) {
		return getAnonymousLegalEntity(DEFAULT_FETCH_GROUP_ANONYMOUS, NLJDOHelper.MAX_FETCH_DEPTH_NO_LIMIT, monitor);
	}

	/**
	 * Get the list of LegalEntity for the given AnchorIDs detached with the
	 * given fetchGroups.
	 * @param anchorIDs The LegalEntities AnchorIDs
	 * @param fetchGroups The fetch-groups to detach the LegalEntities with.
	 * @param maxFetchDepth The maximum fetch-depth to use while detaching.
	 * @param monitor The monitor to report progress.
	 */
	public Collection<LegalEntity> getLegalEntities(Set<AnchorID> leAnchorIDs, String[] fetchGroups, int maxFetchDepth, ProgressMonitor monitor) {
		return getJDOObjects(null, leAnchorIDs, fetchGroups, maxFetchDepth, monitor);
	}

	/**
	 * {@inheritDoc}
	 */
	public LegalEntity storeJDOObject(LegalEntity jdoObject, boolean get,
			String[] fetchGroups, int maxFetchDepth, ProgressMonitor monitor) {
		try {
			TradeManagerRemote tradeManager = getEjbProvider().getRemoteBean(TradeManagerRemote.class);
			LegalEntity le = tradeManager.storeLegalEntity(jdoObject, get, fetchGroups, maxFetchDepth);
			if (le != null)
				getCache().put(null, le, fetchGroups, maxFetchDepth);
			return le;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 *
	 * @param jdoObject
	 * @param get
	 * @param fetchGroups
	 * @param maxFetchDepth
	 * @param monitor
	 * @return
	 */
	public LegalEntity storeLegalEntity(LegalEntity jdoObject, boolean get,
			String[] fetchGroups, int maxFetchDepth, ProgressMonitor monitor) {
		return storeJDOObject(jdoObject, get, fetchGroups, maxFetchDepth, monitor);
	}

	public OrganisationLegalEntity getOrganisationLegalEntity(String organisationID, boolean throwExceptionIfNotExistent, String[] fetchGroups, int maxFetchDepth, ProgressMonitor monitor)
	{
		monitor.beginTask("Getting organisation legal entity", 100);
		try {
			String objectID = OrganisationLegalEntity.class + "/" + organisationID;

			OrganisationLegalEntity le = (OrganisationLegalEntity) getCache().get(null, objectID, fetchGroups, maxFetchDepth);
			if (le != null)
				return le;

			TradeManagerRemote tradeManager = getEjbProvider().getRemoteBean(TradeManagerRemote.class);
			le = tradeManager.getOrganisationLegalEntity(organisationID, throwExceptionIfNotExistent, fetchGroups, maxFetchDepth);
			if (le != null) {
				getCache().put(null, objectID, le, fetchGroups, maxFetchDepth);
				getCache().put(null, le, fetchGroups, maxFetchDepth);
			}
			return le;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			monitor.done();
		}
	}
	
	public LegalEntity getLegalEntityForPerson(PropertySetID personID, String[] fetchGroups, 
			int maxFetchDepth, ProgressMonitor monitor) 
	{
		TradeManagerRemote tradeManager = JFireEjb3Factory.getRemoteBean(
				TradeManagerRemote.class, GlobalSecurityReflector.sharedInstance().getInitialContextProperties()
		);
		LegalEntity le = tradeManager.getLegalEntityForPerson(personID, fetchGroups, maxFetchDepth);
		if (le != null) {
			getCache().put(null, le, fetchGroups, maxFetchDepth);
		}
		monitor.done();
		return le;
	}
}
