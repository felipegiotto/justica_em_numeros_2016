package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
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

/**
 * Chama os webservices do CNJ, enviando os XMLs que foram gerados pela classe {@link Op_3_UnificaArquivosXML}.
 * 
 * @author felipe.giotto@trt4.jus.br
 */
public class Op_4_ValidaEnviaArquivosCNJ {
	
	private static final Logger LOGGER = LogManager.getLogger(Op_4_ValidaEnviaArquivosCNJ.class);
	private CloseableHttpClient client;
	private String authHeader;
	private static Pattern pProcessoJaEnviado = Pattern.compile("\\{\"status\":\"ERRO\",\"mensagem\":\"(\\d+) processo\\(s\\) não foi\\(ram\\) inserido\\(s\\), pois já existe\\(m\\) na base de dados!\"\\}");
	
	public static void main(String[] args) throws Exception {
		
		Auxiliar.prepararPastaDeSaida();
		
		Op_4_ValidaEnviaArquivosCNJ operacao = new Op_4_ValidaEnviaArquivosCNJ();
		operacao.testarConexaoComCNJ();
		operacao.enviarXMLsUnificadosAoCNJ();
		
		DadosInvalidosException.mostrarWarningSeHouveAlgumErro();
		LOGGER.info("Fim!");
	}

	/**
	 * Prepara o componente HttpClient para conectar aos serviços REST do CNJ
	 * 
	 * @throws Exception
	 */
	public Op_4_ValidaEnviaArquivosCNJ() throws Exception {
		
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        
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
    		
            // Autenticação do Proxy
            String proxyUsername = Auxiliar.getParametroConfiguracao(Parametro.proxy_username, false);
            if (proxyUsername != null) {
            	String proxyPassword = Auxiliar.getParametroConfiguracao(Parametro.proxy_password, false);
            	Credentials credentials = new UsernamePasswordCredentials(proxyUsername, proxyPassword);
            	AuthScope authScope = new AuthScope(proxyHost, proxyPort);
            	CredentialsProvider credsProvider = new BasicCredentialsProvider();
            	credsProvider.setCredentials(authScope, credentials);
            	httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
            }
        }

        // Autenticação do tribunal junto ao CNJ
		String usuario = Auxiliar.getParametroConfiguracao(Parametro.sigla_tribunal, true);
		String senha = Auxiliar.getParametroConfiguracao(Parametro.password_tribunal, true);
		String autenticacao = usuario + ":" + senha;
		byte[] encodedAuth = Base64.encodeBase64(autenticacao.getBytes(Charset.forName("UTF-8")));
		authHeader = "Basic " + new String(encodedAuth);
		
		// Cria um HttpClient para acessar o CNJ
		client = httpClientBuilder.build();
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
    private static SSLContext getSSLContext() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException {
    	
        KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream instream = new FileInputStream(new File("src/main/resources/certificados_rest_cnj/keystore/cnj.keystore"));
        try {
            trustStore.load(instream, "storepasscnj".toCharArray());
        } finally {
            instream.close();
        }
        return SSLContexts.custom().loadTrustMaterial(trustStore).build();
    }	
	
	private void testarConexaoComCNJ() throws DadosInvalidosException, IOException {
		
		LOGGER.info("Testando conexão com o webservice do CNJ...");
		
		HttpGet get = new HttpGet(Auxiliar.getParametroConfiguracao(Parametro.url_webservice_cnj, true));
		adicionarCabecalhoAutenticacao(get);
		HttpResponse response = client.execute(get);

		HttpEntity entity = response.getEntity();
		String body = EntityUtils.toString(entity, Charset.forName("UTF-8"));
		LOGGER.info("Resposta recebida: " + body);

		conferirRespostaSucesso(response.getStatusLine().getStatusCode(), null, null, null);
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
	 * @throws IOException
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
				
				// Quantidade de processos enviados no lote
				Processos processosXML;
				try {
					processosXML = (Processos) unmarshaller.unmarshal(arquivoXML);
				} catch (JAXBException e) {
					throw new DadosInvalidosException("Erro ao tentar analisar a quantidade de processos no arquivo " + arquivoXML + ": " + e.getLocalizedMessage());
				}
				int qtdProcessosXML = processosXML.getProcesso().size();
				
				if (qtdProcessosNegados == qtdProcessosXML) {
					LOGGER.debug("CNJ informou que todos os processos deste arquivo já foram recebidos. Arquivo será marcado como 'enviado'");
					return;
				}
			}
		}
		
		if (statusCode != 200 && statusCode != 201) {
			throw new DadosInvalidosException("Falha ao conectar no Webservice do CNJ (esperado codigo 200 ou 201, recebido codigo " + statusCode + ")");
		}
		if (body != null && body.contains("\"ERRO\"")) {
			throw new DadosInvalidosException("Falha ao conectar no Webservice do CNJ (body retornou 'ERRO')");
		}
		LOGGER.debug("Resposta: " + statusCode);
	}

	/**
	 * Carrega os arquivos XML unificados das instâncias selecionadas (1G e/ou 2G) e envia ao CNJ.
	 * 
	 * @throws IOException
	 * @throws InvalidCredentialsException
	 * @throws DadosInvalidosException 
	 * @throws InterruptedException 
	 * @throws JAXBException 
	 */
	private void enviarXMLsUnificadosAoCNJ() throws IOException, InvalidCredentialsException, DadosInvalidosException, InterruptedException, JAXBException {
		
		// Verifica se deve gerar XML para 2o Grau
		if (Auxiliar.getParametroBooleanConfiguracao(Parametro.gerar_xml_2G)) {
			enviarXMLsUnificadosAoCNJ(2);
		}
		
		// Verifica se deve gerar XML para 1o Grau
		if (Auxiliar.getParametroBooleanConfiguracao(Parametro.gerar_xml_1G)) {
			enviarXMLsUnificadosAoCNJ(1);
		}
	}

	/**
	 * Carrega os arquivos XML unificados da instância selecionada (1G ou 2G) e envia ao CNJ.
	 * 
	 * @param grau
	 * @throws IOException
	 * @throws DadosInvalidosException 
	 * @throws InterruptedException 
	 * @throws JAXBException 
	 */
	private void enviarXMLsUnificadosAoCNJ(int grau) throws IOException, DadosInvalidosException, InterruptedException, JAXBException {
		Auxiliar.prepararPastaDeSaida();
		File pastaXMLsParaEnvio;
		if (Auxiliar.deveMontarLotesDeProcessos()) {
			pastaXMLsParaEnvio = Auxiliar.getPastaXMLsUnificados(grau);
		} else {
			pastaXMLsParaEnvio = Auxiliar.getPastaXMLsIndividuais(grau);
		}
		
		// Prepara objetos para LER os arquivos XML e analisar a quantidade de processos dentro de cada um
		JAXBContext jaxbContext = JAXBContext.newInstance(Processos.class);
		final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		
		
		// Monta a URL para enviar processos ao CNJ.
		// Exemplo de URL: https://wwwh.cnj.jus.br/selo-integracao-web/v1/processos/G2
		final String url = Auxiliar.getParametroConfiguracao(Parametro.url_webservice_cnj, true) + "/G" + grau;
		LOGGER.info("URL onde os arquivos serão enviados: " + url);
		
		// Objeto que fará o envio dos arquivos em várias threads
		int numeroThreads = Auxiliar.getParametroInteiroConfiguracao(Parametro.numero_threads_simultaneas, 1);
		LOGGER.info("Iniciando o envio de arquivos utilizando " + numeroThreads + " thread(s)");
		ProdutorConsumidorMultiThread<File> enviarMultiThread = new ProdutorConsumidorMultiThread<File>(100, numeroThreads, Thread.NORM_PRIORITY) {

			@Override
			public void consumir(File arquivo) {
				
				Auxiliar.prepararThreadLog();
				LOGGER.trace("* Enviando arquivo " + arquivo + "...");
				
				HttpPost post = new HttpPost(url);
				adicionarCabecalhoAutenticacao(post);
				
				// Prepara um request com Multipart
				HttpEntity entity = MultipartEntityBuilder.create().addBinaryBody("file", arquivo).build();
				post.setEntity(entity);
				
				String body = "";
				try {
					try {
						// Executa o POST
						HttpResponse response = client.execute(post);
			
						HttpEntity result = response.getEntity();
						body = EntityUtils.toString(result, Charset.forName("UTF-8"));
						LOGGER.trace("  * Resposta: " + body);
						conferirRespostaSucesso(response.getStatusLine().getStatusCode(), body, jaxbUnmarshaller, arquivo);
						LOGGER.trace("  * Arquivo enviado!");
						LOGGER.info("* Arquivo enviado com sucesso: " + arquivo + " / Resposta: " + body);
						marcarArquivoComoEnviado(arquivo);
					} catch (IOException ex) {
						throw new DadosInvalidosException(ex.getLocalizedMessage());
					}
				} catch (DadosInvalidosException ex) {
					LOGGER.error("* Erro ao enviar arquivo: " + arquivo + " / Resposta: " + body + " / Erro: " + ex.getLocalizedMessage());
				}
			}
		};
		
		LOGGER.info("Enviando todos os arquivos da pasta '" + pastaXMLsParaEnvio.getAbsolutePath() + "' via 'API REST' do CNJ.");
		enviarArquivoRecursivamente(pastaXMLsParaEnvio, url, enviarMultiThread);
		
		LOGGER.info("Aguardando término das threads de envio...");
		enviarMultiThread.aguardarTermino();
		LOGGER.info("Threads de envio terminadas!");
	}

	private void enviarArquivoRecursivamente(File arquivoPasta, String url, ProdutorConsumidorMultiThread<File> enviarMultiThread) throws IOException, DadosInvalidosException, InterruptedException {
		
		if (!arquivoPasta.isDirectory()) {
			throw new DadosInvalidosException("Pasta não existe (" + arquivoPasta + ") talvez falte executar tarefas anteriores.");
		}
		File[] arquivos = arquivoPasta.listFiles();
		Arrays.sort(arquivos);
		for (File filho: arquivos) {
			
			if (filho.isDirectory()) {
				enviarArquivoRecursivamente(filho, url, enviarMultiThread);
				
			} else {
				
				if (deveEnviarArquivo(filho)) {
					enviarMultiThread.produzir(filho);
				}
			}
		}
	}

	private boolean deveEnviarArquivo(File arquivo) {
		
		// Envia somente arquivos XML
		if (!arquivo.getName().toUpperCase().endsWith(".XML")) {
			return false;
		}
		
		// Não envia arquivos que já foram enviados NESTA REMESSA
		File confirmacaoEnvio = new File(arquivo.getAbsolutePath() + ".enviado");
		if (confirmacaoEnvio.exists()) {
			LOGGER.debug("Arquivo já foi enviado anteriormente: " + arquivo);
			return false;
		}
		
		return true;
	}
	
	private void marcarArquivoComoEnviado(File arquivo) {
		File confirmacaoEnvio = new File(arquivo.getAbsolutePath() + ".enviado");
		try {
			confirmacaoEnvio.createNewFile();
		} catch (IOException ex) {
			LOGGER.warn("Não foi possível marcar arquivo como enviado: " + arquivo, ex);
		}
	}
}
