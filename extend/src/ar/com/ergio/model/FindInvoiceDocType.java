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
package ar.com.ergio.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;

import ar.com.ergio.util.LAR_Utils;

/**
 * Encuentra el tipo de Factura/Nota de Crédito para un determinado
 * Socio del Negocio, a partir del C_POS_ID y el AD_Org_ID
 * 
 * @author      Marcos Zúñiga    - http://www.ergio.com.ar
 * @contributor Emiliano Pereyra - http://www.ergio.com.ar
 */
public final class FindInvoiceDocType
{
    private MBPartner bpartner;
    private int c_POS_ID;
    private int ad_Org_ID;
    private MDocType docType;
    private int lar_DocumentLetter_ID;
    private String docBaseType;
    private MInvoice invoice;
    private boolean esFce = false;

    public FindInvoiceDocType(final MBPartner bpartner, final int c_POS_ID, final int ad_Org_ID, final String docBaseType)
    {
        this.bpartner = bpartner;
        this.c_POS_ID = c_POS_ID;
        this.ad_Org_ID = ad_Org_ID;
        this.docBaseType = docBaseType;
        retrieveDocType();
    }

    /**
     * Nuevo contructor, que identifica si la factura es MiPymes.
     *
     * @author fchiappano
     *
     * @param bpartner
     * @param c_POS_ID
     * @param ad_Org_ID
     * @param docBaseType
     * @param invoice
     */
    public FindInvoiceDocType(final MBPartner bpartner, final int c_POS_ID, final int ad_Org_ID, final String docBaseType, final MInvoice invoice)
    {
        this.bpartner = bpartner;
        this.c_POS_ID = c_POS_ID;
        this.ad_Org_ID = ad_Org_ID;
        this.docBaseType = docBaseType;
        this.invoice = invoice;
        setEsFce();
        retrieveDocType();
    }

    /**
     * Dependiendo de la Condición ante el IVA
     * del SdN (TaxPAyerType), la Organización y el Punto de Venta
     *encuentra el tipo de Factura a realizar
     *
     * @param bpartner Socio del Negocio
     * @param c_POS_ID ID del Punto de Venta
     * @param ad_Org_ID ID de la Organización
     * @exception Exception
     *                si la configuración del SdN o del POs no es válida.
     */
    private void retrieveDocType()
    {
        // Obtiene el tipo de documento Factura correspondiente
        int ad_Client_ID = Env.getAD_Client_ID(Env.getCtx());
        final MOrgInfo orgInfo = MOrgInfo.get(Env.getCtx(), ad_Org_ID, bpartner.get_TrxName());
        int lco_TaxPayerType_Vendor_ID = orgInfo.get_ValueAsInt("LCO_TaxPayerType_ID");
        int lco_TaxPayerType_Customer_ID = bpartner.get_ValueAsInt("LCO_TaxPayerType_ID");

        // Chequea la categoría ante el IVA del SdN (Emisior)
        if (lco_TaxPayerType_Vendor_ID == 0)
        {
            throw new AdempiereException("VendorTaxPayerTypeNotFound");
        }
        
        // Chequea la categoría ante el IVA del SdN (Cliente)
        if (lco_TaxPayerType_Customer_ID == 0)
        {
            throw new AdempiereException("CustomerTaxPayerTypeNotFound");
        }

        // Chequea el Nº de POS
        if (c_POS_ID == 0) 
        {
            throw new AdempiereException("PosConfigNotFound");
        }
        
        // Determina la letra a Facturar
        // Emisor > AD_Org | Cliente > BPartner
        String sql = "SELECT L.LAR_DocumentLetter_ID"
                   + "  FROM LAR_DocumentLetter L"
                   + "  JOIN LAR_LetterRule R ON R.LAR_DocumentLetter_ID = L.LAR_DocumentLetter_ID"
                   + " WHERE R.LCO_TaxPayerType_Vendor_ID=?"
                   + "   AND R.LCO_TaxPayerType_Customer_ID=?";

        lar_DocumentLetter_ID = DB.getSQLValue(bpartner.get_TrxName(), sql,
                lco_TaxPayerType_Vendor_ID, lco_TaxPayerType_Customer_ID);
        
        StringBuilder whereClause = new StringBuilder("AD_Client_ID=?")
                                              .append(" AND AD_Org_ID=?")
                                              .append(" AND IsActive=?")
                                              .append(" AND DocBaseType=?")
                                              .append(" AND FiscalDocument=?") // 'F' > Invoice
                                              .append(" AND LAR_DocumentLetter_ID=?")
                                              .append(" AND C_POS_ID=?")
                                              // @fchiappano Filtrar facturas de credito electronicas.
                                              .append(" AND EsFce =  ?");
        // @mzuniga Determina si es Factura o Nota de Crédito
        // utilizando el tipo de documentobase
        Object[] params = new Object[] {
                ad_Client_ID,
                ad_Org_ID,
                "Y",
                docBaseType,
                (docBaseType.equals(MDocType.DOCBASETYPE_ARInvoice) ? LAR_MDocType.FISCALDOCUMENT_Invoice
                        : LAR_MDocType.FISCALDOCUMENT_CreditNote),
                lar_DocumentLetter_ID,
                c_POS_ID, esFce };
        docType = new Query(Env.getCtx(), MDocType.Table_Name, whereClause.toString(), bpartner.get_TrxName())
                .setParameters(params)
                .firstOnly();
    } // retrieveDocType

    /**
     * Validar si el tipo de doc que se requiere, debe ser MiPymes (FCE).
     */
    private void setEsFce()
    {
        // @fchiappano si se trata de una nota de credito, chequear si la factura origen es FCE.
        if (docBaseType.equals(MDocType.DOCBASETYPE_ARCreditMemo))
        {
            MInvoice facturaOrigen = new MInvoice(invoice.getCtx(), invoice.get_ValueAsInt("Source_Invoice_ID"), invoice.get_TrxName());
            esFce = ((MDocType) facturaOrigen.getC_DocTypeTarget()).get_ValueAsBoolean("EsFce");
        }
        else
        {
            // @fchiappano Chequear si el tipo de documento, debe ser MiPyme (FCE).
            BigDecimal minimo_fce = new BigDecimal(MSysConfig.getDoubleValue("LAR_MinimoFacturaMiPyme",
                    Env.ZERO.doubleValue(), Env.getAD_Client_ID(Env.getCtx())));

            BigDecimal total = invoice.getGrandTotal();

            // @fchiappano si la moneda de la factura es distinta de la
            // predeterminada, realizar conversión.
            int moneda_predeterminada_ID = LAR_Utils.getMonedaPredeterminada(Env.getCtx(), invoice.getAD_Client_ID(), invoice.get_TrxName());
            if (invoice.getC_Currency_ID() != moneda_predeterminada_ID)
            {
                BigDecimal tasa = LAR_Utils.getTasaCambio(moneda_predeterminada_ID, invoice.getC_Currency_ID(), invoice.getDateInvoiced(),
                        invoice.getC_ConversionType_ID(), invoice.getAD_Client_ID(), ad_Org_ID, Env.getCtx(),
                        invoice.get_TrxName());

                total = total.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
            }

            if (bpartner.get_ValueAsBoolean("EsGrande") && total.compareTo(minimo_fce) >= 0)
                esFce = true;
        }
    }

    /**
     *  Obtiene el Tipo de Documento
     *  @return Doctype
     */
    public MDocType getDocType()
    {
        return docType;
    } // getDocType

    /**
     *  Obtiene la letra del Documento
     *  @return LAR_DocumentLetter_ID
     */
    public int getLAR_DocumentLetter_ID()
    {
        return lar_DocumentLetter_ID;
    } // getLAR_DocumentLetter_ID

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                    .append(bpartner.getTaxID())
                    .append(bpartner.getName())
                    .append((docType.getName() == null) ? "null" : (docType.getName()))
                    .toString();
    } // toString
} // FindInvoiceDocType