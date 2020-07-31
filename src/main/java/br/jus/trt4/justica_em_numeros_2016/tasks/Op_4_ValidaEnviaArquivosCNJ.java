package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.AcumuladorExceptions;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ArquivoComInstancia;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ControleAbortarOperacao;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.HttpUtil;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Parametro;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ProdutorConsumidorMultiThread;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ProgressoInterfaceGrafica;

/**
 * Chama os webservices do CNJ, enviando os XMLs que foram gerados pela classe {@link Op_3_UnificaArquivosXML}.
 * 
 * @author felipe.giotto@trt4.jus.br
 */
public class Op_4_ValidaEnviaArquivosCNJ {
	
	private static final Logger LOGGER = LogManager.getLogger(Op_4_ValidaEnviaArquivosCNJ.class);
	private CloseableHttpClient httpClient;
	private final List<Long> temposEnvioCNJ = new ArrayList<>();
	private long ultimaExibicaoProgresso;
	private int numeroThreads;
	private final AtomicLong qtdEnviadaComSucesso = new AtomicLong(0);
	private static ProgressoInterfaceGrafica progresso;
	
	public static void main(String[] args) throws Exception {
		
		System.out.println("Se algum arquivo for negado no CNJ, você quer que a operação seja reiniciada?");
		System.out.println("Responda 'S' para que o envio ao CNJ rode diversas vezes, até que o webservice não recuse nenhum arquivo.");
		System.out.println("Responda 'N' para que o envio ao CNJ rode somente uma vez.");
		String resposta = Auxiliar.readStdin().toUpperCase();
		
		validarEnviarArquivosCNJ("S".equals(resposta));
	}

	public static void validarEnviarArquivosCNJ(boolean continuarEmCasoDeErro) throws Exception {
		
		Auxiliar.prepararPastaDeSaida();
		
		if (continuarEmCasoDeErro) {
			LOGGER.info("Se ocorrer algum erro no envio, a operação será reiniciada quantas vezes for necessário!");
		}
		
		progresso = new ProgressoInterfaceGrafica("(4/6) Envio dos arquivos ao CNJ");
		try {
			boolean executar = true;
			do {
				progresso.setProgress(0);
				
				Op_4_ValidaEnviaArquivosCNJ operacao = new Op_4_ValidaEnviaArquivosCNJ();
				
				operacao.localizarEnviarXMLsAoCNJ();
				
				AcumuladorExceptions.instance().mostrarExceptionsAcumuladas();
				
				// Verifica se deve executar novamente em caso de erros
				if (continuarEmCasoDeErro) {
					if (AcumuladorExceptions.instance().isExisteExceptionRegistrada()) {
						progresso.setInformacoes("Aguardando para reiniciar...");
						LOGGER.warn("A operação foi concluída com erros! O envio será reiniciado em 2min... Se desejar, aborte este script.");
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
	 * Prepara o componente HttpClient para conectar aos serviços REST do CNJ
	 * 
	 * @throws Exception
	 */
	public Op_4_ValidaEnviaArquivosCNJ() throws Exception {
		
		// Número de threads simultâneas para conectar ao CNJ
		numeroThreads = Auxiliar.getParametroInteiroConfiguracao(Parametro.numero_threads_simultaneas, 1);
		
		httpClient = HttpUtil.criarNovoHTTPClientComAutenticacaoCNJ();
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
			throw new IOException("Falha ao conectar no Webservice do CNJ (codigo " + statusCode + ", esperado 200 ou 201)");
		}
		
		// Ex: statusCode=202, body={"status":"ERRO","protocolo":"TRT479782202007171595018643017","mensagem":"Não foi possível fazer a recepção do arquivo. Tente novamente mais tarde"}
		if (body != null && body.contains("\"ERRO\"")) {
			throw new IOException("Falha ao conectar no Webservice do CNJ (body retornou 'ERRO')");
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
		Auxiliar.prepararPastaDeSaida();
		List<ArquivoComInstancia> arquivosXML = ArquivoComInstancia.localizarArquivosInstanciasHabilitadas(".xml");
		
		ArquivoComInstancia.mostrarTotalDeArquivosPorPasta(arquivosXML, "Total de arquivos XML encontrados");
		int totalArquivos = arquivosXML.size();
		
		// Filtra somente os arquivos que ainda não foram enviados
		List<ArquivoComInstancia> arquivosParaEnviar = filtrarSomenteArquivosPendentesDeEnvio(arquivosXML);
		
		// Verifica se não há arquivos muito pequenos, que com certeza não contém um processo dentro (como ocorreu em Jan/2020 no TRT4)
		List<File> arquivosPequenos = arquivosParaEnviar
			.stream()
			.map(ArquivoComInstancia::getArquivo)
			.filter(a -> a.length() < 200)
			.collect(Collectors.toList());
		if (!arquivosPequenos.isEmpty()) {
			LOGGER.warn("");
			LOGGER.warn("");
			LOGGER.warn("");
			for (File arquivo: arquivosPequenos) {
				LOGGER.warn("* " + arquivo);
			}
			LOGGER.warn("Os arquivos acima são muito pequenos e, por isso, provavelmente estão incompletos.");
			Auxiliar.aguardaUsuarioApertarENTERComTimeout(1);
		}
		
		// Mostra os arquivos que serão enviados
		ArquivoComInstancia.mostrarTotalDeArquivosPorPasta(arquivosParaEnviar, "Arquivos XML que precisam ser enviados");
		
		// Atualiza o progresso na interface
		if (progresso != null) {
			progresso.setMax(totalArquivos);
			progresso.setProgress(totalArquivos - arquivosParaEnviar.size());
		}
		
		// Inicia o envio
		enviarXMLsAoCNJ(arquivosParaEnviar);
		
		// Envio finalizado
		LOGGER.info("Total de arquivos enviados com sucesso: " + qtdEnviadaComSucesso.get());
		List<ArquivoComInstancia> arquivosXMLPendentes = filtrarSomenteArquivosPendentesDeEnvio(arquivosParaEnviar);
		ArquivoComInstancia.mostrarTotalDeArquivosPorPasta(arquivosXMLPendentes, "Arquivos XML ainda pendentes de envio");
	}

	public List<ArquivoComInstancia> filtrarSomenteArquivosPendentesDeEnvio(List<ArquivoComInstancia> arquivosXML) {
		List<ArquivoComInstancia> arquivosXMLParaEnviar = new ArrayList<>();
		for (ArquivoComInstancia xml: arquivosXML) {
			if (deveEnviarArquivo(xml.getArquivo())) {
				arquivosXMLParaEnviar.add(xml);
			}
		}
		return arquivosXMLParaEnviar;
	}

	private void enviarXMLsAoCNJ(List<ArquivoComInstancia> arquivosParaEnviar) throws JAXBException, InterruptedException {

		//Para evitar a exceção "Unable to invoke factory method in class org.apache.logging.log4j.core.appender.RollingFileAppender 
		//for element RollingFile" ao tentar criar um appender RollingFile para uma thread de um arquivo inexistente
		numeroThreads = numeroThreads > arquivosParaEnviar.size() ? arquivosParaEnviar.size() : numeroThreads;
		
		// Objeto que fará o envio dos arquivos em várias threads
		LOGGER.info("Iniciando o envio de " + arquivosParaEnviar.size() + " arquivos utilizando " + numeroThreads + " thread(s)");
		ProdutorConsumidorMultiThread<ArquivoComInstancia> enviarMultiThread = new ProdutorConsumidorMultiThread<ArquivoComInstancia>(numeroThreads, numeroThreads, Thread.NORM_PRIORITY) {

			@Override
			public void consumir(ArquivoComInstancia xml) {
				
				Auxiliar.prepararThreadLog();
				LOGGER.trace("Enviando arquivo " + xml + "...");
				
				// Monta a URL para enviar processos ao CNJ.
				// Exemplo de URL: https://wwwh.cnj.jus.br/selo-integracao-web/v1/processos/G2
				final String url = Auxiliar.getParametroConfiguracao(Parametro.url_webservice_cnj, true) + "/G" + xml.getGrau();
				LOGGER.trace("* URL para onde o arquivo será enviado: " + url);
				
				HttpPost post = new HttpPost(url);
				HttpUtil.adicionarCabecalhoAutenticacao(post);
				
				// Timeout
				int CONNECTION_TIMEOUT_MS = 300_000; // Timeout in millis (5min)
				RequestConfig requestConfig = RequestConfig.custom()
				    .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
				    .setConnectTimeout(CONNECTION_TIMEOUT_MS)
				    .setSocketTimeout(CONNECTION_TIMEOUT_MS)
				    .build();
				post.setConfig(requestConfig);
				
				// Prepara um request com Multipart
				HttpEntity entity = MultipartEntityBuilder.create().addBinaryBody("file", xml.getArquivo()).build();
				post.setEntity(entity);
				
				String origem = "Envio do arquivo " + xml.getArquivo().getAbsolutePath();
				try {
					// Indica que o envio do arquivo está iniciando
					File arquivoTentativaEnvio = xml.getArquivoControleTentativaEnvio();
					arquivoTentativaEnvio.createNewFile();
					
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
						LOGGER.trace("* Arquivo: '" + xml + "', tempo=" + tempo + "ms, statusCode=" + statusCode + ", body=" + resumirBodyRequisicao(body, result.getContentType()));
						conferirRespostaSucesso(statusCode, body);
						marcarArquivoComoEnviado(xml.getArquivo(), body);
						LOGGER.info("* Arquivo enviado com sucesso: " + xml);
						qtdEnviadaComSucesso.incrementAndGet();
						
						arquivoTentativaEnvio.delete();
					} finally {
						EntityUtils.consumeQuietly(response.getEntity());
					}
					
					AcumuladorExceptions.instance().removerException(origem);
				} catch (Exception ex) {
					AcumuladorExceptions.instance().adicionarException(origem, ex.getLocalizedMessage(), ex, true);
					
				} finally {
					if (progresso != null) {
						progresso.incrementProgress();
					}
				}
			}
		};
		
		int qtdArquivos = arquivosParaEnviar.size();
		for (int i = 0; i < qtdArquivos; i++) {
			ArquivoComInstancia xml = arquivosParaEnviar.get(i);
			
			// Coloca cada um dos arquivos na fila para envio
			enviarMultiThread.produzir(xml);
			
			// Mostra previsão de conclusão
			if ((System.currentTimeMillis() - ultimaExibicaoProgresso) > 5_000) {
				ultimaExibicaoProgresso = System.currentTimeMillis();
				StringBuilder sbProgresso = new StringBuilder();
				sbProgresso.append("Envio dos arquivos pendentes: " + i + "/" + qtdArquivos);
				double percentual = i * 10000 / qtdArquivos / 100.0;
				sbProgresso.append(" (" + percentual + "%");
				synchronized (temposEnvioCNJ) {
					int arquivosMedicao = temposEnvioCNJ.size();
					if (arquivosMedicao > 0) {
						long totalTempo = 0;
						for (Long tempo: temposEnvioCNJ) {
							totalTempo += tempo;
						}
						long tempoMedio = totalTempo / arquivosMedicao;
						long tempoRestante = (qtdArquivos - i) * tempoMedio;
						String tempoRestanteStr = "ETA " + DurationFormatUtils.formatDurationHMS(tempoRestante/numeroThreads);
						sbProgresso.append(" - " + tempoRestanteStr + " em " + numeroThreads + " thread(s)");
						sbProgresso.append(" - media de " + DurationFormatUtils.formatDurationHMS(tempoMedio) + "/arquivo");
						if (progresso != null) {
							progresso.setInformacoes(tempoRestanteStr);
						}
					}
					sbProgresso.append(")");
				}
				LOGGER.debug(sbProgresso);
			}
			
			// Verifica se o usuário quer abortar o envio ao CNJ
			if (ControleAbortarOperacao.instance().isDeveAbortar()) {
				break;
			}
		}
		
		LOGGER.info("Aguardando término das threads de envio...");
		enviarMultiThread.aguardarTermino();
		LOGGER.info("Threads de envio terminadas!");
	}
	
	private String resumirBodyRequisicao(String body, Header contentType) {
		boolean isHtml = contentType != null && contentType.getValue() != null && contentType.getValue().contains("html");

		if (isHtml) {
			return StringUtils.abbreviate(body, 200).replaceAll("\n", "");
		} else {
			return body;
		}
	}

	/**
	 * Verifica se um determinado arquivo deve ser enviado. Condições:
	 * 1. Deve ser um XML
	 * 2. Não deve existir um arquivo com sufixo ".enviado", que indica que ele já foi enviado previamente.
	 *  
	 * @param arquivo
	 * @return
	 */
	private boolean deveEnviarArquivo(File arquivo) {
		
		// Envia somente arquivos XML
		if (!arquivo.getName().toUpperCase().endsWith(".XML")) {
			return false;
		}
		
		// Não envia arquivos que já foram enviados NESTA REMESSA
		File confirmacaoEnvio = new File(arquivo.getAbsolutePath() + Auxiliar.SUFIXO_ARQUIVO_ENVIADO);
		if (confirmacaoEnvio.exists()) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Cria um arquivo indicando que o XML foi enviado (sufixo ".enviado") e um arquivo com o
	 * protocolo do CNJ, para posterior validação (sufixo ".protocolo").
	 *
	 * @param arquivo
	 * @param jsonRespostaCNJ
	 * @throws IOException
	 */
	private void marcarArquivoComoEnviado(File arquivo, String jsonRespostaCNJ) throws IOException {
		
		// Verifica se o CNJ informou um protocolo no retorno JSON, para que esse protocolo seja validado posteriormente.
		try {
			JsonObject rootObject = JsonParser.parseString(jsonRespostaCNJ).getAsJsonObject();
			String protocolo = rootObject.get("protocolo").getAsString();
			File confirmacaoEnvio = Auxiliar.gerarNomeArquivoProtocoloProcessoEnviado(arquivo);
			FileUtils.write(confirmacaoEnvio, protocolo, StandardCharsets.UTF_8);
		} catch (JsonParseException ex) {
			LOGGER.warn("Não foi possível ler o número do protocolo JSON do CNJ");
		}
		
		// Cria um arquivo para indicar que o arquivo foi enviado com sucesso ao CNJ
		File confirmacaoEnvio = new File(arquivo.getAbsolutePath() + Auxiliar.SUFIXO_ARQUIVO_ENVIADO);
		try {
			confirmacaoEnvio.createNewFile();
		} catch (IOException ex) {
			LOGGER.warn("Não foi possível marcar arquivo como enviado: " + arquivo, ex);
		}
	}
}
