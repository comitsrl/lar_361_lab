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
package ar.com.ergio.print.fiscal.document;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ar.com.ergio.print.fiscal.exception.DocumentException;

/**
 * Clase que representa un documento fiscal/no fiscal a imprimir en una
 * impresora fiscal. Cualquier documento que se deba imprimir en una
 * <code>FiscalPrinter</code> debe estar conformado por una clase que
 * especialice esta clase abstracta.
 * @author Franco Bonafine
 * @date 11/02/2008
 * @contributor Emiliano Pereyra - http://www.ergio.com.ar
 */
public abstract class Document implements Serializable{

	/**
	 * 
	 */
	protected static final long serialVersionUID = 1L;
	// Tipos de documento.
	/** Tipo de documento: Factura */
	public static final String DT_INVOICE = "I";
	/** Tipo de documento: Nota de Cr??dito */
	public static final String DT_CREDIT_NOTE = "CN";
	/** Tipo de documento: Nota de D??bito */
	public static final String DT_DEBIT_NOTE = "DN";
    /** Tipo de documento: Remito */
	public static final String DT_SHIPMENT = "S";
	
	// Letras de documento.
	/** Letra de Documento: A */
	public static final String DOC_LETTER_A = "A";
	/** Letra de Documento: B */
	public static final String DOC_LETTER_B = "B";
	/** Letra de Documento: C */
	public static final String DOC_LETTER_C = "C";
		
	/** Ciente asociado al documento */
	private Customer customer;
	/** N??mero de documento/comprobante */
	private String documentNo;
	/** L??neas del documento */
	private List<DocumentLine> lines;
	/** Percepcion general (emmie) */
    private PerceptionLine perceptionLine;
	/** Descuento general */
	private DiscountLine generalDiscount;
	/** Observaciones o descripci??nes del documento */
	private List<String> observations;
	/** Letra del documento. */ 
	private String letter;
	/** Descuentos a nivel documento */
	private List<DiscountLine> documentDiscounts = null;
	/** Apertura de caj??n de dinero */
	protected boolean aperturaCajon;
		
	public Document() {
		super();
		customer = new Customer();
		lines = new ArrayList<DocumentLine>();
		observations = new ArrayList<String>();
		documentDiscounts = new ArrayList<DiscountLine>();
		// por defecto, no se ejecuta el comando de apertura de caj??n
		aperturaCajon = false;
	}
	
	/**
	 * @return Returns the customer.
	 */
	public Customer getCustomer() {
		return customer;
	}
	
	/**
	 * @param customer The customer to set.
	 */
	public void setCustomer(Customer customer) {
		this.customer = customer;
	}
	
	/**
	 * @return Returns the documentNo.
	 */
	public String getDocumentNo() {
		return documentNo;
	}
	
	/**
	 * @param documentNo The documentNo to set.
	 */
	public void setDocumentNo(String documentNo) {
		this.documentNo = documentNo;
	}
	
	/**
	 * Agrega una l??nea al documento. 
	 * @param line L??nea a agregar.
	 */	
	public void addLine(DocumentLine line) {
		getLines().add(line);
	}
	
	/**
	 * Elimina una l??nea del documento.
	 * @param line L??nea a eliminar.
	 */
	public void removeLine(DocumentLine line) {
		getLines().remove(line);
	}

	/**
	 * @return Returns the lines.
	 */
	public List<DocumentLine> getLines() {
		return lines;
	}

    public void setPerceptionLine(PerceptionLine perceptionLine)
    {
        this.perceptionLine = perceptionLine;
    }

    public PerceptionLine getPerceptionLine()
    {
        return perceptionLine;
    }

	/**
	 * @return Returns the generalDiscount.
	 */
	public DiscountLine getGeneralDiscount() {
		return generalDiscount;
	}

	/**
	 * @param generalDiscount The generalDiscount to set.
	 */
	public void setGeneralDiscount(DiscountLine generalDiscount) {
		this.generalDiscount = generalDiscount;
	}

	/**
	 * @return Returns the observations.
	 */
	public List<String> getObservations() {
		return observations;
	}
	
	/**
	 * Agrega una observaci??n al documento.
	 * @param observation Texto de la observaci??n.
	 */
	public void addObservation(String observation) {
		observations.add(observation);
	}

	/**
	 * Elimina una observaci??n del documento
	 * @param observation Observaci??n a eliminar.
	 */
	public void removeObservation(Object observation) {
		observations.remove(observation);
	}


	/**
	 * @return Indica si el documento tiene o no asignado un descuento general.
	 */
	public boolean hasGeneralDiscount() {
		return getGeneralDiscount() != null;
	}

	/**
	 * @return Returns the letter.
	 */
	public String getLetter() {
		if(letter == null)
			letter = "";
		return letter;
	}

	/**
	 * @param letter The letter to set.
	 */
	public void setLetter(String letter) {
		this.letter = letter;
	}
	
	/**
	 * @return Retorna veraddero en caso de que el documento tenga una letra
	 * asignada.
	 */
	public boolean hasLetter() {
		return getLetter() != null;
	}
	
	/**
	 * @return Retorna el tipo de documento.
	 */
	public abstract String getDocumentType();
	
	/**
	 * Validaci??n del documento.
	 * @throws DocumentException cuando el documento no puede enviarse 
	 * a imprimir dado que esta acci??n producir??a un estado de error en la
	 * impresora fiscal.
	 */
	public void validate() throws DocumentException {
		try {
			// Se validan los datos del cliente.
			getCustomer().validate();
		} catch (DocumentException e) {
			// Se relanza la excepci??n agregando este documento como
			// par??metro.
			e.setDocument(this);
			throw e;
		}
		
		// Validar total del documento distinto de 0.
		validateNumber(getTotal(), "!=", BigDecimal.ZERO, "InvalidDocumentTotalAmount");
		
		// Validar las l??neas del documento.
		for (DocumentLine docLine : getLines()) {
			docLine.validate();
		}
		
		// Validar el descuento general.
		if(hasGeneralDiscount())
			getGeneralDiscount().validate();
		
		// Validar descuentos / recargos del documento
		for (DiscountLine discount : getDocumentDiscounts()) {
			discount.validate();
		}
	}
	
	/**
	 * @return Retorna el monto total del documento.
	 */
	public BigDecimal getTotal() {
		BigDecimal sum = BigDecimal.ZERO;
		// Se suma el total de cada l??nea.
		for (DocumentLine docLine : getLines()) {
			sum = sum.add(docLine.getLineTotal());
		}
		// Se suma el descuento en caso de existir.
		if(hasGeneralDiscount())
			sum = sum.add(getGeneralDiscount().getAmount());
		
		return sum;
	}
	
	
	/**
	 * Validaci??n de un texto. Valida que no sea null y que contenga al menos
	 * un caracter visible. 
	 * @param text Texto a validar.
	 * @param errorMsg Mensaje de error a lanzar en caso de que no sea v??lido.
	 * @throws DocumentException cuando el texto no es v??lido.
	 */
	public static void validateText(String text, String errorMsg) throws DocumentException {
		if(text == null || text.trim().length() == 0)
			throw createDocumentException(errorMsg);
	}
	
	/**
	 * Validaci??n de un n??mero. 
	 * @param number N??mero a validar.
	 * @param operand Operaci??n a realizar con <code>otherNumber</code> (<code><=, <, >, >=, ==, !=</code>).
	 * @param otherNumber N??mero a comparar.
	 * @param errorMsg Mensaje de error a lanzar en caso de que no sea v??lido.
	 * @throws DocumentException cuando el n??mero no cumple la condici??n.
	 */
	public static void validateNumber(BigDecimal number, String operand, BigDecimal otherNumber, String errorMsg) throws DocumentException {
		boolean operResult = false;
		if(operand.equals("<="))
			operResult = number.compareTo(otherNumber) <= 0;
		else if(operand.equals("<"))
			operResult = number.compareTo(otherNumber) < 0;
		else if(operand.equals(">"))
			operResult = number.compareTo(otherNumber) > 0;
		else if(operand.equals(">="))	
			operResult = number.compareTo(otherNumber) >= 0;
		else if(operand.equals("=="))
			operResult = number.compareTo(otherNumber) == 0;
		else if(operand.equals("!="))
			operResult = number.compareTo(otherNumber) != 0;
		else
			operResult = false;
		
		if(number == null || !operResult) 
			throw createDocumentException(errorMsg);
	}

	/**
	 * M??todo factory de excepciones de documento. 
	 * @param msg Mensaje de la excepci??n.
	 * @return La excepci??n.
	 */
	protected static DocumentException createDocumentException(String msg) {
		return createDocumentException(msg, null);
	}

	/**
	 * M??todo factory de excepciones de documento. 
	 * @param msg Mensaje de la excepci??n.
	 * @param document Documento origen de la excepci??n.
	 * @return La excepci??n.
	 */
	protected static DocumentException createDocumentException(String msg, Document document) {
		return new DocumentException(msg, document);
	}

	/**
	 * @return el valor de documentDiscounts
	 */
	public List<DiscountLine> getDocumentDiscounts() {
		return documentDiscounts;
	}
	
	/**
	 * @return Indica si ese documento tiene descuentos de encabezado asociados.
	 */
	public boolean hasDocumentDiscounts() {
		return !getDocumentDiscounts().isEmpty();
	}
	
	/**
	 * Agrega un descuento a nivel documento a este documento
	 * @param discount L??nea de descuento a agregar.
	 */
	public void addDocumentDiscount(DiscountLine discount) {
		getDocumentDiscounts().add(discount);
	}

    /**
     * Determina si es necesario ejecutar el comando de apertura del caj??n de
     * dinero.
     *
     */
    public void setAperturaCajon(final boolean aperturaCajon)
    {
        this.aperturaCajon = aperturaCajon;
    }

    /**
     * Determina si es necesario ejecutar el comando de apertura del caj??n de
     * dinero.
     * <p/>
     * Es necesario sobreescribir este m??todo en los documentos que necesiten
     * ejecutar este comando (por defecto es false).
     *
     * @return true si es necesario ejecutar el comando; false en caso contrario
     */
    public boolean isAperturaCajon()
    {
        return aperturaCajon;
    }

    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer()
                .append("documentNo=").append(documentNo)
                .append(",letter=").append(letter)
                .append(",customer=").append(customer)
                .append(",obs=").append(observations)
                .append(",perception=").append(perceptionLine)
                .append(",gd=").append(generalDiscount)
                .append(",dd=").append(documentDiscounts)
                .append(",aperturaCajon=").append(aperturaCajon)
                .append(",lines=").append(lines);
        return sb.toString();
    }
}
