package br.jus.trt4.justica_em_numeros_2016.enums;

import java.util.Objects;

/**
 * Enum para mapear a situacao de um LoteProcesso.
 * 
 * @author ivan.franca@trt6.jus.br
 */
public enum SituacaoLoteProcessoEnum {
	XML_GERADO_COM_SUCESSO("1", "XML Gerado com Sucesso", ""), XML_GERADO_COM_ERRO("2", "XML Gerado com Erro", ""),
	ENVIADO("3", "Enviado", ""), RECEBIDO_CNJ("4", "Recebido no CNJ", "4"),
	AGUARDANDO_PROCESSAMENTO_CNJ("5", "Aguardando Processamento no CNJ", "1"),
	// Estados finais
	PROCESSADO_COM_SUCESSO_CNJ("6", "Processado com Sucesso no CNJ", "3"), DUPLICADO_CNJ("7", "Duplicado no CNJ", "5"),
	PROCESSADO_COM_ERRO_CNJ("8", "Processado com Erro no CNJ", "6"),
	ERRO_NO_ARQUIVO_CNJ("9", "Erro no Arquivo no CNJ", "7");

	private final String codigo;
	private final String label;
	// Codigo retornado pelo serviço do CNJ
	private final String codigoCNJ;

	private SituacaoLoteProcessoEnum(String codigo, String label, String codigoCNJ) {
		this.codigo = codigo;
		this.label = label;
		this.codigoCNJ = codigoCNJ;
	}

	public String getCodigo() {
		return this.codigo;
	}

	public String getCodigoCNJ() {
		return codigoCNJ;
	}

	public String getLabel() {
		return this.label;
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

	/**
	 * Método responsável por retornar o enum associado ao código.
	 * 
	 * @param codigoCNJ codigoCNJ do enum
	 * @return Retorna a situação de um loteProcesso em {@link SituacaoLoteProcessoEnum}.
	 */
	public static SituacaoLoteProcessoEnum criarApartirCodigoCNJ(String codigoCNJ) {
		if (Objects.nonNull(codigoCNJ)) {
			for (SituacaoLoteProcessoEnum loteProcesso : SituacaoLoteProcessoEnum.values()) {
				if (loteProcesso.getCodigoCNJ().equals(codigoCNJ)) {
					return loteProcesso;
				}
			}
		}
		return null;
	}

	public boolean equals(SituacaoLoteProcessoEnum situacaoLoteProcessoEnum) {
		return this.getCodigo().equals(situacaoLoteProcessoEnum.getCodigo());
	}

	public boolean isSituacaoErro() {
		return this.codigoCNJ.equals(SituacaoLoteProcessoEnum.PROCESSADO_COM_ERRO_CNJ.getCodigoCNJ())
				|| this.codigoCNJ.equals(SituacaoLoteProcessoEnum.ERRO_NO_ARQUIVO_CNJ.getCodigoCNJ());
	}

	@Override
	public String toString() {
		return getLabel();
	}
}
