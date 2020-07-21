package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.AcumuladorExceptions;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ArquivoComInstancia;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DataJudException;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.HttpUtil;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Parametro;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ProgressoInterfaceGrafica;

/**
 * Chama os webservices do CNJ, validando cada um dos protocolos recebidos previamente do CNJ na fase 4: {@link Op_4_ValidaEnviaArquivosCNJ}.
 * 
 * TODO: Conferência pode ser assim: armazena a faixa de dias que teve envio, armazena o último protocolo recebido do CNJ.
 * Vai consultando quando que esse ÚLTIMO protocolo é analisado no CNJ. Nesse momento, presume-se que todos os demais também estão 
 * processados. Então, faz uma busca paginada, considerando a faixa de dias e buscando somente os ERROS (tem vários tipos de erro).
 * Os outros, presume-se que estão OK.
 *
 * @author felipe.giotto@trt4.jus.br
 */
public class Op_5_ConfereProtocolosCNJ {
	
	private static final Logger LOGGER = LogManager.getLogger(Op_5_ConfereProtocolosCNJ.class);
	private CloseableHttpClient httpClient;
	private static ProgressoInterfaceGrafica progresso;
	
	public static void main(String[] args) throws Exception {

		System.out.println("Se algum arquivo ainda não foi processado no CNJ, ou se ocorrer algum erro na resposta do CNJ, você quer que a operação seja reiniciada?");
		System.out.println("Responda 'S' para que as validações no CNJ rodem diversas vezes, até que o webservice não recuse nenhum arquivo e até que todos os XMLs sejam processados.");
		System.out.println("Responda 'N' para que o envio ao CNJ rode somente uma vez.");
		String resposta = Auxiliar.readStdin().toUpperCase();
		
		consultarProtocolosCNJ("S".equals(resposta));
	}

	public static void consultarProtocolosCNJ(boolean continuarEmCasoDeErro) throws Exception {
		
		if (continuarEmCasoDeErro) {
			LOGGER.info("Se ocorrer algum erro no envio, a operação será reiniciada quantas vezes for necessário!");
		}
		
		progresso = new ProgressoInterfaceGrafica("(5/6) Conferência dos protocolos no CNJ");
		try {
			boolean executar = true;
			do {
				progresso.setProgress(0);
				
				Op_5_ConfereProtocolosCNJ operacao = new Op_5_ConfereProtocolosCNJ();
				
				operacao.localizarProtocolosConsultarNoCNJ();
				
				operacao.gravarTotalProtocolosRecusados();
				
				AcumuladorExceptions.instance().mostrarExceptionsAcumuladas();
				
				// Verifica se deve executar novamente em caso de erros
				if (continuarEmCasoDeErro) {
					if (AcumuladorExceptions.instance().isExisteExceptionRegistrada()) {
						progresso.setInformacoes("Aguardando para reiniciar...");
						LOGGER.warn("A operação foi concluída com erros! O envio será reiniciado em 10min... Se desejar, aborte este script.");
						Thread.sleep(10 * 60_000);
						progresso.setInformacoes("");
					} else {
						executar = false;
					}
				} else {
					executar = false;
				}
			} while (executar);
		} finally {
			progresso.setInformacoes("");
			progresso.close();
			progresso = null;
		}
		
		LOGGER.info("Fim!");
	}

	/**
	 * Prepara o componente HttpClient para conectar aos serviços REST do CNJ
	 * 
	 * @throws Exception
	 */
	public Op_5_ConfereProtocolosCNJ() throws Exception {
		httpClient = HttpUtil.criarNovoHTTPClientComAutenticacaoCNJ();
	}
	
	
	/**
	 * Carrega os arquivos XML das instâncias selecionadas (1G e/ou 2G) e envia ao CNJ.
	 * 
	 * @throws JAXBException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private void localizarProtocolosConsultarNoCNJ() throws JAXBException, InterruptedException, IOException {
		
		// Lista com todos os arquivos pendentes
		Auxiliar.prepararPastaDeSaida();
		List<ArquivoComInstancia> arquivosProtocolos = ArquivoComInstancia.localizarArquivosInstanciasHabilitadas(Auxiliar.SUFIXO_PROTOCOLO);
		
		ArquivoComInstancia.mostrarTotalDeArquivosPorPasta(arquivosProtocolos, "Total de arquivos protocolos encontrados");
		int totalArquivos = arquivosProtocolos.size();
		
		// Filtra somente os arquivos XML que possuem protocolos e que ainda NÃO foram processados no CNJ
		List<ArquivoComInstancia> arquivosParaConsultar = filtrarSomenteArquivosPendentesDeConsulta(arquivosProtocolos);
		
		// Mostra os arquivos que serão enviados
		ArquivoComInstancia.mostrarTotalDeArquivosPorPasta(arquivosParaConsultar, "Protocolos que ainda precisam ser conferidos");
		
		// Atualiza o progresso na interface
		progresso.setMax(totalArquivos);
		progresso.setProgress(totalArquivos - arquivosParaConsultar.size());
		
		// Inicia o envio
		consultarProtocolosNoCNJ(arquivosParaConsultar);
		
		// Envio finalizado
		List<ArquivoComInstancia> arquivosXMLPendentes = filtrarSomenteArquivosPendentesDeConsulta(arquivosParaConsultar);
		ArquivoComInstancia.mostrarTotalDeArquivosPorPasta(arquivosXMLPendentes, "Arquivos XML ainda pendentes de envio");
	}

	public List<ArquivoComInstancia> filtrarSomenteArquivosPendentesDeConsulta(List<ArquivoComInstancia> arquivosProtocolos) {
		List<ArquivoComInstancia> arquivosPendentes = new ArrayList<>();
		for (ArquivoComInstancia arquivo: arquivosProtocolos) {
			if (deveConsultarArquivo(arquivo.getArquivo())) {
				arquivosPendentes.add(arquivo);
			}
		}
		return arquivosPendentes;
	}

	private void consultarProtocolosNoCNJ(List<ArquivoComInstancia> arquivosParaConsultar) {

		// Campo "status" da URL de consulta de protocolos:
		// 1 = Aguardando processamento
		// 3 = Processado com sucesso
		// 4 = Enviado
		// 5 = Duplicado
		// 6 = Processado com Erro
		// 7 = Erro no Arquivo
		
		// Monta a URL para consultar protocolos no CNJ.
		// Exemplo de URL: https://www.cnj.jus.br/modelo-de-transferencia-de-dados/v1/processos/protocolos/all
		final String url = Auxiliar.getParametroConfiguracao(Parametro.url_webservice_cnj, true) + "/protocolos/all";
		LOGGER.info("Consultando lista de protocolos no CNJ: " + url);
		
		HttpGet get = new HttpGet(url);
		HttpUtil.adicionarCabecalhoAutenticacao(get);

		// TODO: Validar esse timeout considerando os muitos dados de produção
//		// Timeout
//		int CONNECTION_TIMEOUT_MS = 300_000; // Timeout in millis (5min)
//		RequestConfig requestConfig = RequestConfig.custom()
//		    .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
//		    .setConnectTimeout(CONNECTION_TIMEOUT_MS)
//		    .setSocketTimeout(CONNECTION_TIMEOUT_MS)
//		    .build();
//		post.setConfig(requestConfig);
		
		
		// Consulta todos os protocolos no CNJ
		JsonArray protocolos;
		String origemOperacaoConsulta = "Consulta de todos os protocolos no CNJ";
		try {
			
			// Executa o GET
			long antes = System.currentTimeMillis();
			HttpResponse response = httpClient.execute(get);
			try {
				
				HttpEntity result = response.getEntity();
				String body = EntityUtils.toString(result, Charset.forName("UTF-8"));
				int statusCode = response.getStatusLine().getStatusCode();
				LOGGER.info("* Resposta em " + (System.currentTimeMillis() - antes) + "ms (" + statusCode + ")");
				if (statusCode != 200 && statusCode != 201) {
					throw new IOException("Falha ao conectar no Webservice do CNJ (codigo " + statusCode + ", esperado 200 ou 201)");
				}
				
				File listaRecusados = new File(Auxiliar.prepararPastaDeSaida(), "lista_todos_protocolos_cnj.txt");
				LOGGER.info("* Gravando arquivo com a situação de todos os protocolos do CNJ: " + listaRecusados);
				FileUtils.write(listaRecusados, body, StandardCharsets.UTF_8);
				
				LOGGER.info("* Analisando JSON de resposta...");
				protocolos = JsonParser.parseString(body).getAsJsonArray();
				
			} finally {
				EntityUtils.consumeQuietly(response.getEntity());
			}
			
			AcumuladorExceptions.instance().removerException(origemOperacaoConsulta);
		} catch (Exception ex) {
			AcumuladorExceptions.instance().adicionarException(origemOperacaoConsulta, ex.getLocalizedMessage(), ex, true);
			return;
		}
		
		
		// Agrupa os objetos recebidos por número de protocolo
		Map<String, JsonObject> protocolosAgrupados = new HashMap<>();
		protocolos.forEach(element -> {
			JsonObject jsonObject = element.getAsJsonObject();
			String protocolo = jsonObject.get("numProtocolo").getAsString();
			protocolosAgrupados.put(protocolo, jsonObject);
		});
		
		
		// Busca cada um dos protocolos pendentes na lista
		int qtdArquivos = arquivosParaConsultar.size();
		int qtdArquivosBaixados = 0;
		int qtdArquivosAindaPendentes = 0;
		for (int i = 0; i < qtdArquivos; i++) {
			ArquivoComInstancia xml = arquivosParaConsultar.get(i);
			String origemOperacao = xml.getArquivo().getAbsolutePath();
			try {
				
				String numeroProtocolo = FileUtils.readFileToString(xml.getArquivo(), StandardCharsets.UTF_8);
				JsonObject objetoProtocolo = protocolosAgrupados.get(numeroProtocolo);
				if (objetoProtocolo == null) {
					throw new DataJudException("CNJ não informou resultado do protocolo");
				}
				
				// Marca o arquivo como SUCESSO ou ERRO
				int qtdProcessosSucesso = objetoProtocolo.get("qtdProcessosSucesso").getAsInt();
				int qtdProcessosErro = objetoProtocolo.get("qtdProcessosErro").getAsInt();
				
				if (qtdProcessosSucesso > 0) {
					marcarArquivoComoProcessado(xml.getArquivo(), objetoProtocolo.toString(), true);
					qtdArquivosBaixados++;
					LOGGER.debug("Protocolo baixado com SUCESSO: " + numeroProtocolo + ", arquivo '" + xml.getArquivo() + "'");
					
				} else if (qtdProcessosErro > 0) {
					marcarArquivoComoProcessado(xml.getArquivo(), objetoProtocolo.toString(), false);
					qtdArquivosBaixados++;
					LOGGER.warn("Protocolo baixado com ERRO: " + numeroProtocolo + ", arquivo '" + xml.getArquivo() + "'");
					
				} else {
					qtdArquivosAindaPendentes++;
				}
				
				AcumuladorExceptions.instance().removerException(origemOperacao);
			} catch (Exception ex) {
				String mensagem = "Erro ao conferir protocolo do arquivo: " + ex.getLocalizedMessage();
				AcumuladorExceptions.instance().adicionarException(origemOperacao, mensagem, ex, true);
				
			} finally {
				progresso.incrementProgress();
			}
		}
		
		LOGGER.info("Quantidade de protocolos baixados nesta iteração: " + qtdArquivosBaixados);
		if (qtdArquivosAindaPendentes > 0) {
			LOGGER.info("Ainda há protocolos aguardando processamento no CNJ. Quantidade=: " + qtdArquivosAindaPendentes);
		}
	}
	
	/**
	 * Verifica se um determinado protocolo ainda está pendente de conferência no CNJ
	 *  
	 * @param arquivo
	 * @return
	 */
	private boolean deveConsultarArquivo(File arquivo) {
		return !Auxiliar.gerarNomeArquivoProcessoSucesso(arquivo).exists() && !Auxiliar.gerarNomeArquivoProcessoNegado(arquivo).exists();
	}

	private void marcarArquivoComoProcessado(File arquivo, String json, boolean sucesso) {
		File arquivoConfirmacao = sucesso ? Auxiliar.gerarNomeArquivoProcessoSucesso(arquivo) : Auxiliar.gerarNomeArquivoProcessoNegado(arquivo);
		try {
			FileUtils.write(arquivoConfirmacao, json, StandardCharsets.UTF_8);
		} catch (IOException ex) {
			LOGGER.warn("Não foi possível marcar arquivo como processado: " + arquivo, ex);
		}
	}
	
	private void gravarTotalProtocolosRecusados() throws IOException {
		List<ArquivoComInstancia> arquivosProtocolos = ArquivoComInstancia.localizarArquivosInstanciasHabilitadas(Auxiliar.SUFIXO_PROTOCOLO_ERRO);
		if (!arquivosProtocolos.isEmpty()) {
			File listaRecusados = new File(Auxiliar.prepararPastaDeSaida(), "lista_protocolos_recusados_cnj.txt");
			try (FileWriter fw = new FileWriter(listaRecusados)) {
				for (ArquivoComInstancia arquivo : arquivosProtocolos) {
					fw.append(arquivo.getArquivo().toString());
					fw.append("\n");
				}
			}
			LOGGER.warn("Um total de " + arquivosProtocolos.size() + " processos foram RECUSADOS no CNJ. A lista completa foi gravada neste arquivo: " + listaRecusados);
		}
	}
}
