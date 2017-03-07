package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DadosInvalidosException;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Parametro;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ThreadBackupArquivosCNJ;
import br.jus.trt4.justica_em_numeros_2016.tabelas_cnj.AnalisaServentiasCNJ;

/**
 * Prototipo de rotina de envio completo dos dados ao CNJ
 * 
 * Esta classe seguirá o mais próximo possível das instruções descritas no arquivo CHECKLIST_RESUMO.txt,
 * executando as operações necessárias em sequência.
 * 
 * Se algum erro ocorrer, a operação será abortada, mas posteriormente poderá continuar da etapa em que parou,
 * reexecutando a classe. O controle das operações já executadas ficará no arquivo "operacao_atual.dat", dentro
 * da pasta "backups/?????"
 * 
 * @author felipe.giotto@trt4.jus.br
 */
public class Op_X_OperacaoCompleta {

	// Constantes com todas as operações necessárias.
	private static final int OP_0_CRIACAO_PASTA_OUTPUT = 50;
	private static final int OP_1_BAIXAR_LISTA = 100;
	private static final int OP_2_GERAR_XMLS_INDIVIDUAIS = 200;
	private static final int OP_2_BACKUP_ARQUIVOS_INDIVIDUAIS = 250;
	private static final int OP_3_UNIFICA_ARQUIVOS_XML = 300;
	private static final int OP_3_BACKUP_XMLS_UNIFICADOS = 350;
	private static final int OP_4_VALIDA_ENVIA_ARQUIVOS_CNJ = 400;
	private static final int OP_9_ULTIMOS_BACKUPS = 900;
	private static final int OP_9_COMPACTAR_PASTA_BACKUP = 950;
	private static final Logger LOGGER = LogManager.getLogger(Op_X_OperacaoCompleta.class);

	private interface Operacao {
		void run() throws Exception;
	}
	File pastaOutput;
	File pastaBackup;

	public static void main(String[] args) throws Exception {
		Op_X_OperacaoCompleta operacaoCompleta = new Op_X_OperacaoCompleta();
		operacaoCompleta.executarOperacaoCompleta();
	}

	public Op_X_OperacaoCompleta() {
		this.pastaOutput = Auxiliar.prepararPastaDeSaida();
		this.pastaBackup = criarPastaParaBackup();
		
		LOGGER.info("Os dados referentes a este envio ao CNJ, inclusive os backup, serão gravados na pasta '" + pastaBackup + "'.");
	}

	/**
	 * Método que executa todas as operações que ainda estão pendentes.
	 * 
	 * @throws Exception
	 */
	private void executarOperacaoCompleta() throws Exception {

		// CHECKLIST: 2. Verifique se existe uma pasta "output"
		executaOperacaoSeAindaNaoFoiExecutada(OP_0_CRIACAO_PASTA_OUTPUT, new Operacao() {

			@Override
			public void run() {
				File pasta1G = new File(pastaOutput, "1g");
				if (pasta1G.isDirectory()) {
					throw new RuntimeException("A pasta '" + pasta1G + "' já existe! Por questões de segurança, a operação completa deve ser executada desde o início, antes mesmo da criação desta pasta de saída! Exclua-a (fazendo backup, se necessário) e tente novamente.");
				}
				File pasta2G = new File(pastaOutput, "2g");
				if (pasta2G.isDirectory()) {
					throw new RuntimeException("A pasta '" + pasta1G + "' já existe! Por questões de segurança, a operação completa deve ser executada desde o início, antes mesmo da criação desta pasta de saída! Exclua-a (fazendo backup, se necessário) e tente novamente.");
				}
			}
		});

		// CHECKLIST: 4. Execute a classe "Op_1_BaixaListaDeNumerosDeProcessos".
		executaOperacaoSeAindaNaoFoiExecutada(OP_1_BAIXAR_LISTA, new Operacao() {

			@Override
			public void run() throws SQLException, IOException {
				Op_1_BaixaListaDeNumerosDeProcessos.main(null);
			}
		});

		// CHECKLIST: 5. Execute a classe "Op_2_GeraXMLsIndividuais"
		executaOperacaoSeAindaNaoFoiExecutada(OP_2_GERAR_XMLS_INDIVIDUAIS, new Operacao() {

			@Override
			public void run() throws SQLException, Exception {
				Op_2_GeraXMLsIndividuais.main(null);
			}
		});

		// CHECKLIST: 6. Efetue backup dos arquivos gerados pela rotina anterior (pastas "output/1g" e "output/2g")
		executaOperacaoSeAindaNaoFoiExecutada(OP_2_BACKUP_ARQUIVOS_INDIVIDUAIS, new Operacao() {

			@Override
			public void run() throws IOException {
				efetuarBackupDeArquivosIndividuais(1);
				efetuarBackupDeArquivosIndividuais(2);
			}

		});

		// CHECKLIST: 7. Execute a classe "Op_3_UnificaArquivosXML"
		executaOperacaoSeAindaNaoFoiExecutada(OP_3_UNIFICA_ARQUIVOS_XML, new Operacao() {

			@Override
			public void run() throws Exception {
				Op_3_UnificaArquivosXML.main(null);
			}
		});

		// Backup dos XMLs unificados
		executaOperacaoSeAindaNaoFoiExecutada(OP_3_BACKUP_XMLS_UNIFICADOS, new Operacao() {

			@Override
			public void run() throws IOException {
				File pastaOutputBackup = getPastaOutputBackup();

				// Backup da pasta xmls_unificados
				File pastaXMLs = Auxiliar.getPastaXMLsUnificados();
				if (pastaXMLs.exists()) {
					LOGGER.info("Efetuando backup dos XMLs unificados");
					File pastaBackupXMLs = new File(pastaOutputBackup, pastaXMLs.getName());
					FileUtils.copyDirectory(pastaXMLs, pastaBackupXMLs);
				}
			}
		});

		// CHECKLIST: 9. Execute a classe "Op_4_ValidaEnviaArquivosCNJ", mas NÃO RESPONDA AS PERGUNTAS efetuadas sem ler as instruções abaixo!
		executaOperacaoSeAindaNaoFoiExecutada(OP_4_VALIDA_ENVIA_ARQUIVOS_CNJ, new Operacao() {
			
			@Override
			public void run() throws Exception {
				
				// Thread que irá monitorar os arquivos gerados pela JAR do CNJ e fazer backup.
				File pastaXMLs = Auxiliar.getPastaXMLsUnificados();
				File pastaBackupXMLs = new File(getPastaOutputBackup(), pastaXMLs.getName());
				ThreadBackupArquivosCNJ threadBackupArquivosCNJ = new ThreadBackupArquivosCNJ(pastaXMLs, pastaBackupXMLs);
				threadBackupArquivosCNJ.start();
				try {
					
					// Chama a JAR do CNJ para validar os arquivos e enviar ao FTP.
					Op_4_ValidaEnviaArquivosCNJ.main(null);
					
				} finally {
					threadBackupArquivosCNJ.finalizar();
					threadBackupArquivosCNJ.join();
				}
				
				// Verificando se todos os arquivos foram processados corretamente pela JAR do CNJ
				int qtdXMLsGerados = pastaBackupXMLs.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.toUpperCase().endsWith(".XML");
					}
				}).length;
				conferirQuantidadeDeArquivosNaPasta(qtdXMLsGerados, new File(pastaBackupXMLs, "convertidos"));
				conferirQuantidadeDeArquivosNaPasta(qtdXMLsGerados, new File(pastaBackupXMLs, "enviados"));
				conferirArquivosEnviadosAoFTPDoCNJ(new File(pastaBackupXMLs, "enviados"));
			}
		});
		
		// CHECKLIST: 12. Efetue backup dos seguintes dados, para referência futura: ...
		executaOperacaoSeAindaNaoFoiExecutada(OP_9_ULTIMOS_BACKUPS, new Operacao() {
			
			@Override
			public void run() throws Exception {
				
				// Backup da JAR do CNJ
				LOGGER.info("Efetuando backup da JAR do CNJ...");
				File arquivoJarCNJOrigem = new File(Auxiliar.getParametroConfiguracao(Parametro.jar_replicacao_nacional_cnj, true));
				if (!arquivoJarCNJOrigem.exists()) {
					throw new IOException("Arquivo não encontrado para fazer backup: " + arquivoJarCNJOrigem);
				}
				File arquivoJarCNJDestino = new File(pastaBackup, arquivoJarCNJOrigem.getName());
				FileUtils.copyFile(arquivoJarCNJOrigem, arquivoJarCNJDestino);
				
				// Backup do arquivo de configurações
				LOGGER.info("Efetuando backup do arquivo de configurações...");
				try (Scanner scanner = new Scanner(Auxiliar.arquivoConfiguracoes, "ISO-8859-1")) {
					File arquivoConfiguracaoBackup = new File(pastaBackup, Auxiliar.arquivoConfiguracoes.getName());
					try (FileWriter fw = new FileWriter(arquivoConfiguracaoBackup)) {
						while (scanner.hasNextLine()) {
							String line = scanner.nextLine();
							if (line.contains("url_jdbc_")) {
								fw.append("# (linha omitida por questões de segurança\n");
							} else {
								fw.append(line + "\n");
							}
						}
					}
				}
				
				// Registrando último commit do git
				LOGGER.info("Efetuando backup do último commit do git...");
				File arquivoGit = new File(pastaBackup, "informacoes_git.txt");
				try (FileOutputStream fos = new FileOutputStream(arquivoGit)) {
					try {
						ProcessBuilder pb = new ProcessBuilder("git", "log", "-n", "1");
						Process p = pb.start();
						IOUtils.copy(p.getInputStream(), fos);
						IOUtils.copy(p.getErrorStream(), fos);
						p.waitFor();
					} catch (Exception ex) {
						String erro = "\n\n\nErro executando comando 'git': " + ex.getLocalizedMessage();
						fos.write(erro.getBytes());
					}
				}
				
				// Backup da pasta "log"
				LOGGER.info("Efetuando backup da pasta de logs...");
				File pastaLogOutput = new File(pastaOutput, "log");
				File pastaLogBackup = new File(pastaBackup, "log");
				FileUtils.copyDirectory(pastaLogOutput, pastaLogBackup);
				
				// Backup do arquivo de serventias
				LOGGER.info("Efetuando backup do arquivo de serventias CNJ...");
				File arquivoServentiasOrigem = AnalisaServentiasCNJ.getArquivoServentias();
				File arquivoServentiasDestino = new File(pastaBackup, arquivoJarCNJOrigem.getName());
				FileUtils.copyFile(arquivoServentiasOrigem, arquivoServentiasDestino);
			}
		});
		
		// CHECKLIST: 14. SOMENTE TRT4: Compacte toda a pasta de backup
		executaOperacaoSeAindaNaoFoiExecutada(OP_9_COMPACTAR_PASTA_BACKUP, new Operacao() {
			
			@Override
			public void run() throws Exception {
				
				// Prepara o ambiente
				File arquivoZipBackupDefinitivo = new File(pastaBackup.getAbsolutePath() + ".zip");
				File arquivoZipBackupTemporario = new File(pastaBackup.getAbsolutePath() + ".zip.tmp");
				arquivoZipBackupTemporario.delete();
				if (arquivoZipBackupDefinitivo.exists()) {
					throw new IOException("O arquivo de saída já existe: " + arquivoZipBackupDefinitivo);
				}
				
				// Compacta em um ZIP temporário
				LOGGER.info("Compactando pasta de backup no arquivo '" + arquivoZipBackupDefinitivo + "'...");
				Auxiliar.compressZipfile(pastaBackup.getAbsolutePath(), arquivoZipBackupTemporario.getAbsolutePath());
				
				// Renomeia para o ZIP definitivo
				FileUtils.moveFile(arquivoZipBackupTemporario, arquivoZipBackupDefinitivo);
			}
		});
		
		LOGGER.info("Operação completa realizada com sucesso!");
		LOGGER.info("Os dados referentes a este envio ao CNJ, inclusive os backup, foram gravados na pasta '" + pastaBackup + "'.");
	}

	/**
	 * Efetua backup da pasta de XMLs individuais de uma determinada instância
	 * 
	 * @param grau
	 * @throws IOException
	 */
	private void efetuarBackupDeArquivosIndividuais(int grau) throws IOException {
		File pastaOutputBackup = getPastaOutputBackup();
		File pastaOrigem = new File(pastaOutput, grau + "g");
		if (pastaOrigem.exists()) {
			File pastaDestino = new File(pastaOutputBackup, pastaOrigem.getName());
			LOGGER.info("Efetuando backup da pasta " + pastaOrigem);
			FileUtils.copyDirectory(pastaOrigem, pastaDestino);
		}
	}

	/**
	 * Conecta no FTP do CNJ e verifica se todos os arquivos ZIP foram enviados corretamente.
	 * 
	 * @param pastaArquivosEnviados : pasta onde estão, localmente, os arquivos que devem estar no servidor.
	 * 
	 * @throws SocketException
	 * @throws IOException
	 */
	private void conferirArquivosEnviadosAoFTPDoCNJ(File pastaArquivosEnviados) throws SocketException, IOException {
		
		// Conecta e autentica no servidor FTP.
		LOGGER.info("Conferindo se todos os arquivos foram enviados ao FTP do CNJ...");
		String servidorFTPCNJ = "ftp.cnj.jus.br";
		String usuarioFTPCNJ = "ftp.repnac";
		String senhaFTPCNJ = Auxiliar.pedirParaUsuarioDigitarSenha("Digite a senha do usuário '" + usuarioFTPCNJ + "' para acessar o servidor '" + servidorFTPCNJ + "'");
		
		LOGGER.info("Conectando no servidor FTP do CNJ...");
		FTPClient ftp = new FTPClient();
		ftp.connect(servidorFTPCNJ);
		ftp.login(usuarioFTPCNJ, senhaFTPCNJ);
		
		// Carrega uma lista com todos os arquivos que estão no FTP
		Map<String, Long> arquivosNoServidor = new HashMap<>();
		for (FTPFile file: ftp.listFiles(Auxiliar.getParametroConfiguracao(Parametro.sigla_tribunal, true))) {
			arquivosNoServidor.put(file.getName(), file.getSize());
		}
		
		// Itera sobre os arquivos locais, verificando se cada um existe no FTP
		for (File arquivoEnviado: pastaArquivosEnviados.listFiles()) {
			String nomeArquivo = arquivoEnviado.getName();
			if (!arquivosNoServidor.containsKey(nomeArquivo)) {
				throw new IOException("O arquivo " + nomeArquivo + " consta como enviado pela JAR do CNJ, mas não está no servidor FTP!");
			}
			Long tamanhoRemoto = arquivosNoServidor.get(nomeArquivo);
			long tamanhoLocal = arquivoEnviado.length();
			if (tamanhoRemoto != tamanhoLocal) {
				throw new IOException("O arquivo " + nomeArquivo + " possui " + tamanhoLocal + " Bytes, mas no servidor FTP do CNJ ele possui " + tamanhoRemoto + " Bytes.");
			}
			
			LOGGER.info("Arquivo presente no FTP: " + arquivoEnviado.getName() + " (" + tamanhoRemoto + " Bytes)");
		}
	}
	
	/**
	 * Confere se a quantidade de arquivos na pasta é a mesma dos XMLs que foram gerados localmente.
	 * 
	 * @param qtdXMLsGerados
	 * @param pasta
	 * 
	 * @throws IOException
	 */
	private void conferirQuantidadeDeArquivosNaPasta(int qtdXMLsGerados, File pasta) throws IOException {
		if (!pasta.isDirectory()) {
			throw new IOException("Pasta não existe: " + pasta);
		}
		
		int qtdXMLsPasta = pasta.listFiles().length;
		if (qtdXMLsGerados != qtdXMLsPasta) {
			throw new IOException("Foram gerados " + qtdXMLsGerados + " arquivos XML unificados, mas há " + qtdXMLsPasta + " na pasta " + pasta + ". Este erro não deve ocorrer depois que o envio ao CNJ for realizado com sucesso!");
		}
	}

	/**
	 * Retorna a pasta onde deve ser realizado o backup da pasta "output" (arquivos gerados)
	 * @return
	 */
	private File getPastaOutputBackup() {
		File pastaOutputBackup = new File(pastaBackup, "output");
		return pastaOutputBackup;
	}

	/**
	 * Verifica se determinada operação já foi executada (a última operação que foi executada está 
	 * no arquivo "operacao_atual.dat" da pasta de backups
	 * 
	 * @param codigoOperacao : número (inteiro) da operação que deve ser executada
	 * @param operacao
	 * 
	 * @throws Exception
	 */
	private void executaOperacaoSeAindaNaoFoiExecutada(int codigoOperacao, Operacao operacao) throws Exception {

		if (getUltimaOperacaoExecutada() < codigoOperacao) {

			LOGGER.info("Iniciando operação " + codigoOperacao + "...");
			operacao.run();

			// Se algum problema foi identificado, aborta.
			if (DadosInvalidosException.getQtdErros() > 0) {
				throw new Exception("Operação abortada!");
			}

			setUltimaOperacaoExecutada(codigoOperacao);

		} else {
			LOGGER.info("Operação " + codigoOperacao + " já foi executada!");
		}
	}

	/**
	 * Lê, do arquivo "operacao_atual.dat", qual a última operação executada.
	 * 
	 * @return
	 */
	private int getUltimaOperacaoExecutada() {
		File arquivoOperacaoAtual = getArquivoOperacaoAtual(pastaBackup);
		try {
			String operacaoAtualString = FileUtils.readFileToString(arquivoOperacaoAtual, Charset.defaultCharset());
			return Integer.parseInt(operacaoAtualString);
		} catch (NumberFormatException | IOException ex) {
			return 0;
		}
	}

	/**
	 * Grava, no arquivo "operacao_atual.dat", o código da última operação que foi executada
	 * @param ultimaOperacaoExecutada
	 * @throws IOException
	 */
	private void setUltimaOperacaoExecutada(int ultimaOperacaoExecutada) throws IOException {
		File arquivoOperacaoAtual = getArquivoOperacaoAtual(pastaBackup);
		try (FileWriter fw = new FileWriter(arquivoOperacaoAtual)) {
			fw.append(Integer.toString(ultimaOperacaoExecutada));
		}
	}

	/**
	 * Indica o arquivo que armazena a última operação executada
	 * 
	 * @param pastaBackup
	 * @return
	 */
	private static File getArquivoOperacaoAtual(File pastaBackup) {
		return new File(pastaBackup, "operacao_atual.dat");
	}

	/**
	 * Prepara uma pasta onde será efetuado backup de toda a operação de envio.
	 * 
	 * @return
	 */
	private static File criarPastaParaBackup() {
		String tipoCarga = Auxiliar.getParametroConfiguracao(Parametro.tipo_carga_xml, true);
		File backupRoot = new File("backup_operacao_completa");
		backupRoot.mkdirs();
		File pastaBackup = new File(backupRoot, tipoCarga);
		pastaBackup.mkdirs();
		return pastaBackup;
	}

}
