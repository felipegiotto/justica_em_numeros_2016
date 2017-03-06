package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.tasks.Op_X_OperacaoCompleta;

/**
 * Classe utilizada por {@link Op_X_OperacaoCompleta}, para monitorar os arquivos gravados pela JAR do CNJ (replicacao-client)
 * e efetuar o backup automático.
 * 
 * @author felipe.giotto@trt4.jus.br
 *
 */
public class ThreadBackupArquivosCNJ extends Thread {

	private static final Logger LOGGER = LogManager.getLogger(ThreadBackupArquivosCNJ.class);

	// Pasta de onde os arquivos serão lidos
	private File pastaOrigem;

	// Pasta onde os arquivos serão gravados
	private File pastaDestino;

	public ThreadBackupArquivosCNJ(File pastaOrigem, File pastaDestino) throws IOException {
		this.pastaOrigem = pastaOrigem;
		this.pastaDestino = pastaDestino;

		if (!pastaOrigem.isDirectory()) {
			throw new IOException("Pasta deveria existir: " + pastaOrigem);
		}
		if (!pastaDestino.isDirectory()) {
			throw new IOException("Pasta deveria existir: " + pastaDestino);
		}
	}

	@Override
	public void run() {
		super.run();
		Auxiliar.prepararPastaDeSaida();

		while (!isInterrupted()) {

			// Verifica se algum arquivo novo foi gravado na pasta
			efetuarBackupIncrementalCompleto();

			// Aguarda um intervalo antes de tentar novamente
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				break;
			}
		}

		// Depois que thread terminou, faz backup mais uma vez, para garantir
		efetuarBackupIncrementalCompleto();
	}

	/**
	 * Verifica todas as subpastas geradas pelo "replicacao-client" do CNJ e efetua backup
	 * dos arquivos necessários.
	 */
	private void efetuarBackupIncrementalCompleto() {
		efetuarBackupIncremental("convertidos", ".json");
		efetuarBackupIncremental("enviados", ".zip");
		efetuarBackupIncremental("erros", null);
		efetuarBackupIncremental("recibos", null);
		// efetuarBackupIncremental("validados"); // Pasta 'validados' não precisa fazer backup, pois serão os mesmos que já foram copiados.
	}

	/**
	 * Lê todos os arquivos da subpasta "nomePasta", dentro da "pastaOrigem", e grava os que foram
	 * alterados na subpasta de mesmo nome em "pastaDestino".
	 * 
	 * @param nomePasta : nome da subpasta que será identificada dentro de "pastaOrigem"
	 * @param sufixoArquivos : Se for informado, efetua backup somente os arquivos com este sufixo (extensão).
	 *                         Se for nulo, efetua backup de TODOS os arquivos na pasta.
	 *                         Isso é útil pois a JAR do CNJ gera os arquivos ZIP, antes de ser enviados ao FTP,
	 *                         na pasta "convertidos", para só depois movê-los para a pasta "enviados". Esse parâmetro
	 *                         permite ignorar os arquivos ZIP da pasta "convertidos".
	 */
	private void efetuarBackupIncremental(String nomePasta, String sufixoArquivos) {

		// Prepara as pastas de origem e destino
		File origem = new File(pastaOrigem, nomePasta);
		File destino = new File(pastaDestino, nomePasta);
		if (!origem.isDirectory()) {
			return;
		}
		if (!destino.isDirectory()) {
			destino.mkdirs();
		}

		// Localiza todos os arquivos na pasta de origem
		for (File arquivoOrigem: origem.listFiles()) {

			// Filtra somente determinados tipos de arquivo, quando o parâmetro "sufixoArquivos" existir. 
			if (sufixoArquivos == null || arquivoOrigem.getName().toUpperCase().endsWith(sufixoArquivos.toUpperCase())) {

				// Verifica se o arquivo de destino existe e se o arquivo de origem foi alterado
				File arquivoDestino = new File(destino, arquivoOrigem.getName());
				if (!arquivoDestino.exists() || arquivoDestino.length() != arquivoOrigem.length()) {

					// Efetua backup do arquivo de origem
					LOGGER.info("Efetuando backup do arquivo " + arquivoOrigem);
					try {
						FileUtils.copyFile(arquivoOrigem, arquivoDestino);
					} catch (IOException e) {
						LOGGER.error("Não foi possível fazer backup do arquivo " + arquivoOrigem, e);
					}
				}
			}
		}
	}
}
