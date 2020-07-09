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
 * <p>Classe Java de tipoOrgaoJulgador complex type.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * <pre>
 * &lt;complexType name="tipoOrgaoJulgador">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="codigoOrgao" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="nomeOrgao" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="instancia" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="ORIG"/>
 *             &lt;enumeration value="REV"/>
 *             &lt;enumeration value="ESP"/>
 *             &lt;enumeration value="EXT"/>
 *             &lt;enumeration value="ADM"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="codigoMunicipioIBGE" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tipoOrgaoJulgador")
public class TipoOrgaoJulgador {

    @XmlAttribute(name = "codigoOrgao", required = true)
    protected int codigoOrgao;
    @XmlAttribute(name = "nomeOrgao", required = true)
    protected String nomeOrgao;
    @XmlAttribute(name = "instancia", required = true)
    protected String instancia;
    @XmlAttribute(name = "codigoMunicipioIBGE", required = true)
    protected int codigoMunicipioIBGE;

    /**
     * Obtém o valor da propriedade codigoOrgao.
     * 
     */
    public int getCodigoOrgao() {
        return codigoOrgao;
    }

    /**
     * Define o valor da propriedade codigoOrgao.
     * 
     */
    public void setCodigoOrgao(int value) {
        this.codigoOrgao = value;
    }

    /**
     * Obtém o valor da propriedade nomeOrgao.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNomeOrgao() {
        return nomeOrgao;
    }

    /**
     * Define o valor da propriedade nomeOrgao.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNomeOrgao(String value) {
        this.nomeOrgao = value;
    }

    /**
     * Obtém o valor da propriedade instancia.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInstancia() {
        return instancia;
    }

    /**
     * Define o valor da propriedade instancia.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInstancia(String value) {
        this.instancia = value;
    }

    /**
     * Obtém o valor da propriedade codigoMunicipioIBGE.
     * 
     */
    public int getCodigoMunicipioIBGE() {
        return codigoMunicipioIBGE;
    }

    /**
     * Define o valor da propriedade codigoMunicipioIBGE.
     * 
     */
    public void setCodigoMunicipioIBGE(int value) {
        this.codigoMunicipioIBGE = value;
    }

}
