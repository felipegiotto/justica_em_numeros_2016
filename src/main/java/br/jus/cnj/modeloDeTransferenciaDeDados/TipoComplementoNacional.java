//
// Este arquivo foi gerado pela Arquitetura JavaTM para Implementação de Referência (JAXB) de Bind XML, v2.2.8-b130911.1802 
// Consulte <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Todas as modificações neste arquivo serão perdidas após a recompilação do esquema de origem. 
// Gerado em: 2020.03.25 às 02:38:12 PM BRT 
//


package br.jus.cnj.modeloDeTransferenciaDeDados;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * Tipo de elemento destinado a permitir a inclusão de
 * 				complementos de movimentações processuais.
 * 			
 * 
 * <p>Classe Java de tipoComplementoNacional complex type.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * <pre>
 * &lt;complexType name="tipoComplementoNacional">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="codComplemento" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="descricaoComplemento" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tipoComplementoNacional")
public class TipoComplementoNacional {

    @XmlAttribute(name = "codComplemento", required = true)
    protected int codComplemento;
    @XmlAttribute(name = "descricaoComplemento", required = true)
    protected String descricaoComplemento;

    /**
     * Obtém o valor da propriedade codComplemento.
     * 
     */
    public int getCodComplemento() {
        return codComplemento;
    }

    /**
     * Define o valor da propriedade codComplemento.
     * 
     */
    public void setCodComplemento(int value) {
        this.codComplemento = value;
    }

    /**
     * Obtém o valor da propriedade descricaoComplemento.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescricaoComplemento() {
        return descricaoComplemento;
    }

    /**
     * Define o valor da propriedade descricaoComplemento.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescricaoComplemento(String value) {
        this.descricaoComplemento = value;
    }

}
