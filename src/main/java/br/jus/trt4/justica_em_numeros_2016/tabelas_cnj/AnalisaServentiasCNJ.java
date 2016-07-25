package br.jus.trt4.justica_em_numeros_2016.tabelas_cnj;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;

/**
 * Classe responsável por ler os arquivos de serventia do CNJ (conforme parâmetro arquivo_serventias_cnj)
 * e auxiliar no preenchimento correto dos XMLs.
 * 
 * Objeto que fará o "de/para" dos OJ e OJC do PJe para as serventias do CNJ
 * 
 * @author fgiotto
 */
public class AnalisaServentiasCNJ {

	private static final Logger LOGGER = LogManager.getLogger(AnalisaServentiasCNJ.class);
	private Map<String, ServentiaCNJ> serventiasCNJ = new HashMap<>();
	private File arquivoServentias;
	
	public AnalisaServentiasCNJ() throws IOException {
		
		// Arquivo de onde os dados das serventias serão lidos, conforme configuração.
		arquivoServentias = new File("src/main/resources/serventias_cnj/" + Auxiliar.getParametroConfiguracao("arquivo_serventias_cnj", true));
		if (!arquivoServentias.exists()) {
			throw new IOException("O arquivo '" + arquivoServentias + "' não existe! Verifique o arquivo de configuração.");
		}
		
		// Abre o arquivo e lê, linha por linha
		Scanner scanner = new Scanner(arquivoServentias);
		try {
			int linha = 0;
			while (scanner.hasNextLine()) {
				linha++;
				String line = scanner.nextLine();
				
				// Quebra cada linha em três partes: o nome do OJ/OJC no PJe, o código da serventia no CNJ e o nome da serventia no CNJ
				String[] partes = line.split(";");
				if (partes.length != 3) {
					throw new IOException("Inconsistência na linha " + linha + " do arquivo '" + arquivoServentias + "': a linha deve conter 3 campos, separados por ponto e vírgula: o nome do OJ/OJC no PJe, o código da serventia no CNJ e o nome da serventia no CNJ.");
				}
				String orgaoJulgadorPJe = partes[0];
				String codigoServentiaCNJ = partes[1];
				String nomeServentiaCNJ = partes[2];
				
				if (!StringUtils.isBlank(orgaoJulgadorPJe)) {
					
					// Verifica se não há OJs/OJCs declarados em duplicidade
					if (serventiasCNJ.containsKey(orgaoJulgadorPJe)) {
						LOGGER.warn("Inconsistência na linha " + linha + " do arquivo '" + arquivoServentias + "': o órgão julgador '" + orgaoJulgadorPJe + "' está definido mais de uma vez.");
					}
					
					// Adiciona o OJ/OJC na lista de serventias conhecidas
					ServentiaCNJ serventia = new ServentiaCNJ(codigoServentiaCNJ, nomeServentiaCNJ);
					serventiasCNJ.put(orgaoJulgadorPJe, serventia);
				}
			}
		} finally {
			scanner.close();
		}
	}

	public ServentiaCNJ getServentiaByOJ(String nomePJe) {
		if (serventiasCNJ.containsKey(nomePJe)) {
			return serventiasCNJ.get(nomePJe);
		} else {
			LOGGER.warn("Inconsistência no arquivo '" + arquivoServentias + "': não há nenhuma linha definindo o código e o nome da serventia para o OJ/OJC '" + nomePJe + "', do PJe. Para evitar interrupção da rotina, será utilizada uma serventia temporária.");
			return new ServentiaCNJ("CODIGO_INEXISTENTE", "SERVENTIA INEXISTENTE");
		}
	}
}
