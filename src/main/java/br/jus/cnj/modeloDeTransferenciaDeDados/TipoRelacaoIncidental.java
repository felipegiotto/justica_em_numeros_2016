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
 * 				Tipo de elemento que permite identificar se existe
 * 				algum elemento incidental que tenha
 * 				gerado novo processo judicial, em razão da existência de uma nova relação
 * 				processual jurídica
 * 				(por exemplo, como pode ocorrer em embargos à execução de títulos
 * 				extrajudiciais,
 * 				embargos de terceiro, em recursos internos, impugnações ao valor da causa,
 * 				entre outras
 * 				Situações, em que há criação de novos autos vinculados ao processo principal).
 * 			
 * 
 * <p>Classe Java de tipoRelacaoIncidental complex type.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * <pre>
 * &lt;complexType name="tipoRelacaoIncidental">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="numeroProcesso" type="{http://www.cnj.jus.br/modelo-de-transferencia-de-dados-1.0}tipoNumeroUnico" />
 *       &lt;attribute name="tipoRelacao" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="classeProcessual" type="{http://www.w3.org/2001/XMLSchema}int" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tipoRelacaoIncidental")
public class TipoRelacaoIncidental {

    @XmlAttribute(name = "numeroProcesso")
    protected String numeroProcesso;
    @XmlAttribute(name = "tipoRelacao")
    protected String tipoRelacao;
    @XmlAttribute(name = "classeProcessual")
    protected Integer classeProcessual;

    /**
     * Obtém o valor da propriedade numeroProcesso.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    /**
     * Define o valor da propriedade numeroProcesso.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNumeroProcesso(String value) {
        this.numeroProcesso = value;
    }

    /**
     * Obtém o valor da propriedade tipoRelacao.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTipoRelacao() {
        return tipoRelacao;
    }

    /**
     * Define o valor da propriedade tipoRelacao.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTipoRelacao(String value) {
        this.tipoRelacao = value;
    }

    /**
     * Obtém o valor da propriedade classeProcessual.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getClasseProcessual() {
        return classeProcessual;
    }

    /**
     * Define o valor da propriedade classeProcessual.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setClasseProcessual(Integer value) {
        this.classeProcessual = value;
    }

}
