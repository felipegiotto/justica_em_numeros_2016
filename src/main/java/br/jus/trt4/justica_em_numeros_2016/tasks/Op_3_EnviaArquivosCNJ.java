package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.AcumuladorExceptions;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DataJudException;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.HttpUtil;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ProgressoInterfaceGrafica;
import br.jus.trt4.justica_em_numeros_2016.dao.JPAUtil;
import br.jus.trt4.justica_em_numeros_2016.dao.LoteDao;
import br.jus.trt4.justica_em_numeros_2016.dao.LoteProcessoDao;
import br.jus.trt4.justica_em_numeros_2016.entidades.ChaveProcessoCNJ;
import br.jus.trt4.justica_em_numeros_2016.entidades.Lote;
import br.jus.trt4.justica_em_numeros_2016.entidades.LoteProcesso;
import br.jus.trt4.justica_em_numeros_2016.enums.Parametro;
import br.jus.trt4.justica_em_numeros_2016.enums.SituacaoLoteEnum;
import br.jus.trt4.justica_em_numeros_2016.enums.SituacaoLoteProcessoEnum;
import br.jus.trt4.justica_em_numeros_2016.enums.SituacaoXMLParaEnvioEnum;
import br.jus.trt4.justica_em_numeros_2016.enums.TipoRemessaEnum;
import br.jus.trt4.justica_em_numeros_2016.util.DataJudUtil;

/**
 * Chama os webservices do CNJ, enviando os XMLs que foram gerados pela classe {@link Op_2_GeraEValidaXMLsIndividuais}.
 * 
 * @author felipe.giotto@trt4.jus.br
 */
public class Op_3_EnviaArquivosCNJ {

	private static final Logger LOGGER = LogManager.getLogger(Op_3_EnviaArquivosCNJ.class);
	private CloseableHttpClient httpClient;
	private final List<Long> temposEnvioCNJ = new ArrayList<>();
	private long ultimaExibicaoProgresso;
	private boolean considerarXMLsComErro;

	private final AtomicLong qtdEnviadaComSucesso = new AtomicLong(0);
	private static ProgressoInterfaceGrafica progresso;

	private static final LoteDao loteDAO = new LoteDao();
	private static final LoteProcessoDao loteProcessoDAO = new LoteProcessoDao();

	public static void main(String[] args) throws Exception {

		String resposta;

		if (args != null && args.length > 0) {
			resposta = args[0];
		} else {
			System.out.println("Se algum arquivo for negado no CNJ, você quer que a operação seja reiniciada?");
			System.out.println(
					"Responda 'S' para que o envio ao CNJ rode diversas vezes, até que o webservice não recuse nenhum arquivo.");
			System.out.println("Responda 'N' para que o envio ao CNJ rode somente uma vez.");
			resposta = Auxiliar.readStdin().toUpperCase();
		}

		validarEnviarArquivosCNJ("S".equals(resposta));
	}

	/**
	 * Prepara o componente HttpClient para conectar aos serviços REST do CNJ
	 * 
	 * @throws Exception
	 */
	public Op_3_EnviaArquivosCNJ() throws Exception {
		this.httpClient = HttpUtil.criarNovoHTTPClientComAutenticacaoCNJ();
		this.considerarXMLsComErro = false;
		String situacaoXMLParaEnvio = Auxiliar.getParametroConfiguracao(Parametro.situacao_xml_para_envio_operacao_3,
				false);
		if (situacaoXMLParaEnvio == null || SituacaoXMLParaEnvioEnum.ENVIAR_TODOS_OS_XMLS.getCodigo().equals(situacaoXMLParaEnvio)) {
			this.considerarXMLsComErro = true;
		} else if (SituacaoXMLParaEnvioEnum.ENVIAR_APENAS_XMLS_GERADOS_COM_SUCESSO.getCodigo().equals(situacaoXMLParaEnvio)) {
			this.considerarXMLsComErro = false;
		} else {
			throw new RuntimeException("Valor desconhecido para o parâmetro 'situacao_xml_para_envio_operacao_3': "
					+ situacaoXMLParaEnvio);
		}
	}

	public static void validarEnviarArquivosCNJ(boolean reiniciarEmCasoDeErro) throws Exception {

		Auxiliar.prepararPastaDeSaida();

		if (reiniciarEmCasoDeErro) {
			LOGGER.info("Se ocorrer algum erro no envio, a operação será reiniciada quantas vezes for necessário!");
		}

		progresso = new ProgressoInterfaceGrafica("(3/5) Envio dos arquivos ao CNJ");
		try {
			boolean executar = true;

			do {
				progresso.setProgress(0);

				Op_3_EnviaArquivosCNJ operacao = new Op_3_EnviaArquivosCNJ();

				operacao.localizarEnviarXMLsAoCNJ();

				AcumuladorExceptions.instance().mostrarExceptionsAcumuladas();

				// Verifica se deve executar novamente em caso de erros
				if (reiniciarEmCasoDeErro) {
					if (AcumuladorExceptions.instance().isExisteExceptionRegistrada()) {
						progresso.setInformacoes("Aguardando para reiniciar...");
						LOGGER.warn(
								"A operação foi concluída com erros! O envio será reiniciado em 2min... Se desejar, aborte este script.");
						Thread.sleep(2 * 60_000);
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
	 * Confere se a requisição HTTP teve reposta válida
	 *
	 * @param statusCode
	 * @throws DadosInvalidosException
	 */
	private void conferirRespostaSucesso(int statusCode, String body) throws IOException {

		// 20/07/2020: {"status":"ERRO","protocolo":"...","mensagem":"Arquivo duplicado"}
		if (statusCode == 409 && body.contains("Arquivo duplicado")) {
			LOGGER.info("Arquivo duplicado (já está no CNJ), será marcado como enviado");
			return;
		}

		// 200: SUCCESS
		// 201: CREATED
		if (statusCode != 200 && statusCode != 201 && statusCode != 409) {
			throw new IOException(
					"Falha ao conectar no Webservice do CNJ (codigo " + statusCode + ", esperado 200 ou 201). Resposta do servidor: " + body);
		}

		// Ex: statusCode=202, body={"status":"ERRO","protocolo":"TRT479782202007171595018643017","mensagem":"Não foi
		// possível fazer a recepção do arquivo. Tente novamente mais tarde"}
		if (body != null && body.contains("\"ERRO\"")) {
			throw new IOException("Falha ao conectar no Webservice do CNJ (body retornou 'ERRO'). Resposta do servidor: " + body);
		}
	}

	/**
	 * Carrega os arquivos XML das instâncias selecionadas (1G e/ou 2G) e envia ao CNJ.
	 * 
	 * @throws DadosInvalidosException
	 * @throws JAXBException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void localizarEnviarXMLsAoCNJ() throws JAXBException, InterruptedException, IOException {

		// Lista com todos os arquivos pendentes
		LOGGER.info("* Localizando os arquivos .xml.");
		Auxiliar.prepararPastaDeSaida();

		LocalDate dataCorteRemessaAtual = DataJudUtil.getDataCorte();
		TipoRemessaEnum tipoRemessaAtual = DataJudUtil.getTipoRemessa();

		Auxiliar.validarTipoRemessaAtual(tipoRemessaAtual);

		Lote loteAtual = loteDAO.getUltimoLoteDeUmaRemessa(dataCorteRemessaAtual, tipoRemessaAtual);

		if (loteAtual == null) {
			throw new RuntimeException(
					"Não foi possível localizar o último lote da Remessa indicada em config.properties. "
							+ "Certifique-se de que a operação Op_2_GeraEValidaXMLsIndividuais foi executada com sucesso.");
		}
		
		int [] graus = {1, 2};
		List<String> grausAProcessar = new ArrayList<String>();
		for (int grau : graus) {
			if (Auxiliar.deveProcessarGrau(grau)) {
				grausAProcessar.add(Integer.toString(grau));			
			}
		}

		Long tamanhoDoLoteAtual = loteProcessoDAO.getQuantidadeProcessosPorLote(loteAtual, grausAProcessar);

		LOGGER.info("Total de arquivos XML encontrados: " + tamanhoDoLoteAtual.intValue());

		List<Long> idProcessosComXMLParaEnvio = loteProcessoDAO.getIDProcessosPorLoteESituacao(loteAtual,
				this.getSituacoesProcessosEnvio(), grausAProcessar);

		LOGGER.info("Arquivos XML que precisam ser enviados: " + idProcessosComXMLParaEnvio.size());

		// Atualiza o progresso na interface
		if (progresso != null) {
			progresso.setMax(tamanhoDoLoteAtual.intValue());
			progresso.setProgress(tamanhoDoLoteAtual.intValue() - idProcessosComXMLParaEnvio.size());
		}

		if (idProcessosComXMLParaEnvio.size() > 0) {
			// Inicia o envio
			enviarXMLsAoCNJ(idProcessosComXMLParaEnvio);
		}

		Long qtdProcessosPendentes = loteProcessoDAO.getQuantidadeProcessosPorLoteESituacao(loteAtual,
				this.getSituacoesProcessosEnvio(), grausAProcessar, true);

		// Envio finalizado
		LOGGER.info("Total de arquivos enviados com sucesso: " + qtdEnviadaComSucesso.get());
		LOGGER.info("Arquivos XMLs ainda pendentes de envio: " + qtdProcessosPendentes.intValue());
		
		if (qtdProcessosPendentes.intValue() == 0 && loteAtual.getSituacao().in(SituacaoLoteEnum.CRIADO_PARCIALMENTE,
				SituacaoLoteEnum.CRIADO_COM_ERROS, SituacaoLoteEnum.CRIADO_SEM_ERROS)) {
			atualizarSituacaoLoteAtual(loteAtual);
		}
	}

	private static void atualizarSituacaoLoteAtual(Lote lote) {
		try {
			JPAUtil.iniciarTransacao();

			lote.setSituacao(SituacaoLoteEnum.ENVIADO);
			loteDAO.alterar(lote);

			JPAUtil.commit();
			LOGGER.info("Todos os XMLs do Lote Atual foram enviados com sucesso.");
		} catch (Exception e) {
			String origemOperacao = "Erro ao salvar situação do lote durante envio.";
			AcumuladorExceptions.instance().adicionarException(origemOperacao,
					"Erro ao salvar situação do lote durante envio: " + e.getLocalizedMessage(), e, true);
			JPAUtil.rollback();
		} finally {
			// JPAUtil.printEstatisticas();
			JPAUtil.close();
		}
	}

	private List<SituacaoLoteProcessoEnum> getSituacoesProcessosEnvio() {
		List<SituacaoLoteProcessoEnum> situacoes = new ArrayList<SituacaoLoteProcessoEnum>();
		situacoes.add(SituacaoLoteProcessoEnum.XML_GERADO_COM_SUCESSO);
		if (this.considerarXMLsComErro) {
			situacoes.add(SituacaoLoteProcessoEnum.XML_GERADO_COM_ERRO);
		}
		return situacoes;
	}

	private void enviarXMLsAoCNJ(List<Long> idProcessosComXMLParaEnviar) throws JAXBException, InterruptedException {
		// Agrupa os processos pendentes de geração em lotes para serem carregados do banco
		final int tamanhoLote = Math
				.max(Auxiliar.getParametroInteiroConfiguracaoComValorPadrao(Parametro.tamanho_lote_envio_operacao_3, 1), 1);
		final AtomicInteger counter = new AtomicInteger();

		final Collection<List<Long>> idsLoteLoteProcessos = idProcessosComXMLParaEnviar.stream()
				.collect(Collectors.groupingBy(it -> counter.getAndIncrement() / tamanhoLote)).values();

		// Para evitar a exceção "Unable to invoke factory method in class
		// org.apache.logging.log4j.core.appender.RollingFileAppender
		// for element RollingFile" ao tentar criar um appender RollingFile para uma thread de um arquivo inexistente
		int numeroThreads = Auxiliar.getParametroInteiroConfiguracaoComValorPadrao(Parametro.numero_threads_simultaneas_operacao_3,
				1) > idsLoteLoteProcessos.size() ? idsLoteLoteProcessos.size()
						: Auxiliar.getParametroInteiroConfiguracaoComValorPadrao(Parametro.numero_threads_simultaneas_operacao_3, 1);

		// Objeto que fará o envio dos arquivos em várias threads
		LOGGER.info("Iniciando o envio de " + idProcessosComXMLParaEnviar.size() + " XMLs, utilizando " + numeroThreads
				+ " thread(s)");
		AtomicInteger posicaoAtual = new AtomicInteger();
		for (List<Long> idsLoteProcessos : idsLoteLoteProcessos) {
			try {

				List<LoteProcesso> loteProcessosComXML = loteProcessoDAO.getProcessosComXMLPorIDs(idsLoteProcessos);

				JPAUtil.iniciarTransacao();

				ExecutorService threadPool = Executors.newFixedThreadPool(numeroThreads);
				for (LoteProcesso loteProcesso : loteProcessosComXML) {
					int i = posicaoAtual.incrementAndGet();
					threadPool.execute(() -> {
						Auxiliar.prepararThreadLog();
						ChaveProcessoCNJ processo = loteProcesso.getChaveProcessoCNJ();

						// Verifica se não há arquivos muito pequenos, que com certeza não contém um processo dentro
						// (como ocorreu em Jan/2020 no TRT4)
						if (loteProcesso.getXmlProcesso().getConteudoXML().length < 200) {
							LOGGER.warn("");
							LOGGER.warn("");

							LOGGER.warn("O arquivo do processo " + processo.getNumeroProcesso() + ", grau "
									+ loteProcesso.getChaveProcessoCNJ().getGrau()
									+ " são muito pequenos e, por isso, provavelmente estão incompletos.");
						}

						LOGGER.trace("Enviando XML do processo: " + processo.getNumeroProcesso() + ". Grau: "
								+ processo.getGrau() + " ...");

						// Monta a URL para enviar processos ao CNJ.
						// Exemplo de URL: https://wwwh.cnj.jus.br/selo-integracao-web/v1/processos/G2
						final String url = Auxiliar.getParametroConfiguracao(Parametro.url_webservice_cnj, true) + "/G"
								+ processo.getGrau();
						LOGGER.trace("* URL para onde o arquivo será enviado: " + url);

						HttpPost post = new HttpPost(url);
						HttpUtil.adicionarCabecalhoAutenticacao(post);

						// Timeout
						int CONNECTION_TIMEOUT_MS = 300_000; // Timeout in millis (5min)
						RequestConfig requestConfig = RequestConfig.custom()
								.setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
								.setConnectTimeout(CONNECTION_TIMEOUT_MS).setSocketTimeout(CONNECTION_TIMEOUT_MS)
								.build();
						post.setConfig(requestConfig);

						// Prepara um request com Multipart
						String nomeArquivo = processo.getGrau() + "_" + processo.getNumeroProcesso();
						HttpEntity entity = MultipartEntityBuilder.create().addBinaryBody("file",
								loteProcesso.getXmlProcesso().getConteudoXML(), ContentType.DEFAULT_BINARY, nomeArquivo)
								.build();

						post.setEntity(entity);

						String origem = "Envio do arquivo XML do processo: " + processo.getNumeroProcesso() + ". Grau: "
								+ processo.getGrau();
						try {
							// Executa o POST
							long tempo = System.currentTimeMillis();
							HttpResponse response = httpClient.execute(post);
							try {

								// Estatísticas de tempo dos últimos 1000 arquivos
								tempo = System.currentTimeMillis() - tempo;
								synchronized (temposEnvioCNJ) {
									temposEnvioCNJ.add(tempo);
									if (temposEnvioCNJ.size() > 1000) {
										temposEnvioCNJ.remove(0);
									}
								}

								HttpEntity result = response.getEntity();
								String body = EntityUtils.toString(result, Charset.forName("UTF-8"));

								int statusCode = response.getStatusLine().getStatusCode();
								LOGGER.trace("* Arquivo do processo: '" + processo.getNumeroProcesso() + "', tempo="
										+ tempo + "ms, statusCode=" + statusCode + ", body="
										+ resumirBodyRequisicao(body, result.getContentType()));
								this.conferirRespostaSucesso(statusCode, body);
								this.marcarLoteProcessoComoEnviado(loteProcesso, body);
								LOGGER.info("* Arquivo enviado com sucesso: " + processo.getNumeroProcesso());
								qtdEnviadaComSucesso.incrementAndGet();

								// Mostra previsão de conclusão
								if ((System.currentTimeMillis() - ultimaExibicaoProgresso) > 5_000) {
									ultimaExibicaoProgresso = System.currentTimeMillis();
									StringBuilder sbProgresso = new StringBuilder();
									sbProgresso.append("Envio dos arquivos pendentes: " + i + "/"
											+ idProcessosComXMLParaEnviar.size());
									double percentual = i * 10000 / idProcessosComXMLParaEnviar.size() / 100.0;
									sbProgresso.append(" (" + percentual + "%");
									synchronized (temposEnvioCNJ) {
										int arquivosMedicao = temposEnvioCNJ.size();
										if (arquivosMedicao > 0) {
											long totalTempo = 0;
											for (Long tempoEnvio : temposEnvioCNJ) {
												totalTempo += tempoEnvio;
											}
											long tempoMedio = totalTempo / arquivosMedicao;
											long tempoRestante = (idProcessosComXMLParaEnviar.size() - i) * tempoMedio;
											String tempoRestanteStr = "ETA " + DurationFormatUtils
													.formatDurationHMS(tempoRestante / numeroThreads);
											sbProgresso.append(
													" - " + tempoRestanteStr + " em " + numeroThreads + " thread(s)");
											sbProgresso.append(" - media de "
													+ DurationFormatUtils.formatDurationHMS(tempoMedio) + "/arquivo");
											if (progresso != null) {
												progresso.setInformacoes(tempoRestanteStr);
											}
										}
										sbProgresso.append(")");
									}
									LOGGER.debug(sbProgresso);
								}

							} finally {
								EntityUtils.consumeQuietly(response.getEntity());
							}

							AcumuladorExceptions.instance().removerException(origem);
						} catch (Exception ex) {
							AcumuladorExceptions.instance().adicionarException(origem, ex.getLocalizedMessage(), ex,
									true);

						} finally {
							if (progresso != null) {
								progresso.incrementProgress();
							}
						}
					});
				}

				threadPool.shutdown();
				threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
				JPAUtil.commit();
			} catch (Exception e) {
				String origemOperacao = "Erro ao enviar XMLs de processos.";
				AcumuladorExceptions.instance().adicionarException(origemOperacao,
						"Erro ao enviar XMLs de processos: " + e.getLocalizedMessage(), e, true);
				JPAUtil.rollback();
			} finally {
				// JPAUtil.printEstatisticas();
				JPAUtil.close();
			}
		}
		LOGGER.info("Threads de envio terminadas!");
	}

	private String resumirBodyRequisicao(String body, Header contentType) {
		boolean isHtml = contentType != null && contentType.getValue() != null
				&& contentType.getValue().contains("html");

		if (isHtml) {
			return StringUtils.abbreviate(body, 200).replaceAll("\n", "");
		} else {
			return body;
		}
	}

	/**
	 * Cria um arquivo indicando que o XML foi enviado (sufixo ".enviado") e um arquivo com o protocolo do CNJ, para
	 * posterior validação (sufixo ".protocolo").
	 *
	 * @param arquivo
	 * @param jsonRespostaCNJ
	 * @throws IOException
	 */
	private void marcarLoteProcessoComoEnviado(LoteProcesso loteProcesso, String jsonRespostaCNJ)
			throws DataJudException {
		// Verifica se o CNJ informou um protocolo no retorno JSON, para que esse protocolo seja validado
		// posteriormente.
		String protocolo = null;
		ChaveProcessoCNJ processo = loteProcesso.getChaveProcessoCNJ();
		try {
			JsonObject rootObject = JsonParser.parseString(jsonRespostaCNJ).getAsJsonObject();
			protocolo = rootObject.get("protocolo").getAsString();
		} catch (JsonParseException ex) {
			throw new DataJudException("Não foi possível marcar como ENVIADO o processo: "
					+ processo.getNumeroProcesso() + ". Grau: " + processo.getGrau()
					+ ". Não foi possível ler o número do protocolo JSON do CNJ: " + ex.getLocalizedMessage(), ex);
		}

		// Cria um arquivo para indicar que o arquivo foi enviado com sucesso ao CNJ
		try {
			loteProcesso.setSituacao(SituacaoLoteProcessoEnum.ENVIADO);
			loteProcesso.setDataEnvioLocal(LocalDateTime.now());
			loteProcesso.setProtocoloCNJ(protocolo);

			loteProcessoDAO.incluirOuAlterar(loteProcesso);

		} catch (Exception ex) {
			LOGGER.warn("Não foi possível marcar como ENVIADO o processo: " + processo.getNumeroProcesso() + ". Grau: "
					+ processo.getGrau(), ex);
		}
	}
}
