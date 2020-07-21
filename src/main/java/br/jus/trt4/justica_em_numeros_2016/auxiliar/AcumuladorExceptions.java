package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Classe que acumula as exceções ocorridas no processamento dos dados, para reportar ao usuário em uma interface
 *
 * @author felipe.giotto@trt4.jus.br
 */
public class AcumuladorExceptions {

	private static final int QTD_ERROS_MOSTRAR_NO_LOG = 10;
	private static final Logger LOGGER = LogManager.getLogger(AcumuladorExceptions.class);
	private static AcumuladorExceptions instance;
	
	private Map<String, List<String>> listaOrigensAgrupadas = new ConcurrentHashMap<>();
	
	public static AcumuladorExceptions instance() {
		if (instance == null) {
			synchronized (AcumuladorExceptions.class) {
				if (instance == null) {
					instance = new AcumuladorExceptions();
				}
			}
		}
		return instance;
	}
	
	private AcumuladorExceptions() {
	}
	
	public void adicionarException(String origem, String agrupador) {
		adicionarException(origem, agrupador, null, false);
	}
	
	public synchronized void adicionarException(String origem, String agrupador, Throwable cause, boolean escreverLog) {
		if (escreverLog) {
			LOGGER.error(origem + ": " + agrupador, cause);
		}
		if (!listaOrigensAgrupadas.containsKey(agrupador)) {
			listaOrigensAgrupadas.put(agrupador, new ArrayList<>());
		}
		listaOrigensAgrupadas.get(agrupador).add(origem);
	}
	
	public synchronized void removerException(String origem) {
		ArrayList<String> grupos = new ArrayList<>(listaOrigensAgrupadas.keySet());
		for (String grupo : grupos) {
			List<String> listaOrigens = listaOrigensAgrupadas.get(grupo);
			listaOrigens.remove(origem);
			if (listaOrigens.isEmpty()) {
				listaOrigensAgrupadas.remove(grupo);
			}
		}
	}
	
	public synchronized void removerExceptionsDoAgrupador(String agrupador) {
		listaOrigensAgrupadas.remove(agrupador);
	}
	
	public void mostrarExceptionsAcumuladas() {
		for (String linha: recuperarLinhasExceptionsAcumuladas()) {
            LOGGER.warn(linha);
		}
	}
	
	public List<String> recuperarLinhasExceptionsAcumuladas() {
		List<String> linhas = new ArrayList<>();
		if (!listaOrigensAgrupadas.isEmpty()) {
			linhas.add("Ocorreram erros durante a execução dessa rotina! Verifique atentamente os arquivos de log! Estes são alguns erros ocorridos:");
	        for (String grupo: listaOrigensAgrupadas.keySet()) {
	        	List<String> origens = listaOrigensAgrupadas.get(grupo);
	        	linhas.add("* " + grupo + " (qtd=" + origens.size() + "): ");
				for (int i=0; i<origens.size() && i < QTD_ERROS_MOSTRAR_NO_LOG; i++) {
					linhas.add("    => " + origens.get(i));
	        	}
				if (origens.size() > QTD_ERROS_MOSTRAR_NO_LOG) {
					linhas.add("    => (outros: " + (origens.size() - QTD_ERROS_MOSTRAR_NO_LOG) + ")");
				}
	        }
		}
		return linhas;
	}

	public boolean isExisteExceptionRegistrada() {
		return !listaOrigensAgrupadas.isEmpty();
	}
}
