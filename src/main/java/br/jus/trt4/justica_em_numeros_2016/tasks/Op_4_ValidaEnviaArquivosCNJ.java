package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
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

import br.jus.trt4.justica_em_numeros_2016.auxiliar.ArquivoComInstancia;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DadosInvalidosException;
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
	private final File arquivoAbortar;
	private final List<Long> temposEnvioCNJ = new ArrayList<>();
	private long ultimaExibicaoProgresso;
	private final int numeroThreads;
	private final AtomicLong qtdEnviadaComSucesso = new AtomicLong(0);
	private static final Pattern pProcessoJaEnviado = Pattern.compile("\\{\"status\":\"ERRO\",\"mensagem\":\"(\\d+) processo\\(s\\) não foi\\(ram\\) inserido\\(s\\), pois já existe\\(m\\) na base de dados!\"\\}");
	private static final String NOME_ARQUIVO_ABORTAR = "ABORTAR.txt"; // Arquivo que pode ser gravado na pasta "output/[tipo_carga]", que faz com que o envio dos dados ao CNJ seja abortado
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
				
				// O serviço de teste de conexão não funciona mais na API nova, por enquanto
				// operacao.testarConexaoComCNJ(continuarEmCasoDeErro);
				
				// O serviço de consulta de totais de arquivos não funciona mais na API nova, por enquanto.
				// operacao.consultarTotaisDeProcessosNoCNJ(); // Antes do envio
				
				operacao.localizarEnviarXMLsAoCNJ();
				
				// O serviço de consulta de totais de arquivos não funciona mais na API nova, por enquanto.
				// operacao.consultarTotaisDeProcessosNoCNJ(); // Depois do envio
				
				DadosInvalidosException.mostrarWarningSeHouveAlgumErro();
				
				// Verifica se deve executar novamente em caso de erros
				if (continuarEmCasoDeErro) {
					if (DadosInvalidosException.getQtdErros() > 0) {
						DadosInvalidosException.zerarQtdErros();
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
		
		arquivoAbortar = new File(Auxiliar.prepararPastaDeSaida(), NOME_ARQUIVO_ABORTAR);
		
		// Número de threads simultâneas para conectar ao CNJ
		numeroThreads = Auxiliar.getParametroInteiroConfiguracao(Parametro.numero_threads_simultaneas, 1);
		
		httpClient = HttpUtil.criarNovoHTTPClientComAutenticacaoCNJ();
	}

	/**
	 * Confere se a requisição HTTP teve reposta 200 (SUCCESS) ou 201 (CREATED)
	 * 
	 * UPDATE: Também confere se o serviço não retornou "ERRO" dentro do "body" do request,
	 * pois algumas requisições retornam "200" mas informam erro no conteúdo. 
	 * 
	 * @param statusCode
	 * @throws DadosInvalidosException
	 */
	private void conferirRespostaSucesso(int statusCode, String body, File arquivoXML) throws IOException {
		
		// Se o serviço informar "processo(s) não foi(ram) inserido(s), pois já existe(m) na base de dados",
		// considera que o envio foi bem sucedido, pois de alguma forma os processos já estão na base do CNJ
		// (pode ser por algum outro envio anterior, por exemplo)
		Matcher m = pProcessoJaEnviado.matcher(body);
		if (m.find()) {
			LOGGER.debug("CNJ informou que todos o processo do arquivo '" + arquivoXML + "' já foi recebido. Arquivo será marcado como 'enviado'");
			return;
		}
		
		if (statusCode != 200 && statusCode != 201) {
			throw new IOException("Falha ao conectar no Webservice do CNJ (codigo " + statusCode + ", esperado 200 ou 201)");
		}
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
	private void localizarEnviarXMLsAoCNJ() throws DadosInvalidosException, JAXBException, InterruptedException, IOException {
		
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
		progresso.setMax(totalArquivos);
		progresso.setProgress(totalArquivos - arquivosParaEnviar.size());
		
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
				
				try {
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
							boolean isHtml = result.getContentType() != null && result.getContentType().getValue() != null && result.getContentType().getValue().contains("html");
							LOGGER.trace("* Arquivo: '" + xml + "', tempo=" + tempo + "ms, statusCode=" + statusCode + ", body=" + (isHtml ? "[HTML]" : body));
							conferirRespostaSucesso(statusCode, body, xml.getArquivo());
							marcarArquivoComoEnviado(xml.getArquivo(), body);
							LOGGER.info("* Arquivo enviado com sucesso: " + xml);
							qtdEnviadaComSucesso.incrementAndGet();
							
							arquivoTentativaEnvio.delete();
						} finally {
							EntityUtils.consumeQuietly(response.getEntity());
						}
					} catch (IOException ex) {
						throw new DadosInvalidosException(ex.getLocalizedMessage(), xml.getArquivo().toString());
					}
				} catch (DadosInvalidosException ex) {
					LOGGER.error("* Erro ao enviar arquivo '" + xml + "': " + ex.getLocalizedMessage());
				} finally {
					progresso.incrementProgress();
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
						progresso.setInformacoes(tempoRestanteStr);
					}
					sbProgresso.append(")");
				}
				LOGGER.debug(sbProgresso);
			}
			
			// Verifica se o usuário quer abortar o envio ao CNJ
			if (arquivoAbortar.exists()) {
				LOGGER.info("Abortando envio ao CNJ por causa do arquivo '" + arquivoAbortar.getAbsolutePath() + "'!");
				FileUtils.deleteQuietly(arquivoAbortar);
				break;
			}
		}
		
		LOGGER.info("Aguardando término das threads de envio...");
		enviarMultiThread.aguardarTermino();
		LOGGER.info("Threads de envio terminadas!");
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
			File confirmacaoEnvio = new File(arquivo.getAbsolutePath() + Auxiliar.SUFIXO_PROTOCOLO);
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
