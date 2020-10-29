package br.jus.trt4.justica_em_numeros_2016.enums;

/**
 * Enum para mapear a situacao de um protocolo no CNJ.
 * 
 * @author ivan.franca@trt6.jus.br
 */
public enum SituacaoProtocoloCNJ {

    AGUARDANDO_PROCESSAMENTO("Aguardando Processamento", 1),
    SUCESSO("Processado com Sucesso", 3),
    ENVIADO("Enviado", 4),
    DUPLICADO("Duplicado", 5),
    PROCESSADO_COM_ERRO("Processado com Erro", 6),
    ERRO_NO_ARQUIVO("Erro no Arquivo", 7);
	
    private String descricao;
    private Integer id;


    private SituacaoProtocoloCNJ(String descricao, Integer id) {
        this.descricao = descricao;
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }
    
    public static String getDescricaoPeloID(Integer id) {
        for (SituacaoProtocoloCNJ situacao : SituacaoProtocoloCNJ.values()) {
			if (situacao.getId().equals(id)) {
				return situacao.getDescricao(); 
			}
		}
        return null;
    }
    
    public static boolean hasStatusErro(Integer id) {
    	return id.equals(SituacaoProtocoloCNJ.PROCESSADO_COM_ERRO.getId())
    			|| id.equals(SituacaoProtocoloCNJ.ERRO_NO_ARQUIVO.getId());
    }

    
    

}