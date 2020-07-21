package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import br.jus.trt4.justica_em_numeros_2016.tasks.Op_Y_OperacaoFluxoContinuo;

/**
 * Trata as requisições HTTP recebidas pela interface de acompanhamento web
 * 
 * @see HttpServerStatus
 *
 * @author felipe.giotto@trt4.jus.br
 */
public class HttpServerRequestHandler implements Runnable {

	private Socket socket;
	private Op_Y_OperacaoFluxoContinuo operacao;
	
	public HttpServerRequestHandler(Socket socket, Op_Y_OperacaoFluxoContinuo operacao) {
		this.socket = socket;
		this.operacao = operacao;
	}
	
	@Override
	public void run() {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
			try (PrintWriter out = new PrintWriter(socket.getOutputStream())) {
					
				// Request recebido
				String input = in.readLine();
				if (input == null || !input.startsWith("GET ")) {
					return;
				}
				
				if (input.contains("parar")) {
					ControleAbortarOperacao.instance().setAbortar(true);
				}
	
				// Resposta
				out.println("HTTP/1.1 200 OK");
				out.println("Date: " + new Date());
				out.println("Content-type: text/html; charset=UTF-8");
				out.println();
				
				if (operacao != null) {
					
					String html;
					try (InputStream isHTML = HttpServerRequestHandler.class.getResourceAsStream("/operacao_fluxo_continuo_status.html")) {
						html = IOUtils.toString(isHTML, "UTF-8");
					}
					
					// Cabeçalho
					html = html.replaceAll("META_REFRESH", "5; url=/");
					html = html.replaceAll("NOME_TRIBUNAL", Auxiliar.getParametroConfiguracao(Parametro.sigla_tribunal, false));
					html = html.replaceAll("ANO_MES", Auxiliar.getParametroConfiguracao(Parametro.tipo_carga_xml, false));
					
					// Quantidades de processos por status
					DecimalFormat decimalFormat = new DecimalFormat("###.###");
					int total = operacao.getProcessosFluxos().size();
					Map<ProcessoSituacaoEnum, AtomicInteger> quantidades = new TreeMap<>();
					Map<ProcessoSituacaoEnum, Integer> percentuais = new TreeMap<>();
					for (ProcessoSituacaoEnum situacao : ProcessoSituacaoEnum.values()) {
						quantidades.put(situacao, new AtomicInteger());
						percentuais.put(situacao, 0);
					}
					for (ProcessoFluxo processoFluxo : operacao.getProcessosFluxos()) {
						quantidades.get(processoFluxo.getSituacao()).incrementAndGet();
					}
					
					int totalPercentuais = 0;
					// Calcula os percentuais
					for (ProcessoSituacaoEnum situacao : ProcessoSituacaoEnum.values()) {
						int percentual = (int) Math.ceil(quantidades.get(situacao).get() * 100.0 / total);
						percentuais.put(situacao, percentual);
						totalPercentuais += percentual;
					}
					// Ajuste fino no percentual, pois nunca fecha 100%
					// Pega um dos percentuais maiores e ajusta para fechar os 100%
					for (ProcessoSituacaoEnum situacao : ProcessoSituacaoEnum.values()) {
						if (percentuais.get(situacao) > 15) {
							percentuais.put(situacao, percentuais.get(situacao) + (100-totalPercentuais));
							break;
						}
					}
					
					// Substitui os tokens na interface
					// TODO: No início da geração (quando tudo está em 0%), o HTML fica todo bagunçado.
					for (ProcessoSituacaoEnum situacao : ProcessoSituacaoEnum.values()) {
						html = html.replaceAll("TOTAL_" + situacao, decimalFormat.format(quantidades.get(situacao)));
						html = html.replaceAll("PERCENTUAL_" + situacao, Integer.toString(percentuais.get(situacao)));
					}
					html = html.replaceAll("TOTAL_GERAL", decimalFormat.format(total));
					
					
					// Mensagem de operação interrompida
					if (ControleAbortarOperacao.instance().isDeveAbortar()) {
						if (operacao.isExecutandoAlgumaOperacao()) {
							html = html.replaceAll("MENSAGEM_INTERROMPER", "Interrompendo! Aguarde término das operações pendentes. CNJ pode demorar para responder ou abortar por timeout.");
						} else {
							html = html.replaceAll("MENSAGEM_INTERROMPER", "Operação finalizada! Encerre esse processo.");
						}
					} else {
						html = html.replaceAll("MENSAGEM_INTERROMPER", "");
					}
					
					// Status das operações
					html = html.replaceAll("STATUS_GERACAO", operacao.isExecutandoOperacao2Geracao() ? "Gerando arquivos XML..." : "");
					html = html.replaceAll("STATUS_ENVIO", operacao.isExecutandoOperacao4Envio() ? "Enviando dados ao CNJ..." : "");
					
					// Erros encontrados
					if (AcumuladorExceptions.instance().isExisteExceptionRegistrada()) {
						html = html.replace("ERROS_REPORTADOS", StringUtils.join(AcumuladorExceptions.instance().recuperarLinhasExceptionsAcumuladas(), "\n"));
					} else {
						html = html.replace("ERROS_REPORTADOS", "Nenhum problema identificado até o momento...");
					}
					
					out.print(html);
				} else {
					out.println("Operação não identificada");
				}

				out.flush();
			}
		} catch (IOException ex) {
			// Não precisa fazer nada
		} finally {
			this.operacao = null;
			this.socket = null;
		}
	}
}
