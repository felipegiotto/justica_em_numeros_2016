//
// Este arquivo foi gerado pela Arquitetura JavaTM para Implementação de Referência (JAXB) de Bind XML, v2.2.8-b130911.1802 
// Consulte <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Todas as modificações neste arquivo serão perdidas após a recompilação do esquema de origem. 
// Gerado em: 2020.03.25 às 02:38:12 PM BRT 
//


package br.jus.cnj.modeloDeTransferenciaDeDados;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java de modalidadesRelacionamentoPessoal.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * <p>
 * <pre>
 * &lt;simpleType name="modalidadesRelacionamentoPessoal">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="P"/>
 *     &lt;enumeration value="AP"/>
 *     &lt;enumeration value="SP"/>
 *     &lt;enumeration value="T"/>
 *     &lt;enumeration value="C"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "modalidadesRelacionamentoPessoal")
@XmlEnum
public enum ModalidadesRelacionamentoPessoal {

    P,
    AP,
    SP,
    T,
    C;

    public String value() {
        return name();
    }

    public static ModalidadesRelacionamentoPessoal fromValue(String v) {
        return valueOf(v);
    }

}
