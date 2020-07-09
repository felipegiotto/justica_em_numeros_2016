//
// Este arquivo foi gerado pela Arquitetura JavaTM para Implementação de Referência (JAXB) de Bind XML, v2.2.8-b130911.1802 
// Consulte <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Todas as modificações neste arquivo serão perdidas após a recompilação do esquema de origem. 
// Gerado em: 2020.07.09 às 02:03:35 PM BRT 
//


package br.jus.cnj.modeloDeTransferenciaDeDados;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java de modalidadeVinculacaoProcesso.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * <p>
 * <pre>
 * &lt;simpleType name="modalidadeVinculacaoProcesso">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="CX"/>
 *     &lt;enumeration value="CT"/>
 *     &lt;enumeration value="DP"/>
 *     &lt;enumeration value="AR"/>
 *     &lt;enumeration value="CD"/>
 *     &lt;enumeration value="OR"/>
 *     &lt;enumeration value="RR"/>
 *     &lt;enumeration value="RG"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "modalidadeVinculacaoProcesso")
@XmlEnum
public enum ModalidadeVinculacaoProcesso {

    CX,
    CT,
    DP,
    AR,
    CD,
    OR,
    RR,
    RG;

    public String value() {
        return name();
    }

    public static ModalidadeVinculacaoProcesso fromValue(String v) {
        return valueOf(v);
    }

}
