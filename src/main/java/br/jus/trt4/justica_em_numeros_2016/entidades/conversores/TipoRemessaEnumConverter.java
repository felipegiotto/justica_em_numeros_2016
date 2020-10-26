package br.jus.trt4.justica_em_numeros_2016.entidades.conversores;

import javax.persistence.AttributeConverter;

import br.jus.trt4.justica_em_numeros_2016.enums.TipoRemessaEnum;

/**
 * Conversor para o enum TipoRemessaEnum
 */
public class TipoRemessaEnumConverter
        implements AttributeConverter<TipoRemessaEnum, String> {
    
    @Override
    public String convertToDatabaseColumn(TipoRemessaEnum tipoRemessa) {
        return (tipoRemessa == null) ? null : tipoRemessa.getCodigo();
    }
    
    @Override
    public TipoRemessaEnum convertToEntityAttribute(String string) {
        return TipoRemessaEnum.criar(string);
    }
}
