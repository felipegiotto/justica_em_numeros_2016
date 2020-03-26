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
 * <p>Classe Java de tipoPrazo.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * <p>
 * <pre>
 * &lt;simpleType name="tipoPrazo">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="HOR"/>
 *     &lt;enumeration value="DIA"/>
 *     &lt;enumeration value="MES"/>
 *     &lt;enumeration value="ANO"/>
 *     &lt;enumeration value="DATA_CERTA"/>
 *     &lt;enumeration value="SEMPRAZO"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "tipoPrazo")
@XmlEnum
public enum TipoPrazo {

    HOR,
    DIA,
    MES,
    ANO,
    DATA_CERTA,
    SEMPRAZO;

    public String value() {
        return name();
    }

    public static TipoPrazo fromValue(String v) {
        return valueOf(v);
    }

}
