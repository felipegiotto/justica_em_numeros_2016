//
// Este arquivo foi gerado pela Arquitetura JavaTM para Implementação de Referência (JAXB) de Bind XML, v2.2.8-b130911.1802 
// Consulte <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Todas as modificações neste arquivo serão perdidas após a recompilação do esquema de origem. 
// Gerado em: 2020.07.09 às 02:03:35 PM BRT 
//


package br.jus.cnj.modeloDeTransferenciaDeDados;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 				Tipo destinado a permitir a identificação de um
 * 				assunto
 * 				processual.
 * 			
 * 
 * <p>Classe Java de tipoAssuntoProcessual complex type.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * <pre>
 * &lt;complexType name="tipoAssuntoProcessual">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice>
 *         &lt;element name="codigoNacional" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="assuntoLocal" type="{http://www.cnj.jus.br/modelo-de-transferencia-de-dados-1.0}tipoAssuntoLocal"/>
 *       &lt;/choice>
 *       &lt;attribute name="principal" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tipoAssuntoProcessual", propOrder = {
    "codigoNacional",
    "assuntoLocal"
})
public class TipoAssuntoProcessual {

    protected Integer codigoNacional;
    protected TipoAssuntoLocal assuntoLocal;
    @XmlAttribute(name = "principal")
    protected Boolean principal;

    /**
     * Obtém o valor da propriedade codigoNacional.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getCodigoNacional() {
        return codigoNacional;
    }

    /**
     * Define o valor da propriedade codigoNacional.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setCodigoNacional(Integer value) {
        this.codigoNacional = value;
    }

    /**
     * Obtém o valor da propriedade assuntoLocal.
     * 
     * @return
     *     possible object is
     *     {@link TipoAssuntoLocal }
     *     
     */
    public TipoAssuntoLocal getAssuntoLocal() {
        return assuntoLocal;
    }

    /**
     * Define o valor da propriedade assuntoLocal.
     * 
     * @param value
     *     allowed object is
     *     {@link TipoAssuntoLocal }
     *     
     */
    public void setAssuntoLocal(TipoAssuntoLocal value) {
        this.assuntoLocal = value;
    }

    /**
     * Obtém o valor da propriedade principal.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isPrincipal() {
        if (principal == null) {
            return false;
        } else {
            return principal;
        }
    }

    /**
     * Define o valor da propriedade principal.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setPrincipal(Boolean value) {
        this.principal = value;
    }

}
