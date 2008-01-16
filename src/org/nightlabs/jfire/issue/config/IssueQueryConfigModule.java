package org.nightlabs.jfire.issue.config;

import java.util.ArrayList;
import java.util.List;

import org.nightlabs.jfire.config.ConfigModule;

/**
 * @author Chairat Kongarayawetchakun <!-- chairat at nightlabs dot de -->
 *
 * @jdo.persistence-capable
 *		identity-type="application"
 *		persistence-capable-superclass="org.nightlabs.jfire.config.ConfigModule"
 *		detachable="true"
 *		table="JFireIssueTracking_IssueQueryConfigModule"
 *
 * @jdo.inheritance strategy="new-table"
 *
 * @jdo.fetch-group name="IssueQueryConfigModule.storedIssueQueryList" fetch-groups="default" fields="storedIssueQueryList"
 **/
public class IssueQueryConfigModule 
extends ConfigModule
{

	private static final long serialVersionUID = 1L;

	public static final String FETCH_GROUP_STOREDISSUEQUERRYLIST = "IssueQueryConfigModule.storedIssueQueryList";
	
	/**
	 * @jdo.field
	 *		persistence-modifier="persistent"
	 *		collection-type="collection"
	 *		element-type="java.lang.String"
	 *		table="JFireIssueTracking_StoredIssueQuery_storedIssueQueryList"
	 *		null-value="exception"
	 *
	 * @jdo.join
	 */
	private List<StoredIssueQuery> storedIssueQueryList;
	
	public List<StoredIssueQuery> getStoredIssueQueryList() {
		return storedIssueQueryList;
	}

	@Override
	public void init() {
		storedIssueQueryList = new ArrayList<StoredIssueQuery>();
	}
}
