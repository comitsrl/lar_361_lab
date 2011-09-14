/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2007 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package ar.com.ergio.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Model for LAR_Fiscal_Printer_Type
 *  @author Adempiere (generated) 
 *  @version 3.6.0LTS+P20110709 - $Id$ */
public class X_LAR_Fiscal_Printer_Type extends PO implements I_LAR_Fiscal_Printer_Type, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20110831L;

    /** Standard Constructor */
    public X_LAR_Fiscal_Printer_Type (Properties ctx, int LAR_Fiscal_Printer_Type_ID, String trxName)
    {
      super (ctx, LAR_Fiscal_Printer_Type_ID, trxName);
      /** if (LAR_Fiscal_Printer_Type_ID == 0)
        {
			setclazz (null);
			setLAR_Fiscal_Printer_Type_ID (0);
			setName (null);
        } */
    }

    /** Load Constructor */
    public X_LAR_Fiscal_Printer_Type (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 7 - System - Client - Org 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_LAR_Fiscal_Printer_Type[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set clazz.
		@param clazz clazz	  */
	public void setclazz (String clazz)
	{
		set_Value (COLUMNNAME_clazz, clazz);
	}

	/** Get clazz.
		@return clazz	  */
	public String getclazz () 
	{
		return (String)get_Value(COLUMNNAME_clazz);
	}

	/** Set Description.
		@param Description 
		Optional short description of the record
	  */
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription () 
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** Set Fiscal Printer Type.
		@param LAR_Fiscal_Printer_Type_ID Fiscal Printer Type	  */
	public void setLAR_Fiscal_Printer_Type_ID (int LAR_Fiscal_Printer_Type_ID)
	{
		if (LAR_Fiscal_Printer_Type_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_LAR_Fiscal_Printer_Type_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_LAR_Fiscal_Printer_Type_ID, Integer.valueOf(LAR_Fiscal_Printer_Type_ID));
	}

	/** Get Fiscal Printer Type.
		@return Fiscal Printer Type	  */
	public int getLAR_Fiscal_Printer_Type_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LAR_Fiscal_Printer_Type_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Name.
		@param Name 
		Alphanumeric identifier of the entity
	  */
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName () 
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair() 
    {
        return new KeyNamePair(get_ID(), getName());
    }

	/** Set parameters.
		@param parameters parameters	  */
	public void setparameters (String parameters)
	{
		set_Value (COLUMNNAME_parameters, parameters);
	}

	/** Get parameters.
		@return parameters	  */
	public String getparameters () 
	{
		return (String)get_Value(COLUMNNAME_parameters);
	}
}