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
 *     http://www.gnu.org/copyleft/lesser.html                                 *
 *                                                                             *
 *                                                                             *
 ******************************************************************************/
package org.nightlabs.jfire.scripting.condition;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Daniel.Mazurek [at] NightLabs [dot] de
 *
 */
public class ConditionScriptRegistry 
{
	private Map<String, IConditionContextProvider> context2ContextProvider;
	protected Map<String, IConditionContextProvider> getContext2ContextProvider() {
		if (context2ContextProvider == null) {
			context2ContextProvider = new HashMap<String, IConditionContextProvider>();
		}
		return context2ContextProvider;
	}
	
	public void registerConditionContextProvider(IConditionContextProvider contextProvider) 
	{
		if (contextProvider == null)
			throw new IllegalArgumentException("Param contextProvider must NOT be null!");
		
		getContext2ContextProvider().put(contextProvider.getConditionContext(), contextProvider);
	}
	
	public IConditionContextProvider getConditionContextProvider(String context) {
		return getContext2ContextProvider().get(context);
	}
	
}
