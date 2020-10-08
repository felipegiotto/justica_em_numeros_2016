package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.enums.Parametro;

@SuppressWarnings("deprecation")
public class HttpUtil {

	private static final Logger LOGGER = LogManager.getLogger(HttpUtil.class);
	private static String authHeaderCache = null;

	public synchronized static CloseableHttpClient criarNovoHTTPClientComAutenticacaoCNJ() {
		LOGGER.info("Criando novo CloseableHttpClient");

		// Objeto que criará cada request a ser feito ao CNJ
		HttpClientBuilder httpClientBuilder = HttpClients.custom();

		// Aumenta o limite de conexoes, para permitir acesso multi-thread
		int numeroThreads = Auxiliar.getParametroInteiroConfiguracao(Parametro.numero_threads_simultaneas, 1);
		httpClientBuilder.setMaxConnPerRoute(numeroThreads * 2);
		httpClientBuilder.setMaxConnTotal(numeroThreads * 2);

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

		// Autenticação do tribunal junto ao CNJ
		if (authHeaderCache == null) {
			String usuario = Auxiliar.getParametroConfiguracao(Parametro.sigla_tribunal, true);
			String senha = Auxiliar.getParametroConfiguracao(Parametro.password_tribunal, true);
			String autenticacao = usuario + ":" + senha;
			byte[] encodedAuth = Base64.encodeBase64(autenticacao.getBytes(Charset.forName("UTF-8")));
			authHeaderCache = "Basic " + new String(encodedAuth);
		}

		// Cria um novo HttpClient para acessar o CNJ
		return httpClientBuilder.build();
	}

	/**
	 * Adiciona o header "Authorization", informando autenticação "Basic", conforme
	 * documento "API REST.pdf"
	 * 
	 * @param get
	 */
	public static void adicionarCabecalhoAutenticacao(HttpRequestBase get) {
		get.setHeader(HttpHeaders.AUTHORIZATION, authHeaderCache);
	}

	/**
	 * Carrega a keystore com os certificados do CNJ
	 * 
	 * @return
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws IOException
	 * @throws KeyManagementException
	 */
	private static SSLContext getSSLContext() {
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			FileInputStream instream = new FileInputStream(
					new File("src/main/resources/certificados_rest_cnj/keystore/cnj.keystore"));
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
}
