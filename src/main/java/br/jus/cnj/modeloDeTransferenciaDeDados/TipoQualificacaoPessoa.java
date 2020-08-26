//
// Este arquivo foi gerado pela Arquitetura JavaTM para Implementação de Referência (JAXB) de Bind XML, v2.2.8-b130911.1802 
// Consulte <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Todas as modificações neste arquivo serão perdidas após a recompilação do esquema de origem. 
// Gerado em: 2020.07.09 às 02:03:35 PM BRT 
//


package br.jus.cnj.modeloDeTransferenciaDeDados;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java de tipoQualificacaoPessoa.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * <p>
 * <pre>
 * &lt;simpleType name="tipoQualificacaoPessoa">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="fisica"/>
 *     &lt;enumeration value="juridica"/>
 *     &lt;enumeration value="autoridade"/>
 *     &lt;enumeration value="orgaorepresentacao"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "tipoQualificacaoPessoa")
@XmlEnum
public enum TipoQualificacaoPessoa {

    @XmlEnumValue("fisica")
    FISICA("fisica"),
    @XmlEnumValue("juridica")
    JURIDICA("juridica"),
    @XmlEnumValue("autoridade")
    AUTORIDADE("autoridade"),
    @XmlEnumValue("orgaorepresentacao")
    ORGAOREPRESENTACAO("orgaorepresentacao"),
    //FIXME: valor adicionado para contornar tipos de pessoa inválidos nas bases do legado.
    @XmlEnumValue("invalido")
    INVALIDO("invalido");
    private final String value;

    TipoQualificacaoPessoa(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TipoQualificacaoPessoa fromValue(String v) {
        for (TipoQualificacaoPessoa c: TipoQualificacaoPessoa.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
