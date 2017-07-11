package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DadosInvalidosException;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Parametro;
import br.jus.trt4.justica_em_numeros_2016.tabelas_cnj.AnalisaServentiasCNJ;

/**
 * Prototipo de rotina de envio completo dos dados ao CNJ
 * 
 * Esta classe seguirá o mais próximo possível das instruções descritas no arquivo CHECKLIST_RESUMO.txt,
 * executando as operações necessárias em sequência.
 * 
 * Se algum erro ocorrer, a operação será abortada, mas posteriormente poderá continuar da etapa em que parou,
 * reexecutando a classe. O controle das operações já executadas ficará no arquivo "operacao_atual.dat", dentro
 * da pasta "output/<TIPO_CARGA_XML>_backup_operacao_completa"
 * 
 * @author felipe.giotto@trt4.jus.br
 */
public class Op_X_OperacaoCompleta {

	// Enum com todas as operações que serão executadas.
	public enum ControleOperacoes {
	    
		OP_0_CRIACAO_PASTA_OUTPUT          (50),
		OP_1_BAIXAR_LISTA                  (100),
		OP_1_CONFERIR_SERVENTIAS           (150),
		OP_2_GERAR_XMLS_INDIVIDUAIS        (200),
		OP_2_BACKUP_ARQUIVOS_INDIVIDUAIS   (250),
		OP_3_UNIFICA_ARQUIVOS_XML          (300), 
		OP_3_BACKUP_XMLS_UNIFICADOS        (350), 
		OP_4_VALIDA_ENVIA_ARQUIVOS_CNJ     (400), 
		OP_9_ULTIMOS_BACKUPS               (900),
		OP_9_COMPACTAR_PASTA_BACKUP        (950);
		
	    private int ordem;
	    
	    ControleOperacoes(int ordem) {
	        this.ordem = ordem;
	    }
	    
	    public int getOrdem() {
	        return this.ordem;
	    }
	}

	private static final Logger LOGGER = LogManager.getLogger(Op_X_OperacaoCompleta.class);

	private interface Operacao {
		void run() throws Exception;
	}
	
	File pastaOutput;
	File pastaBackup;

	public static void main(String[] args) throws Exception {
		try {
			Op_X_OperacaoCompleta operacaoCompleta = new Op_X_OperacaoCompleta();
			operacaoCompleta.executarOperacaoCompleta();
		} catch (Exception ex) {
			LOGGER.error("Op_X_OperacaoCompleta abortada", ex);
		}
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
		executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes.OP_0_CRIACAO_PASTA_OUTPUT, new Operacao() {

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
		executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes.OP_1_BAIXAR_LISTA, new Operacao() {

			@Override
			public void run() throws SQLException, IOException {
				Op_1_BaixaListaDeNumerosDeProcessos.main(null);
			}
		});

		// Passo extra: Conferir lista de serventias CNJ
		executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes.OP_1_CONFERIR_SERVENTIAS, new Operacao() {
			
			@Override
			public void run() throws Exception {
				AnalisaServentiasCNJ analisaServentiasCNJ = new AnalisaServentiasCNJ();
				if (analisaServentiasCNJ.diagnosticarServentiasInexistentes()) {
					
					LOGGER.warn("Pressione ENTER ou aguarde 2 minutos para que a geração dos XMLs continue. Se você preferir, aborte este script e corrija o arquivo de serventias");
					Auxiliar.aguardaUsuarioApertarENTERComTimeout(120);
				}
			}
		});
		
		// CHECKLIST: 5. Execute a classe "Op_2_GeraXMLsIndividuais"
		executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes.OP_2_GERAR_XMLS_INDIVIDUAIS, new Operacao() {

			@Override
			public void run() throws SQLException, Exception {
				Op_2_GeraXMLsIndividuais.main(null);
			}
		});

		// CHECKLIST: 6. Efetue backup dos arquivos gerados pela rotina anterior (pastas "output/.../1g" e "output/.../2g")
		executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes.OP_2_BACKUP_ARQUIVOS_INDIVIDUAIS, new Operacao() {

			@Override
			public void run() throws IOException {
				efetuarBackupDeArquivosIndividuais(1);
				efetuarBackupDeArquivosIndividuais(2);
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
			
			
		});

		// CHECKLIST: 7. Execute a classe "Op_3_UnificaArquivosXML"
		executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes.OP_3_UNIFICA_ARQUIVOS_XML, new Operacao() {

			@Override
			public void run() throws Exception {
				Op_3_UnificaArquivosXML.main(null);
			}
		});

		// Backup dos XMLs unificados
		executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes.OP_3_BACKUP_XMLS_UNIFICADOS, new Operacao() {

			@Override
			public void run() throws IOException {
				efeguarBackupDeArquivosUnificados(1);
				efeguarBackupDeArquivosUnificados(2);
			}

			private void efeguarBackupDeArquivosUnificados(int grau) throws IOException {
				File pastaOutputBackup = new File(getPastaOutputBackup(), "G" + grau);

				// Backup da pasta xmls_unificados
				File pastaXMLs = Auxiliar.getPastaXMLsUnificados(grau);
				if (pastaXMLs.exists()) {
					LOGGER.info("Efetuando backup dos XMLs unificados da pasta " + pastaXMLs);
					File pastaBackupXMLs = new File(pastaOutputBackup, pastaXMLs.getName());
					FileUtils.copyDirectory(pastaXMLs, pastaBackupXMLs);
				}
			}
		});

		// CHECKLIST: 9. Execute a classe "Op_4_ValidaEnviaArquivosCNJ", ...
		executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes.OP_4_VALIDA_ENVIA_ARQUIVOS_CNJ, new Operacao() {
			
			@Override
			public void run() throws Exception {
				
				// Envia os XMLs ao CNJ
				Op_4_ValidaEnviaArquivosCNJ.main(null);
			}
		});
		
		// CHECKLIST: 12. Efetue backup dos seguintes dados, para referência futura: ...
		executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes.OP_9_ULTIMOS_BACKUPS, new Operacao() {
			
			@Override
			public void run() throws Exception {
				
				// Backup do arquivo de configurações
				LOGGER.info("Efetuando backup do arquivo de configurações...");
				try (Scanner scanner = new Scanner(Auxiliar.arquivoConfiguracoes, "ISO-8859-1")) {
					File arquivoConfiguracaoBackup = new File(pastaBackup, Auxiliar.arquivoConfiguracoes.getName());
					try (FileWriter fw = new FileWriter(arquivoConfiguracaoBackup)) {
						while (scanner.hasNextLine()) {
							String line = scanner.nextLine();
							if (line.contains("url_jdbc_")) {
								fw.append("# (linha omitida por questões de segurança)\n");
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
				
				// Backup do arquivo de serventias
				LOGGER.info("Efetuando backup do arquivo de serventias CNJ...");
				File arquivoServentiasOrigem = AnalisaServentiasCNJ.getArquivoServentias();
				File arquivoServentiasDestino = new File(pastaBackup, arquivoServentiasOrigem.getName());
				FileUtils.copyFile(arquivoServentiasOrigem, arquivoServentiasDestino);
				
				// Backup da pasta "log"
				LOGGER.info("Efetuando backup da pasta de logs...");
				File pastaLogOutput = new File(pastaOutput, "log");
				File pastaLogBackup = new File(pastaBackup, "log");
				FileUtils.copyDirectory(pastaLogOutput, pastaLogBackup);
			}
		});
		
		// CHECKLIST: 14. SOMENTE TRT4: Compacte toda a pasta de backup
		executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes.OP_9_COMPACTAR_PASTA_BACKUP, new Operacao() {
			
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
				LOGGER.info("Arquivo ZIP gerado com todas as informações desta extração: '" + arquivoZipBackupDefinitivo + "'");
			}
		});
		
		LOGGER.info("Operação completa realizada com sucesso!");
		LOGGER.info("Os dados referentes a este envio ao CNJ, inclusive os backup, foram gravados na pasta '" + pastaBackup + "'.");
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
	private void executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes controleOperacoes, Operacao operacao) throws Exception {

		String descricaoOperacao = controleOperacoes + " (" + controleOperacoes.getOrdem() + ")";
		if (getUltimaOperacaoExecutada() < controleOperacoes.getOrdem()) {

			LOGGER.info("Iniciando operação " + descricaoOperacao + "...");
			
			try {
				operacao.run();
			} catch (Exception ex) {
				LOGGER.error("Erro na operação " + descricaoOperacao + ": " + ex.getLocalizedMessage(), ex);
				throw ex;
			}
			LOGGER.info("Operação " + descricaoOperacao + " concluída!");

			// Se algum problema foi identificado, aborta.
			if (DadosInvalidosException.getQtdErros() > 0) {
				throw new Exception("Operação " + descricaoOperacao + " abortada!");
			}

			setUltimaOperacaoExecutada(controleOperacoes.getOrdem());

		} else {
			LOGGER.info("Operação " + descricaoOperacao + " já foi executada!");
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
		File outputRoot = Auxiliar.getPastaOutputRaiz();
		File pastaBackup = new File(outputRoot, tipoCarga + "_backup_operacao_completa");
		pastaBackup.mkdirs();
		return pastaBackup;
	}

}
