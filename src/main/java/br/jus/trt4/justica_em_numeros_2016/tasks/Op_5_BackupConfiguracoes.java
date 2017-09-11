package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.tabelas_cnj.AnalisaServentiasCNJ;

/**
 * Efetua backup de arquivos de configuração que foram utilizados neste envio, para caso seja necessário
 * fazer alguma consulta futura
 * 
 * @author felipe.giotto@trt4.jus.br
 */
public class Op_5_BackupConfiguracoes {

	private static final Logger LOGGER = LogManager.getLogger(Op_5_BackupConfiguracoes.class);
	
	public static void main(String[] args) throws IOException {
		efetuarBackupArquivosDeConfiguracao();
	}
	
	public static void efetuarBackupArquivosDeConfiguracao() throws IOException {
		
		File pastaOutput = Auxiliar.prepararPastaDeSaida();
		
		// Backup do arquivo de configurações
		LOGGER.info("Efetuando backup do arquivo de configurações...");
		try (Scanner scanner = new Scanner(Auxiliar.getArquivoconfiguracoes(), "ISO-8859-1")) {
			File arquivoConfiguracaoBackup = new File(pastaOutput, Auxiliar.getArquivoconfiguracoes().getName());
			try (FileWriter fw = new FileWriter(arquivoConfiguracaoBackup)) {
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					
					// Se for uma linha que contém alguma senha, não grava no backup
					if (line.contains("password")) {
						
						// Mostra somente o "nome" da propriedade, omitindo seu valor
						int posicao = line.indexOf('=');
						fw.append(line.substring(0, posicao) + "=(linha omitida por questões de segurança)\n");
						
					} else {
						fw.append(line + "\n");
					}
				}
			}
		}
		
		// Registrando último commit do git
		LOGGER.info("Efetuando backup do último commit do git...");
		File arquivoGit = new File(pastaOutput, "informacoes_git.txt");
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
		File arquivoServentiasDestino = new File(pastaOutput, arquivoServentiasOrigem.getName());
		FileUtils.copyFile(arquivoServentiasOrigem, arquivoServentiasDestino);
		
		LOGGER.info("FIM!");
	}
}
