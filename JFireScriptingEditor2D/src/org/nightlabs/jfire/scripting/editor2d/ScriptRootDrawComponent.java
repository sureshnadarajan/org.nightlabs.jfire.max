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
package org.nightlabs.jfire.scripting.editor2d;

import java.util.Map;
import java.util.Set;

import org.nightlabs.editor2d.DrawComponent;
import org.nightlabs.editor2d.RootDrawComponent;
import org.nightlabs.jfire.scripting.condition.Script;
import org.nightlabs.jfire.scripting.id.ScriptRegistryItemID;

/**
 * The interface for the root drawcomponent {@link RootDrawComponent} which
 * contains {@link ScriptDrawComponent}s and which is responsible for assigning the values
 * of the scripts to the corresponding {@link ScriptDrawComponent}s
 * 
 * @author Daniel.Mazurek <at> NightLabs <dot> de
 *
 */
public interface ScriptRootDrawComponent
extends RootDrawComponent
{
	public static final String PROP_SCRIPT_VALUES = "Script Values";
	
	/**
	 * returns a {@link Set} of all {@link ScriptRegistryItemID}s which are contained in
	 * the {@link ScriptRootDrawComponent}
	 * @return a Set of all ScriptRegistryItemIDs
	 */
	public Set<ScriptRegistryItemID> getScriptRegistryItemIDs();
	
	/**
	 * assigns all the values of the scripts to the contained {@link ScriptDrawComponent}s
	 * with the corresponding {@link ScriptRegistryItemID}
	 * 
	 * @param scriptValues a {@link Map} which contains all the values of the scripts
	 * for the corresponding {@link ScriptRegistryItemID}s as key
	 * key: ScriptRegistryItemID
	 * value: value of the script
	 */
	public void assignScriptResults(Map<ScriptRegistryItemID, Object> scriptValues);
		
	/**
	 * returns a Map with all visibleScripts and the corresponding drawComponent ID
	 *
	 * key: DrawComponent ID {@link DrawComponent#getId()}
	 * value: VisibleScript {@link DrawComponent#getProperties().get(ScriptingConstants.PROP_VISIBLE_SCRIPT)}
	 * 
	 * @return a {@link Map} with the drawComponent ID as key, and
	 * the VisibleScript as value
	 */
	public Map<Long, Script> getVisibleScripts();
	
	/**
	 * assigns the visible property according to value of the visibleScript,
	 * obtained from the Method {@link ScriptRootDrawComponent#getVisibleScripts()}
	 * 
	 * key: DrawComponentID {@link DrawComponent#getId()}
	 * value: result of visibleScript {@link DrawComponent#getProperties().get(ScriptingConstants.PROP_VISIBLE_SCRIPT)}
	 * 
	 * @param scriptValues a map with the drawComponent ID as key, and the value
	 * of the visibleScript as value
	 */
	public void assignVisibleScriptResults(Map<Long, Boolean> scriptValues);
}
