package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.tasks.Op_Y_OperacaoFluxoContinuo;

/**
 * Exibe uma interface de acompanhamento do progresso da operação completa (geração, envio e conferência no CNJ)
 *
 * Fonte: https://medium.com/@ssaurel/create-a-simple-http-web-server-in-java-3fc12b29d5fd
 *
 * @author felipe.giotto@trt4.jus.br
 */
public class HttpServerStatus implements AutoCloseable {

	private static final Logger LOGGER = LogManager.getLogger(HttpServerStatus.class);
	private static File NULL_FILE = new File((System.getProperty("os.name").startsWith("Windows") ? "NUL" : "/dev/null"));
	private boolean aberto = true;
	private ServerSocket serverConnect;
	
	/**
	 * Inicia um socket, buscando uma porta HTTP livre.
	 *
	 * @param operacao
	 * @throws IOException
	 */
	public HttpServerStatus(Op_Y_OperacaoFluxoContinuo operacao) throws IOException {
		serverConnect = criarServerSocket();
		
		new Thread(() -> {
			try {
				while (aberto) {
					HttpServerRequestHandler handler = new HttpServerRequestHandler(serverConnect.accept(), operacao);
					new Thread(handler).start();
				}
			} catch (IOException ex) {
				// Nada a fazer
			}
		}).start();
	}
	
	public void close() throws IOException {
		this.aberto = false;
		this.serverConnect.close();
	}
	
	/**
	 * Tenta abrir o navegador do usuário para acompanhar o envio, mas se não conseguir não faz nada.
	 *
	 */
	public void abrirNavegador() {
		
		String url = "http://localhost:" + serverConnect.getLocalPort();
		LOGGER.info("Servidor Web iniciado em " + url);
		
		ProcessBuilder processBuilder = new ProcessBuilder(new String[] {"firefox", url});
		processBuilder.redirectOutput(NULL_FILE);
		processBuilder.redirectError(NULL_FILE);
		try {
			processBuilder.start();
		} catch (IOException e) {
			LOGGER.warn("Não foi possível abrir o navegador para acompanhar a operação");
		}
	}

	/**
	 * Busca uma porta livre para iniciar o socket.
	 *
	 * @return
	 * @throws IOException
	 */
	private ServerSocket criarServerSocket() throws IOException {
		for (int port = 9090; port < 9100; port++) {
			try {
				ServerSocket serverSocket = new ServerSocket(port);
				return serverSocket;
				
			} catch (IOException ex) {
				
			}
		}
		throw new IOException("Não há portas disponíveis para exibir interface de status web");
	}
}
