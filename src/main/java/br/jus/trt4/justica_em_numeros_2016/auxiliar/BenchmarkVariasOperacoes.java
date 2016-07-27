package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Classe utilizada para fazer o benchmark de uma série de operações, inclusive operações "encadeadas", onde ao executar
 * uma "sub-tarefa", a contagem da tarefa "pai" para de contar e, depois que a "sub-tarefa" termina, a tarefa "pai" 
 * volta a contar.
 * 
 * Ex:
 * <pre>
 * {@code
 * 
 * benchmark.inicioOperacao("Operação Pai");
 * operacaoPai();
 * benchmark.fimOperacao();
 * 
 * void operacaoPai() {
 *   benchmark.inicioOperacao("Operacao filha");
 *   operacaoFilha();
 *   benchmark.fimOperacao();
 * }
 * 
 * }
 * </pre>
 * @author fgiotto
 */
public class BenchmarkVariasOperacoes {

	private static final Logger LOGGER = LogManager.getLogger(BenchmarkVariasOperacoes.class);
	private LinkedHashMap<String, StopWatch> todasOperacoes = new LinkedHashMap<String, StopWatch>();
	private ArrayList<StopWatch> operacoesEmExecucao = new ArrayList<StopWatch>();
	private int tamanhoMaiorNomeOperacao = 0;
	private StopWatch tempoTotal;
	private static final String STRING_TEMPO_TOTAL = "TOTAL";
	private String logPrefix = null;
	private StringBuilder dadosExtras = null;
	
	public BenchmarkVariasOperacoes(boolean contarTempoTotal) {
		if (contarTempoTotal) {
			tempoTotal = new StopWatch();
			tempoTotal.start();
			tempoTotal.suspend();
		} else {
			tempoTotal = null;
		}
	}
	
	public void inicioOperacao(String nomeOperacao) {
		
		// LOGGER.info("BenchmarkVariasOperacoes inicioOperacao: " + nomeOperacao);
		
		// Se houver alguma operação em execução, interrompe a contagem dela (última da lista)
		if (!operacoesEmExecucao.isEmpty()) {
			StopWatch ultimo = operacoesEmExecucao.get(operacoesEmExecucao.size()-1);
			ultimo.suspend();
		}

		// Instancia a nova operação que será contabilizada
		StopWatch sw;
		if (todasOperacoes.containsKey(nomeOperacao)) {
			sw = todasOperacoes.get(nomeOperacao);
			sw.resume();
		} else {
			sw = new StopWatch();
			sw.start();
			todasOperacoes.put(nomeOperacao, sw);
		}
		operacoesEmExecucao.add(sw);
		
		// Começa a contar o "tempo total"
		if (tempoTotal != null && tempoTotal.isSuspended()) {
			tempoTotal.resume();
		}
		
		// Variável auxiliar para "alinhar" os tempos no método toString();
		if (nomeOperacao.length() > tamanhoMaiorNomeOperacao) {
			tamanhoMaiorNomeOperacao = nomeOperacao.length();
		}
		
		// Escreve no LOG o início da operação.
		if (logPrefix != null) {
			LOGGER.info(logPrefix + ": INICIADO: " + nomeOperacao);
		}
	}

	public void fimOperacao() {
		
		// LOGGER.info("BenchmarkVariasOperacoes fimOperacao");
		
		// Fecha a última operação em execução
		if (!operacoesEmExecucao.isEmpty()) {
			int ultimoIndex = operacoesEmExecucao.size()-1;
			StopWatch ultimo = operacoesEmExecucao.get(ultimoIndex);
			ultimo.suspend();
			operacoesEmExecucao.remove(ultimoIndex);
		}
		
		// Reinicia a "nova última", se houver.
		if (!operacoesEmExecucao.isEmpty()) {
			StopWatch ultimo = operacoesEmExecucao.get(operacoesEmExecucao.size()-1);
			ultimo.resume();
		} else {
			
			// Se não houver mais nada contando, para de contar o tempo total.
			if (tempoTotal != null && !tempoTotal.isSuspended()) {
				tempoTotal.suspend();
			}
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		// Mostra o status de todas as tarefas
		for (String nomeOperacao: todasOperacoes.keySet()) {
			
			// Nome da operação
			sb.append(nomeOperacao + ": ");
			
			// Alinha com a operação com nome mais comprido, para todas ficarem "bonitas" na exibição em um terminal.
			for (int i=0; i<tamanhoMaiorNomeOperacao - nomeOperacao.length(); i++) {
				sb.append(" ");
			}
			
			// Mostra o tempo dessa tarefa
			StopWatch sw = todasOperacoes.get(nomeOperacao);
			sb.append(sw);
			
			// Mostra o percentual dessa tarefa em relação ao tempo total.
			if (tempoTotal != null && tempoTotal.getTime() > 0) {
				sb.append(" (" + (sw.getTime() * 100 / tempoTotal.getTime()) + "%)");
			}
			
			// Exibe um "<--" se a tarefa estiver em execução agora.
			if (sw.isStarted() && !sw.isSuspended()) {
				sb.append(" <--");
			}
			
			sb.append("\n");
		}
		
		// Mostra o tempo total.
		if (tempoTotal != null) {
			sb.append(STRING_TEMPO_TOTAL + ": ");
			for (int i=0; i<tamanhoMaiorNomeOperacao - STRING_TEMPO_TOTAL.length(); i++) {
				sb.append(" ");
			}
			sb.append(tempoTotal + "\n");
		}
		
		// Mostra dados extras que possam ter sido gravados
		if (dadosExtras != null) {
			sb.append(dadosExtras.toString());
		}
		
		return sb.toString();
	}

	public String getLogPrefix() {
		return logPrefix;
	}

	public void setLogPrefix(String logPrefix) {
		this.logPrefix = logPrefix;
	}
	
	public StopWatch getTempoTotal() {
		return tempoTotal;
	}
	
	public synchronized void addDadoExtra(String mensagem) {
		if (dadosExtras == null) {
			dadosExtras = new StringBuilder();
		}
		dadosExtras.append(mensagem).append("\n");
	}
	
	public void close() {
		
		// Fecha as contagens em execução
		try {
			for (StopWatch operacao: operacoesEmExecucao) {
				operacao.stop();
			}
			operacoesEmExecucao.clear();
			operacoesEmExecucao = null;
		} catch (Throwable ex) {
			LOGGER.error("Erro fechando BenchmarkVariasOperacoes: " + ex.getLocalizedMessage(), ex);
		}

		// Fecha o tempo total (se estiver contando)
		try {
			if (tempoTotal != null && !tempoTotal.isSuspended()) {
				tempoTotal.suspend();
			}
			tempoTotal = null;
		} catch (Throwable ex) {
			LOGGER.error("Erro fechando BenchmarkVariasOperacoes: " + ex.getLocalizedMessage(), ex);
		}

		// Limpa todas as contagens
		try {
			todasOperacoes.clear();
			todasOperacoes = null;
		} catch (Throwable ex) {
			LOGGER.error("Erro fechando BenchmarkVariasOperacoes: " + ex.getLocalizedMessage(), ex);
		}
	}
}
