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
 * 
 * 				Tipo de elemento destinado a permitir prestar
 * 				informações relativas a assuntos criados localmente pelo
 * 				tribunal.
 * 			
 * 
 * <p>Classe Java de tipoAssuntoLocal complex type.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * <pre>
 * &lt;complexType name="tipoAssuntoLocal">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="assuntoLocalPai" type="{http://www.cnj.jus.br/modelo-de-transferencia-de-dados-1.0}tipoAssuntoLocal" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="codigoAssunto" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="codigoPaiNacional" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="descricao" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tipoAssuntoLocal", propOrder = {
    "assuntoLocalPai"
})
public class TipoAssuntoLocal {

    protected TipoAssuntoLocal assuntoLocalPai;
    @XmlAttribute(name = "codigoAssunto", required = true)
    protected int codigoAssunto;
    @XmlAttribute(name = "codigoPaiNacional", required = true)
    protected int codigoPaiNacional;
    @XmlAttribute(name = "descricao", required = true)
    protected String descricao;

    /**
     * Obtém o valor da propriedade assuntoLocalPai.
     * 
     * @return
     *     possible object is
     *     {@link TipoAssuntoLocal }
     *     
     */
    public TipoAssuntoLocal getAssuntoLocalPai() {
        return assuntoLocalPai;
    }

    /**
     * Define o valor da propriedade assuntoLocalPai.
     * 
     * @param value
     *     allowed object is
     *     {@link TipoAssuntoLocal }
     *     
     */
    public void setAssuntoLocalPai(TipoAssuntoLocal value) {
        this.assuntoLocalPai = value;
    }

    /**
     * Obtém o valor da propriedade codigoAssunto.
     * 
     */
    public int getCodigoAssunto() {
        return codigoAssunto;
    }

    /**
     * Define o valor da propriedade codigoAssunto.
     * 
     */
    public void setCodigoAssunto(int value) {
        this.codigoAssunto = value;
    }

    /**
     * Obtém o valor da propriedade codigoPaiNacional.
     * 
     */
    public int getCodigoPaiNacional() {
        return codigoPaiNacional;
    }

    /**
     * Define o valor da propriedade codigoPaiNacional.
     * 
     */
    public void setCodigoPaiNacional(int value) {
        this.codigoPaiNacional = value;
    }

    /**
     * Obtém o valor da propriedade descricao.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Define o valor da propriedade descricao.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescricao(String value) {
        this.descricao = value;
    }

}
