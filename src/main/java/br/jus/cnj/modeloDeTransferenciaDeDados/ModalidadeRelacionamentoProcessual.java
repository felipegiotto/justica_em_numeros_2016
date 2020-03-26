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
 * <p>Classe Java de modalidadeRelacionamentoProcessual.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * <p>
 * <pre>
 * &lt;simpleType name="modalidadeRelacionamentoProcessual">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="CP"/>
 *     &lt;enumeration value="RP"/>
 *     &lt;enumeration value="TF"/>
 *     &lt;enumeration value="AT"/>
 *     &lt;enumeration value="AS"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "modalidadeRelacionamentoProcessual")
@XmlEnum
public enum ModalidadeRelacionamentoProcessual {

    CP,
    RP,
    TF,
    AT,
    AS;

    public String value() {
        return name();
    }

    public static ModalidadeRelacionamentoProcessual fromValue(String v) {
        return valueOf(v);
    }

}
