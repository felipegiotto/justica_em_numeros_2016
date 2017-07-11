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

import javax.net.ssl.SSLContext;

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

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DadosInvalidosException;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Parametro;

/**
 * Chama os webservices do CNJ, enviando os XMLs que foram gerados pela classe {@link Op_3_UnificaArquivosXML}.
 * 
 * @author felipe.giotto@trt4.jus.br
 */
public class Op_4_ValidaEnviaArquivosCNJ {
	
	private static final Logger LOGGER = LogManager.getLogger(Op_4_ValidaEnviaArquivosCNJ.class);
	private CloseableHttpClient client;
	private String authHeader;
	
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

		conferirRespostaSucesso(response.getStatusLine().getStatusCode());
	}

	/**
	 * Adiciona o header "Authorization", informando autenticação "Basic", conforme documento "API REST.pdf"
	 * @param get
	 */
	private void adicionarCabecalhoAutenticacao(HttpRequestBase get) {
		get.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
	}

	/**
	 * Confere se a requisição HTTP teve reposta 200 (SUCCESS)
	 * 
	 * @param statusCode
	 * @throws IOException
	 */
	private void conferirRespostaSucesso(int statusCode) throws DadosInvalidosException {
		if (statusCode != 200) {
			throw new DadosInvalidosException("Falha ao conectar no Webservice do CNJ (esperado codigo 200, recebido codigo " + statusCode + ")");
		}
	}

	/**
	 * Carrega os arquivos XML unificados das instâncias selecionadas (1G e/ou 2G) e envia ao CNJ.
	 * 
	 * @throws IOException
	 * @throws InvalidCredentialsException
	 */
	private void enviarXMLsUnificadosAoCNJ() throws IOException, InvalidCredentialsException {
		
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
	 */
	private void enviarXMLsUnificadosAoCNJ(int grau) throws IOException {
		Auxiliar.prepararPastaDeSaida();
		File pastaXMLsUnificados = Auxiliar.getPastaXMLsUnificados(grau);
		
		LOGGER.info("Enviando todos os arquivos da pasta '" + pastaXMLsUnificados.getAbsolutePath() + "' via 'API REST' do CNJ.");
		File[] arquivos = pastaXMLsUnificados.listFiles();
		Arrays.sort(arquivos);
		for (File arquivoXML: arquivos) {
			LOGGER.info("* Enviando arquivo " + arquivoXML + "...");
			
			// Exemplo de URL: https://wwwh.cnj.jus.br/selo-integracao-web/v1/processos/G2
			String url = Auxiliar.getParametroConfiguracao(Parametro.url_webservice_cnj, true) + "/G" + grau;
			HttpPost post = new HttpPost(url);
			adicionarCabecalhoAutenticacao(post);
			
			// Prepara um request com Multipart
			HttpEntity entity = MultipartEntityBuilder
				    .create()
				    .addBinaryBody("file", arquivoXML)
				    .build();
			post.setEntity(entity);
			
			String body;
			try {
				
				// Executa o POST
				HttpResponse response = client.execute(post);
	
				HttpEntity result = response.getEntity();
				body = EntityUtils.toString(result, Charset.forName("UTF-8"));
				LOGGER.debug("  * Resposta: " + body);
				conferirRespostaSucesso(response.getStatusLine().getStatusCode());
				LOGGER.debug("  * Arquivo enviado!");
			} catch (DadosInvalidosException ex) {
				LOGGER.error("  * Erro: " + ex.getLocalizedMessage());
			}
		}
	}
}
