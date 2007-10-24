package org.nightlabs.jfire.issue;

import java.io.Serializable;

import org.nightlabs.util.Util;

/**
 * @author Chairat Kongarayawetchakun - chairat at nightlabs dot de
 *
 * @jdo.persistence-capable
 *		identity-type = "application"
 *		objectid-class = "org.nightlabs.jfire.issue.id.IssueStatusID"
 *		detachable = "true"
 *		table="JFireIssueTracking_IssueStatus"
 *
 * @jdo.create-objectid-class
 *
 * @jdo.inheritance strategy = "new-table"
 * 
 * @jdo.fetch-group name="IssueStatus.this" fields="text"
 */
public class IssueStatus 
implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	public static final String FETCH_GROUP_THIS = "IssueStatus.this";
	/**
	 * @jdo.field primary-key="true"
	 * @jdo.column length="100"
	 */
	private String issueStatusID;

	/**
	 * @jdo.field persistence-modifier="persistent" dependent="true" mapped-by="issueStatus"
	 */
	private IssueStatusText text;

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private Issue issue;
	
	protected IssueStatus()
	{
	}

	public IssueStatus(String issueStatusID){
		if (issueStatusID == null)
			throw new IllegalArgumentException("issueStatusID must not be null!");

		this.issue = issue;
		this.issueStatusID = issueStatusID;
		this.text = new IssueStatusText(this);
	}
	
	public void setIssueStatusID(String issueStatusID) {
		this.issueStatusID = issueStatusID;
	}
	
	/**
	 * @return Returns the issueStatusID.
	 */
	public String getIssueStatusID()
	{
		return issueStatusID;
	}

	public IssueStatusText getIssueStatusText()
	{
		return text;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this) return true;
		if (!(obj instanceof IssueStatus)) return false;
		IssueStatus o = (IssueStatus) obj;
		return Util.equals(o.issueStatusID, this.issueStatusID);
	}

	@Override
	public int hashCode()
	{
		return Util.hashCode(issueStatusID);
	}
}
