/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/

package ar.com.comit.factura.electronica;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Properties;

import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.util.Env;

public class ElectronicInvoiceProvider
{

    /** Propiedades del entorno. */
    public static Properties ctx = Env.getCtx();

    /** Preferencia sobre proveedor del servicio de FE */
    public static String PREFERENCE_WSFE_PROVIDER = "ar.com.comit.factura.electronica.ProcesadorWSFE";

    /** Preferencia sobre proveedor del servicio de FE de Exportacion */
    public static String PREFERENCE_WSFEX_PROVIDER = "";

    // @fchiappano Se comentan variables, ya que actualmente manejamos una sola implementación.
    // Evaluar a futuro si es necesario utilizarla, quizas en una transición de funcionalidad.
    /** Nombre de la clase que provee el servicio de FE (si es que existe) */
    //public static MPreference wsfeProviderClass = MPreference.getClientPreference(Env.getAD_Client_ID(ctx),
            //PREFERENCE_WSFE_PROVIDER, ctx, null);

    /** Nombre de la clase que provee el servicio de FEX (si es que existe) */
    //public static MPreference wsfexProviderClass = MPreference.getClientPreference(Env.getAD_Client_ID(ctx),
            //PREFERENCE_WSFEX_PROVIDER, ctx, null);

    /**
     * Listado de tipos de documento de exportacion segun definicion de FE de
     * AFIP
     */
    private static ArrayList<String> exportacionDocTypes;
    static
    {
        // Nomina de tipos de documento de exportacin
        exportacionDocTypes = new ArrayList<String>();
        exportacionDocTypes.add(MDocType.DOCSUBTYPECAE_FacturaDeExportaciónE);
        exportacionDocTypes.add(MDocType.DOCSUBTYPECAE_NotaDeDébitoPorOperacionesEnElExterior);
        exportacionDocTypes.add(MDocType.DOCSUBTYPECAE_NotaDeCréditoPorOperacionesEnElExterior);
    }

    /**
     * Retorna la implementacion (si es que existe) encargada de gestionar la
     * registracion de la FE
     */
    public static ElectronicInvoiceInterface getImplementation(MInvoice inv)
    {
        try
        {
            // Recuperar el docType de la factura para determinar si es de
            // exportacion o no
            MDocType docType = new MDocType(Env.getCtx(), inv.getC_DocTypeTarget_ID(), inv.get_TrxName());
            if (exportacionDocTypes.contains(docType.getdocsubtypecae()))
            {
                return getProvider(inv, PREFERENCE_WSFEX_PROVIDER);
            }
            return getProvider(inv, PREFERENCE_WSFE_PROVIDER);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /** Retorna el provider si es que existe, ya sea de exportacion o no */
    protected static ElectronicInvoiceInterface getProvider(MInvoice inv, String providerClass) throws Exception
    {
        // Si no hay proveedor alguno, retornar null
        if (providerClass == null || providerClass.length() == 0)
            return null;
        // Intentar instanciar y utilizar dicho proveedor
        Class<?> clazz = Class.forName(providerClass);
        Constructor<?> constructor = clazz.getDeclaredConstructor(new Class[] { MInvoice.class });
        return (ElectronicInvoiceInterface) constructor.newInstance(new Object[] { inv });
    }

} // ElectronicInvoiceProvider
