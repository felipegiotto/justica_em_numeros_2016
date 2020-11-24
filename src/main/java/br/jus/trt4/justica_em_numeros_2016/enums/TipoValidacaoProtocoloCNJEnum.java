package br.jus.trt4.justica_em_numeros_2016.enums;

import java.util.Objects;

/**
 * Enum para mapear o tipo de validação de protocolos no CNJ.
 *
 * @author ivan.franca@trt6.jus.br
 */
public enum TipoValidacaoProtocoloCNJEnum {
	VALIDACAO_CNJ_TODOS("TODOS", "Valida todos os protocolos enviados ao CNJ."),
	VALIDACAO_CNJ_TODOS_COM_ERRO("TODOS_COM_ERRO", "Busca todos os protocolos com status de erro (6 e 7)."),
	VALIDACAO_CNJ_APENAS_COM_ERRO_PROCESSADO_COM_ERRO("APENAS_COM_ERRO_PROCESSADO_COM_ERRO", "Busca todos os protocolos com status de erro 6 (processado com erro)."),
	VALIDACAO_CNJ_APENAS_COM_ERRO_NO_ARQUIVO("APENAS_COM_ERRO_NO_ARQUIVO", "Busca todos os protocolos com status de erro 7 (Erro no arquivo).");

	private final String codigo;
	private final String label;

	private TipoValidacaoProtocoloCNJEnum(String codigo, String label) {
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
	 * @return Retorna o tipo de validação em {@link TipoValidacaoProtocoloCNJEnum}.
	 */
	public static TipoValidacaoProtocoloCNJEnum criar(String codigo) {
		if (Objects.nonNull(codigo)) {
			for (TipoValidacaoProtocoloCNJEnum tipoValidacao : TipoValidacaoProtocoloCNJEnum.values()) {
				if (tipoValidacao.getCodigo().equals(codigo)) {
					return tipoValidacao;
				}
			}
		}
		return null;
	}

	/**
	 * Método responsável por retornar o enum associado ao label.
	 * 
	 * @param label label do enum
	 * @return Retorna o tipo de validação do protocolo em {@link TipoValidacaoProtocoloCNJEnum}.
	 */
	public static TipoValidacaoProtocoloCNJEnum criarApartirDoLabel(String label) {
		if (Objects.nonNull(label)) {
			for (TipoValidacaoProtocoloCNJEnum tipoValidacao : TipoValidacaoProtocoloCNJEnum.values()) {
				if (tipoValidacao.getLabel().equals(label)) {
					return tipoValidacao;
				}
			}
		}
		return null;
	}

	public boolean equals(TipoValidacaoProtocoloCNJEnum tipoValidacaoEnum) {
		return this.getCodigo().equals(tipoValidacaoEnum.getCodigo());
	}

	public String getLabel() {
		return this.label;
	}

	@Override
	public String toString() {
		return getLabel();
	}
}
