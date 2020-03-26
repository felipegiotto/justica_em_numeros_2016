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
 * <p>Classe Java de modalidadeDocumentoIdentificador.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * <p>
 * <pre>
 * &lt;simpleType name="modalidadeDocumentoIdentificador">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="CI"/>
 *     &lt;enumeration value="CNH"/>
 *     &lt;enumeration value="TE"/>
 *     &lt;enumeration value="CN"/>
 *     &lt;enumeration value="CC"/>
 *     &lt;enumeration value="PAS"/>
 *     &lt;enumeration value="CT"/>
 *     &lt;enumeration value="RIC"/>
 *     &lt;enumeration value="CMF"/>
 *     &lt;enumeration value="PIS_PASEP"/>
 *     &lt;enumeration value="CEI"/>
 *     &lt;enumeration value="NIT"/>
 *     &lt;enumeration value="CP"/>
 *     &lt;enumeration value="IF"/>
 *     &lt;enumeration value="OAB"/>
 *     &lt;enumeration value="RJC"/>
 *     &lt;enumeration value="RGE"/>
 *     &lt;enumeration value="NB"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "modalidadeDocumentoIdentificador")
@XmlEnum
public enum ModalidadeDocumentoIdentificador {

    CI,
    CNH,
    TE,
    CN,
    CC,
    PAS,
    CT,
    RIC,
    CMF,
    PIS_PASEP,
    CEI,
    NIT,
    CP,
    IF,
    OAB,
    RJC,
    RGE,
    NB;

    public String value() {
        return name();
    }

    public static ModalidadeDocumentoIdentificador fromValue(String v) {
        return valueOf(v);
    }

}
