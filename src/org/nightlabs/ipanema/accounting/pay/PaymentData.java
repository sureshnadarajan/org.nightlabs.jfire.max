/*
 * Created on Jun 6, 2005
 */
package org.nightlabs.ipanema.accounting.pay;

import java.io.Serializable;

/**
 * Subclass in order to hold specific data for your payment process.
 * This additional data can be defined by the client payment processor (gathered by
 * wizard pages or other input forms) and is
 * passed to the {@link org.nightlabs.ipanema.accounting.pay.ServerPaymentProcessor}. 
 * <p>
 * Instances of this class are only stored temporarily and might be removed
 * from the datastore, afer a payment has been completed.
 * See {@link #clearSensitiveInformation()}
 *
 * @see org.nightlabs.ipanema.accounting.pay.PaymentDataCreditCard
 *
 * @author Marco Schulze - marco at nightlabs dot de
 *
 * @jdo.persistence-capable
 *		identity-type="application"
 *		objectid-class="org.nightlabs.ipanema.accounting.pay.id.PaymentDataID"
 *		detachable="true"
 *		table="JFireTrade_PaymentData"
 *
 * @jdo.inheritance strategy="new-table"
 */
public class PaymentData
implements Serializable
{
	/**
	 * @jdo.field primary-key="true"
	 * @jdo.column length="100"
	 */
	private String organisationID;
	/**
	 * @jdo.field primary-key="true"
	 * @jdo.column length="100"
	 */
	private String paymentID;

	/**
	 * @jdo.field persistence-modifier="persistent"
	 */
	private Payment payment;

	/**
	 * @jdo.field persistence-modifier="none"
	 */
	private transient Payment paymentBackupForUpload = null;

	/**
	 * @deprecated Only for JDO!
	 */
	protected PaymentData()
	{
	}

	public PaymentData(Payment payment)
	{
		this.payment = payment;
		this.organisationID = payment.getOrganisationID();
		this.paymentID = payment.getPaymentID();
	}

	/**
	 * This method is called multiple times, for initialization and after data
	 * has been written.
	 * It allows to set <tt>null</tt> members to empty strings or other
	 * "healthy" data. It is called, too, before the payment is performed to
	 * prevent <tt>NullPointerException</tt>s and similar.
	 * <p>
	 * If you don't overwrite it, this method is a no-op.
	 */
	public void init()
	{
	}

	/**
	 * @return Returns the organisationID.
	 */
	public String getOrganisationID()
	{
		return organisationID;
	}
	/**
	 * @return Returns the paymentID.
	 */
	public String getPaymentID()
	{
		return paymentID;
	}
	/**
	 * @return Returns the payment.
	 */
	public Payment getPayment()
	{
		return payment;
	}

	/**
	 * This method is called a certain time after payment (e.g. a few weeks). Overwrite
	 * it to remove sensitive information from your fields (e.g. set the credit card number
	 * to an empty string or keep only the last 4 digits). If the instance shall be removed
	 * from the datastore completely, you don't need to overwrite this method, because the
	 * default implementation returns <tt>true</tt>.
	 *
	 * @return Whether to delete the instance from datastore (<tt>true</tt>) or to keep it
	 *		(<tt>false</tt>).
	 */
	public boolean clearSensitiveInformation()
	{
		return true;
	}

	/**
	 * This method backups {@link #payment} by copying it to
	 * the transient non-persistent field {@link #paymentBackupForUpload}.
	 * {@link #payment} is set to the result of {@link Payment#cloneForUpload()}
	 * in order to minimize traffic.
	 *
	 * @see #restoreAfterUpload()
	 */
	public void prepareUpload()
	{
		paymentBackupForUpload = payment;
		payment = payment.cloneForUpload();
	}

	/**
	 * This method is called after upload to undo the changes done by
	 * {@link #prepareUpload()}.
	 */
	public void restoreAfterUpload()
	{
		if (paymentBackupForUpload == null)
			throw new IllegalStateException("paymentBackupForUpload == null! It seems as if prepareForUpload() was not called before!");

		payment = paymentBackupForUpload;
		paymentBackupForUpload = null;
	}
}
