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
 * 				Tipo de elemento destinado a permitir a identificação
 * 				de
 * 				um advogado inscrito na Ordem dos Advogados do Brasil,
 * 				de um
 * 				escritório de advocacia inscrito ou de um órgão de
 * 				representação
 * 				processual (advocacia pública, MP e
 * 				defensoria pública).
 * 			
 * 
 * <p>Classe Java de tipoRepresentanteProcessual complex type.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * <pre>
 * &lt;complexType name="tipoRepresentanteProcessual">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="endereco" type="{http://www.cnj.jus.br/modelo-de-transferencia-de-dados-1.0}tipoEndereco" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="nome" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="inscricao" type="{http://www.cnj.jus.br/modelo-de-transferencia-de-dados-1.0}tipoCadastroOAB" />
 *       &lt;attribute name="numeroDocumentoPrincipal" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="intimacao" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="tipoRepresentante" use="required" type="{http://www.cnj.jus.br/modelo-de-transferencia-de-dados-1.0}modalidadeRepresentanteProcessual" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tipoRepresentanteProcessual", propOrder = {
    "endereco"
})
public class TipoRepresentanteProcessual {

    protected List<TipoEndereco> endereco;
    @XmlAttribute(name = "nome", required = true)
    protected String nome;
    @XmlAttribute(name = "inscricao")
    protected String inscricao;
    @XmlAttribute(name = "numeroDocumentoPrincipal")
    protected String numeroDocumentoPrincipal;
    @XmlAttribute(name = "intimacao", required = true)
    protected boolean intimacao;
    @XmlAttribute(name = "tipoRepresentante", required = true)
    protected ModalidadeRepresentanteProcessual tipoRepresentante;

    /**
     * Gets the value of the endereco property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the endereco property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEndereco().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TipoEndereco }
     * 
     * 
     */
    public List<TipoEndereco> getEndereco() {
        if (endereco == null) {
            endereco = new ArrayList<TipoEndereco>();
        }
        return this.endereco;
    }

    /**
     * Obtém o valor da propriedade nome.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNome() {
        return nome;
    }

    /**
     * Define o valor da propriedade nome.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNome(String value) {
        this.nome = value;
    }

    /**
     * Obtém o valor da propriedade inscricao.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInscricao() {
        return inscricao;
    }

    /**
     * Define o valor da propriedade inscricao.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInscricao(String value) {
        this.inscricao = value;
    }

    /**
     * Obtém o valor da propriedade numeroDocumentoPrincipal.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNumeroDocumentoPrincipal() {
        return numeroDocumentoPrincipal;
    }

    /**
     * Define o valor da propriedade numeroDocumentoPrincipal.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNumeroDocumentoPrincipal(String value) {
        this.numeroDocumentoPrincipal = value;
    }

    /**
     * Obtém o valor da propriedade intimacao.
     * 
     */
    public boolean isIntimacao() {
        return intimacao;
    }

    /**
     * Define o valor da propriedade intimacao.
     * 
     */
    public void setIntimacao(boolean value) {
        this.intimacao = value;
    }

    /**
     * Obtém o valor da propriedade tipoRepresentante.
     * 
     * @return
     *     possible object is
     *     {@link ModalidadeRepresentanteProcessual }
     *     
     */
    public ModalidadeRepresentanteProcessual getTipoRepresentante() {
        return tipoRepresentante;
    }

    /**
     * Define o valor da propriedade tipoRepresentante.
     * 
     * @param value
     *     allowed object is
     *     {@link ModalidadeRepresentanteProcessual }
     *     
     */
    public void setTipoRepresentante(ModalidadeRepresentanteProcessual value) {
        this.tipoRepresentante = value;
    }

}
