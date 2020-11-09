package br.jus.trt4.justica_em_numeros_2016.enums;

import java.util.Objects;

/**
 * Enum para mapear a situacao de um LoteProcesso.
 * 
 * @author ivan.franca@trt6.jus.br
 */
public enum SituacaoLoteProcessoEnum {
	XML_GERADO_COM_SUCESSO("1", "XML Gerado com Sucesso"),
	XML_GERADO_COM_ERRO("2", "XML Gerado com Erro"), 
	ENVIADO("3", "Enviado"), 
	RECEBIDO_CNJ ("4", "Recebido no CNJ"),
	AGUARDANDO_PROCESSAMENTO_CNJ ("5", "Aguardando Processamento no CNJ"), 
	//Estados finais
	PROCESSADO_COM_SUCESSO_CNJ("6", "Processado com Sucesso no CNJ"), 
	DUPLICADO_CNJ ("7", "Duplicado no CNJ"),
	PROCESSADO_COM_ERRO_CNJ("8", "Processado com Erro no CNJ"), 
	ERRO_NO_ARQUIVO_CNJ("9", "Erro no Arquivo no CNJ");
    
    private final String codigo;
    private final String label;
    
    private SituacaoLoteProcessoEnum(String codigo, String label) {
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
     * @return Retorna a situação de um loteProcesso em {@link SituacaoLoteProcessoEnum}.
     */
    public static SituacaoLoteProcessoEnum criar(String codigo) {
        if (Objects.nonNull(codigo)) {
            for (SituacaoLoteProcessoEnum loteProcesso : SituacaoLoteProcessoEnum.values()) {
                if (loteProcesso.getCodigo().equals(codigo)) {
                    return loteProcesso;
                }
            }
        }
        return null;
    }
    
    /**
     * Método responsável por retornar o enum associado ao label.
     * 
     * @param label label do enum
     * @return Retorna a situação de um loteProcesso em {@link SituacaoLoteProcessoEnum}.
     */
    public static SituacaoLoteProcessoEnum criarApartirDoLabel(String label) {
        if (Objects.nonNull(label)) {
            for (SituacaoLoteProcessoEnum loteProcesso : SituacaoLoteProcessoEnum.values()) {
                if (loteProcesso.getLabel().equals(label)) {
                    return loteProcesso;
                }
            }
        }
        return null;
    }
    
    public String getLabel() {
        return this.label;
    }
    
    public boolean equals(SituacaoLoteProcessoEnum situacaoLoteProcessoEnum) {
        return this.getCodigo().equals(situacaoLoteProcessoEnum.getCodigo());
    }
    
    @Override
    public String toString() {
        return getLabel();
    }
}

