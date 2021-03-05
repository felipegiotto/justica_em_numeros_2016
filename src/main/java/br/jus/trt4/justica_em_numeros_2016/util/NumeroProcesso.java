package br.jus.trt4.justica_em_numeros_2016.util;

import org.apache.commons.lang3.StringUtils;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.DataJudException;

public class NumeroProcesso {

	private static final int TAMANHO_SEQUENCIAL_NUM_PROCESSO = 7;
	private static final int TAMANHO_DIGITO_VERIFICADOR_NUM_PROCESSO = 2;
	private static final int TAMANHO_ANO_PROCESSO_NUM_PROCESSO = 4;
	private static final int TAMANHO_REGIONAL_NUM_PROCESSO = 2;
	private static final int TAMANHO_ORIGEM_NUM_PROCESSO = 4;

	private static final char SEPARADOR_PADRAO = '.';
	private static final char SEPARADOR_NUMERO_DV = '-';

	private String sequencialProcesso;
	private String dvProcesso;
	private String anoProcesso;
	private String orgaoJustica;
	private String regional;
	private String origemProcesso;

	private String numeroProcessoFormatado;

	/**
	 * Constrói uma instância a partir dos valores de cada campo, preenchendo-os com zeros à esquerda, se necessário.
	 * 
	 * @param sequencialProcesso
	 * @param dvProcesso
	 * @param anoProcesso
	 * @param orgaoJustica
	 * @param regional
	 * @param origemProcesso
	 * @throws DataJudException
	 */
	NumeroProcesso(String sequencialProcesso, String dvProcesso, String anoProcesso, String orgaoJustica,
			String regional, String origemProcesso) throws DataJudException {

		verificarPreenchimento(sequencialProcesso, dvProcesso, anoProcesso, orgaoJustica, regional, origemProcesso);

		inicializar(sequencialProcesso, dvProcesso, anoProcesso, orgaoJustica, regional, origemProcesso);

		formatarNumeroProcesso();
	}


	/**
	 * Verifica que os parâmetros não são nulos e, se forem String, que não estão vazios
	 * 
	 * @param parametros
	 * @throws DataJudException
	 */
	protected void verificarPreenchimento(Object... parametros) throws DataJudException {
		for (Object parametro : parametros) {
			boolean ok = (parametro != null)
					&& ((parametro instanceof String) ? !((String) parametro).isEmpty() : true);
			if (!ok) {
				throw new DataJudException("Parâmetros da composição do número do processo são inválidos");
			}
		}
	}

	private void inicializar(String sequencialProcesso, String dvProcesso, String anoProcesso, String orgaoJustica,
			String regional, String origemProcesso) {
		this.sequencialProcesso = completarComZeros(sequencialProcesso, NumeroProcesso.TAMANHO_SEQUENCIAL_NUM_PROCESSO);
		this.dvProcesso = completarComZeros(dvProcesso, NumeroProcesso.TAMANHO_DIGITO_VERIFICADOR_NUM_PROCESSO);
		this.anoProcesso = completarComZeros(anoProcesso, NumeroProcesso.TAMANHO_ANO_PROCESSO_NUM_PROCESSO);
		this.orgaoJustica = completarComZeros(orgaoJustica, 1);
		this.regional = completarComZeros(regional, NumeroProcesso.TAMANHO_REGIONAL_NUM_PROCESSO);
		this.origemProcesso = completarComZeros(origemProcesso, NumeroProcesso.TAMANHO_ORIGEM_NUM_PROCESSO);
	}

	public String getSequencialProcesso() {
		return this.sequencialProcesso;
	}

	public String getDvProcesso() {
		return this.dvProcesso;
	}

	public String getAnoProcesso() {
		return this.anoProcesso;
	}

	public String getOrgaoJustica() {
		return this.orgaoJustica;
	}

	public String getRegional() {
		return this.regional;
	}

	public String getOrigemProcesso() {
		return this.origemProcesso;
	}

	private void formatarNumeroProcesso() {
		StringBuilder numeroFormatado = new StringBuilder();

		numeroFormatado.append(this.sequencialProcesso).append(NumeroProcesso.SEPARADOR_NUMERO_DV);
		numeroFormatado.append(this.dvProcesso).append(NumeroProcesso.SEPARADOR_PADRAO);
		numeroFormatado.append(this.anoProcesso).append(NumeroProcesso.SEPARADOR_PADRAO);
		numeroFormatado.append(this.orgaoJustica).append(NumeroProcesso.SEPARADOR_PADRAO);
		numeroFormatado.append(this.regional).append(NumeroProcesso.SEPARADOR_PADRAO);
		numeroFormatado.append(this.origemProcesso);

		this.numeroProcessoFormatado = numeroFormatado.toString();
	}

	public String getNumeroProcessoFormatado() {
		return this.numeroProcessoFormatado;
	}

	public String getNumeroProcessoSemFormatacao() {
		return this.numeroProcessoFormatado.replace(Character.toString(NumeroProcesso.SEPARADOR_NUMERO_DV), "")
				.replace(Character.toString(NumeroProcesso.SEPARADOR_PADRAO), "");
	}

	@Override
	public String toString() {
		String padrao = "NumeroProcesso [sequencialProcesso=%s, dvProcesso=%s, anoProcesso=%s, orgaoJustica=%s, "
				+ "regional=%s, origemProcesso=%s]";
		return String.format(padrao, this.sequencialProcesso, this.dvProcesso, this.anoProcesso, this.orgaoJustica,
				this.regional, this.origemProcesso);
	}

	/**
	 * Completa campo com zeros à esquerda, se necessário
	 * 
	 * @param numero
	 * @param tamanho
	 * @return Uma nova string com o valor entrada no tamanho do campo, preenchida com zeros à esquerda se necessário.
	 */
	private String completarComZeros(String numero, int tamanho) {
		return StringUtils.leftPad(numero, tamanho, "0");
	}

}
