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
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
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
		Op_4_ValidaEnviaArquivosCNJ operacao = new Op_4_ValidaEnviaArquivosCNJ();
		operacao.testarConexaoComCNJ();
		operacao.enviarXMLsUnificadosAoCNJ();
		LOGGER.info("Fim!");
	}

	public Op_4_ValidaEnviaArquivosCNJ() throws Exception {
		
        // SSL
        SSLContext sslcontext = getSSLContext();
        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext,
                SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
        
        // TODO! Configurar proxy via parâmetro!
        // Proxy
		HttpHost proxy = new HttpHost("127.0.0.1", 3129);
		DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
        
        // Autenticação
		String usuario = Auxiliar.getParametroConfiguracao(Parametro.sigla_tribunal, true);
		String senha = Auxiliar.pedirParaUsuarioDigitarSenha("Digite a senha do usuário '" + usuario + "' para acessar os webservices do CNJ");
		String autenticacao = usuario + ":" + senha;
		byte[] encodedAuth = Base64.encodeBase64(autenticacao.getBytes(Charset.forName("UTF-8")));
		authHeader = "Basic " + new String(encodedAuth);
		
        client = HttpClients.custom().setSSLSocketFactory(factory).setRoutePlanner(routePlanner).build();
	}
	
    private static SSLContext getSSLContext() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException {
    	
    	// TODO!!! Documentar e gerar keystore corretamente
        KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream instream = new FileInputStream(new File("src/main/resources/certificados_rest_cnj/keystore/cnj.keystore"));
        try {
            trustStore.load(instream, "storepasscnj".toCharArray());
        } finally {
            instream.close();
        }
        return SSLContexts.custom().loadTrustMaterial(trustStore).build();
    }	
	
	private void testarConexaoComCNJ() throws ClientProtocolException, IOException, InvalidCredentialsException {
		
		LOGGER.info("Testando conexão com o webservice do CNJ...");
		
		HttpGet get = new HttpGet(Auxiliar.getParametroConfiguracao(Parametro.url_webservice_cnj, true));
		adicionarCabecalhoAutenticacao(get);
		
		HttpResponse response = client.execute(get);

		HttpEntity entity = response.getEntity();
		String body = EntityUtils.toString(entity, Charset.forName("UTF-8"));
		LOGGER.info("Resposta recebida: " + body);

		conferirRespostaSucesso(response.getStatusLine().getStatusCode());
	}

	private void adicionarCabecalhoAutenticacao(HttpRequestBase get) {
		get.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
	}

	private void conferirRespostaSucesso(int statusCode) throws IOException {
		if (statusCode != 200) {
			throw new IOException("Falha ao conectar no Webservice do CNJ (esperado codigo 200, recebido codigo " + statusCode + ")");
		}
	}

	private void enviarXMLsUnificadosAoCNJ() throws ClientProtocolException, IOException, InvalidCredentialsException {
		
		// Verifica se deve gerar XML para 2o Grau
		if (Auxiliar.getParametroBooleanConfiguracao(Parametro.gerar_xml_2G)) {
			enviarXMLsUnificadosAoCNJ(2);
		}
		
		// Verifica se deve gerar XML para 1o Grau
		if (Auxiliar.getParametroBooleanConfiguracao(Parametro.gerar_xml_1G)) {
			enviarXMLsUnificadosAoCNJ(1);
		}
	}

	private void enviarXMLsUnificadosAoCNJ(int grau) throws ClientProtocolException, IOException {
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
			} catch (IOException ex) {
				LOGGER.error("  * Erro!");
			}
		}
	}
}
