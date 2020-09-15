package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
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
	
	private static final int TAMANHO_PAGINA_CONSULTA_PROTOCOLO_CNJ= 40;
	
	//Codigo de status de processo com status "Processado com Erro".
	private static final String STATUS_CNJ_PROCESSADO_COM_ERRO = "6";
	//Codigo de status de processo com status "Erro no Arquivo"
	private static final String STATUS_CNJ_ERRO_ARQUIVO = "7";
	
	private static final String NOME_PARAMETRO_PROTOCOLO = "protocolo";
	private static final String NOME_PARAMETRO_DATA_INICIO = "dataInicio";
	private static final String NOME_PARAMETRO_DATA_FIM = "dataFim";
	private static final String NOME_PARAMETRO_STATUS = "status";
	private static final String NOME_PARAMETRO_PAGINA = "page";
	
	//Padrão de como a última página consultada no CNJ de um status é armazenada no arquivo.
	//Composto pelo número do status (que é preenchido dinamicamente), espaço e o número da página (dígito com 1 a 3 números).
	private static final String PADRAO_ER_ARQUIVO_ULTIMA_PAGINA_CONSULTADA = "^(%s )(\\d{1,3})$";
		
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
				
				if (tipo_validacao_protocolo_cnj == null
						|| Auxiliar.VALIDACAO_CNJ_TODOS_COM_ERRO.equals(tipo_validacao_protocolo_cnj)
						|| Auxiliar.VALIDACAO_CNJ_APENAS_COM_ERRO_PROCESSADO_COM_ERRO.equals(tipo_validacao_protocolo_cnj)
						|| Auxiliar.VALIDACAO_CNJ_APENAS_COM_ERRO_NO_ARQUIVO.equals(tipo_validacao_protocolo_cnj)) {
					operacao.consultarProtocolosComErroNoCNJ(tipo_validacao_protocolo_cnj);
				} else if (Auxiliar.VALIDACAO_CNJ_TODOS.equals(tipo_validacao_protocolo_cnj)) {
					throw new RuntimeException("Tipo de validação TODOS ainda não implementado corretamente.");
//					operacao.localizarProtocolosConsultarNoCNJ();
//					operacao.gravarTotalProtocolosRecusados();					
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
		
		//Apaga o arquivo com a última página consultada no CNJ. Como a operação foi finalizada com sucesso,
		//não tem sentido mantê-lo, pois ele terá consultado todas as páginas.
		File arquivo = Auxiliar.getArquivoUltimaPaginaConsultaCNJ();
		
		if (arquivo.exists()) {
			arquivo.delete();
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
		List<ArquivoComInstancia> arquivosProtocolos = ArquivoComInstancia.localizarArquivosInstanciasHabilitadas(Auxiliar.SUFIXO_PROTOCOLO, false);
		
		ArquivoComInstancia.mostrarTotalDeArquivosPorPasta(arquivosProtocolos, "Total de arquivos protocolos encontrados");
		int totalArquivos = arquivosProtocolos.size();
		
		LOGGER.info("Filtrando os protocolos que ainda não foram processados no CNJ.");
		
		// Filtra somente os arquivos XML que possuem protocolos e que ainda NÃO foram processados no CNJ
		//TODO Avaliar se não haveria outra forma mais eficiente de executar a lógica do método abaixo, como essa implementação está muito demorada
		List<ArquivoComInstancia> arquivosParaConsultar = filtrarSomenteArquivosPendentesDeConsulta(arquivosProtocolos);
		
		// Mostra os arquivos que serão consultados
		ArquivoComInstancia.mostrarTotalDeArquivosPorPasta(arquivosParaConsultar, "Protocolos que ainda precisam ser conferidos");
		
		// Atualiza o progresso na interface
		if (progresso != null) {
			progresso.setMax(totalArquivos);
			progresso.setProgress(totalArquivos - arquivosParaConsultar.size());
		}
		
		// Inicia o pesquisa
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
		
		builder.setParameter(NOME_PARAMETRO_PAGINA, parametroPagina)
				.setParameter(NOME_PARAMETRO_DATA_INICIO, parametroDataInicio)
				.setParameter(NOME_PARAMETRO_DATA_FIM, parametroDataFim)
				.setParameter(NOME_PARAMETRO_STATUS, parametroStatus);		
		
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
			
			builder.setParameter(NOME_PARAMETRO_PROTOCOLO, xml.getProtocolo());
			
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
	 * Consulta os protocolos com erro no CNJ de acordo com o tipo de validação preenchido no arquivo de configuração: 
	 * TODOS_COM_ERRO (status 6 e 7), APENAS_COM_ERRO_PROCESSADO_COM_ERRO (status 6) e APENAS_COM_ERRO_NO_ARQUIVO (status 7).
	 * Salva a lista de processos em arquivo e também a página atual da consulta para permitir o reinício a partir dessa página.
	 * 
	 * TODO: Reenviar automaticamente os protocolos com erro.
	 * 
	 */
	private void consultarProtocolosComErroNoCNJ(String tipo_validacao_protocolo_cnj) {
		
		String parametroProcolo = "";		
		String parametroDataInicio = "";
		String parametroDataFim = "";
		
		int totalProcessosRecusados = 0;
		
		if(tipo_validacao_protocolo_cnj != null) {
			LOGGER.info(String.format("Tipo de validação escolhido: %s.", tipo_validacao_protocolo_cnj));
		} else {
			//Validação padrão quando nenhum valor é preenchido
			LOGGER.info("Tipo de validação escolhido: VALIDACAO_CNJ_TODOS_COM_ERRO.");
		}
		
		LOGGER.info("Carregando os arquivos de protocolos.");
		
		// Lista com todos os protocolos
		List<ArquivoComInstancia> arquivosProtocolos = ArquivoComInstancia.localizarArquivosInstanciasHabilitadas(Auxiliar.SUFIXO_PROTOCOLO, false);
		
		ArquivoComInstancia.mostrarTotalDeArquivosPorPasta(arquivosProtocolos, "Total de arquivos protocolos encontrados");
		
		if(arquivosProtocolos.size() == 0) {
			LOGGER.error(String.format("Não foi encontrado nenhum arquivo .PROTOCOLO nas pastas %s e/ou %s.", 
					Auxiliar.getPastaXMLsIndividuais(1).getAbsolutePath(), Auxiliar.getPastaXMLsIndividuais(2).getAbsolutePath()));
			return;
		}
		
		LOGGER.info("Carregando a data de início e data de fim da remessa.");
		
		List<String> parametrosData = carregarParametrosData(arquivosProtocolos);
		
		parametroDataInicio = parametrosData.get(0);
		parametroDataFim = parametrosData.get(1);
		
		LOGGER.info(String.format("Data de início: %s. Data fim: %s.", parametroDataInicio, parametroDataFim));

		LOGGER.info("Carregando os protocolos dos arquivos.");
		
		//Carregando a tupla protocolo/processo num Map pois a consulta média é bem mais rápida do que em uma lista.
		Map<String, String> mapProtocolosComProcessos = carregarListaProcessosPorProtocolo(arquivosProtocolos);
		
		Set<String> setTodosProcessosRecusadosG1 = new HashSet<String>();
		Set<String> setTodosProcessosRecusadosG2 = new HashSet<String>();
		
		URIBuilder builder;
		
		if (tipo_validacao_protocolo_cnj == null
				|| Auxiliar.VALIDACAO_CNJ_TODOS_COM_ERRO.equals(tipo_validacao_protocolo_cnj)
				|| Auxiliar.VALIDACAO_CNJ_APENAS_COM_ERRO_PROCESSADO_COM_ERRO.equals(tipo_validacao_protocolo_cnj)) {
			//Constrói a URL com o status "Processado com erro"
			builder = construirBuilderWebServiceCNJ(parametroProcolo, parametroDataInicio, parametroDataFim, STATUS_CNJ_PROCESSADO_COM_ERRO);

			executarConsultasProtocolosComErroCNJ(builder, mapProtocolosComProcessos, setTodosProcessosRecusadosG1, setTodosProcessosRecusadosG2);
		}
		
		if (tipo_validacao_protocolo_cnj == null
				|| Auxiliar.VALIDACAO_CNJ_TODOS_COM_ERRO.equals(tipo_validacao_protocolo_cnj)
				|| Auxiliar.VALIDACAO_CNJ_APENAS_COM_ERRO_NO_ARQUIVO.equals(tipo_validacao_protocolo_cnj)) {		
			//Constrói a URL com o status "Erro no arquivo"
			builder = construirBuilderWebServiceCNJ(parametroProcolo, parametroDataInicio, parametroDataFim, STATUS_CNJ_ERRO_ARQUIVO);

			executarConsultasProtocolosComErroCNJ(builder, mapProtocolosComProcessos, setTodosProcessosRecusadosG1, setTodosProcessosRecusadosG2);		
		}
		
		if(setTodosProcessosRecusadosG1.size() > 0 || setTodosProcessosRecusadosG2.size() > 0) {
			if(Auxiliar.deveProcessarPrimeiroGrau()) {
				
				totalProcessosRecusados += setTodosProcessosRecusadosG1.size();
				
				if(Auxiliar.deveProcessarSegundoGrau()) {
					totalProcessosRecusados += setTodosProcessosRecusadosG2.size();
					LOGGER.warn("Um total de " + totalProcessosRecusados + " processos foram RECUSADOS no CNJ. A lista completa foi gravada "
							+ "nos arquivos " + Auxiliar.getArquivoListaProcessosErroProtocolo(1).getAbsoluteFile() + " e " + Auxiliar.getArquivoListaProcessosErroProtocolo(2).getAbsoluteFile() + ".");
				} else {
					LOGGER.warn("Um total de " + totalProcessosRecusados + " processos foram RECUSADOS no CNJ. A lista completa foi gravada "
							+ "no arquivo " + Auxiliar.getArquivoListaProcessosErroProtocolo(1).getAbsoluteFile() + ".");
				}
			} else if(Auxiliar.deveProcessarSegundoGrau()) {
				totalProcessosRecusados += setTodosProcessosRecusadosG2.size();
				LOGGER.warn("Um total de " + totalProcessosRecusados + " processos foram RECUSADOS no CNJ. A lista completa foi gravada "
						+ "no arquivo " + Auxiliar.getArquivoListaProcessosErroProtocolo(2).getAbsoluteFile() + ".");
			}
		} else {
			LOGGER.warn("Nenhum protocolo com o status \"Processado com Erro\" (6) ou \"Erro no Arquivo\" (7) foi encontrado.");
		}
	}
	
	/**
	 * Itera sobre todos os arquivos carregados para buscar a menor e maior data dos arquivos de protocolos. 
	 * 
	 * @param arquivosProtocolos Arquivos de protocolos
	 * @return Lista com duas String: a primeira com a menor data e a segunda com a maior data 
	 */
	private List<String> carregarParametrosData(List<ArquivoComInstancia> arquivosProtocolos) {

		List<String> datas = new ArrayList<>();
		
		Date dataInicio;
		Date dataFim;
		
		BasicFileAttributes atributos;
		
		try {
			atributos = Files.readAttributes(arquivosProtocolos.get(0).getArquivo().toPath(), BasicFileAttributes.class);
			dataInicio = new Date(atributos.lastModifiedTime().toMillis());
			dataFim = dataInicio;
		} catch (IOException e1) {
			LOGGER.warn("Não foi possível ler os atributos do arquivo " + arquivosProtocolos.get(0).getArquivo().toPath());			
			dataInicio = new GregorianCalendar(2100, Calendar.DECEMBER, 31).getTime();
			dataFim = new GregorianCalendar(1900, Calendar.JANUARY, 01).getTime();
		}
		
		
		for (ArquivoComInstancia arquivoComInstancia : arquivosProtocolos) {
			try {
				atributos = Files.readAttributes(arquivoComInstancia.getArquivo().toPath(), BasicFileAttributes.class);
				Date dataModificacaoArquivo = new Date(atributos.lastModifiedTime().toMillis());
				
				if (dataInicio.after(dataModificacaoArquivo)) {
					dataInicio = dataModificacaoArquivo;
				} else if (dataFim.before(dataModificacaoArquivo)) {
					dataFim = dataModificacaoArquivo;
				}
				
			} catch (IOException e) {
				LOGGER.warn("Não foi possível ler os atributos do arquivo " + arquivoComInstancia.getArquivo().toPath());
			}
		}
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		
		datas.add(formatter.format(dataInicio));
		datas.add(formatter.format(dataFim));
		
		return datas;
	}

	/**
	 * Realiza a consulta ao webservice do CNJ e gera um arquivo com a lista de processos recusados para cada instância.
	 * 
	 * @param builder URIBuilder com a URI a ser consultada
	 * @param mapProtocolosComProcessos Map com a tupla Protocolo/Processo para conseguir o número do processo a partir do protocolo retornado pelo serviço do CNJ.
	 * @param setTodosProcessosRecusadosG1 Lista com os processos recusados do 1° Grau
	 * @param setTodosProcessosRecusadosG2 Lista com os processos recusados do 2° Grau
	 */
	private void executarConsultasProtocolosComErroCNJ(URIBuilder builder,
			Map<String, String> mapProtocolosComProcessos, Set<String> setTodosProcessosRecusadosG1, 
			Set<String> setTodosProcessosRecusadosG2) {

		String agrupadorErrosConsultaProtocolosErroCNJ = "Consulta de protocolos com erro no CNJ";
		
		String statusErro = consultarValorParametroBuilder(builder, NOME_PARAMETRO_STATUS);
		
		int parametroPaginaAtual = buscarUltimaPaginaConsultadaPorStatus(statusErro) + 1;
		
		File arquivoProcessosRecusadosG1 = Auxiliar.getArquivoListaProcessosErroProtocolo(1);
		File arquivoProcessosRecusadosG2 = Auxiliar.getArquivoListaProcessosErroProtocolo(2);
		
		if(!Auxiliar.getArquivoUltimaPaginaConsultaCNJ().exists()) {
			//Consulta está começando pela primeira vez, então se existirem os arquivos com a lista de processos com erro, 
			//eles serão removidos, pois serão lixo. Não faz a comparação com o número da página, pois já pode ter acontecido
			//a busca por processos com outro status com erro, e nesse caso o arquivo com processos pode estar preenchido
			//com processos com outro status e esse arquivo não deve ser apagado nesse caso.
			
			if(Auxiliar.deveProcessarPrimeiroGrau() && arquivoProcessosRecusadosG1.exists()) {
				arquivoProcessosRecusadosG1.delete();
			} else if(Auxiliar.deveProcessarSegundoGrau() && arquivoProcessosRecusadosG2.exists()) {
				arquivoProcessosRecusadosG2.delete();
			}
			
		}
		
		if(parametroPaginaAtual > 0) {
			LOGGER.info("Reiniciando o processo da página " + parametroPaginaAtual + "...");
		}

		//TODO Avaliar se vale à pena verificar e alertar se ainda existe algum protocolo com o status em processamento
		
		//Como o serviço do CNJ não está implementado corretamente, decidiu-se por verificar se a consulta a uma página 
		//retornou algum resultado para para de consultar em vez de pegar o total de registros e dividir pelo tamanho da página.
		//Sempre será efetuada uma consulta a mais, porém esse método fica mais livre de alterações e falhas como a alteração do tamanho
		//da página ou a correção do serviço.
		boolean consultaCNJRetornouResultado = false;
		
		do {
			URI uri = atualizarParametroURIBuilder(builder, NOME_PARAMETRO_PAGINA, Integer.toString(parametroPaginaAtual));
			
			LOGGER.info(String.format("Consultando a página %s com status %s da lista de protocolos no CNJ. URL: %s.", 
					parametroPaginaAtual, statusErro, uri.toString()));

			String body = "";
			try {
				body = executarRequisicaoWebServiceCNJ(uri); 
			} catch (ParseException | IOException ex) {
				AcumuladorExceptions.instance().adicionarException(agrupadorErrosConsultaProtocolosErroCNJ, ex.getLocalizedMessage(), ex, true);
				
				return;
			} 

			LOGGER.info("* Analisando a resposta JSON...");
			
			List<List<String>> protocolosRespostaJson = carregaProtocolosRespostaJson(body);
			
			List<String> listaProtocolosRecusadosG1 = protocolosRespostaJson.get(0); 
			List<String> listaProtocolosRecusadosG2 = protocolosRespostaJson.get(1);
			
			//Verifica se a consulta ao CNJ retornou algum resultado.
			consultaCNJRetornouResultado = !listaProtocolosRecusadosG1.isEmpty() || !listaProtocolosRecusadosG2.isEmpty();
			
			List<Set<String>> listaProcessosRespostaJson = carregaListaProcessos(mapProtocolosComProcessos, listaProtocolosRecusadosG1, listaProtocolosRecusadosG2);
			
			Set<String> setProcessosRecusadosRespostaG1 = listaProcessosRespostaJson.get(0);
			Set<String> setProcessosRecusadosRespostaG2 = listaProcessosRespostaJson.get(1);
			
			//Remove os duplicados. É usado o iterator no lugar do foreach pois a exceção ConcurrentModificationException 
			//será lançada ao tentar iterar sobre a lista após apagar um de seus elementos.
			for (Iterator<String> i = setProcessosRecusadosRespostaG1.iterator(); i.hasNext();) {
				String numProcesso = i.next();
				if(setTodosProcessosRecusadosG1.contains(numProcesso)) {
					i.remove();
				}
			}
			
			for (Iterator<String> i = setProcessosRecusadosRespostaG2.iterator(); i.hasNext();) {
				String numProcesso = i.next();
				if(setTodosProcessosRecusadosG2.contains(numProcesso)) {
					i.remove();
				}
			}
		
			//Salva o número de processos nos arquivos			
			if(Auxiliar.deveProcessarPrimeiroGrau()) {
				try {
					gravarListaProcessosEmArquivo(setProcessosRecusadosRespostaG1, Auxiliar.getArquivoListaProcessosErroProtocolo(1), true);
				} catch (IOException e) {
					LOGGER.warn("Não foi possível escrever a lista de processos recusados no arquivo " + Auxiliar.getArquivoListaProcessosErroProtocolo(1).getPath());
				}				
			}

			if(Auxiliar.deveProcessarSegundoGrau()) {
				try {
					gravarListaProcessosEmArquivo(setProcessosRecusadosRespostaG2, Auxiliar.getArquivoListaProcessosErroProtocolo(2), true);
				} catch (IOException e) {
					LOGGER.warn("Não foi possível escrever a lista de processos recusados no arquivo " + Auxiliar.getArquivoListaProcessosErroProtocolo(2).getPath());
				}
			}
			
			//Adiciona os processos da resposta atual ao conjunto total de processos
			setTodosProcessosRecusadosG1.addAll(setProcessosRecusadosRespostaG1);
			setTodosProcessosRecusadosG2.addAll(setProcessosRecusadosRespostaG2);
			
			gravarUltimaPaginaConsultada(statusErro, Integer.toString(parametroPaginaAtual));
			
			parametroPaginaAtual++;

		}  while (consultaCNJRetornouResultado);
	}

	/**
	 * Retorna o valor de um parâmetro do URIBuilder
	 * 
	 * @param builder URIBuilder a ser consultado
	 * @param nomeParametro nome do parâmetro do URIBuilder
	 * @return Valor do parâmetro
	 */
	private String consultarValorParametroBuilder(URIBuilder builder, String nomeParametro) {
		
		List<NameValuePair> params = builder.getQueryParams();
		
		for (NameValuePair nameValuePair : params) {
			if(nameValuePair.getName().equals(nomeParametro)) {
				return nameValuePair.getValue();
			}
		}
		
		throw new RuntimeException ("Não foi possível encontrar o parâmetro " + nomeParametro + " no URIBuilder " + builder.getPath());
	}

	/**
	 * Carrega o número total de processos recusados, além da lista de protocolos retornados no objeto JSON.
	 * 
	 * @param body Corpo da resposta da requisição feita ao serviço do CNJ
	 * 
	 * @return Lista com 3 objetos: 1) número total de processos recusados; 2) Lista de protocolos recusados do 1° Grau; 
	 * 	3) Lista de protocolos recusados do 2° Grau;
	 */
	private List<List<String>> carregaProtocolosRespostaJson(String body) {
	
		List<List<String>> resposta = new ArrayList<List<String>>();
		
		List<String> listaProtocolosRecusadosG1 = new ArrayList<String>(); 
		List<String> listaProtocolosRecusadosG2 = new ArrayList<String>();
		
		JsonObject rootObject = JsonParser.parseString(body).getAsJsonObject();
		
		JsonArray processos = rootObject.get("resultado").getAsJsonArray();
		
		for (JsonElement processoElement : processos) {
			
			JsonObject processoObject = processoElement.getAsJsonObject();
			String numProtocolo = processoObject.get("numProtocolo").getAsString();
			String grau = processoObject.get("grau").getAsString();

			if (numProtocolo != null && grau != null) {
				if ("G1".equals(grau)) {
					listaProtocolosRecusadosG1.add(numProtocolo);
				} else if ("G2".equals(grau)) {
					listaProtocolosRecusadosG2.add(numProtocolo);
				}
			} else {
				LOGGER.warn(String.format("Erro ao carregar o protocolo %s do Grau %s.", numProtocolo, grau));
			}
		}
		
		resposta.add(listaProtocolosRecusadosG1);
		resposta.add(listaProtocolosRecusadosG2);
		
		return resposta;
	}
	
	/**
	 * Carrega a lista de processos a partir dos protocolos.
	 * 
	 * @param mapProtocolosComProcessos Tupla Protocolo/Processo
	 * @param listaProtocolosRecusadosG1 Lista de protocolos recusados do 1° grau
	 * @param listaProtocolosRecusadosG2 Lista de protocolos recusados do 2° grau
	 * 
	 * @return Lista com 2 posições: 1) Set com lista de processos recusados do 1° grau; 
	 * 	2) Set com lista de processos recusados do 2° grau. 
	 */
	private List<Set<String>> carregaListaProcessos(Map<String, String> mapProtocolosComProcessos,
			List<String> listaProtocolosRecusadosG1, List<String> listaProtocolosRecusadosG2) {

		List<Set<String>> resposta = new ArrayList<Set<String>>();
		
		Set<String> setProcessosRecusadosG1 = new HashSet<String>(); 
		Set<String> setProcessosRecusadosG2 = new HashSet<String>();
		
		for (String numProtocoloG1 : listaProtocolosRecusadosG1) {
			
			//Pesquisa o número do processo a partir dos protocolo lidos nos arquivos
			String numProcesso = mapProtocolosComProcessos.get(numProtocoloG1);

			if (numProcesso != null) {
				setProcessosRecusadosG1.add(numProcesso);
			} else {
				LOGGER.warn("Não foi encontrado nos arquivos de protocolo o processo referente ao protocolo " + numProtocoloG1);
			}
		}
		
		for (String numProtocoloG2 : listaProtocolosRecusadosG2) {
			
			//Pesquisa o número do processo a partir dos protocolo lidos nos arquivos
			String numProcesso = mapProtocolosComProcessos.get(numProtocoloG2);

			if (numProcesso != null) {
				setProcessosRecusadosG2.add(numProcesso);
			} else {
				LOGGER.warn("Não foi encontrado nos arquivos de protocolo o processo referente ao protocolo " + numProtocoloG2);
			}
		}
		
		resposta.add(setProcessosRecusadosG1);
		resposta.add(setProcessosRecusadosG2);
		
		return resposta;
	}

	/**
	 * Atualiza o valor de um parâmetro do URIBuilder retornando a URI alterada
	 * 
	 * @param builder URIBuilder
	 * @param nomeParametro Nome do parâmetro contido no URIBuilder
	 * @param valorParametro Valor do parâmetro a ser atualizado.
	 * 
	 * @return Nova URI com o parâmetro atualizado
	 */
	private URI atualizarParametroURIBuilder(URIBuilder builder, String nomeParametro, String valorParametro) {
		
		builder.setParameter(nomeParametro, valorParametro);			

		try {
			return builder.build();
		} catch (URISyntaxException e) {
			throw new RuntimeException("Erro na criação da URL para consultar protocolos.", e);
		}			
	}

	/**
	 * Constrói o URIBuilder a partir do parâmetro Parametro.url_webservice_cnj concatenada com a string "/protocolos"
	 * e os parâmetros da URL passados para esse método.
	 * 
	 * @param parametroProcolo Número do protocolo
	 * @param parametroDataInicio Data de início do envio
	 * @param parametroDataFim Data de Fim do Envio
	 * @param parametroStatus Status do protocolo: 1 (Aguardando processamento), 3 (Processado com sucesso), 4 (Enviado), 
	 * 5 (Duplicado), 6 (Processado com Erro), 7 (Erro no Arquivo)
	 *  
	 * @return URIBuilder composto por Parametro.url_webservice_cnj mais "/protocolos" e os parâmetros passados para o método. 
	 */
	private URIBuilder construirBuilderWebServiceCNJ(String parametroProcolo, String parametroDataInicio, String parametroDataFim, String parametroStatus) {
		
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

		URIBuilder builder = null;
		
		try {
			builder = new URIBuilder(url);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Erro na criação da URL para consultar protocolos.", e);
		}
				
		builder.setParameter(NOME_PARAMETRO_PROTOCOLO, parametroProcolo)
				.setParameter(NOME_PARAMETRO_DATA_INICIO, parametroDataInicio)
				.setParameter(NOME_PARAMETRO_DATA_FIM, parametroDataFim)
				.setParameter(NOME_PARAMETRO_STATUS, parametroStatus);
		
		return builder;
	}
	
	/**
	 * Realiza a requisição HTTP ao serviço do CNJ. Mesmo quando o serviço está ativo ele retorna o erro 504 algumas vezes, 
	 * dessa forma são feitas 10 tentativas com 5 minutos entre cada uma antes de lançar a exceção IOException. 
	 * 
	 * @param uri URI do serviço
	 * 
	 * @return O corpo (body) da resposta do serviço 
	 * 
	 * @throws IOException
	 */
	private String executarRequisicaoWebServiceCNJ(URI uri) throws IOException {
		
		HttpGet get = new HttpGet(uri);
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
		int statusCode = 0;

		// Como o erro 504 acontece com alguma frequência mesmo quando o
		// serviço está funcionando, faz a tentativa até 10 vezes
		int tentativasTimeOut = 0;
		
		String body = ""; 

		try {
			do {
				// Executa o GET
				tempo = System.currentTimeMillis();
				response = httpClient.execute(get);
				HttpEntity result = response.getEntity();
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
		} catch (ClientProtocolException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			EntityUtils.consumeQuietly(response.getEntity());
		}
		
		if (statusCode != 200 && statusCode != 201) {
			throw new IOException(
					"Falha ao conectar no Webservice do CNJ (codigo " + statusCode + ", esperado 200 ou 201)");
		} 
		
		return body;
	}

	/**
	 * Retorna a última página consultada de acordo com o status do processamento do protocolo. A página inicial é a 0.
	 * 
	 * @param status Status de processamento do protocolo no CNJ
	 * @return -1 ou a última página consultada
	 */
	private int buscarUltimaPaginaConsultadaPorStatus(String status) {
		
		File arquivoPaginaAtual = Auxiliar.getArquivoUltimaPaginaConsultaCNJ();
		
		int numUltimaPaginaConsultada = -1;
		
		if (arquivoPaginaAtual.exists() && arquivoPaginaAtual.length() != 0) {
			try {
				Pattern padraoER = Pattern.compile(String.format(PADRAO_ER_ARQUIVO_ULTIMA_PAGINA_CONSULTADA, status));
				
				List<String> conteudoArquivo = new ArrayList<>(Files.readAllLines(arquivoPaginaAtual.toPath(), StandardCharsets.UTF_8));

				for (int i = 0; i < conteudoArquivo.size(); i++) {
					Matcher matcher = padraoER.matcher(conteudoArquivo.get(i));
					
					if(matcher.find()) {
						return Integer.valueOf(matcher.group(2));
					}				    
				}
			} catch (IOException e) {
				LOGGER.warn("* Não foi possível ler o arquivo " + arquivoPaginaAtual.toString());
			} catch (NumberFormatException e) {
				LOGGER.warn("* Não foi possível converter para inteiro o conteúdo do arquivo " + arquivoPaginaAtual.toString());
			} 
		}
		
		return numUltimaPaginaConsultada;
	}
	
	private void gravarUltimaPaginaConsultada(String status, String pagina) {
		
		File arquivoPaginaAtual = Auxiliar.getArquivoUltimaPaginaConsultaCNJ();
		
		if (arquivoPaginaAtual.exists() && arquivoPaginaAtual.length() != 0) {
			try {
				Pattern padraoER = Pattern.compile(String.format(PADRAO_ER_ARQUIVO_ULTIMA_PAGINA_CONSULTADA, status));
				
				List<String> conteudoArquivo = new ArrayList<>(Files.readAllLines(arquivoPaginaAtual.toPath(), StandardCharsets.UTF_8));

				for (int i = 0; i < conteudoArquivo.size(); i++) {
					Matcher matcher = padraoER.matcher(conteudoArquivo.get(i));
					
					if(matcher.find()) {
						//Encontrou uma linha com o formato, então apenas atualiza.
						conteudoArquivo.set(i, String.format("%s %s", status, pagina));
						Files.write(arquivoPaginaAtual.toPath(), conteudoArquivo, StandardCharsets.UTF_8);
						return;
					}				    
				}
				//Não encontrou uma linha com o formato
				FileUtils.write(arquivoPaginaAtual, String.format("%s %s", status, pagina), StandardCharsets.UTF_8, true);			
				
			} catch (IOException e) {
				LOGGER.warn("* Não foi possível ler o arquivo " + arquivoPaginaAtual.toString());
			} catch (NumberFormatException e) {
				LOGGER.warn("* Não foi possível converter para inteiro o conteúdo do arquivo " + arquivoPaginaAtual.toString());
			} 
		} else {
			//Arquivo não existente
			try {
				FileUtils.write(arquivoPaginaAtual, String.format("%s %s", status, pagina), StandardCharsets.UTF_8);
			} catch (IOException e) {
				LOGGER.warn("Não foi possível escrever o número da página atual consultada no arquivo " + arquivoPaginaAtual.getPath());
			}
		}	
	}
	
	

	/**
	 * Itera sobre todos os arquivos com o número do protocolo para associá-los aos processos respectivos.
	 * 
	 * @param arquivosComProtocolo 
	 * @return HashMap com a dupla Protocolo/Número do processo de todos os processos com protocolo a serem pesquisados.
	 */
	private Map<String, String> carregarListaProcessosPorProtocolo(List<ArquivoComInstancia> arquivosComProtocolo) {

		Map<String, String> protocolosComProcessos = new HashMap<String, String>();

		for (ArquivoComInstancia arquivoComInstancia : arquivosComProtocolo) {
			
			protocolosComProcessos.put(arquivoComInstancia.getProtocolo(),
					arquivoComInstancia.getArquivo().getName().replace(".xml.protocolo", ""));
		}
		
		return protocolosComProcessos;
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
	
	/**
	 * Grava o conjunto de processos recusados no arquivo de saída.
	 * 
	 * @param setProcessosRecusados Conjunto de processos recusados
	 * @param arquivoSaida Arquivo a ser gravado
	 * @param acrescentarFinalArquivo Indica se os processos devem ser acrescentados ao final do arquivo, ou se um novo arquivo será gerado
	 * 
	 * @throws IOException
	 */
	public void gravarListaProcessosEmArquivo(Set<String> setProcessosRecusados, File arquivoSaida, boolean acrescentarFinalArquivo) throws IOException {
		
		arquivoSaida.getParentFile().mkdirs();			
		FileWriter fw = new FileWriter(arquivoSaida, acrescentarFinalArquivo);
		try {
			for (String processo: setProcessosRecusados) {
				fw.append(processo);
				fw.append("\r\n");
			}
		} finally {
			fw.close();
		}
	}
	
	/**
	 * Grava em um arquivo as seguintes informações separadas por ponto e vírgula: instância, número do processo, número do protocolo
	 * e data de geração do arquivo .PROTOCOLO.
	 * 
	 * @param arquivosComProtocolo Lista de arquivos com instância e protocolo
	 * @param arquivoSaida Arquivo que será gravado. Como sugestão usar o método new File(Auxiliar.getPastaOutputRaiz(), "/nomeDoArquivo.txt"), 
	 * 		  caso tenha realizado envios de vários tipos como COMPLETA, TESTES, PROCESSO, ETC. e queria que o arquivo seja gravado no mesmo local
	 * 		  independentemente do método de geração usado. 
	 * @param acrescentarFinalArquivo Indica se deverá sobrescrever o conteúdo do arquivo existente, ou então acrescentar o conteúdo
	 *		  ao seu final, mantendo o conteúdo do arquivo. 
	 * 
	 * @throws IOException
	 */
	private void GravarListaProcessoProtocoloInstanciaEmArquivo(List<ArquivoComInstancia> arquivosComProtocolo, File arquivoSaida, boolean acrescentarFinalArquivo) throws IOException {

		FileWriter fw = new FileWriter(arquivoSaida, acrescentarFinalArquivo);

		for (ArquivoComInstancia arquivoComInstancia : arquivosComProtocolo) {

			BasicFileAttributes atributos = Files.readAttributes(arquivoComInstancia.getArquivo().toPath(), BasicFileAttributes.class);
			Date dataInicio = new Date(atributos.lastModifiedTime().toMillis());
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");

			fw.append(String.format("%s;%s;%s;%s", arquivoComInstancia.getGrau(), 
					arquivoComInstancia.getArquivo().getName().replace(".xml.protocolo", ""),
					arquivoComInstancia.getProtocolo(),
					formatter.format(dataInicio)));
			fw.append("\r\n");
		}

		fw.close();			
	}
}
