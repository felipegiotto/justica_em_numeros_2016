package br.jus.trt4.justica_em_numeros_2016.enums;

import java.util.Objects;

/**
 * Enum para mapear a situação de XMLs para envio
 *
 * @author ivan.franca@trt6.jus.br
 */
public enum SituacaoXMLParaEnvioEnum {
	ENVIAR_TODOS_OS_XMLS("TODOS", "Todos os XMLs gerados serão enviados independente de terem sido reportados erros pelo validador local."),
	ENVIAR_APENAS_XMLS_GERADOS_COM_SUCESSO("APENAS_SUCESSO", "Apenas XMLs gerados sem erros reportados pelo validador local serão enviados.");
	private final String codigo;
	private final String label;

	private SituacaoXMLParaEnvioEnum(String codigo, String label) {
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
	 * @return Retorna a situação do XML para envio em {@link SituacaoXMLParaEnvioEnum}.
	 */
	public static SituacaoXMLParaEnvioEnum criar(String codigo) {
		if (Objects.nonNull(codigo)) {
			for (SituacaoXMLParaEnvioEnum situacaoXML : SituacaoXMLParaEnvioEnum.values()) {
				if (situacaoXML.getCodigo().equals(codigo)) {
					return situacaoXML;
				}
			}
		}
		return null;
	}

	/**
	 * Método responsável por retornar o enum associado ao label.
	 * 
	 * @param label label do enum
	 * @return Retorna a situação do XML para envio em {@link SituacaoXMLParaEnvioEnum}.
	 */
	public static SituacaoXMLParaEnvioEnum criarApartirDoLabel(String label) {
		if (Objects.nonNull(label)) {
			for (SituacaoXMLParaEnvioEnum situacaoXML : SituacaoXMLParaEnvioEnum.values()) {
				if (situacaoXML.getLabel().equals(label)) {
					return situacaoXML;
				}
			}
		}
		return null;
	}

	public boolean equals(SituacaoXMLParaEnvioEnum situacaoXMLEnum) {
		return this.getCodigo().equals(situacaoXMLEnum.getCodigo());
	}

	public String getLabel() {
		return this.label;
	}

	@Override
	public String toString() {
		return getLabel();
	}
}
