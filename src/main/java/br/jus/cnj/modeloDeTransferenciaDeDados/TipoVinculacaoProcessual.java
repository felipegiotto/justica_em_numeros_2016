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
 * 				Tipo de elemento que permite informar uma vinculação
 * 				entre um processo judicial e outro.
 * 			
 * 
 * <p>Classe Java de tipoVinculacaoProcessual complex type.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * <pre>
 * &lt;complexType name="tipoVinculacaoProcessual">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="numeroProcesso" use="required" type="{http://www.cnj.jus.br/modelo-de-transferencia-de-dados-1.0}tipoNumeroUnico" />
 *       &lt;attribute name="vinculo" use="required" type="{http://www.cnj.jus.br/modelo-de-transferencia-de-dados-1.0}modalidadeVinculacaoProcesso" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tipoVinculacaoProcessual")
public class TipoVinculacaoProcessual {

    @XmlAttribute(name = "numeroProcesso", required = true)
    protected String numeroProcesso;
    @XmlAttribute(name = "vinculo", required = true)
    protected ModalidadeVinculacaoProcesso vinculo;

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
     * Obtém o valor da propriedade vinculo.
     * 
     * @return
     *     possible object is
     *     {@link ModalidadeVinculacaoProcesso }
     *     
     */
    public ModalidadeVinculacaoProcesso getVinculo() {
        return vinculo;
    }

    /**
     * Define o valor da propriedade vinculo.
     * 
     * @param value
     *     allowed object is
     *     {@link ModalidadeVinculacaoProcesso }
     *     
     */
    public void setVinculo(ModalidadeVinculacaoProcesso value) {
        this.vinculo = value;
    }

}
