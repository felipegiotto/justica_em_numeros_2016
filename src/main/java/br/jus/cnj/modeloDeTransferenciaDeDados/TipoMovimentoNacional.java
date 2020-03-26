//
// Este arquivo foi gerado pela Arquitetura JavaTM para Implementação de Referência (JAXB) de Bind XML, v2.2.8-b130911.1802 
// Consulte <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Todas as modificações neste arquivo serão perdidas após a recompilação do esquema de origem. 
// Gerado em: 2020.03.25 às 02:38:12 PM BRT 
//


package br.jus.cnj.modeloDeTransferenciaDeDados;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 				Tipo de elemento que permite a indicação da realização
 * 				de um movimento existente na tabela unificada de que
 * 				trata a	Resolução 46.
 * 			
 * 
 * <p>Classe Java de tipoMovimentoNacional complex type.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * <pre>
 * &lt;complexType name="tipoMovimentoNacional">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="complemento" type="{http://www.cnj.jus.br/modelo-de-transferencia-de-dados-1.0}tipoComplemento" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="codigoNacional" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tipoMovimentoNacional", propOrder = {
    "complemento"
})
public class TipoMovimentoNacional {

    protected List<String> complemento;
    @XmlAttribute(name = "codigoNacional", required = true)
    protected int codigoNacional;

    /**
     * Gets the value of the complemento property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the complemento property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getComplemento().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getComplemento() {
        if (complemento == null) {
            complemento = new ArrayList<String>();
        }
        return this.complemento;
    }

    /**
     * Obtém o valor da propriedade codigoNacional.
     * 
     */
    public int getCodigoNacional() {
        return codigoNacional;
    }

    /**
     * Define o valor da propriedade codigoNacional.
     * 
     */
    public void setCodigoNacional(int value) {
        this.codigoNacional = value;
    }

}
