package br.jus.trt4.justica_em_numeros_2016.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.DataJudException;

public class ProcessoUtil {
	
	private static final int TAMANHO_NUMERO_PROCESSO_SEM_MASCARA = 20;
	/**
	 * Expressão regular para validação da numeração do processo com formatacao:
	 * (NNNNNNN-DD.AAAA.J.TR.OOOO)
	 */
	private static final String REGEX_VALIDACAO_NUMERO_PROCESSO_COM_MASCARA = "([0-9]{7})-([0-9]{2}).([0-9]{4}).([0-9]{1}).([0-9]{2}).([0-9]{4})";

	/**
	 * Expressão regular para validação da numeração do processo sem formatação:
	 * (NNNNNNNDDAAAAJTROOOO)
	 */
	private static final String REGEX_VALIDACAO_NUMERO_PROCESSO_SEM_MASCARA = "([0-9]{7})([0-9]{2})([0-9]{4})([0-9]{1})([0-9]{2})([0-9]{4})";

	
	public static String formatarNumeroProcesso(String numeroProcesso) throws DataJudException {
		if (StringUtils.isBlank(numeroProcesso)
				|| (numeroProcesso.trim().length() != ProcessoUtil.TAMANHO_NUMERO_PROCESSO_SEM_MASCARA)) {
			throw new DataJudException("Processo com formatação não esperada: " + numeroProcesso);
		}

		return ProcessoUtil.validarComposicaoNumeroProcesso(numeroProcesso).getNumeroProcessoFormatado();
	}

	/**
	 * Verifica formação do número do processo, inclusive o seu dígito verificador
	 * 
	 * @param numeroProcesso número do processo, formatado ou não
	 * @return Objeto representando o número do processo
	 * @throws DataJudException 
	 */
	protected static NumeroProcesso validarComposicaoNumeroProcesso(String numeroProcesso) throws DataJudException {
		String patternPadrao = ProcessoUtil.REGEX_VALIDACAO_NUMERO_PROCESSO_COM_MASCARA;
		if (numeroProcesso.length() == ProcessoUtil.TAMANHO_NUMERO_PROCESSO_SEM_MASCARA) {
			patternPadrao = ProcessoUtil.REGEX_VALIDACAO_NUMERO_PROCESSO_SEM_MASCARA;
		}

		Pattern padrao = Pattern.compile(patternPadrao);
		Matcher m = padrao.matcher(numeroProcesso);
		NumeroProcesso numero = null;
		if (m.matches()) {
			numero = new NumeroProcesso(m.group(Campo.SEQUENCIAL.getPosicao()),
					m.group(Campo.DIGITO_VERIFICADOR.getPosicao()), m.group(Campo.ANO.getPosicao()),
					m.group(Campo.ORGAO_JUSTICA.getPosicao()), m.group(Campo.REGIONAL.getPosicao()),
					m.group(Campo.ORIGEM.getPosicao()));
		} 

		return numero;
	}
	
	/**
	 * Associa significado aos campos referenciados pelos Regex
	 * 
	 */
	private enum Campo {

		SEQUENCIAL(1), DIGITO_VERIFICADOR(2), ANO(3), ORGAO_JUSTICA(4), REGIONAL(5), ORIGEM(6);

		private int posicao;

		private Campo(int posicao) {
			this.posicao = posicao;
		}

		public int getPosicao() {
			return this.posicao;
		}

	}

}
