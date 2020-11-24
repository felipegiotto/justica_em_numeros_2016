package br.jus.trt4.justica_em_numeros_2016.enums;

import java.util.Objects;

/**
 * Enum para mapear o tipo de sistema judicial que será analisado.
 *
 * @author ivan.franca@trt6.jus.br
 */
public enum TipoSistemaJudicialEnum {
	TODOS("TODOS",
			"Os XMLs serão gerados para os processos que forem recuperados do PJe e da base staging do Sistema Judicial Legado."),
	APENAS_PJE("APENAS_PJE", "Os XMLs serão gerados apenas para os processos que forem recuperados do PJe."),
	APENAS_PJE_COM_MIGRADOS_LEGADO("APENAS_PJE_COM_MIGRADOS_LEGADO",
			"Os XMLs serão gerados apenas para os processos que forem recuperados do PJe. Informações de"
					+ " Movimentos e Complementos de processos que tiverem sido migrados do Sistema Judicial Legado e"
					+ " estiverem no staging também serão recuperadas para um merge de informações."),
	APENAS_LEGADO("APENAS_LEGADO",
			"Os XMLs serão gerados apenas para os processos que forem recuperados da base staging "
					+ " do Sistema Judicial Legado e que não foram migrados para o PJe.");

	private final String codigo;
	private final String label;

	private TipoSistemaJudicialEnum(String codigo, String label) {
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
	 * @return Retorna o tipo de sistema judicial em {@link TipoSistemaJudicialEnum}.
	 */
	public static TipoSistemaJudicialEnum criar(String codigo) {
		if (Objects.nonNull(codigo)) {
			for (TipoSistemaJudicialEnum tipoSistemaJudicial : TipoSistemaJudicialEnum.values()) {
				if (tipoSistemaJudicial.getCodigo().equals(codigo)) {
					return tipoSistemaJudicial;
				}
			}
		}
		return null;
	}

	/**
	 * Método responsável por retornar o enum associado ao label.
	 * 
	 * @param label label do enum
	 * @return Retorna o tipo de sistema judicial em {@link TipoSistemaJudicialEnum}.
	 */
	public static TipoSistemaJudicialEnum criarApartirDoLabel(String label) {
		if (Objects.nonNull(label)) {
			for (TipoSistemaJudicialEnum tipoSistemaJudicial : TipoSistemaJudicialEnum.values()) {
				if (tipoSistemaJudicial.getLabel().equals(label)) {
					return tipoSistemaJudicial;
				}
			}
		}
		return null;
	}

	public boolean equals(TipoSistemaJudicialEnum tipoSistemaJudicialEnum) {
		return this.getCodigo().equals(tipoSistemaJudicialEnum.getCodigo());
	}

	public String getLabel() {
		return this.label;
	}

	@Override
	public String toString() {
		return getLabel();
	}
}
