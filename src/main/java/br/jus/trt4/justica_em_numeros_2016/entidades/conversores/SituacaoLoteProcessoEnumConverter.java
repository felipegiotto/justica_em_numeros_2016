package br.jus.trt4.justica_em_numeros_2016.entidades.conversores;

import javax.persistence.AttributeConverter;

import br.jus.trt4.justica_em_numeros_2016.enums.SituacaoLoteProcessoEnum;

/**
 * Conversor para o enum SituacaoLoteProcessoEnum
 * 
 * @author ivan.franca@trt6.jus.br
 */
public class SituacaoLoteProcessoEnumConverter
        implements AttributeConverter<SituacaoLoteProcessoEnum, String> {
    
    @Override
    public String convertToDatabaseColumn(SituacaoLoteProcessoEnum situacaoLoteProcessoEnum) {
        return (situacaoLoteProcessoEnum == null) ? null : situacaoLoteProcessoEnum.getCodigo();
    }
    
    @Override
    public SituacaoLoteProcessoEnum convertToEntityAttribute(String string) {
        return SituacaoLoteProcessoEnum.criar(string);
    }
}
