package org.nightlabs.jfire.dunning;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.PersistenceModifier;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.nightlabs.jdo.ObjectIDUtil;
import org.nightlabs.jfire.accounting.Invoice;
import org.nightlabs.jfire.accounting.Price;
import org.nightlabs.jfire.dunning.id.DunningLetterEntryID;
import org.nightlabs.jfire.organisation.Organisation;

/**
 * A DunningLetterEntry contains all the information needed to list the corresponding 
 * invoice in the the table of dunned invoices in the DunningLetter. 
 * 
 * <br>Among this information is the new due date, the interests already build up 
 * and the severity of the dunning step which reflects how long the invoice 
 * is overdue already.
 * 
 * @author Chairat Kongarayawetchakun - chairat [AT] nightlabs [DOT] de
 */
@PersistenceCapable(
		objectIdClass=DunningLetterEntryID.class,
		identityType=IdentityType.APPLICATION,
		detachable="true",
		table="JFireDunning_DunningLetterEntry")
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
public class DunningLetterEntry 
implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	@PrimaryKey
	@Column(length=100)
	private String organisationID;

	@PrimaryKey
	@Column(length=100)
	private String dunningLetterEntryID;
	
	/**
	 * The severity of the dunning for the corresponding invoice, 
	 * i.e. the InvoiceDunning step in which the process is for the invoice.
	 */
	@Persistent(persistenceModifier=PersistenceModifier.PERSISTENT)
	private int dunningLevel;
	
	/**
	 * The invoice that is overdue and represented by this entry.
	 */
	@Persistent(persistenceModifier=PersistenceModifier.PERSISTENT)
	private Invoice invoice;
	
	/**
	 * Copied during creation from corresponding dunningStep.
	 */
	@Persistent(persistenceModifier=PersistenceModifier.PERSISTENT)
	private long periodOfGraceMSec;
	
	/**
	 * The due-date until which this DunningLetter needs to be 
	 * paid. It is calculated when the DunningLetter is finalized 
	 * (and null till finalization):  this.finalizeDT + this.periodOfGraceMSec.
	 */
	@Persistent(persistenceModifier=PersistenceModifier.PERSISTENT)
	private Date extendedDueDateForPayment;
	
	/**
	 * The interests that already arose until the time 
	 * of the creation of the corresponding DunningLetter. 
	 * Note: These will include all the Interests that were 
	 * already contained in the previous DunningLetter, but 
	 * they may be summarized by the DunningInterestCalculator.
	 */
	@Join
	@Persistent(
		table="JFireDunning_DunningLetterEntry_dunningFees",
		persistenceModifier=PersistenceModifier.PERSISTENT)
	private List<DunningInterest> dunningInterests;
	
	/**
	 * The total amount to pay for the overdue invoice.
	 */
	@Persistent(persistenceModifier=PersistenceModifier.PERSISTENT)
	private Price priceIncludingInvoice;
	
	/**
	 * @deprecated Only for JDO!!!!
	 */
	@Deprecated
	protected DunningLetterEntry() { }
	
	public DunningLetterEntry(String organisationID, String dunningLetterEntryID, int dunningLevel, Invoice invoice) {
		Organisation.assertValidOrganisationID(organisationID);
		ObjectIDUtil.assertValidIDString(dunningLetterEntryID, "dunningLetterEntryID"); //$NON-NLS-1$
	
		this.organisationID = organisationID;
		this.dunningLetterEntryID = dunningLetterEntryID;
		this.dunningLevel = dunningLevel;
		this.invoice = invoice;
	}
	
	public String getOrganisationID() {
		return organisationID;
	}
	
	public String getDunningLetterEntryID() {
		return dunningLetterEntryID;
	}
	
	public int getDunningLevel() {
		return dunningLevel;
	}
	
	public Invoice getInvoice() {
		return invoice;
	}
	
	public void setPeriodOfGraceMSec(long periodOfGraceMSec) {
		this.periodOfGraceMSec = periodOfGraceMSec;
	}
	
	public long getPeriodOfGraceMSec() {
		return periodOfGraceMSec;
	}
	
	public Date getExtendedDueDateForPayment() {
		return extendedDueDateForPayment;
	}
	
	public void setExtendedDueDateForPayment(Date extendedDueDateForPayment) {
		this.extendedDueDateForPayment = extendedDueDateForPayment;
	}
	
	public List<DunningInterest> getDunningInterests() {
		return dunningInterests;
	}
	
	public void setPriceIncludingInvoice(Price priceIncludingInvoice) {
		this.priceIncludingInvoice = priceIncludingInvoice;
	}
	
	public Price getPriceIncludingInvoice() {
		return priceIncludingInvoice;
	}
}