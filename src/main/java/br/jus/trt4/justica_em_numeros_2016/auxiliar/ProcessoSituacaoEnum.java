package br.jus.trt4.justica_em_numeros_2016.auxiliar;

/**
 * Situação da geração e envio dos dados de um processo.
 *
 * @author felipe.giotto@trt4.jus.br
 *
 */
public enum ProcessoSituacaoEnum {

	INICIO(1), XML_GERADO(2), ENVIADO(4), CONCLUIDO(5), ERRO(6);
	
	private int ordem;
	
	private ProcessoSituacaoEnum(int ordem) {
		this.ordem = ordem;
	}
	
	public int getOrdem() {
		return ordem;
	}
}
