package br.jus.trt4.justica_em_numeros_2016.entidades.conversores;

import javax.persistence.AttributeConverter;

import br.jus.trt4.justica_em_numeros_2016.enums.SituacaoLoteEnum;

/**
 * Conversor para o enum SituacaoLoteEnum
 * 
 * @author ivan.franca@trt6.jus.br
 */
public class SituacaoLoteEnumConverter
        implements AttributeConverter<SituacaoLoteEnum, String> {
    
    @Override
    public String convertToDatabaseColumn(SituacaoLoteEnum situacaoLoteEnum) {
        return (situacaoLoteEnum == null) ? null : situacaoLoteEnum.getCodigo();
    }
    
    @Override
    public SituacaoLoteEnum convertToEntityAttribute(String string) {
        return SituacaoLoteEnum.criar(string);
    }
}
