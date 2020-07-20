package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ControleAbortarOperacao;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DadosInvalidosException;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.HttpServerStatus;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ProcessoFluxo;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ProcessoSituacaoEnum;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ProgressoInterfaceGrafica;

/**
 * Novo protótipo de operação completa que trabalha como uma linha de produção, gerando os arquivos XML, enviando-os ao CNJ
 * e validando o processamentos dos protocolos, de forma paralela.
 *
 * TODO: Tratar exceções pois a DadosInvalidosException acumula as exceções ocorridas em uma operação, mostra nos logs e limpa esse cache.
 * TODO: Conferir erro no "Conferindo protocolos no CNJ"
 * TODO: Criar botão para interromper operação "gracefully"
 * TODO: Mostrar status de cada operação na interface HTML
 * TODO: Tratar travamentos da VPN (tentar forçar fechar conexão ao banco)
 * TODO: Marcar arquivos que não foram gerados na fase 2 por erro no validador, avisar dos problemas ocorridos e permitir gerá-los novamente.
 *
 * @author felipe.giotto@trt4.jus.br
 */
public class Op_Y_OperacaoFluxoContinuo implements AutoCloseable {

	private static final Logger LOGGER = LogManager.getLogger(Op_Y_OperacaoFluxoContinuo.class);
	
	private Collection<ProcessoFluxo> processosFluxos = new ConcurrentLinkedDeque<>();
	private HttpServerStatus serverStatus;
	private boolean executandoOperacao2Geracao;
	private boolean executandoOperacao4Envio;
	
	private void iniciar() throws IOException, SQLException, DadosInvalidosException {
		
		// Servidor web, para controlar o progresso da execução
		iniciarServidorWeb();
		
		// Baixa lista de processos do PJe
		if (Auxiliar.deveProcessarPrimeiroGrau()) {
			op1BaixarListaProcesssoSeNecessario(1);
		}
		if (Auxiliar.deveProcessarSegundoGrau()) {
			op1BaixarListaProcesssoSeNecessario(2);
		}
		
		iniciarAtualizacaoStatusBackground();
		iniciarOperacoesGeracaoEnvioValidacaoEmBackground();
		
		// TODO: Implementar condição de saída
	}

	/**
	 * Exibe uma interface de acompanhamento do progresso da operação completa
	 */
	private void iniciarServidorWeb() {
		try {
			serverStatus = new HttpServerStatus(this);
			
			Auxiliar.safeSleep(1000);
			
			serverStatus.abrirNavegador();
		} catch (IOException e) {
			LOGGER.warn(e.getLocalizedMessage(), e);
		}
	}


	@Override
	public void close() throws Exception {
		if (serverStatus != null) {
			serverStatus.close();
			serverStatus = null;
		}
	}

	/**
	 * Baixa, somente uma vez, a lista de processos que devem ser processados no PJe.
	 *
	 * @param baixaDados
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws DadosInvalidosException 
	 */
	private void op1BaixarListaProcesssoSeNecessario(int grau) throws IOException, SQLException, DadosInvalidosException {

		try (Op_1_BaixaListaDeNumerosDeProcessos baixaListaProcessos = new Op_1_BaixaListaDeNumerosDeProcessos(grau)) {
			if (!baixaListaProcessos.getArquivoSaida().exists()) {
				baixaListaProcessos.baixarListaProcessos();
			}
		}
		
		for (String numeroProcesso : Auxiliar.carregarListaProcessosDoArquivo(Auxiliar.getArquivoListaProcessos(grau))) {
			processosFluxos.add(new ProcessoFluxo(grau, numeroProcesso));
		}
	}
	
	/**
	 * Inicia uma thread, em background, que ficará monitorando as alterações nos status dos processos
	 */
	private void iniciarAtualizacaoStatusBackground() {
		new Thread(() -> {
			Auxiliar.prepararThreadLog();
			
			while (isAlgumProcessoComOrdemAnterior(ProcessoSituacaoEnum.CONCLUIDO) && !ControleAbortarOperacao.instance().isDeveAbortar()) {
				for (ProcessoFluxo processoFluxo : processosFluxos) {
					processoFluxo.identificarSituacao();
				}
				
				Auxiliar.safeSleep(5_000);
			}
			
		}).start();
	}
	
	/**
	 * Inicia uma thread, em background, que ficará gerando os XMLs enquanto houver processos pendentes
	 */
	private void iniciarOperacoesGeracaoEnvioValidacaoEmBackground() {
		
		// Gera XMLs enquanto houver processos pendentes de geração
		new Thread(() -> {
			Auxiliar.prepararThreadLog();
			
			while (isAlgumProcessoComOrdemAnterior(ProcessoSituacaoEnum.XML_GERADO) && !ControleAbortarOperacao.instance().isDeveAbortar()) {
				
				// TODO: Somente instanciar esses objetos se realmente existirem processos na fase correta (FILA)
				try {
					this.executandoOperacao2Geracao = true;
					Op_2_GeraXMLsIndividuais.main(null);
				} catch (Exception e) {
					ControleAbortarOperacao.instance().aguardarSomenteSeOperacaoContinua(10_000);
				} finally {
					this.executandoOperacao2Geracao = false;
				}
				
				ControleAbortarOperacao.instance().aguardarSomenteSeOperacaoContinua(60_000);
			}
			
		}).start();
		
		// Envia arquivos enquanto houver processos pendentes de envio
		new Thread(() -> {
			Auxiliar.prepararThreadLog();
			
			// TODO: Somente instanciar esses objetos se realmente existirem processos na fase correta (XML_GERADO)
			while (isAlgumProcessoComOrdemAnterior(ProcessoSituacaoEnum.ENVIADO) && !ControleAbortarOperacao.instance().isDeveAbortar()) {
				try {
					this.executandoOperacao4Envio = true;
					Op_4_ValidaEnviaArquivosCNJ operacao4Envia = new Op_4_ValidaEnviaArquivosCNJ();
					operacao4Envia.localizarEnviarXMLsAoCNJ();
				} catch (Exception e) {
					LOGGER.error("Erro enviando dados ao CNJ: " + e.getLocalizedMessage(), e);
				} finally {
					this.executandoOperacao4Envio = false;
				}
				
				ControleAbortarOperacao.instance().aguardarSomenteSeOperacaoContinua(60_000);
			}
			
		}).start();

		// Confere protocolos no CNJ enquanto houver protocolos pendentes de conferência
		// TODO: Retornar esse código quando o serviço estiver funcionando adequadamente (hoje só dá timeout)
//		new Thread(() -> {
//			Auxiliar.prepararThreadLog();
//			
//			// TODO: Somente instanciar esses objetos se realmente existirem processos na fase correta (ENVIADO)
//			while (isAlgumProcessoComOrdemAnterior(ProcessoSituacaoEnum.CONCLUIDO)) {
//				try {
//					Op_5_ConfereProtocolosCNJ.consultarProtocolosCNJ(false);
//				} catch (Exception e) {
//				}
//				
//				Auxiliar.safeSleep(60_000);
//			}
//			
//		}).start();
	}

	private boolean isAlgumProcessoComOrdemAnterior(ProcessoSituacaoEnum status) {
		int ordem = status.getOrdem();
		return processosFluxos.stream().anyMatch(p -> p.getSituacao().getOrdem() < ordem);
	}
	
	public Collection<ProcessoFluxo> getProcessosFluxos() {
		return processosFluxos;
	}
	
	public boolean isExecutandoOperacao2Geracao() {
		return executandoOperacao2Geracao;
	}
	
	public boolean isExecutandoOperacao4Envio() {
		return executandoOperacao4Envio;
	}
	
	public boolean isExecutandoAlgumaOperacao() {
		return isExecutandoOperacao2Geracao() || isExecutandoOperacao4Envio();
	}
	
	public static void main(String[] args) throws Exception {
		
		// Não utilizar a interface AWT nessa operação (que será acompanhada via web)
		ProgressoInterfaceGrafica.setTentarMontarInterface(false);
		Auxiliar.prepararPastaDeSaida();
		
		Op_Y_OperacaoFluxoContinuo operacaoCompleta = new Op_Y_OperacaoFluxoContinuo();
		operacaoCompleta.iniciar();
	}
}