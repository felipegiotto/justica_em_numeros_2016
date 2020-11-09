package br.jus.trt4.justica_em_numeros_2016.enums;

import java.util.Objects;

/**
 * Enum para mapear a origem de um processo.
 * 
 * @author ivan.franca@trt6.jus.br
 */
public enum OrigemProcessoEnum {
	HIBRIDO("H", "Híbrido"), 
	LEGADO("L", "Legado"),
	PJE("P", "PJe");
    
    private final String codigo;
    private final String label;
    
    private OrigemProcessoEnum(String codigo, String label) {
        this.codigo = codigo;
        this.label = label;
    }
    
    /**
     * Método responsável por retornar a representação do enum como caracter
     * 
     * @return o caracter
     */
    public String getCodigo() {
        return this.codigo;
    }
    
    /**
     * Método responsável por retornar o enum associado ao código.
     * 
     * @param codigo codigo do enum
     * @return Retorna a origem de um processo em {@link OrigemProcessoEnum}.
     */
    public static OrigemProcessoEnum criar(String codigo) {
        if (Objects.nonNull(codigo)) {
            for (OrigemProcessoEnum origemProcesso : OrigemProcessoEnum.values()) {
                if (origemProcesso.getCodigo().equals(codigo)) {
                    return origemProcesso;
                }
            }
        }
        return null;
    }
    
    /**
     * Método responsável por retornar o enum associado ao label.
     * 
     * @param label label do enum
     * @return Retorna a origem de um processo em {@link OrigemProcessoEnum}.
     */
    public static OrigemProcessoEnum criarApartirDoLabel(String label) {
        if (Objects.nonNull(label)) {
            for (OrigemProcessoEnum origemProcesso : OrigemProcessoEnum.values()) {
                if (origemProcesso.getLabel().equals(label)) {
                    return origemProcesso;
                }
            }
        }
        return null;
    }
    
    public String getLabel() {
        return this.label;
    }
    
    public boolean equals(OrigemProcessoEnum origemProcessoEnum) {
        return this.getCodigo().equals(origemProcessoEnum.getCodigo());
    }
    
    @Override
    public String toString() {
        return getLabel();
    }
}

