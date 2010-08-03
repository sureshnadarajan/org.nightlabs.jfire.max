package org.nightlabs.jfire.dunning;

import java.io.Serializable;
import java.util.Map;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.apache.log4j.Logger;
import org.nightlabs.jdo.ObjectIDUtil;
import org.nightlabs.jfire.accounting.Account;
import org.nightlabs.jfire.accounting.Currency;
import org.nightlabs.jfire.dunning.id.DunningMoneyFlowConfigID;
import org.nightlabs.jfire.organisation.Organisation;

/**
 * The abstract base class for all implementations that map a triple consisting 
 * of the DunningFeeType, the currency and the direction of the booking to an 
 * account it shall be booked to.<br>
 * 
 * <br>A simple implementation is the SimpleDunningMoneyFlowConfig that directly maps 
 * such a triple to an account via hash maps.
 * 
 * @author Chairat Kongarayawetchakun - chairat [AT] nightlabs [DOT] de
 */
@PersistenceCapable(
		objectIdClass=DunningMoneyFlowConfigID.class,
		identityType=IdentityType.APPLICATION,
		detachable="true",
		table="JFireDunning_DunningMoneyFlowConfig"
)
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.CLASS_NAME)
public abstract class DunningMoneyFlowConfig 
implements Serializable
{
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(DunningMoneyFlowConfig.class);
	
	@PrimaryKey
	@Column(length=100)
	private String organisationID;

	@PrimaryKey
	@Column(length=100)
	private String dunningMoneyFlowConfigID;
	
	/**
	 * Maps all paid interests of a certain currency to the account it shall be booked to.
	 */
	@Join
	@Persistent(table="JFireDunning_DunningMoneyFlowConfig_currency2InterestAccount")
	private Map<Currency, Account> currency2InterestAccount;
	
	/**
	 * @deprecated This constructor exists only for JDO and should never be used directly!
	 */
	@Deprecated
	protected DunningMoneyFlowConfig() { }
	
	public DunningMoneyFlowConfig(String organisationID, String dunningMoneyFlowConfigID) {
		Organisation.assertValidOrganisationID(organisationID);
		ObjectIDUtil.assertValidIDString(dunningMoneyFlowConfigID, "dunningMoneyFlowConfigID"); //$NON-NLS-1$
		
		this.organisationID = organisationID;
		this.dunningMoneyFlowConfigID = dunningMoneyFlowConfigID;
	}
	
	/**
	 * This method returns the account a specific DunningFeeType will be booked on.
	 * 
	 * @param feeType
	 * @param currency
	 * @param isReverseBooking
	 * @return
	 */
	public Account getAccount(DunningFeeType feeType, Currency currency, boolean isReverseBooking) {
		return null;
	}
}
