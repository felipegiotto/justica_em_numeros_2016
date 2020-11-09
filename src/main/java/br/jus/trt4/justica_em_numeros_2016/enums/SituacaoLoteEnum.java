package br.jus.trt4.justica_em_numeros_2016.enums;

import java.util.Objects;

/**
 * Enum para mapear a situacao de um Lote.
 *
 * @author ivan.franca@trt6.jus.br
 */
public enum SituacaoLoteEnum {
	CRIADO_PARCIALMENTE("1", "Lote criado parcialmente. Nem todos os processos (LoteProcesso) foram criados."), 
	CRIADO_COM_ERROS("2", "Pelo menos um processo do lote teve o XML gerado com erro"),
	CRIADO_SEM_ERROS("3", "Todos os processos do lote tiveram o XML gerado sem erro."), 
	ENVIADO("4", "Todos os processos indicados para envio no lote foram enviados."),
	CONFERIDO_CNJ("5", "Todos os processos do lote (LoteProcesso) se encontram em um dos estados finais: "
			+ "PROCESSADO_COM_SUCESSO_CNJ, DUPLICADO_CNJ, PROCESSADO_COM_ERRO_CNJ, ERRO_NO_ARQUIVO_CNJ");
    
    private final String codigo;
    private final String label;
    
    private SituacaoLoteEnum(String codigo, String label) {
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
     * @return Retorna a situação de um lote em {@link SituacaoLoteEnum}.
     */
    public static SituacaoLoteEnum criar(String codigo) {
        if (Objects.nonNull(codigo)) {
            for (SituacaoLoteEnum situacaoLote : SituacaoLoteEnum.values()) {
                if (situacaoLote.getCodigo().equals(codigo)) {
                    return situacaoLote;
                }
            }
        }
        return null;
    }
    
    /**
     * Método responsável por retornar o enum associado ao label.
     * 
     * @param label label do enum
     * @return Retorna a situação de um lote em {@link SituacaoLoteEnum}.
     */
    public static SituacaoLoteEnum criarApartirDoLabel(String label) {
        if (Objects.nonNull(label)) {
            for (SituacaoLoteEnum situacaoLote : SituacaoLoteEnum.values()) {
                if (situacaoLote.getLabel().equals(label)) {
                    return situacaoLote;
                }
            }
        }
        return null;
    }
    
    public String getLabel() {
        return this.label;
    }
    
    @Override
    public String toString() {
        return getLabel();
    }
}

