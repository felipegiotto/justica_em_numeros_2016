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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 				Tipo destinado a permitir a vinculação entre uma parte
 * 				e outra pessoa, sendo esclarecido o tipo de relacionamento
 * 				por meio
 * 				da modalidade indicada no atributo do elemento.
 * 			
 * 
 * <p>Classe Java de tipoRelacionamentoPessoal complex type.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * <pre>
 * &lt;complexType name="tipoRelacionamentoPessoal">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="pessoa" type="{http://www.cnj.jus.br/modelo-de-transferencia-de-dados-1.0}tipoPessoa"/>
 *       &lt;/sequence>
 *       &lt;attribute name="modalidadeRelacionamento" type="{http://www.cnj.jus.br/modelo-de-transferencia-de-dados-1.0}modalidadesRelacionamentoPessoal" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tipoRelacionamentoPessoal", propOrder = {
    "pessoa"
})
public class TipoRelacionamentoPessoal {

    @XmlElement(required = true)
    protected TipoPessoa pessoa;
    @XmlAttribute(name = "modalidadeRelacionamento")
    protected ModalidadesRelacionamentoPessoal modalidadeRelacionamento;

    /**
     * Obtém o valor da propriedade pessoa.
     * 
     * @return
     *     possible object is
     *     {@link TipoPessoa }
     *     
     */
    public TipoPessoa getPessoa() {
        return pessoa;
    }

    /**
     * Define o valor da propriedade pessoa.
     * 
     * @param value
     *     allowed object is
     *     {@link TipoPessoa }
     *     
     */
    public void setPessoa(TipoPessoa value) {
        this.pessoa = value;
    }

    /**
     * Obtém o valor da propriedade modalidadeRelacionamento.
     * 
     * @return
     *     possible object is
     *     {@link ModalidadesRelacionamentoPessoal }
     *     
     */
    public ModalidadesRelacionamentoPessoal getModalidadeRelacionamento() {
        return modalidadeRelacionamento;
    }

    /**
     * Define o valor da propriedade modalidadeRelacionamento.
     * 
     * @param value
     *     allowed object is
     *     {@link ModalidadesRelacionamentoPessoal }
     *     
     */
    public void setModalidadeRelacionamento(ModalidadesRelacionamentoPessoal value) {
        this.modalidadeRelacionamento = value;
    }

}
