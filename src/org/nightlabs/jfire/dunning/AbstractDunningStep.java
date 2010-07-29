package org.nightlabs.jfire.dunning;

import java.io.Serializable;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.apache.log4j.Logger;
import org.nightlabs.jdo.ObjectIDUtil;
import org.nightlabs.jfire.dunning.id.DunningStepID;
import org.nightlabs.jfire.organisation.Organisation;

/**
 * @author Chairat Kongarayawetchakun - chairat [AT] nightlabs [DOT] de
 */
@PersistenceCapable(
		objectIdClass=DunningStepID.class,
		identityType=IdentityType.APPLICATION,
		detachable="true",
		table="JFireDunning_DunningStep"
)
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.CLASS_NAME)
public abstract class AbstractDunningStep 
implements Serializable
{
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(AbstractDunningStep.class);
	
	@PrimaryKey
	@Column(length=100)
	private String organisationID;

	@PrimaryKey
	@Column(length=100)
	private String dunningStepID;
	
	@Persistent(nullValue=NullValue.EXCEPTION)
	private DunningConfig dunningConfig;
	
	@Persistent(nullValue=NullValue.EXCEPTION)
	private int dunningLevel;
	
	/**
	 * @deprecated This constructor exists only for JDO and should never be used directly!
	 */
	@Deprecated
	protected AbstractDunningStep() { }
	
	/**
	 * Create an instance of <code>AbstractDunningStep</code>.
	 *
	 */
	public AbstractDunningStep(String organisationID, String dunningStepID, DunningConfig dunningConfig, int dunningLevel) {
		Organisation.assertValidOrganisationID(organisationID);
		ObjectIDUtil.assertValidIDString(dunningStepID, "dunningStepID"); //$NON-NLS-1$
		this.organisationID = organisationID;
		this.dunningStepID = dunningStepID;
		this.dunningConfig = dunningConfig;
		this.dunningLevel = dunningLevel;
	}
	
	public String getOrganisationID() {
		return organisationID;
	}
	
	public String getDunningStepID() {
		return dunningStepID;
	}
	
	public DunningConfig getDunningConfig() {
		return dunningConfig;
	}
	
	public int getDunningLevel() {
		return dunningLevel;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((dunningConfig == null) ? 0 : dunningConfig.hashCode());
		result = prime * result + dunningLevel;
		result = prime * result
				+ ((dunningStepID == null) ? 0 : dunningStepID.hashCode());
		result = prime * result
				+ ((organisationID == null) ? 0 : organisationID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractDunningStep other = (AbstractDunningStep) obj;
		if (dunningConfig == null) {
			if (other.dunningConfig != null)
				return false;
		} else if (!dunningConfig.equals(other.dunningConfig))
			return false;
		if (dunningLevel != other.dunningLevel)
			return false;
		if (dunningStepID == null) {
			if (other.dunningStepID != null)
				return false;
		} else if (!dunningStepID.equals(other.dunningStepID))
			return false;
		if (organisationID == null) {
			if (other.organisationID != null)
				return false;
		} else if (!organisationID.equals(other.organisationID))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AbstractDunningStep [dunningConfig=" + dunningConfig
				+ ", dunningLevel=" + dunningLevel + ", dunningStepID="
				+ dunningStepID + ", organisationID=" + organisationID + "]";
	}
}
