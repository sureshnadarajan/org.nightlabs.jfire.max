package org.nightlabs.jfire.voucher.scripting.javaclass;

import org.nightlabs.jfire.scripting.AbstractScriptExecutorJavaClassDelegate;
import org.nightlabs.jfire.scripting.ScriptException;
import org.nightlabs.jfire.store.id.ProductID;
import org.nightlabs.jfire.voucher.scripting.VoucherScriptingConstants;
import org.nightlabs.jfire.voucher.store.Voucher;
import org.nightlabs.jfire.voucher.store.VoucherType;

/**
 * @author Daniel.Mazurek [at] NightLabs [dot] de
 *
 */
public abstract class AbstractVoucherScript 
extends AbstractScriptExecutorJavaClassDelegate 
{

	public AbstractVoucherScript() {
		super();
	}

	public void doPrepare() throws ScriptException {
		// default implementation is empty
	}

	public ProductID getVoucherID() {
		return (ProductID) getParameterValue(VoucherScriptingConstants.PARAMETER_ID_VOUCHER_ID);
	}	
	
	public Voucher getVoucher() {
		return (Voucher) getPersistenceManager().getObjectById(getVoucherID());	
	}
	
	public VoucherType getVoucherType() {
		return (VoucherType) getVoucher().getProductType();
	}
}
