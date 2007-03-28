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
package org.nightlabs.jfire.scripting.editor2d.impl;

import java.awt.Font;
import java.awt.Rectangle;

import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeException;
import net.sourceforge.barbecue.linear.code128.Code128Barcode;

import org.apache.log4j.Logger;
import org.nightlabs.editor2d.DrawComponentContainer;
import org.nightlabs.editor2d.impl.DrawComponentImpl;
import org.nightlabs.editor2d.render.Renderer;
import org.nightlabs.i18n.unit.IUnit;
import org.nightlabs.i18n.unit.MMUnit;
import org.nightlabs.i18n.unit.resolution.DPIResolutionUnit;
import org.nightlabs.i18n.unit.resolution.IResolutionUnit;
import org.nightlabs.jdo.ObjectIDUtil;
import org.nightlabs.jfire.scripting.editor2d.BarcodeDrawComponent;
import org.nightlabs.jfire.scripting.editor2d.render.BarcodeRenderer;
import org.nightlabs.jfire.scripting.id.ScriptRegistryItemID;

/**
 * @author Daniel.Mazurek [at] NightLabs [dot] de
 *
 */
public class BarcodeDrawComponentImpl 
extends DrawComponentImpl 
implements BarcodeDrawComponent 
{
	public static final Logger logger = Logger.getLogger(BarcodeDrawComponentImpl.class);
	
	public BarcodeDrawComponentImpl() {
		super();
	}
	
	public BarcodeDrawComponentImpl(Type type, String value, int x, int y, WidthScale widthScale, 
			int height, Orientation orientation, boolean printHumanReadable, 
			DrawComponentContainer parent, ScriptRegistryItemID scriptID) 
	{
		super();

		if (value == null)
			throw new IllegalArgumentException("Param value must not be null!");
		
		if (scriptID == null)
			throw new IllegalArgumentException("Param scriptID must not be null!");
				
		setParent(parent);
		this.type = type;
		this.x = x;
		this.y = y;		
		this.text = value;		
		this.humanReadable = printHumanReadable;
		this.widthScale = widthScale;
		this.orientation = orientation;
//		this.width = height;
		this.height = height;
		this.scriptRegistryItemID = scriptID;
		this.scriptRegistryItemIDKeyStr = scriptRegistryItemID.toString(); 

		refresh();
	}
	
	private static final IResolutionUnit dpiUnit = new DPIResolutionUnit();
	private int getModelResolution() {
		return (int) getRoot().getResolution().getResolutionX(dpiUnit);		
	}
	
	private transient Barcode barcode = null;
	public Barcode getBarcode() 
	{
		if (barcode == null) 
		{
			try {
				barcode = getBarcode(getType());				
			} catch (BarcodeException e) { 
				throw new RuntimeException(e);
			}
		}
		return barcode;
	}
	
	protected Barcode getBarcode(Type type) 
	throws BarcodeException
	{
		Barcode barcode = null;
		switch (type) 
		{
			case TYPE_128:
				barcode = new Code128Barcode(getText());				
		}
		return barcode;
	}
		
	private Type type = TYPE_DEFAULT;
	public Type getType() {
		return type;
	}
	public void setType(Type type) 
	{
		if (this.type != type) 
		{
			Type oldType = this.type;
			this.type = type;
			refresh();
			firePropertyChange(PROP_TYPE, oldType, type);
		}
	}
	
	private boolean humanReadable = HUMAN_READABLE_DEFAULT;
	public boolean isHumanReadable() {
		return humanReadable;
	}
	public void setHumanReadable(boolean humanReadbale) {
		this.humanReadable = humanReadbale;
		getBarcode().setDrawingText(humanReadbale);
		clearBounds();
		firePropertyChange(PROP_HUMAN_READABLE, !humanReadbale, humanReadbale);
	}

	private transient ScriptRegistryItemID scriptRegistryItemID = null;
	private String scriptRegistryItemIDKeyStr = null;

	public ScriptRegistryItemID getScriptRegistryItemID() {
		if (scriptRegistryItemID == null && scriptRegistryItemIDKeyStr != null)
			scriptRegistryItemID = (ScriptRegistryItemID) ObjectIDUtil.createObjectID(scriptRegistryItemIDKeyStr);
		return scriptRegistryItemID;
	}

	public void setScriptRegistryItemID(ScriptRegistryItemID scriptRegistryItemID) 
	{
		ScriptRegistryItemID oldID = this.scriptRegistryItemID; 
		this.scriptRegistryItemID = scriptRegistryItemID;
		this.scriptRegistryItemIDKeyStr = scriptRegistryItemID == null ? null : scriptRegistryItemID.toString();
		refresh();		
		firePropertyChange(PROP_SCRIPT_REGISTRY_ITEM_ID, oldID, scriptRegistryItemID);
	}

	private Orientation orientation = ORIENTATION_DEFAULT;
	public Orientation getOrientation() {
		return orientation;
	}
	public void setOrientation(Orientation orientation) 
	{
		if (this.orientation != orientation) {
			Orientation oldOrientation = this.orientation;
			this.orientation = orientation;
			refresh();
			firePropertyChange(PROP_ORIENTATION, oldOrientation, orientation);
		}
	}

	private WidthScale widthScale = WIDTH_SCALE_DEFAULT;
	public WidthScale getWidthScale() {
		return widthScale;
	}
	public void setWidthScale(WidthScale widthScale) 
	{
		if (this.widthScale != widthScale) 
		{
			WidthScale oldWidthScale = this.widthScale;
			this.widthScale = widthScale;
			refresh();
			firePropertyChange(PROP_WIDTH_SCALE, oldWidthScale, widthScale);
		}		
	}	

	protected void refresh() 
	{
		clearBounds();
				
		Font scaledFont = getScaledFont(DEFAULT_FONT);		
		getBarcode().setFont(scaledFont);
		
		getBarcode().setDrawingText(isHumanReadable());
		getBarcode().setResolution(getModelResolution());
//		getBarcode().setResolution(300);
						
		double barWidth = getBarWidth(getWidthScale());
		getBarcode().setBarWidth(barWidth);
		
//		if (orientation == Orientation.HORIZONTAL) 
//		{			
//			getBarcode().setBarHeight(height);			
//			this.width = getBarcode().getWidth();	
//			this.height = getBarcode().getHeight();			
//		}
//		if (orientation == Orientation.VERTICAL) 
//		{			
////			getBarcode().setBarHeight(width);			
//			getBarcode().setBarHeight(height);
//			this.width = getBarcode().getHeight();
//			this.height = getBarcode().getWidth(); 			
//		}
		
		getBarcode().setBarHeight(height);
//		this.width = getBarcode().getWidth();				
//		this.height = getBarcode().getHeight();
		
		clearBounds();
		
		if (logger.isDebugEnabled()) 
		{
			logger.debug("Resolution = "+getModelResolution());			
			logger.debug("Orientation = "+getOrientation());
			logger.debug("x = "+x);			
			logger.debug("y = "+y);			
			logger.debug("barWidth = "+barWidth);
			logger.debug("width = "+this.width);			
			logger.debug("height = "+this.height);			
			logger.debug("getBarcode.getBounds() = "+getBarcode().getBounds());
			logger.debug("this.getBounds() = "+this.getBounds());		
		}
	}
		
//	/* (non-Javadoc)
//	 * @see org.nightlabs.editor2d.impl.DrawComponentImpl#getBounds()
//	 */
//	@Override
//	public Rectangle getBounds() 
//	{
//		if (bounds == null) {
//			bounds = getBarcode().getBounds();
//		}
//		return bounds;
//	}

	/* (non-Javadoc)
	 * @see org.nightlabs.editor2d.impl.DrawComponentImpl#getBounds()
	 */
	@Override
	public Rectangle getBounds() 
	{
		if (bounds == null) {
			if (orientation == Orientation.HORIZONTAL) 			
				bounds = new Rectangle(x, y, getBarcode().getWidth(), getBarcode().getHeight());			
			if (orientation == Orientation.VERTICAL) 						
				bounds = new Rectangle(x, y, getBarcode().getHeight(), getBarcode().getWidth());				
		}
		return bounds;
	}	
	
	private static final IUnit mmUnit = new MMUnit();
	protected double getBarWidth(WidthScale scale) 
	{		
		double width = 1;
		switch (scale) 
		{
			case SCALE_1:
				width = 4d;
				break;
			case SCALE_2:
				width = 5;
				break;
			case SCALE_3:
				width = 3;
				break;
			case SCALE_4:
				width = 2;
				break;				
			default:
				width = 5;
				break;		
		}
		
		int resolution = getModelResolution();
		double factor = ((double)resolution) / 300d;		
		double scaledWidth = width * factor;
		if (logger.isDebugEnabled()) {
			logger.debug("width = " + width);
			logger.debug("ModelUnit Factor = "+factor);
			logger.debug("ScaledBarWidth = " + scaledWidth);			
		}		
		return scaledWidth;
		
//		return width;
	}
	
//	protected double getBarWidth(WidthScale scale) 
//	{
//		int barWidth = UnitUtil.getModelValue(DEFAULT_BAR_WIDTH, getRoot().getModelUnit(), mmUnit);
//		return barWidth * getScaleFactor(scale);							
//	}
//	private int getScaleFactor(WidthScale scale) 
//	{
//		switch (scale) 
//		{
////			case SCALE_1:
////				return 1;
////			case SCALE_2:
////				return 2;
////			case SCALE_3:
////				return 3;
////			case SCALE_4:
////				return 4;
//		case SCALE_1:
//			return 4;
//		case SCALE_2:
//			return 5;
//		case SCALE_3:
//			return 3;
//		case SCALE_4:			
//			return 2;		
//		default:
//			return 3;
//		}
//	}
	
//	protected double getBarWidth(WidthScale scale) 
//	{
////		double widthInMM = 70;
////		switch (scale) 
////		{
////			case SCALE_1:
////				widthInMM = 56;
////				break;
////			case SCALE_2:
////				widthInMM = 70;
////				break;
////			case SCALE_3:
////				widthInMM = 42;
////				break;
////			case SCALE_4:
////				widthInMM = 28;
////				break;				
////			default:
////				widthInMM = 70;
////				break;
////		}
//		double widthInMM = 0.70;
//		switch (scale) 
//		{
//			case SCALE_1:
//				widthInMM = 0.56;
//				break;
//			case SCALE_2:
//				widthInMM = 0.70;
//				break;
//			case SCALE_3:
//				widthInMM = 0.42;
//				break;
//			case SCALE_4:
//				widthInMM = 0.28;
//				break;				
//			default:
//				widthInMM = 0.70;
//				break;
//		}
//		
//		double barWidth = UnitUtil.getModelValue(widthInMM, getRoot().getModelUnit(), mmUnit);
//		if (logger.isDebugEnabled()) {
//			logger.debug("widthInMM = " + widthInMM);
//			logger.debug("ModelUnit Factor = "+getRoot().getModelUnit().getFactor());
//			logger.debug("ScaledBarWidth = " + barWidth);			
//		}
//		return barWidth;
//	}

//	protected double getBarWidth(WidthScale scale) 
//	{
//		switch (scale) 
//		{
//			case SCALE_1:
//				return 4;
//			case SCALE_2:
//				return 5;
//			case SCALE_3:
//				return 3;
//			case SCALE_4:
//				return 2;
//			default:
//				return 3;
//		}
//	}
	
	protected Font getScaledFont(Font f) 
	{
		double defaultFontSize = f.getSize();
		double defaultResolutionDPI = 72;
		double resolutionScale = getModelResolution() / defaultResolutionDPI;
		int newFontSize = (int) (defaultFontSize * resolutionScale);
		return new Font(f.getName(), f.getStyle(), newFontSize);		
	}
		
	@Override
	protected void primSetHeight(float height) 
	{
		super.primSetHeight(height);		
		refresh();
	}

	@Override
	public String getTypeName() {
		return "Ticket Barcode";
	}

	@Override
	public Class getRenderModeClass() {
		return BarcodeDrawComponent.class;
	}
	
	@Override
	protected void primSetLocation(int newX, int newY) 
	{		
		this.x = newX;
		this.y = newY;
		getBarcode().setLocation(newX, newY);
	}

	@Override
	protected Renderer initDefaultRenderer() {
		return new BarcodeRenderer();
	}
				
	private transient String text = VALUE_DEFAULT;
	public String getText() {
		return text;
	}
	public void setText(String text) 
	{
		if (this.text == null || 
			(this.text != null && !this.text.equals(text)) )
		{
			String oldValue = this.text;
			this.text = text;
			refresh();			
			firePropertyChange(PROP_VALUE, oldValue, text);
		}
	}

	private transient Object scriptValue;
	public Object getScriptValue() {
		return scriptValue;
	}
	public void setScriptValue(Object scriptValue) {
		this.scriptValue = scriptValue;
		if (scriptValue instanceof String) {
			setText(((String)scriptValue));
		}
	}	
	
}
