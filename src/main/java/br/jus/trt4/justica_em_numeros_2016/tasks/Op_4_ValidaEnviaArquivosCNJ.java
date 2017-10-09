package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.cnj.replicacao_nacional.Processos;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DadosInvalidosException;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Parametro;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ProdutorConsumidorMultiThread;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ProgressoInterfaceGrafica;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.XmlComInstancia;

/**
 * Chama os webservices do CNJ, enviando os XMLs que foram gerados pela classe {@link Op_3_UnificaArquivosXML}.
 * 
 * @author felipe.giotto@trt4.jus.br
 */
public class Op_4_ValidaEnviaArquivosCNJ {
	
	private static final String SUFIXO_ARQUIVO_TENTOU_ENVIAR = ".tentativa_envio";
	private static final Logger LOGGER = LogManager.getLogger(Op_4_ValidaEnviaArquivosCNJ.class);
	private CloseableHttpClient httpClient;
	//private long ultimaCriacaoHttpClient;
	private final String authHeader;
	private final File arquivoAbortar;
	private final List<Long> temposEnvioCNJ = new ArrayList<>();
	private final int numeroThreads;
	private final AtomicLong qtdEnviadaComSucesso = new AtomicLong(0);
	private static final Pattern pProcessoJaEnviado = Pattern.compile("\\{\"status\":\"ERRO\",\"mensagem\":\"(\\d+) processo\\(s\\) não foi\\(ram\\) inserido\\(s\\), pois já existe\\(m\\) na base de dados!\"\\}");
	private static final String NOME_ARQUIVO_ABORTAR = "ABORTAR.txt"; // Arquivo que pode ser gravado na pasta "output/[tipo_carga]", que faz com que o envio dos dados ao CNJ seja abortado
	private static ProgressoInterfaceGrafica progresso;
	
	public static void main(String[] args) throws Exception {
		
		System.out.println("Se algum arquivo for negado n CNJ, você quer que a operação seja reiniciada?");
		System.out.println("Responda 'S' para que o envio ao CNJ rode indefinidamente, até que o webservice não negue nenhum arquivo.");
		System.out.println("Responda 'N' para que o envio ao CNJ rode somente uma vez.");
		String resposta = Auxiliar.readStdin().toUpperCase();
		
		validarEnviarArquivosCNJ("S".equals(resposta));
	}

	public static void validarEnviarArquivosCNJ(boolean continuarEmCasoDeErro) throws Exception, DadosInvalidosException, IOException, InvalidCredentialsException, InterruptedException, JAXBException {
		
		Auxiliar.prepararPastaDeSaida();
		
		if (continuarEmCasoDeErro) {
			LOGGER.info("Se ocorrer algum erro no envio, a operação será reiniciada quantas vezes for necessário!");
		}
		
		progresso = new ProgressoInterfaceGrafica("(4/5) Envio dos arquivos ao CNJ");
		try {
			boolean executar = true;
			do {
				progresso.setProgress(0);
				
				Op_4_ValidaEnviaArquivosCNJ operacao = new Op_4_ValidaEnviaArquivosCNJ();
				operacao.testarConexaoComCNJ(continuarEmCasoDeErro);
				operacao.consultarTotaisDeProcessosNoCNJ(); // Antes do envio
				operacao.localizarEnviarXMLsAoCNJ();
				operacao.consultarTotaisDeProcessosNoCNJ(); // Depois do envio
				
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
		
        // Autenticação do tribunal junto ao CNJ
		String usuario = Auxiliar.getParametroConfiguracao(Parametro.sigla_tribunal, true);
		String senha = Auxiliar.getParametroConfiguracao(Parametro.password_tribunal, true);
		String autenticacao = usuario + ":" + senha;
		byte[] encodedAuth = Base64.encodeBase64(autenticacao.getBytes(Charset.forName("UTF-8")));
		authHeader = "Basic " + new String(encodedAuth);
	}
	
	/**
	 * Retorna um objeto que fará a conexão com o site do CNJ.
	 * 
	 * Renova essa conexão de hora em hora, para tentar evitar que o desempenho diminua com o passar do tempo
	 * 
	 * @return
	 * @throws IOException 
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException 
	 * @throws KeyManagementException 
	 */
	private synchronized CloseableHttpClient getHttpClient() {
		//long agora = System.currentTimeMillis();
		//if ((agora - ultimaCriacaoHttpClient) > 3600_000 || httpClient == null) {
		if (httpClient == null) {
			
			LOGGER.info("Criando novo CloseableHttpClient");
			//ultimaCriacaoHttpClient = agora;
			
			// Objeto que criará cada request a ser feito ao CNJ
	        HttpClientBuilder httpClientBuilder = HttpClients.custom();
	        
	        // Aumenta o limite de conexoes, para permitir acesso multi-thread
	        httpClientBuilder.setMaxConnPerRoute(numeroThreads*2);
	        httpClientBuilder.setMaxConnTotal(numeroThreads*2);
	        
	        // SSL
	        SSLContext sslcontext = getSSLContext();
	        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext);
	        httpClientBuilder.setSSLSocketFactory(factory);
	        
	        // Proxy
	        String proxyHost = Auxiliar.getParametroConfiguracao(Parametro.proxy_host, false);
	        if (proxyHost != null) {
	        	int proxyPort = Auxiliar.getParametroInteiroConfiguracao(Parametro.proxy_port, 3128);
	    		HttpHost proxy = new HttpHost(proxyHost, proxyPort);
	            httpClientBuilder.setProxy(proxy);
	    		LOGGER.info("Será utilizado proxy para conectar ao CNJ: " + proxyHost);
	    		
	            // Autenticação do Proxy
	            String proxyUsername = Auxiliar.getParametroConfiguracao(Parametro.proxy_username, false);
	            if (proxyUsername != null) {
	            	String proxyPassword = Auxiliar.getParametroConfiguracao(Parametro.proxy_password, false);
	            	Credentials credentials = new UsernamePasswordCredentials(proxyUsername, proxyPassword);
	            	AuthScope authScope = new AuthScope(proxyHost, proxyPort);
	            	CredentialsProvider credsProvider = new BasicCredentialsProvider();
	            	credsProvider.setCredentials(authScope, credentials);
	            	httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
	        		LOGGER.info("Será utilizada autenticação no proxy: " + proxyUsername);
	            }
	        } else {
	    		LOGGER.info("Não será utilizado proxy para conectar ao CNJ.");
	        }
	        
			// Cria um novo HttpClient para acessar o CNJ
			httpClient = httpClientBuilder.build();
		}
		return httpClient;
	}
	
	/**
	 * Carrega a keystore com os certificados do CNJ
	 * @return
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws IOException
	 * @throws KeyManagementException
	 */
    private static SSLContext getSSLContext() {
    	try {
	        KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
	        FileInputStream instream = new FileInputStream(new File("src/main/resources/certificados_rest_cnj/keystore/cnj.keystore"));
	        try {
	            trustStore.load(instream, "storepasscnj".toCharArray());
	        } finally {
	            instream.close();
	        }
	        return SSLContexts.custom().loadTrustMaterial(trustStore).build();
    	} catch (Exception ex) {
    		LOGGER.error("Erro ao iniciar contexto SSL: " + ex.getLocalizedMessage(), ex);
    		throw new RuntimeException(ex);
    	}
    }	
	
	private void testarConexaoComCNJ(boolean continuarEmCasoDeErro) throws DadosInvalidosException, IOException {
		
		LOGGER.info("Testando conexão com o webservice do CNJ...");
		
		HttpGet get = new HttpGet(Auxiliar.getParametroConfiguracao(Parametro.url_webservice_cnj, true));
		adicionarCabecalhoAutenticacao(get);
		
		long tempo = System.currentTimeMillis();
		HttpResponse response = getHttpClient().execute(get);
		tempo = System.currentTimeMillis() - tempo;

		HttpEntity entity = response.getEntity();
		String body = EntityUtils.toString(entity, Charset.forName("UTF-8"));
		LOGGER.info("Resposta recebida em " + tempo + "ms: " + body);

		if (!continuarEmCasoDeErro) {
			conferirRespostaSucesso(response.getStatusLine().getStatusCode(), null, null, null);
		}
	}

	private void consultarTotaisDeProcessosNoCNJ() {
		
		if (Auxiliar.deveProcessarPrimeiroGrau()) {
			consultarTotalProcessosNoCNJ(1);
		}
		if (Auxiliar.deveProcessarSegundoGrau()) {
			consultarTotalProcessosNoCNJ(2);
		}
	}
	
	private void consultarTotalProcessosNoCNJ(int instancia) {
		
		LOGGER.info("Consultando total de processos enviados ao serviço do CNJ em G" + instancia + "...");
		HttpGet httpGet = new HttpGet(Auxiliar.getParametroConfiguracao(Parametro.url_webservice_cnj, true) + "/total/G" + instancia);
		adicionarCabecalhoAutenticacao(httpGet);
		try {
			HttpResponse httpResponse = getHttpClient().execute(httpGet);
			HttpEntity httpEntity = httpResponse.getEntity();
			String body = EntityUtils.toString(httpEntity, Charset.forName("UTF-8"));
			LOGGER.info("* G" + instancia + ": " + body);
		} catch (IOException ex) {
			LOGGER.error("* Erro ao consultar total em G" + instancia + ": " + ex.getLocalizedMessage(), ex);
		}
	}

	/**
	 * Adiciona o header "Authorization", informando autenticação "Basic", conforme documento "API REST.pdf"
	 * @param get
	 */
	private void adicionarCabecalhoAutenticacao(HttpRequestBase get) {
		get.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
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
	private void conferirRespostaSucesso(int statusCode, String body, Unmarshaller unmarshaller, File arquivoXML) throws DadosInvalidosException {
		
		// Situação especial: se o serviço informar "processo(s) não foi(ram) inserido(s), pois já existe(m) na base de dados",
		// confere se TODOS os processos enviados já existiam (conferindo a quantidade informada com a quantidade existente
		// no arquivo XML que foi enviado).
		// Se for a mesma quantidade, considera que o envio foi bem sucedido, pois de alguma forma os processos já estão
		// na base do CNJ (pode ser por algum outro envio anterior, por exemplo)
		if (unmarshaller != null) {
			Matcher m = pProcessoJaEnviado.matcher(body);
			if (m.find()) {
				
				// Quantidade de processos negados pelo serviço do CNJ
				int qtdProcessosNegados = Integer.parseInt(m.group(1));
				int qtdProcessosXML;
				
				if (Auxiliar.deveMontarLotesDeProcessos()) {
					// Quantidade de processos enviados no lote
					Processos processosXML;
					try {
						synchronized(unmarshaller) {
							processosXML = (Processos) unmarshaller.unmarshal(arquivoXML);
						}
					} catch (JAXBException e) {
						throw new DadosInvalidosException("Erro ao tentar analisar a quantidade de processos: " + e.getLocalizedMessage(), arquivoXML.toString());
					}
					qtdProcessosXML = processosXML.getProcesso().size();
					
				} else {
					
					// Alguns XMLs, especialmente gerados pela NovaJus4, não podem ser carregados pelo Unmarshaller.
					// Por isso, para evitar problemas, se for realizado envio individual, vou presumir que o tamanho do lote é 1.
					qtdProcessosXML = 1;
				}
				
				if (qtdProcessosNegados == qtdProcessosXML) {
					LOGGER.debug("CNJ informou que todos os processos do arquivo '" + arquivoXML + "' já foram recebidos. Arquivo será marcado como 'enviado'");
					return;
				}
			}
		}
		
		if (statusCode != 200 && statusCode != 201) {
			throw new DadosInvalidosException("Falha ao conectar no Webservice do CNJ (codigo " + statusCode + ", esperado 200 ou 201)", arquivoXML.toString());
		}
		if (body != null && body.contains("\"ERRO\"")) {
			throw new DadosInvalidosException("Falha ao conectar no Webservice do CNJ (body retornou 'ERRO')", arquivoXML.toString());
		}
		//LOGGER.debug("Resposta: " + statusCode);
	}

	/**
	 * Carrega os arquivos XML das instâncias selecionadas (1G e/ou 2G) e envia ao CNJ.
	 * 
	 * @throws DadosInvalidosException 
	 * @throws JAXBException 
	 * @throws InterruptedException 
	 */
	private void localizarEnviarXMLsAoCNJ() throws DadosInvalidosException, JAXBException, InterruptedException {
		
		// Lista com todos os arquivos pendentes
		Auxiliar.prepararPastaDeSaida();
		List<XmlComInstancia> arquivosParaEnviar = new ArrayList<>();
		AtomicInteger totalArquivosXML = new AtomicInteger();
		
		// Consulta arquivos das instâncias selecionadas
		if (Auxiliar.deveProcessarSegundoGrau()) {
			localizarXMLsPendentesDeEnvio(2, arquivosParaEnviar, totalArquivosXML);
		}
		if (Auxiliar.deveProcessarPrimeiroGrau()) {
			localizarXMLsPendentesDeEnvio(1, arquivosParaEnviar, totalArquivosXML);
		}
		progresso.setMax(totalArquivosXML.get());
		progresso.setProgress(totalArquivosXML.get() - arquivosParaEnviar.size());
		
		// Coloca os arquivos que já tentou-se enviar (e, provavelmente, deu erro) no final da lista
		ordenarArquivosNuncaEnviadosPrimeiro(arquivosParaEnviar);
		
		// Mostra o total de arquivos e os pendentes de envio
		String infoTotal = "Total de arquivos XML encontrados: " + totalArquivosXML.get() + ". Arquivos pendentes de envio: " + arquivosParaEnviar.size();
		if (totalArquivosXML.get() > 0) {
			infoTotal += " (" + (arquivosParaEnviar.size() * 100 / totalArquivosXML.get()) + "%)";
		}
		LOGGER.info(infoTotal);
		
		// Inicia o envio
		enviarXMLsAoCNJ(arquivosParaEnviar);
		
		// Envio finalizado
		LOGGER.info("Total de arquivos enviados com sucesso: " + qtdEnviadaComSucesso.get());
	}

	/**
	 * Carrega os arquivos XML (individuais ou unificados) da instância selecionada (1G ou 2G)
	 * 
	 * @param grau
	 * @throws DadosInvalidosException 
	 */
	private void localizarXMLsPendentesDeEnvio(int grau, List<XmlComInstancia> arquivosParaEnviar, AtomicInteger totalArquivosXML) throws DadosInvalidosException {
		
		// Lê arquivos da lista de XMLs individuais ou unificados, conforme parâmetros
		File pastaXMLsParaEnvio;
		if (Auxiliar.deveMontarLotesDeProcessos()) {
			pastaXMLsParaEnvio = Auxiliar.getPastaXMLsUnificados(grau);
		} else {
			pastaXMLsParaEnvio = Auxiliar.getPastaXMLsIndividuais(grau);
		}
		
		localizarArquivosRecursivamente(pastaXMLsParaEnvio, grau, arquivosParaEnviar, totalArquivosXML);
	}

	private void localizarArquivosRecursivamente(File pasta, int grau, List<XmlComInstancia> arquivosParaEnviar, AtomicInteger totalArquivosXML) throws DadosInvalidosException {
		
		LOGGER.debug("Localizando todos os arquivos XML da pasta '" + pasta.getAbsolutePath() + "'...");
		if (!pasta.isDirectory()) {
			throw new DadosInvalidosException("Pasta não existe, talvez falte executar tarefas anteriores", pasta.toString());
		}
		
		// Filtro para localizar arquivos XML a serem enviados, bem como pastas para fazer busca recursiva
		FileFilter aceitarPastasOuXMLs = new FileFilter() {
			
			@Override
			public boolean accept(File file) {
				return file.isDirectory() || file.getName().toUpperCase().endsWith(".XML");
			}
		};
		
		// Localiza todos os arquivos XML da pasta
		File[] arquivos = pasta.listFiles(aceitarPastasOuXMLs);
		for (File filho: arquivos) {
			
			if (filho.isDirectory()) {
				localizarArquivosRecursivamente(filho, grau, arquivosParaEnviar, totalArquivosXML);
				
			} else {
				
				totalArquivosXML.incrementAndGet();
				if (deveEnviarArquivo(filho)) {
					arquivosParaEnviar.add(new XmlComInstancia(filho, grau));
				}
			}
		}
	}

	
	private void enviarXMLsAoCNJ(List<XmlComInstancia> arquivosParaEnviar) throws JAXBException, InterruptedException {

		// Prepara objetos para LER os arquivos XML e analisar a quantidade de processos dentro de cada um
		JAXBContext jaxbContext = JAXBContext.newInstance(Processos.class);
		final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		
		// Objeto que fará o envio dos arquivos em várias threads
		LOGGER.info("Iniciando o envio de " + arquivosParaEnviar.size() + " arquivos utilizando " + numeroThreads + " thread(s)");
		ProdutorConsumidorMultiThread<XmlComInstancia> enviarMultiThread = new ProdutorConsumidorMultiThread<XmlComInstancia>(numeroThreads, numeroThreads, Thread.NORM_PRIORITY) {

			@Override
			public void consumir(XmlComInstancia xml) {
				
				Auxiliar.prepararThreadLog();
				LOGGER.trace("Enviando arquivo " + xml + "...");
				
				// Monta a URL para enviar processos ao CNJ.
				// Exemplo de URL: https://wwwh.cnj.jus.br/selo-integracao-web/v1/processos/G2
				final String url = Auxiliar.getParametroConfiguracao(Parametro.url_webservice_cnj, true) + "/G" + xml.getGrau();
				LOGGER.info("* URL onde o arquivo será enviado: " + url);
				
				HttpPost post = new HttpPost(url);
				adicionarCabecalhoAutenticacao(post);
				
				// Timeout
				int CONNECTION_TIMEOUT_MS = 300_000; // Timeout in millis (5min)
				RequestConfig requestConfig = RequestConfig.custom()
				    .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
				    .setConnectTimeout(CONNECTION_TIMEOUT_MS)
				    .setSocketTimeout(CONNECTION_TIMEOUT_MS)
				    .build();
				post.setConfig(requestConfig);
				
				// Prepara um request com Multipart
				HttpEntity entity = MultipartEntityBuilder.create().addBinaryBody("file", xml.getArquivoXML()).build();
				post.setEntity(entity);
				
				String body = null;
				try {
					try {
						
						// Indica que o envio do arquivo está iniciando
						File arquivoTentativaEnvio = new File(xml.getArquivoXML().getAbsolutePath() + SUFIXO_ARQUIVO_TENTOU_ENVIAR);
						arquivoTentativaEnvio.createNewFile();
						
						// Executa o POST
						long tempo = System.currentTimeMillis();
						HttpResponse response = getHttpClient().execute(post);
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
							body = EntityUtils.toString(result, Charset.forName("UTF-8"));
							int statusCode = response.getStatusLine().getStatusCode();
							LOGGER.debug("* Resposta em " + tempo + "ms (" + statusCode + "): " + body);
							conferirRespostaSucesso(statusCode, body, jaxbUnmarshaller, xml.getArquivoXML());
							LOGGER.info("* Arquivo enviado com sucesso: " + xml + " / Resposta: " + body);
							marcarArquivoComoEnviado(xml.getArquivoXML());
							qtdEnviadaComSucesso.incrementAndGet();
							
							arquivoTentativaEnvio.delete();
						} finally {
							EntityUtils.consumeQuietly(response.getEntity());
						}
					} catch (IOException ex) {
						throw new DadosInvalidosException(ex.getLocalizedMessage(), xml.getArquivoXML().toString());
					}
				} catch (DadosInvalidosException ex) {
					LOGGER.error("* Erro ao enviar arquivo: " + xml + " / Resposta: " + body + " / Erro: " + ex.getLocalizedMessage());
				} finally {
					progresso.incrementProgress();
				}
			}
		};
		
		int qtdArquivos = arquivosParaEnviar.size();
		for (int i = 0; i < qtdArquivos; i++) {
			XmlComInstancia xml = arquivosParaEnviar.get(i);
			
			// Coloca cada um dos arquivos na fila para envio
			enviarMultiThread.produzir(xml);
			
			// Mostra previsão de conclusão
			StringBuilder sbProgresso = new StringBuilder();
			sbProgresso.append("Envio dos arquivos pendentes: " + i + "/" + qtdArquivos);
			sbProgresso.append(" (" + (i * 10000 / qtdArquivos / 100.0) + "%");
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
	 * Coloca os arquivos que já tentou-se enviar (e, provavelmente, deu erro) no final da lista
	 * 
	 * @param arquivos
	 */
	private void ordenarArquivosNuncaEnviadosPrimeiro(List<XmlComInstancia> arquivos) {

		Collections.sort(arquivos, new Comparator<XmlComInstancia>() {
			
			@Override
			public int compare(XmlComInstancia o1, XmlComInstancia o2) {
				
				String o1Path = o1.getArquivoXML().getAbsolutePath();
				String o2Path = o2.getArquivoXML().getAbsolutePath();
				boolean o1TentouEnviar = new File(o1Path + SUFIXO_ARQUIVO_TENTOU_ENVIAR).exists();
				boolean o2TentouEnviar = new File(o2Path + SUFIXO_ARQUIVO_TENTOU_ENVIAR).exists();
				if (o1TentouEnviar && !o2TentouEnviar) {
					return 1;
				} else if (!o1TentouEnviar && o2TentouEnviar) {
					return -1;
				} else {
					return o1Path.compareTo(o2Path);
				}
			}
		});
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
			//LOGGER.debug("Arquivo já foi enviado anteriormente: " + arquivo);
			return false;
		}
		
		return true;
	}
	
	private void marcarArquivoComoEnviado(File arquivo) {
		File confirmacaoEnvio = new File(arquivo.getAbsolutePath() + Auxiliar.SUFIXO_ARQUIVO_ENVIADO);
		try {
			confirmacaoEnvio.createNewFile();
		} catch (IOException ex) {
			LOGGER.warn("Não foi possível marcar arquivo como enviado: " + arquivo, ex);
		}
	}
}
