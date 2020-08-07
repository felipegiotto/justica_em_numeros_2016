package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
 * TODO: Consulta de protocolos do CNJ não está funcionando, então é melhor mudar a regra da seguinte forma:
 * armazenar a faixa de dias que teve envio, armazenar o último protocolo recebido do CNJ.
 * Vai consultando quando que esse ÚLTIMO protocolo é analisado no CNJ. Nesse momento, presume-se que todos os demais também estão 
 * processados. Então, faz uma busca paginada, considerando a faixa de dias e buscando somente os ERROS (tem dois status de erro: 6 e 7, 
 * porém parece que ambos retornam os mesmos registros, é necessário confirmar). Os outros, presume-se que estão OK.
 * Implementar também o reenvio automático, se for erro interno no CNJ, dos processos dos protocolos com erro. Como sugestão criar
 * um arquivo do tipo .protocolo.erro que terá todos os protocolos com status de erro de um processo.
 *
 * @author felipe.giotto@trt4.jus.br
 */
public class Op_5_ConfereProtocolosCNJ {
	
	private static final Logger LOGGER = LogManager.getLogger(Op_5_ConfereProtocolosCNJ.class);
	private CloseableHttpClient httpClient;
	private static ProgressoInterfaceGrafica progresso;
	
	//Codigo de status de processo com status "Processado com Erro". O status 7 refere-se a "Erro no Arquivo",
	//porém os dois retornam os mesmos protocolos
	//TODO Confirmar essa informação do status 6 e 7 retornarem os mesmos protocolos
	private static final String STATUS_PROCESSADO_COM_ERRO_CNJ = "6";
	
	//A consulta ao serviço do CNJ é páginada, e para saber a quantidade total 
	private static final int TAMANHO_PAGINA_CONSULTA_PROTOCOLO_CNJ= 10;
	
	public static void main(String[] args) throws Exception {

		System.out.println("Se algum arquivo ainda não foi processado no CNJ, ou se ocorrer algum erro na resposta do CNJ, você quer que a operação seja reiniciada?");
		System.out.println("Responda 'S' para que as validações no CNJ rodem diversas vezes, até que o webservice não recuse nenhum arquivo e até que todos os XMLs sejam processados.");
		System.out.println("Responda 'N' para que o envio ao CNJ rode somente uma vez.");
		String resposta = Auxiliar.readStdin().toUpperCase();
		
		consultarProtocolosCNJ("S".equals(resposta));
	}

	public static void consultarProtocolosCNJ(boolean continuarEmCasoDeErro) throws Exception {
		
		Auxiliar.prepararPastaDeSaida();
		
		if (continuarEmCasoDeErro) {
			LOGGER.info("Se ocorrer algum erro no envio, a operação será reiniciada quantas vezes for necessário!");
		}
		
		progresso = new ProgressoInterfaceGrafica("(5/6) Conferência dos protocolos no CNJ");
		try {
			boolean executar = true;
			do {
				progresso.setProgress(0);
				
				Op_5_ConfereProtocolosCNJ operacao = new Op_5_ConfereProtocolosCNJ();
				
				String tipo_validacao_protocolo_cnj = Auxiliar.getParametroConfiguracao(Parametro.tipo_validacao_protocolo_cnj, false);
				
				if (tipo_validacao_protocolo_cnj == null || Auxiliar.VALIDACAO_CNJ_TODOS.equals(tipo_validacao_protocolo_cnj)) {
					operacao.localizarProtocolosConsultarNoCNJ();
					operacao.gravarTotalProtocolosRecusados();
				} else if (Auxiliar.VALIDACAO_CNJ_APENAS_COM_ERRO.equals(tipo_validacao_protocolo_cnj)) {
					operacao.consultarProtocolosComErroNoCNJ();
				} else {
					throw new RuntimeException("Valor desconhecido para o parâmetro 'tipo_validacao_protocolo_cnj': " + tipo_validacao_protocolo_cnj);
				}
				
				//TODO Fazer reenvio dos protocolos com erro 
				
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
	public Op_5_ConfereProtocolosCNJ() {
		httpClient = HttpUtil.criarNovoHTTPClientComAutenticacaoCNJ();
	}
	
	
	/**
	 * Carrega os arquivos XML das instâncias selecionadas (1G e/ou 2G) e envia ao CNJ.
	 * 
	 */
	public void localizarProtocolosConsultarNoCNJ() {
		
		// Lista com todos os arquivos pendentes
		Auxiliar.prepararPastaDeSaida();
		
		LOGGER.info("Carregando os arquivos de protocolos.");
		
		//TODO Avaliar a necessidade de ordenação dessa lista, uma vez que será necessário iterar por toda ela para filtrar os 
		//protocolos que foram processados com sucesso e não necessitam de reenvio
		List<ArquivoComInstancia> arquivosProtocolos = ArquivoComInstancia.localizarArquivosInstanciasHabilitadas(Auxiliar.SUFIXO_PROTOCOLO, true);
		
		ArquivoComInstancia.mostrarTotalDeArquivosPorPasta(arquivosProtocolos, "Total de arquivos protocolos encontrados");
		int totalArquivos = arquivosProtocolos.size();
		
		LOGGER.info("Filtrando os protocolos que ainda não foram processados no CNJ.");
		
		// Filtra somente os arquivos XML que possuem protocolos e que ainda NÃO foram processados no CNJ
		//TODO Avaliar se não haveria outra forma mais eficiente de executar a lógica do método abaixo, como essa implementação está muito demorada
		List<ArquivoComInstancia> arquivosParaConsultar = filtrarSomenteArquivosPendentesDeConsulta(arquivosProtocolos);
		
		// Mostra os arquivos que serão enviados
		ArquivoComInstancia.mostrarTotalDeArquivosPorPasta(arquivosParaConsultar, "Protocolos que ainda precisam ser conferidos");
		
		// Atualiza o progresso na interface
		if (progresso != null) {
			progresso.setMax(totalArquivos);
			progresso.setProgress(totalArquivos - arquivosParaConsultar.size());
		}
		
		// Inicia o envio
		consultarTodosProtocolosNoCNJ(arquivosParaConsultar);
		
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

	/**
	 * Esse método usa a URL /protocolos/all que não é mais recomendada pelo CNJ
	 * 
	 * @param arquivosParaConsultar Arquivos de protocolo que ainda não tiveram a confirmação do processamento correto no CNJ 
	 */
	@Deprecated
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
		String agrupadorErrosConsulta = "Consulta de todos os protocolos no CNJ";
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
			
			AcumuladorExceptions.instance().removerExceptionsDoAgrupador(agrupadorErrosConsulta);
		} catch (Exception ex) {
			AcumuladorExceptions.instance().adicionarException(ex.getLocalizedMessage(), agrupadorErrosConsulta, ex, true);
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
				if (progresso != null) {
					progresso.incrementProgress();
				}
			}
		}
		
		LOGGER.info("Quantidade de protocolos baixados nesta iteração: " + qtdArquivosBaixados);
		if (qtdArquivosAindaPendentes > 0) {
			LOGGER.info("Ainda há protocolos aguardando processamento no CNJ. Quantidade: " + qtdArquivosAindaPendentes);
		}
	}
	
	/**
	 * Consulta TODOS os protocolos enviados para o CNJ, verificando se foram processados com sucesso. Se não tiverem sido, 
	 * serão reenviados.
	 * 
	 * TODO: Analisar todos as pendências nesse método
	 * 
	 * @param arquivosParaConsultar Lista de arquivos que ainda não tiveram o processamento no CNJ validados
	 */
	private void consultarTodosProtocolosNoCNJ(List<ArquivoComInstancia> arquivosParaConsultar) {

		// Campo "status" da URL de consulta de protocolos:
		// 1 = Aguardando processamento
		// 3 = Processado com sucesso
		// 4 = Enviado
		// 5 = Duplicado
		// 6 = Processado com Erro
		// 7 = Erro no Arquivo
		
		// Monta a URL para consultar protocolos no CNJ.
		// Exemplo de URL:
		// https://www.cnj.jus.br/modelo-de-transferencia-de-dados/v1/processos/protocolos?protocolo=&dataInicio=2020-07-21&dataFim=2020-07-22&status=6&page=0
		final String url = Auxiliar.getParametroConfiguracao(Parametro.url_webservice_cnj, true) + "/protocolos";
		final String nomeParametroProcolo = "protocolo";
		final String nomeParametroDataInicio = "dataInicio";
		final String nomeParametroDataFim = "dataFim";
		final String nomeParametroStatus = "status";
		final String nomeParametroPagina = "page";

		String parametroPagina = "0";
		//Como a pesquisa será pelo número do protocolo, esses parâmetros não são necessários
		String parametroDataInicio = "";
		String parametroDataFim = "";
		String parametroStatus = "";
		
		URI uri = null;
		
		HttpGet get;

		URIBuilder builder = null;
		try {
			builder = new URIBuilder(url);
		} catch (URISyntaxException ex) {
			throw new RuntimeException("Erro na criação da URL para consultar protocolos.", ex);
		}
		
		builder.setParameter(nomeParametroPagina, parametroPagina)
				.setParameter(nomeParametroDataInicio, parametroDataInicio)
				.setParameter(nomeParametroDataFim, parametroDataFim)
				.setParameter(nomeParametroStatus, parametroStatus);		
		
		// Busca cada um dos protocolos pendentes na lista
		int qtdArquivos = arquivosParaConsultar.size();
		int qtdArquivosBaixados = 0;
		int qtdArquivosAindaPendentes = 0;
		
		//TODO Fazer a separação dos arquivos entre 1o e 2o grau. Se o processamento considerar ambos graus, não dará para saber
		//a qual instância o protocolo com erro se refere.
		File listaRecusados = new File(Auxiliar.prepararPastaDeSaida(), "lista_todos_protocolos_cnj.txt");
		
		for (int i = 0; i < qtdArquivos; i++) {
			
			ArquivoComInstancia xml = arquivosParaConsultar.get(i);
			
			String origemOperacao = xml.getArquivo().getAbsolutePath();
			
			builder.setParameter(nomeParametroProcolo, xml.getProtocolo());
			
			try {
				uri = builder.build();
			} catch (URISyntaxException ex) {
				throw new RuntimeException("Erro na construção da URL para consultar um protocolo: " + uri.toString(), ex);
			}

			// Consulta todos os protocolos no CNJ
			try {
				
				// Executa o GET
				get = new HttpGet(uri);
				HttpUtil.adicionarCabecalhoAutenticacao(get);
	
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
					
					JsonObject rootObject = JsonParser.parseString(body).getAsJsonObject();

					int totalRegistrosEncontrados = rootObject.get("totalRegistros").getAsInt();
					
					//Protocolo não foi encontrado, provável erro interno no CNJ. É necessário reenviar o processo
					if(totalRegistrosEncontrados == 0) {
						//TODO Criar um arquivo de erro para todos os processos recusados
						//Dica: .protocolo.erro contendo todos os protocolos com erro de um processo
						FileUtils.write(listaRecusados, xml.getArquivo().getName(), StandardCharsets.UTF_8, true);
						marcarArquivoComoProcessado(xml.getArquivo(), "", false);
						qtdArquivosBaixados++;
						qtdArquivosAindaPendentes++;
						LOGGER.warn("Protocolo baixado com ERRO: " + xml.getProtocolo() + ", arquivo '" + 
									xml.getArquivo() + "'");
						
					} else {
						//TODO Analisar corretamente o corpo da resposta
						int qtdProcessosSucesso = 0;//objetoProtocolo.get("qtdProcessosSucesso").getAsInt();
						int qtdProcessosErro = 0;//objetoProtocolo.get("qtdProcessosErro").getAsInt();
						
						//TODO Criar um arquivo de erro para todos os processos recusados e de sucesso para os demais.
						//Dica: .protocolo.erro contendo todos os protocolos com erro de um processo
						if (qtdProcessosSucesso > 0) {
							//TODO Gravar corretamente a análise do corpo da resposta
//							marcarArquivoComoProcessado(xml.getArquivo(), objetoProtocolo.toString(), true);
							qtdArquivosBaixados++;
							LOGGER.debug("Protocolo baixado com SUCESSO: " + xml.getProtocolo() + ", arquivo '" + xml.getArquivo() + "'");
							
						} else if (qtdProcessosErro > 0) {
							//TODO Gravar corretamente a análise do corpo da resposta
//							marcarArquivoComoProcessado(xml.getArquivo(), objetoProtocolo.toString(), false);
							qtdArquivosBaixados++;
							LOGGER.warn("Protocolo baixado com ERRO: " + xml.getProtocolo() + ", arquivo '" + xml.getArquivo() + "'");
							
						} else {
							qtdArquivosAindaPendentes++;
						}
					}
				} finally {
					EntityUtils.consumeQuietly(response.getEntity());
				}
				
				AcumuladorExceptions.instance().removerExceptionsDoAgrupador(origemOperacao);
			} catch (Exception ex) {
				AcumuladorExceptions.instance().adicionarException(ex.getLocalizedMessage(), origemOperacao, ex, true);
				return;
			}
		}
		
		//TODO Reenviar automaticamente os protocolos com erro
		
		LOGGER.info("Quantidade de protocolos baixados nesta iteração: " + qtdArquivosBaixados);
		if (qtdArquivosAindaPendentes > 0) {
			LOGGER.info("Ainda há protocolos aguardando processamento no CNJ. Quantidade: " + qtdArquivosAindaPendentes);
		}
	}
	
	
	/**
	 * Consulta todos os protocolos com erro (status 6) no CNJ. Salva num arquivo a página atual da consulta para permitir o 
	 * reinício a partir dela.
	 * 
	 * TODO: Analisar todos as pendências nesse método
	 * 
	 */
	private void consultarProtocolosComErroNoCNJ() {
		
		// Campo "status" da URL de consulta de protocolos:
		// 1 = Aguardando processamento
		// 3 = Processado com sucesso
		// 4 = Enviado
		// 5 = Duplicado
		// 6 = Processado com Erro
		// 7 = Erro no Arquivo

		// Monta a URL para consultar protocolos no CNJ.
		// Exemplo de URL:
		// https://www.cnj.jus.br/modelo-de-transferencia-de-dados/v1/processos/protocolos?protocolo=&dataInicio=2020-07-21&dataFim=2020-07-22&status=6&page=0
		final String url = Auxiliar.getParametroConfiguracao(Parametro.url_webservice_cnj, true) + "/protocolos";
		final String nomeParametroProcolo = "protocolo";
		final String nomeParametroDataInicio = "dataInicio";
		final String nomeParametroDataFim = "dataFim";
		final String nomeParametroStatus = "status";
		final String nomeParametroPaginaAtual = "page";
		final String parametroProcolo = "";
		int parametroPaginaAtual; //Começa com 0.
		
		//TODO Pegar esses parâmetros considerando a data de criação dos arquivos 
		String parametroDataInicio = "2020-07-30";
		String parametroDataFim = "2020-07-31";		
		
		File arquivoPaginaAtual = Auxiliar.getArquivoUltimaPaginaConsultaCNJ();
		
		parametroPaginaAtual = buscarUltimaPaginaConsultada(arquivoPaginaAtual);
		
		File listaProcessosRecusadosG1 = Auxiliar.getArquivoListaProcessosErroProtocolo(1);
		File listaProcessosRecusadosG2 = Auxiliar.getArquivoListaProcessosErroProtocolo(2);
		
		if(parametroPaginaAtual == 0) {
			//Consulta está começando da primeira página, então se existirem os arquivos com a lista de processos com erro, 
			//eles serão removidos, pois serão lixo.
			
			if(Auxiliar.deveProcessarPrimeiroGrau() && listaProcessosRecusadosG1.exists()) {
				listaProcessosRecusadosG1.delete();
			} else if(Auxiliar.deveProcessarSegundoGrau() && listaProcessosRecusadosG2.exists()) {
				listaProcessosRecusadosG2.delete();
			}
			
			//TODO Verificar se o último protocolo enviado já foi processado
		} else {
			LOGGER.info("Reiniciando o processo da página " + parametroPaginaAtual + "...");
		}

		int totalProcessosRecusados = 0;
		URI uri = null;
		HttpGet get;

		LOGGER.info("Carregando os arquivos de protocolos.");
		
		// Lista com todos os protocolos
		List<ArquivoComInstancia> arquivosProtocolos = ArquivoComInstancia.localizarArquivosInstanciasHabilitadas(Auxiliar.SUFIXO_PROTOCOLO, false);
		
		ArquivoComInstancia.mostrarTotalDeArquivosPorPasta(arquivosProtocolos, "Total de arquivos protocolos encontrados");

		LOGGER.info("Carregando os protocolos dos arquivos.");
		
		//Carregando a tupla protocolo/processo num Map pois a consulta média é bem mais rápida do que em uma lista.
		Map<String, String> protocolosComProcessos = carregarListaProcessosPorProtocolo(arquivosProtocolos);
		
		URIBuilder builder = null;
		try {
			builder = new URIBuilder(url);
		} catch (URISyntaxException ex) {
			throw new RuntimeException("Erro na criação da URL para consultar protocolos.", ex);
		}
		builder.setParameter(nomeParametroProcolo, parametroProcolo)
				.setParameter(nomeParametroDataInicio, parametroDataInicio)
				.setParameter(nomeParametroDataFim, parametroDataFim)
				.setParameter(nomeParametroStatus, STATUS_PROCESSADO_COM_ERRO_CNJ);

		String agrupadorErrosConsultaProtocolosErroCNJ = "Consulta de protocolos com erro no CNJ";
		String agrupadorErrosParseJson = "Parse da resposta JSON";

		do {
			builder.setParameter(nomeParametroPaginaAtual, Integer.toString(parametroPaginaAtual));			

			try {
				uri = builder.build();
			} catch (URISyntaxException ex) {
				throw new RuntimeException("Erro na construção da URL para consultar protocolos.", ex);
			}

			if (totalProcessosRecusados == 0) {
				LOGGER.info("Consultando a página " + parametroPaginaAtual + " da lista de protocolos no CNJ: "
						+ uri.toString());
			} else {
				LOGGER.info("Consultando a página " + parametroPaginaAtual + "/" + Math.ceil(totalProcessosRecusados/TAMANHO_PAGINA_CONSULTA_PROTOCOLO_CNJ) + " da lista de protocolos no CNJ: "
						+ uri.toString());
			}
			get = new HttpGet(uri);
			HttpUtil.adicionarCabecalhoAutenticacao(get);

			// TODO: Validar esse timeout considerando os muitos dados de produção
			// // Timeout
			// int CONNECTION_TIMEOUT_MS = 300_000; // Timeout in millis (5min)
			// RequestConfig requestConfig = RequestConfig.custom()
			// .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
			// .setConnectTimeout(CONNECTION_TIMEOUT_MS)
			// .setSocketTimeout(CONNECTION_TIMEOUT_MS)
			// .build();
			// post.setConfig(requestConfig);

			long tempo;
			HttpResponse response = null;
			HttpEntity result;
			String body = null;
			int statusCode = 0;

			// Como o erro 504 acontece com alguma frequência mesmo quando o
			// serviço está funcionando, faz a tentativa até 10 vezes
			int tentativasTimeOut = 0;			
			try {

				do {
					// Executa o GET
					tempo = System.currentTimeMillis();
					response = httpClient.execute(get);
					result = response.getEntity();
					body = EntityUtils.toString(result, Charset.forName("UTF-8"));
					statusCode = response.getStatusLine().getStatusCode();
					LOGGER.info("* Resposta em " + tempo + "ms (" + statusCode + ")");
					if (statusCode == 504 && tentativasTimeOut < 10) {
						LOGGER.info("* Aguardando 5 minutos...");
						try {
							Thread.sleep(5 * 60_000);
						} catch (InterruptedException e) {
							LOGGER.warn("Erro ao tentar pausar por 5 minutos...");
						}
						LOGGER.info("* Tentando novamente...");
					}
				} while (statusCode == 504 && tentativasTimeOut++ < 10); // Timeout

				if (statusCode != 200 && statusCode != 201) {
					throw new IOException(
							"Falha ao conectar no Webservice do CNJ (codigo " + statusCode + ", esperado 200 ou 201)");
				}
			} catch (ParseException | IOException ex) {
				AcumuladorExceptions.instance().adicionarException(agrupadorErrosConsultaProtocolosErroCNJ, ex.getLocalizedMessage(), ex, true);
			} finally {
				EntityUtils.consumeQuietly(response.getEntity());
			}

			LOGGER.info("* Buscando o número do processo nos protocolos retornados em JSON...");

			JsonObject rootObject = JsonParser.parseString(body).getAsJsonObject();

			totalProcessosRecusados = rootObject.get("totalRegistros").getAsInt();
			
			JsonArray processos = rootObject.get("resultado").getAsJsonArray();

			try {
				for (JsonElement processoElement : processos) {
					JsonObject processoObject = processoElement.getAsJsonObject();
					String numProtocolo = processoObject.get("numProtocolo").getAsString();
					String grau = processoObject.get("grau").getAsString();

					//Pesquisa o número do processo a partir dos protocolo lidos nos arquivos
					String numProcesso = protocolosComProcessos.get(numProtocolo);

					if (numProcesso != null && grau != null) {
						if ("G1".equals(grau)) {
							FileUtils.writeLines(listaProcessosRecusadosG1, StandardCharsets.UTF_8.toString(),
									Arrays.asList(numProcesso), true);
						} else if ("G2".equals(grau)) {
							FileUtils.writeLines(listaProcessosRecusadosG2, StandardCharsets.UTF_8.toString(),
									Arrays.asList(numProcesso), true);
						}
					} else {
						LOGGER.warn("Não foi encontrado nos arquivos de protocolo o processo referente ao protocolo " + numProtocolo);
					}
				}
			} catch (IOException ex) {
				String mensagem = "Erro ao consultar protocolo do arquivo: " + ex.getLocalizedMessage();
				AcumuladorExceptions.instance().adicionarException(agrupadorErrosParseJson, mensagem, ex, true);
			}
			
			
			//Atualiza a página atual
			try {
				FileUtils.write(arquivoPaginaAtual, Integer.toString(++parametroPaginaAtual), StandardCharsets.UTF_8);
			} catch (IOException e) {
				LOGGER.warn("Não foi possível escrever a página atual de consulta ao webservice do CNJ.");
			}

		} while (((double) totalProcessosRecusados / TAMANHO_PAGINA_CONSULTA_PROTOCOLO_CNJ) > parametroPaginaAtual);

		if(totalProcessosRecusados > 0) {
			if(Auxiliar.deveProcessarPrimeiroGrau()) {
				if(Auxiliar.deveProcessarSegundoGrau()) {
					LOGGER.warn("Um total de " + arquivosProtocolos.size() + " processos foram RECUSADOS no CNJ. A lista completa foi gravada "
							+ "nos arquivos " + listaProcessosRecusadosG1.getName() + " e " + listaProcessosRecusadosG2.getName() + ".");
				} else {
					LOGGER.warn("Um total de " + arquivosProtocolos.size() + " processos foram RECUSADOS no CNJ. A lista completa foi gravada "
							+ "no arquivo " + listaProcessosRecusadosG1.getName() + ".");
				}
			} else if(Auxiliar.deveProcessarSegundoGrau()) {
				LOGGER.warn("Um total de " + arquivosProtocolos.size() + " processos foram RECUSADOS no CNJ. A lista completa foi gravada "
						+ "no arquivo " + listaProcessosRecusadosG2.getName() + ".");
			}
			//TODO Criar um arquivo de erro para todos os processos recusados e de sucesso para os demais.
			//Dica: .protocolo.erro contendo todos os protocolos com erro de um processo
		}
		
		//TODO Reenviar automaticamente os protocolos com erro
	}

	private int buscarUltimaPaginaConsultada(File arquivoPaginaAtual) {
		
		int numUltimaPaginaConsultada = 0;
		
		if (arquivoPaginaAtual.exists() && arquivoPaginaAtual.length() != 0) {
			try {
				numUltimaPaginaConsultada = Integer.parseInt(FileUtils.readFileToString(arquivoPaginaAtual, "UTF-8"));
				return numUltimaPaginaConsultada;
			} catch (IOException e) {
				LOGGER.warn("* Não foi possível ler o arquivo " + arquivoPaginaAtual.toString());
			} catch (NumberFormatException e) {
				LOGGER.warn("* Não foi possível converter para inteiro o conteúdo do arquivo " + arquivoPaginaAtual.toString());
			} 
		}
		
		return numUltimaPaginaConsultada;
	}

	/**
	 * Itera sobre todos os arquivos com o número do protocolo para associá-los aos processos respectivos.
	 * 
	 * @param arquivosComProtocolo 
	 * @return HashMap com a dupla Protocolo/Número do processo de todos os processos com protocolo a serem pesquisados.
	 */
	private Map<String, String> carregarListaProcessosPorProtocolo(List<ArquivoComInstancia> arquivosComProtocolo) {

		Map<String, String> processosComProtocolos = new HashMap<String, String>();

		for (ArquivoComInstancia arquivoComInstancia : arquivosComProtocolo) {
			processosComProtocolos.put(arquivoComInstancia.getProtocolo(),
					arquivoComInstancia.getArquivo().getName().replace(".xml.protocolo", ""));
		}
		
		return processosComProtocolos;
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
	
	public void gravarTotalProtocolosRecusados() throws IOException {
		List<ArquivoComInstancia> arquivosProtocolos = ArquivoComInstancia.localizarArquivosInstanciasHabilitadas(Auxiliar.SUFIXO_PROTOCOLO_ERRO, true);
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
