package org.nightlabs.jfire.accounting.query;

import java.util.Set;

import javax.jdo.Query;

import org.nightlabs.jdo.query.JDOQuery;
import org.nightlabs.jfire.accounting.Invoice;
import org.nightlabs.jfire.accounting.id.InvoiceID;
import org.nightlabs.jfire.transfer.id.AnchorID;

/**
 * Every field that's <code>null</code> is ignored, every field containing a value
 * will cause the query to filter all non-matching instances.
 *
 * @author Marco Schulze - marco at nightlabs dot de
 */
public class InvoiceQuery
extends JDOQuery<Set<InvoiceID>>
{
	private static final long serialVersionUID = 1L;

	private AnchorID vendorID = null;
	private AnchorID customerID = null;
	private Long amountToPayMin = null;
	private Long amountToPayMax = null;
	private Long amountPaidMin = null;
	private Long amountPaidMax = null;
	private Boolean booked = null;

	@Override
	protected Query prepareQuery()
	{
		Query q = getPersistenceManager().newQuery(Invoice.class);
		q.setResult("JDOHelper.getObjectId(this)");

		StringBuffer filter = new StringBuffer();

		filter.append("1 == 1");

		if (amountToPayMin != null)
			filter.append("\n && this.price.amount - this.invoiceLocal.amountPaid > amountToPayMin");

		if (amountToPayMax != null)
			filter.append("\n && this.price.amount - this.invoiceLocal.amountPaid < amountToPayMax");

		if (amountPaidMin != null)
			filter.append("\n && this.invoiceLocal.amountPaid > amountPaidMin");

		if (amountPaidMax != null)
			filter.append("\n && this.invoiceLocal.amountPaid < amountPaidMax");

		if (booked != null && booked.booleanValue())
			filter.append("\n && this.invoiceLocal.bookDT != null");

		if (booked != null && !booked.booleanValue())
			filter.append("\n && this.invoiceLocal.bookDT == null");

		if (vendorID != null)
			filter.append("\n && JDOHelper.getObjectId(this.vendor) == vendorID");

		if (customerID != null)
			filter.append("\n && JDOHelper.getObjectId(this.customer) == customerID");

		q.setFilter(filter.toString());
		return q;
	}

	public Long getAmountPaidMax()
	{
		return amountPaidMax;
	}

	public void setAmountPaidMax(Long amountPaidMax)
	{
		this.amountPaidMax = amountPaidMax;
	}

	public Long getAmountPaidMin()
	{
		return amountPaidMin;
	}

	public void setAmountPaidMin(Long amountPaidMin)
	{
		this.amountPaidMin = amountPaidMin;
	}

	public Long getAmountToPayMax()
	{
		return amountToPayMax;
	}

	public void setAmountToPayMax(Long amountToPayMax)
	{
		this.amountToPayMax = amountToPayMax;
	}

	public Long getAmountToPayMin()
	{
		return amountToPayMin;
	}

	public void setAmountToPayMin(Long amountToPayMin)
	{
		this.amountToPayMin = amountToPayMin;
	}

	public Boolean getBooked()
	{
		return booked;
	}

	public void setBooked(Boolean booked)
	{
		this.booked = booked;
	}

	public AnchorID getCustomerID()
	{
		return customerID;
	}

	public void setCustomerID(AnchorID customerID)
	{
		this.customerID = customerID;
	}

	public AnchorID getVendorID()
	{
		return vendorID;
	}

	public void setVendorID(AnchorID vendorID)
	{
		this.vendorID = vendorID;
	}

	
}
