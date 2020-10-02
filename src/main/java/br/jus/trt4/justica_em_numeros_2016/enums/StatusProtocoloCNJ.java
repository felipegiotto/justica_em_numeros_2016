package br.jus.trt4.justica_em_numeros_2016.enums;

public enum StatusProtocoloCNJ {

    AGUARDANDO_PROCESSAMENTO("Aguardando Processamento", 1),
    SUCESSO("Processado com Sucesso", 3),
    ENVIADO("Enviado", 4),
    DUPLICADO("Duplicado", 5),
    PROCESSADO_COM_ERRO("Processado com Erro", 6),
    ERRO_NO_ARQUIVO("Erro no Arquivo", 7);
	
    private String descricao;
    private Integer id;


    private StatusProtocoloCNJ(String descricao, Integer id) {
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
        for (StatusProtocoloCNJ status : StatusProtocoloCNJ.values()) {
			if (status.getId().equals(id)) {
				return status.getDescricao(); 
			}
		}
        return null;
    }
    
    public static boolean hasStatusErro(Integer id) {
    	return id.equals(StatusProtocoloCNJ.PROCESSADO_COM_ERRO.getId())
    			|| id.equals(StatusProtocoloCNJ.ERRO_NO_ARQUIVO.getId());
    }

    
    

}