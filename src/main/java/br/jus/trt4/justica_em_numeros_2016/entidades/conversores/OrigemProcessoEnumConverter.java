package br.jus.trt4.justica_em_numeros_2016.entidades.conversores;

import javax.persistence.AttributeConverter;

import br.jus.trt4.justica_em_numeros_2016.enums.OrigemProcessoEnum;

/**
 * Conversor para o enum OrigemProcessoEnum
 */
public class OrigemProcessoEnumConverter
        implements AttributeConverter<OrigemProcessoEnum, String> {
    
    @Override
    public String convertToDatabaseColumn(OrigemProcessoEnum origemProcesso) {
        return (origemProcesso == null) ? null : origemProcesso.getCodigo();
    }
    
    @Override
    public OrigemProcessoEnum convertToEntityAttribute(String string) {
        return OrigemProcessoEnum.criar(string);
    }
}
