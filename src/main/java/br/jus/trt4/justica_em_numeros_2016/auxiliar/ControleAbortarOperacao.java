package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Classe que identifica quando usuário pretende abortar a operação de forma "gracefully", aguardando resposta
 * das operações que estão pendentes.
 *
 * @author felipe.giotto@trt4.jus.br
 *
 */
public class ControleAbortarOperacao {

	private static final Logger LOGGER = LogManager.getLogger(ControleAbortarOperacao.class);
	private static final String NOME_ARQUIVO_ABORTAR = "ABORTAR.txt"; // Arquivo que pode ser gravado na pasta "output/[tipo_carga]", que faz com que o envio dos dados ao CNJ seja abortado
	
	private static ControleAbortarOperacao instance;
	
	/**
	 * Retorna objeto singleton, instanciando um, se necessário.
	 */
	public static ControleAbortarOperacao instance() {
		if (instance == null) {
			
			// Cuidado com acesso multi-thread
			synchronized (ControleAbortarOperacao.class) {
				if (instance == null) {
					instance = new ControleAbortarOperacao();
				}
			}
		}
		return instance;
	}
	
	private boolean abortar = false;
	private final File arquivoAbortar;
	
	private ControleAbortarOperacao() {
		arquivoAbortar = new File(Auxiliar.prepararPastaDeSaida(), NOME_ARQUIVO_ABORTAR);
		LOGGER.info("Para interromper a operação sem perda de dados, crie um arquivo vazio: " + arquivoAbortar.getAbsolutePath());
	}
	
	public boolean isDeveAbortar() {
		if (abortar) {
			return true;
		}
		
		if (arquivoAbortar.exists()) {
			arquivoAbortar.delete();
			abortar = true;
			LOGGER.info("Operação será abortada assim que operações forem concluídas");
		}
		return abortar;
	}
	
	public void setAbortar(boolean abortar) {
		this.abortar = abortar;
	}
	
	/**
	 * Espera um tempo, mas somente se a operação deve continuar (ou seja, se usuário não solicitou cancelamento)
	 *
	 * @param millis
	 */
	public void aguardarSomenteSeOperacaoContinua(int millis) {
		if (!isDeveAbortar()) {
			Auxiliar.safeSleep(millis);
		}
	}
}
