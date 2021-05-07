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
package ar.com.comit.remito.electronico;

import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MLocation;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MRegion;
import org.compiere.model.MUOM;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.ValueNamePair;
import org.globalqss.model.X_LCO_TaxIdType;

import ar.com.ergio.model.LAR_TaxPayerType;

/**
 * Clase que procesara los datos del remito y solicitara el codigo COT a traves
 * del WS de ARBA.
 *
 * @author fchiappano
 */
public class ProcesadorCOT
{
    protected CLogger log = CLogger.getCLogger(ProcesadorCOT.class);

    // @fchiappano Remito por el cual se solicitara el codigo COT.
    private final MInOut remito;
    private final MOrgInfo orgInfo;
    private final String cuitOrg;
    private final Properties ctx = Env.getCtx();
    private final String separador = "|";
    private final String separadorNombre = "_";
    private final String finalLinea = "\r\n";

    // @fchiappano parametros fijos.
    private final String sujetoGenerador = "E";

    // @fchiappano parametros a mapear en config.
    private final String esDevolucion = "0";
    private final String codigoProd = "650300";
    private final String planta = "000";
    private final String puerta = "000";
    private final String extencion = ".txt";
    private final String codSecuencial;
    private final String nroDirec = "1000";

    public ProcesadorCOT(final MInOut remito)
    {
        this.remito = remito;
        this.orgInfo = MOrgInfo.get(ctx, Env.getAD_Org_ID(ctx), remito.get_TrxName());
        this.cuitOrg = orgInfo.get_ValueAsString("TaxID").replaceAll("-", "");
        int length = remito.getDocumentNo().length();
        this.codSecuencial = remito.getDocumentNo().substring(length - 6, length);
    } // ProcesadorCOT

    /**
     * Enviar solicitud de COT a WebService de ARBA.
     * @author fchiappano
     * @return mensaje
     */
    public String solcitarCOT()
    {
        // @fchiappano Generar el archivo txt que se enviara al WS.
        File informe = generarTXT();

        // @fchiappano Realizar la solicitud de COT.
        if (!COTWebServiceCliente.solicitarCOT(informe))
        {
            String errorMsg = COTWebServiceCliente.getMsgError();
            remito.set_ValueOfColumn("ErrorCOT", errorMsg);
            remito.saveEx(remito.get_TrxName());
            log.log(Level.SEVERE, "Error al solicitar COT: " + errorMsg);
            return errorMsg;
        }

        // @fchiappano Recuperar datos de autorización y setearlos en el remito.
        List<ValueNamePair> datosCOT = COTWebServiceCliente.getDatosCOT();

        for (ValueNamePair dato : datosCOT)
            remito.set_ValueOfColumn(dato.getName(), dato.getValue());

        if(!remito.save(remito.get_TrxName()))
            return "Error al actualizar el Remito";

        return "";
    } // solcitarCOT

    /**
     * Generar archivo Txt, con los datos requeridos del remito, para la solicitud del COT.
     * @author fchiappano
     * @return
     */
    private File generarTXT()
    {
        // @fchiappano Crear el archivo
        String nombreArchivo = "PB"
                             + separadorNombre + cuitOrg
                             + separadorNombre + planta + puerta
                             + separadorNombre + getFecha(remito.getMovementDate())
                             + separadorNombre + codSecuencial + extencion;

        File archivo = new File(System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + nombreArchivo);

        try
        {
            // @fchiappano Crear el StringBuffer y cargar los datos de la
            // cabecera del remito.
            StringBuffer cuerpoTxt = cargarDatosCabecera();

            // @fchiappano agregar al StringBuffer, los datos de las lineas del
            // remito.
            agregarLineas(cuerpoTxt);

            // @fchiappano Agregar final del TXT.
            cuerpoTxt.append("04" + separador + "1" + finalLinea);

            // @fchiappano Escribir el archivo.
            FileWriter escritor = new FileWriter(archivo);
            escritor.write(cuerpoTxt.toString());
            escritor.close();
        }
        catch (Exception ex)
        {
            log.log(Level.SEVERE, "Error al escribir archivo " + extencion);
        }

        return archivo;
    } // generarTXT

    /**
     * Crear el string buffer y cargarlo con todos los datos referidos a la
     * cabecera del remito.
     *
     * @author fchiappano
     * @return
     */
    private StringBuffer cargarDatosCabecera()
    {
        StringBuffer cabecera = new StringBuffer();

        // @fchiappano linea cabecera.
        cabecera.append("01" + separador + cuitOrg + finalLinea);

        MBPartner cliente = remito.getBPartner();

        // @fchiappano linea descripción del remito.
        String fechaEmision = getFecha(remito.getMovementDate());
        cabecera.append("02" + separador + fechaEmision);

        // @fchiappano Codigo tipo de comprobante (DocSubTypeCAE).
        cabecera.append(separador + generarCodigoUnicoRemito());

        // @fchiappano Fecha de salida.
        cabecera.append(separador + getFecha(new Timestamp(System.currentTimeMillis())));

        // @fchiappano Hora de salida
        cabecera.append(separador + " ");

        // @fchiappano Sujeto Generador (E: Emisor. D: Destinatario).
        cabecera.append(separador + sujetoGenerador);

        // @fchiappano parametro es consumidor final.
        LAR_TaxPayerType taxPayerType = LAR_TaxPayerType.getTaxPayerType(cliente);
        boolean esConsumidorFinal = taxPayerType.equals(LAR_TaxPayerType.CONSUMIDOR_FINAL);
        cabecera.append(separador + (esConsumidorFinal ? "1" : "0"));

        // @fchiappano tipo de documento cliente (DNI, CL,CUIL, etc. No informar si no se trata de un CF)
        if (esConsumidorFinal)
        {
            X_LCO_TaxIdType tipoDoc = new X_LCO_TaxIdType(ctx, cliente.get_ValueAsInt("LCO_TaxIDType_ID"),
                    remito.get_TrxName());
            cabecera.append(separador + tipoDoc.getName());
        }
        else
            cabecera.append(separador + " ");

        // @fchiappano nro de documento del cliente (CF).
        cabecera.append(separador + (esConsumidorFinal ? cliente.getTaxID().replaceAll("-", "") : " "));

        // @fchiappano numero de cuit del cliente (si no es CF)
        cabecera.append(separador + (!esConsumidorFinal ? cliente.getTaxID().replaceAll("-", "") : " "));

        // @fchiappano Razon social del cliente.
        cabecera.append(separador + cliente.getName());

        // @fchiappano Destinatario Tenedor (depende de la razon social del cliente?)
        cabecera.append(separador + (esConsumidorFinal ? "0" : "1"));

        // @fchiappano Datos de la direccion del cliente.
        MLocation direccion = (MLocation) remito.getC_BPartner_Location().getC_Location();
        cabecera.append(separador + direccion.getAddress1());
        // Numero
        cabecera.append(separador + nroDirec);
        // Destino_Domicilio_Comple
        cabecera.append(separador + " ");
        // Piso
        cabecera.append(separador + " ");
        // Departamento
        cabecera.append(separador + " ");
        // Barrio
        cabecera.append(separador + " ");
        // Codigo Postal
        cabecera.append(separador + direccion.getPostal());
        // Ciudad
        cabecera.append(separador + direccion.getCity());
        // Codigo Provincia
        cabecera.append(separador + ((MRegion) direccion.getC_Region()).get_ValueAsString("CodigoCOT"));
        // PROPIO_DESTINO_DOMICILIO_CODIGO
        cabecera.append(separador + " ");

        // @fchiappano Entrega domicilio Origen.
        cabecera.append(separador + "NO");

        // @fchiappano cuit origen (Si Sujeto_Generador = E, entonces debe ir el
        // CUIT de la org. Sino, CUIT del SdN).
        cabecera.append(separador + cuitOrg);

        // @fchiappano Razon social del emisor
        cabecera.append(separador + MClient.get(ctx).getName());

        // @fachiappano Emisor Tenedor.
        cabecera.append(separador + "1");

        // @fchiappano Datos de dirección origen.
        // Calle
        MLocation direcOrigen = (MLocation) orgInfo.getC_Location();
        cabecera.append(separador + direcOrigen.getAddress1());
        // Numero
        cabecera.append(separador + nroDirec);
        // ORIGEN_DOMICILIO_COMPLE
        cabecera.append(separador + " ");
        // Piso
        cabecera.append(separador + " ");
        // Departamento
        cabecera.append(separador + " ");
        // Barrio
        cabecera.append(separador + " ");
        // Codigo postal
        cabecera.append(separador + direcOrigen.getPostal());
        // Localidad
        cabecera.append(separador + direcOrigen.getCity());
        // Cod Provincia
        cabecera.append(separador + ((MRegion) direcOrigen.getC_Region()).get_ValueAsString("CodigoCOT"));

        // @fchiappano Cuit del transportista.
        cabecera.append(separador + cuitOrg); // FIXME Recuperar CUIT desde la tabla del transportista.

        // @fchiappano Tipo Recorrido (‘U’, ‘R’, ‘M’ o ‘ ’).
        cabecera.append(separador + " ");

        // @fchiappano Localidad del Recorrido.
        cabecera.append(separador + " ");

        // @fchiappano Calle del Recorrido.
        cabecera.append(separador + " ");

        // @fchiappano Ruta del Recorrido.
        cabecera.append(separador + " ");

        // @fchiappano Patente del vehiculo.
        cabecera.append(separador + remito.get_Value("Patente"));

        // @fchiappano Patente del Acoplado del vehiculo.
        cabecera.append(separador + remito.get_Value("Patente_Acoplado"));

        // @fchiappano Es devolucion
        cabecera.append(separador + esDevolucion);

        // @rchiappano Importe total remito (descontando impuestos = neto)
        cabecera.append(separador + formatearBigDecimal(remito.getC_Order().getTotalLines()));

        // @fchiappano Agregar final de Linea.
        cabecera.append(finalLinea);

        return cabecera;
    } // cargarDatosCabecera

    /**
     * Agregar al cuerpo del txt, los datos requeridos de las lineas del remito.
     * @author fchiappano
     * @param cuerpoTxt
     */
    private void agregarLineas(final StringBuffer cuerpoTxt)
    {
        MInOutLine[] lineas = remito.getLines();

        for (int x = 0; x < lineas.length; x ++)
        {
            MInOutLine linea = lineas[x];
            MUOM unidadMedida = (MUOM) linea.getC_UOM();

            // @fchiappano agregar codigo de linea.
            cuerpoTxt.append("03");

            // @fchiappano Codigo de producto.
            cuerpoTxt.append(separador + codigoProd);

            // @fchiappano Codigo de UM
            cuerpoTxt.append(separador + unidadMedida.get_ValueAsString("CodigoCOT"));

            // @fchiappano Cantidad
            cuerpoTxt.append(separador + formatearBigDecimal(linea.getQtyEntered()));

            // @fchiappano Datos del producto.
            cuerpoTxt.append(separador + linea.getM_Product().getValue());// Codigo.
            cuerpoTxt.append(separador + linea.getM_Product().getName());// Nombre/Descripcion

            // @fchiappano Descripcion UM
            cuerpoTxt.append(separador + unidadMedida.get_Translation("Name"));

            // @fchiappano Cantidad Ajustada FIXME (Determinar diferencia con el parametro cantidad).
            cuerpoTxt.append(separador + formatearBigDecimal(linea.getQtyEntered()));

            // @fchiappano Final de Linea.
            cuerpoTxt.append(finalLinea);
        }
    } // agregarLineas

    /**
     * Formatear BigDecimal a un string, según requerimiento del WS.
     * @author fchiappano
     * @param decimal
     * @return decimalStr
     */
    private String formatearBigDecimal(BigDecimal decimal)
    {
        String decimalStr = decimal.setScale(2, RoundingMode.HALF_UP).toString();
        decimalStr = decimalStr.replace(",", "").replace(".", "");
        return decimalStr;
    } // formatearBigDecimal

    /**
     * Obtener Fecha de Vencimiento de Pago.
     * @author fchiappano
     * @return
     */
    private String getFecha(final Timestamp fecha)
    {
        if (fecha != null)
            return formatTime(fecha, "yyyyMMdd");
        else
            return formatTime(new Timestamp(System.currentTimeMillis()), "yyyyMMdd");
    } // getFecha

    /**
     * Formatear fecha a tipo String.
     * @author fchiappano
     * @param time
     * @param format
     * @return
     */
    private String formatTime(Timestamp time, String format)
    {
        SimpleDateFormat simpleformat = new SimpleDateFormat(format);
        Date date = new Date(time.getTime());
        return simpleformat.format(date);
    } // formatTime

    /**
     * Generar codigo unico de remito (codigo tipo doc + pdv + numero documento).
     * @author fchiappano
     * @return codigo unico
     */
    private String generarCodigoUnicoRemito()
    {
        String codigoUnico = "";

        // @fchiappano Recuperar el codigo del tipo de documento.
        String tipoCbte = ((MDocType) remito.getC_DocType()).get_ValueAsString("DocSubTypeCAE");

        // @fchiappano estandarizar numero punto de venta.
        // FIXME Determinar desde donde recuperar el nro de punto de venta.

        // @fchiappano recuperar numero de documento del remito.
        String nroDoc = remito.getDocumentNo().replace("-", "").replace("R", "0");
//        nroDoc = nroDoc.substring(nroDoc.length() -8, nroDoc.length());

        codigoUnico = tipoCbte + nroDoc;

        return codigoUnico;
    } // generarCodigoUnicoRemito

} // ProcesadorCOT
