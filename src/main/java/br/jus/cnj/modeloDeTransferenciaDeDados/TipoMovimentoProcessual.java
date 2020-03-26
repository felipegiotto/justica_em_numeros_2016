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
 * 				Tipo de elemento destinado a permitir apresentar
 * 				informações relativas à movimentação processual.
 * 			
 * 
 * <p>Classe Java de tipoMovimentoProcessual complex type.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * <pre>
 * &lt;complexType name="tipoMovimentoProcessual">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="complemento" type="{http://www.cnj.jus.br/modelo-de-transferencia-de-dados-1.0}tipoComplemento" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;choice>
 *           &lt;element name="movimentoNacional" type="{http://www.cnj.jus.br/modelo-de-transferencia-de-dados-1.0}tipoMovimentoNacional"/>
 *           &lt;element name="movimentoLocal" type="{http://www.cnj.jus.br/modelo-de-transferencia-de-dados-1.0}tipoMovimentoLocal"/>
 *         &lt;/choice>
 *         &lt;element name="complementoNacional" type="{http://www.cnj.jus.br/modelo-de-transferencia-de-dados-1.0}tipoComplementoNacional" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="magistradoProlator" type="{http://www.cnj.jus.br/modelo-de-transferencia-de-dados-1.0}tipoCadastroIdentificador" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="idDocumentoVinculado" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="orgaoJulgador" type="{http://www.cnj.jus.br/modelo-de-transferencia-de-dados-1.0}tipoOrgaoJulgador" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="dataHora" use="required" type="{http://www.cnj.jus.br/modelo-de-transferencia-de-dados-1.0}tipoDataHora" />
 *       &lt;attribute name="nivelSigilo" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="identificadorMovimento" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="responsavelMovimento" type="{http://www.cnj.jus.br/modelo-de-transferencia-de-dados-1.0}tipoCadastroIdentificador" />
 *       &lt;attribute name="tipoResponsavelMovimento" type="{http://www.w3.org/2001/XMLSchema}int" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tipoMovimentoProcessual", propOrder = {
    "complemento",
    "movimentoNacional",
    "movimentoLocal",
    "complementoNacional",
    "magistradoProlator",
    "idDocumentoVinculado",
    "orgaoJulgador"
})
public class TipoMovimentoProcessual {

    protected List<String> complemento;
    protected TipoMovimentoNacional movimentoNacional;
    protected TipoMovimentoLocal movimentoLocal;
    protected List<TipoComplementoNacional> complementoNacional;
    protected List<String> magistradoProlator;
    protected List<String> idDocumentoVinculado;
    protected TipoOrgaoJulgador orgaoJulgador;
    @XmlAttribute(name = "dataHora", required = true)
    protected String dataHora;
    @XmlAttribute(name = "nivelSigilo")
    protected Integer nivelSigilo;
    @XmlAttribute(name = "identificadorMovimento")
    protected String identificadorMovimento;
    @XmlAttribute(name = "responsavelMovimento")
    protected String responsavelMovimento;
    @XmlAttribute(name = "tipoResponsavelMovimento")
    protected Integer tipoResponsavelMovimento;

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
     * Obtém o valor da propriedade movimentoNacional.
     * 
     * @return
     *     possible object is
     *     {@link TipoMovimentoNacional }
     *     
     */
    public TipoMovimentoNacional getMovimentoNacional() {
        return movimentoNacional;
    }

    /**
     * Define o valor da propriedade movimentoNacional.
     * 
     * @param value
     *     allowed object is
     *     {@link TipoMovimentoNacional }
     *     
     */
    public void setMovimentoNacional(TipoMovimentoNacional value) {
        this.movimentoNacional = value;
    }

    /**
     * Obtém o valor da propriedade movimentoLocal.
     * 
     * @return
     *     possible object is
     *     {@link TipoMovimentoLocal }
     *     
     */
    public TipoMovimentoLocal getMovimentoLocal() {
        return movimentoLocal;
    }

    /**
     * Define o valor da propriedade movimentoLocal.
     * 
     * @param value
     *     allowed object is
     *     {@link TipoMovimentoLocal }
     *     
     */
    public void setMovimentoLocal(TipoMovimentoLocal value) {
        this.movimentoLocal = value;
    }

    /**
     * Gets the value of the complementoNacional property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the complementoNacional property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getComplementoNacional().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TipoComplementoNacional }
     * 
     * 
     */
    public List<TipoComplementoNacional> getComplementoNacional() {
        if (complementoNacional == null) {
            complementoNacional = new ArrayList<TipoComplementoNacional>();
        }
        return this.complementoNacional;
    }

    /**
     * Gets the value of the magistradoProlator property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the magistradoProlator property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMagistradoProlator().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getMagistradoProlator() {
        if (magistradoProlator == null) {
            magistradoProlator = new ArrayList<String>();
        }
        return this.magistradoProlator;
    }

    /**
     * Gets the value of the idDocumentoVinculado property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the idDocumentoVinculado property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getIdDocumentoVinculado().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getIdDocumentoVinculado() {
        if (idDocumentoVinculado == null) {
            idDocumentoVinculado = new ArrayList<String>();
        }
        return this.idDocumentoVinculado;
    }

    /**
     * Obtém o valor da propriedade orgaoJulgador.
     * 
     * @return
     *     possible object is
     *     {@link TipoOrgaoJulgador }
     *     
     */
    public TipoOrgaoJulgador getOrgaoJulgador() {
        return orgaoJulgador;
    }

    /**
     * Define o valor da propriedade orgaoJulgador.
     * 
     * @param value
     *     allowed object is
     *     {@link TipoOrgaoJulgador }
     *     
     */
    public void setOrgaoJulgador(TipoOrgaoJulgador value) {
        this.orgaoJulgador = value;
    }

    /**
     * Obtém o valor da propriedade dataHora.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDataHora() {
        return dataHora;
    }

    /**
     * Define o valor da propriedade dataHora.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDataHora(String value) {
        this.dataHora = value;
    }

    /**
     * Obtém o valor da propriedade nivelSigilo.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getNivelSigilo() {
        return nivelSigilo;
    }

    /**
     * Define o valor da propriedade nivelSigilo.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setNivelSigilo(Integer value) {
        this.nivelSigilo = value;
    }

    /**
     * Obtém o valor da propriedade identificadorMovimento.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIdentificadorMovimento() {
        return identificadorMovimento;
    }

    /**
     * Define o valor da propriedade identificadorMovimento.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIdentificadorMovimento(String value) {
        this.identificadorMovimento = value;
    }

    /**
     * Obtém o valor da propriedade responsavelMovimento.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResponsavelMovimento() {
        return responsavelMovimento;
    }

    /**
     * Define o valor da propriedade responsavelMovimento.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setResponsavelMovimento(String value) {
        this.responsavelMovimento = value;
    }

    /**
     * Obtém o valor da propriedade tipoResponsavelMovimento.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getTipoResponsavelMovimento() {
        return tipoResponsavelMovimento;
    }

    /**
     * Define o valor da propriedade tipoResponsavelMovimento.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setTipoResponsavelMovimento(Integer value) {
        this.tipoResponsavelMovimento = value;
    }

}
