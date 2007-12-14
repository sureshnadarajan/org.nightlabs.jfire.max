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

package org.nightlabs.jfire.issue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;

import org.apache.log4j.Logger;
import org.nightlabs.jdo.ObjectID;
import org.nightlabs.jfire.jbpm.graph.def.Statable;
import org.nightlabs.jfire.jbpm.graph.def.StatableLocal;
import org.nightlabs.jfire.jbpm.graph.def.State;
import org.nightlabs.jfire.security.User;
import org.nightlabs.util.Utils;

/**
 * @author Chairat Kongarayawetchakun <!-- chairat at nightlabs dot de -->
 *
 * @jdo.persistence-capable
 *		identity-type="application"
 *		objectid-class="org.nightlabs.jfire.issue.id.IssueID"
 *		detachable="true"
 *		table="JFireIssueTracking_Issue"
 *
 * @jdo.inheritance strategy="new-table"
 *
 * @jdo.create-objectid-class
 *		field-order="organisationID, issueID"
 *
 * @jdo.query
 *		name="getIssuesByIssueTypeID"
 *		query="SELECT
 *			WHERE this.issueTypeID == paramIssueTypeID                    
 *			PARAMETERS String paramIssueTypeID
 *			import java.lang.String"
 *
 * @jdo.fetch-group name="Issue.fileList" fetch-groups="default" fields="fileList"
 * @jdo.fetch-group name="Issue.description" fetch-groups="default" fields="description"
 * @jdo.fetch-group name="Issue.subject" fetch-groups="default" fields="subject" 
 * @jdo.fetch-group name="Issue.issuePriority" fetch-groups="default" fields="issuePriority"
 * @jdo.fetch-group name="Issue.issueSeverityType" fetch-groups="default" fields="issueSeverityType"
 * @jdo.fetch-group name="Issue.issueResolution" fetch-groups="default" fields="issueResolution"
 * @jdo.fetch-group name="Issue.status" fetch-groups="default" fields="stateDefinition"
 * @jdo.fetch-group name="Issue.issueType" fetch-groups="default" fields="issueType"
 * @jdo.fetch-group name="Issue.this" fetch-groups="default" fields="fileList, issueType, description, subject, issuePriority, issueSeverityType, issueResolution, stateDefinition, reporter, assignee"
 *
 **/
public class Issue
implements 	
		Serializable,
		Statable
{

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(Issue.class);

	public static final String FETCH_GROUP_THIS = "Issue.this";
	public static final String FETCH_GROUP_DESCRIPTION = "Issue.description";
	public static final String FETCH_GROUP_SUBJECT = "Issue.subject";
	public static final String FETCH_GROUP_ISSUE_SEVERITYTYPE = "Issue.issueSeverityType";
	public static final String FETCH_GROUP_STATE = "Issue.state";
	public static final String FETCH_GROUP_ISSUE_PRIORITY = "Issue.issuePriority";
	public static final String FETCH_GROUP_ISSUE_RESOLUTION = "Issue.issueResolution";
	public static final String fETCH_GROUP_ISSUETYPE = "Issue.issueType";
	
	/**
	 * @jdo.field primary-key="true"
	 * @jdo.column length="100"
	 */
	private String organisationID;

	/**
	 * @jdo.field primary-key="true"
	 */
	private long issueID;

	/**
	 * @jdo.field persistence-modifier="persistent"
	 * @jdo.column length="100"
	 */
	private IssueType issueType;

//	/** Documents for the issue
//	*
//	* @jdo.field
//	*    persistence-modifier="persistent"
//	*    collection-type="collection"
//	*    element-type="ObjectID" 
//	*    mapped-by="supplier"
//	**/
//	private Collection attachedDocuments = new HashSet();

	/**
	 * Instances of String that are representations of {@link ObjectID}s.
	 *
	 * @jdo.field
	 *		persistence-modifier="persistent"
	 *		collection-type="collection"
	 *		element-type="String"
	 *		dependent-value="true"
	 *		table="JFireIssueTracking_Issue_referencedObjectIDs"
	 *
	 * @jdo.join
	 */
	private Set<String> referencedObjectIDs;
	
	/**
	 * Instances of {@link IssueFileAttachment}.
	 *
	 * @jdo.field
	 *		persistence-modifier="persistent"
	 *		collection-type="collection"
	 *		element-type="IssueFileAttachment"
	 *		dependent-value="true"
	 *		mapped-by="issue"
	 */
	private List<IssueFileAttachment> fileList;
	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private IssuePriority issuePriority;

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private IssueSeverityType issueSeverityType;

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private IssueSubject subject;

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private IssueDescription description;

	/**
	 * @jdo.field persistence-modifier="persistent" load-fetch-group="all"
	 */
	private User reporter; 
	
	/**
	 * @jdo.field persistence-modifier="persistent" load-fetch-group="all"
	 */
	private User assignee; 

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private Date createTimestamp;

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private Date updateTimestamp;

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private IssueResolution issueResolution;
	
	/**
	 * @jdo.field persistence-modifier="persistent" mapped-by="issue"	 
	 */
	private IssueLocal issueLocal;
	
	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private State state;
	
	/**
	 * This is the history of <b>public</b> {@link State}s with the newest last and the oldest first.
	 *
	 * @jdo.field
	 *		persistence-modifier="persistent"
	 *		collection-type="collection"
	 *		element-type="State"
	 *		table="JFireIssueTracking_Issue_states"
	 *		null-value="exception"
	 *
	 * @jdo.join
	 */
	private List<State> states;
	
	
	/**
	 * @deprecated Constructor exists only for JDO! 
	 */
	protected Issue() { }

	public Issue(String organisationID, long issueID)
	{
		this.organisationID = organisationID;
		this.createTimestamp = new Date();
		this.issueID = issueID;
		
		subject = new IssueSubject(this);
		description = new IssueDescription(this);
		
		fileList = new ArrayList<IssueFileAttachment>();
		referencedObjectIDs = new HashSet<String>();
	}
	
	public Issue(String organisationID, long issueID, IssueType issueType)
	{
		this(organisationID, issueID);
		this.issueType = issueType;
	}

	/**
	 * @return Returns the organisationID.
	 */
	public String getOrganisationID() {
		return organisationID;
	}

	/**
	 * @return Returns the issueID.
	 */
	public long getIssueID() {
		return issueID;
	}

	/**
	 * @return Returns the issueType.
	 */
	public IssueType getIssueType() {
		return issueType;
	}

	/**
	 * @param issueTypeID The issueTypeID to set.
	 */
	public void setIssueType(IssueType issueType) {
		this.issueType = issueType;
	}

	/**
	 * @return Returns the create timestamp.
	 */
	public Date getCreateTimestamp() {
		return createTimestamp;
	}

	/**
	 * @param timestamp The timestamp to set.
	 */
	public void setCreateTimestamp(Date timestamp) {
		this.createTimestamp = timestamp;
	}

	/**
	 * @return Returns the update timestamp.
	 */
	public Date getUpdateTimestamp() {
		return updateTimestamp;
	}

	/**
	 * @param timestamp The timestamp to set.
	 */
	public void setUpdateTimestamp(Date timestamp) {
		this.updateTimestamp = timestamp;
	}

	/**
	 * @return Returns the description.
	 */
	public IssueDescription getDescription() {
		return description;
	}

	/**
	 * @param description The description to set.
	 */
	public void setDescription(IssueDescription description) {
		this.description = description;
	}

	/**
	 * @return Returns the subject.
	 */
	public IssueSubject getSubject() {
		return subject;
	}

	/**
	 * @param subject The subject to set.
	 */
	public void setSubject(IssueSubject subject) {
		this.subject = subject;
	}

	/**
	 * @return Returns the reporter.
	 */
	public User getReporter() {
		return reporter;
	}

	/**
	 * @param reporter The user to set.
	 */
	public void setReporter(User reporter) {
		this.reporter = reporter;
	}

	/**
	 * @return Returns the assignee.
	 */
	public User getAssignee() {
		return assignee;
	}

	/**
	 * @param assignee The user to set.
	 */
	public void setAssignee(User assignee) {
		this.assignee = assignee;
	}

	public IssuePriority getIssuePriority() {
		return issuePriority;
	}

	public void setIssuePriority(IssuePriority issuePriority) {
		this.issuePriority = issuePriority;
	}

	public IssueSeverityType getIssueSeverityType() {
		return issueSeverityType;
	}

	public void setIssueSeverityType(IssueSeverityType issueSeverityType) {
		this.issueSeverityType = issueSeverityType;
	}

	/**
	 * @param organisationID The organisationID to set.
	 */
	public void setOrganisationID(String organisationID) {
		this.organisationID = organisationID;
	}

	public List<IssueFileAttachment> getFileList() {
		return fileList;
	}
	
	public Set<String> getReferencedObjectIDs() {
		return referencedObjectIDs;
	}

	public IssueResolution getIssueResolution() {
		return issueResolution;
	}
	
	public void setIssueResolution(IssueResolution issueResolution) {
		this.issueResolution = issueResolution;
	}

	/**
	 * {@inheritDoc}}
	 */
	public StatableLocal getStatableLocal() {
		return issueLocal;
	}
	
	/**
	 * {@inheritDoc}}
	 */
	public State getState() {
		return state;
	}
	
	/**
	 * {@inheritDoc}}
	 */
	public List<State> getStates() {		
		return states;
	}
	
	/**
	 * {@inheritDoc}}
	 */
	public void setState(State state) {
		this.state = state;
	}
	
	protected PersistenceManager getPersistenceManager()
	{
		PersistenceManager pm = JDOHelper.getPersistenceManager(this);
		if (pm == null)
			throw new IllegalStateException("This instance of Issue is currently not attached to the datastore. Cannot obtain PersistenceManager!");
		return pm;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
			return true;

		if (!(obj instanceof Issue))
			return false;

		Issue o = (Issue) obj;

		return
		Utils.equals(this.organisationID, o.organisationID); //&& 
	}

	@Override
	public int hashCode()
	{
		return
		Utils.hashCode(this.organisationID); //^
	}

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private String primaryKey;

	public static String getPrimaryKey(String organisationID, long issueID)
	{
		return organisationID + '/' + Long.toString(issueID);
	}
	public String getPrimaryKey()
	{
		return primaryKey;
	}
	
}
