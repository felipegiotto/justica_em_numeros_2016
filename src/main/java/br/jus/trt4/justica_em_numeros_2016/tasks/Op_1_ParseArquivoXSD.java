package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;

/**
 * IMPORTANTE!! Essa classe só precisará ser executada novamente se houver alguma alteração no arquivo XSD do CNJ.
 * 
 * Processa o arquivo XSD recebido do CNJ (src/main/resources/replicacao-nacional.xsd) e gera as classes
 * Java correspondentes, para permitir a geração dos arquivos XML.
 * 
 * Fonte: https://sanaulla.info/2010/08/29/using-jaxb-to-generate-xml-from-the-java-xsd-2/
 * 
 * @author fgiotto
 */
public class Op_1_ParseArquivoXSD {
	
	private static final Logger LOGGER = LogManager.getLogger(Op_1_ParseArquivoXSD.class);
	
	public static void main(String[] args) throws Exception {
		
		ArrayList<String> comandos = new ArrayList<>();
		
		// Utilitário do Java para processar schemas XSD e gerar classes Java
		comandos.add("xjc");
		
		// Verifica se será necessário utilizar proxy
		String proxy = Auxiliar.getParametroConfiguracao("configuracao_proxy", false);
		if (proxy != null) {
			comandos.add("-httpproxy");
			comandos.add(proxy);
		}
		
		// Arquivo XSD recebido do CNJ
		comandos.add("src/main/resources/replicacao-nacional.xsd");
		
		// Pasta de saída dos arquivos Java gerados
		comandos.add("-d");
		comandos.add("src/main/java");
		
		// Executa o comando, mostrando o resultado no console
		LOGGER.info("Executando comando: " + comandos);
		Process process;
		try {
			process = Runtime.getRuntime().exec(comandos.toArray(new String[comandos.size()]));
		} catch (IOException ex) {
			throw new IOException("Não foi possível executar o comando 'xjc', do Java, para analisar os arquivos XSD do CNJ. Verifique se a pasta 'bin' do Java está no seu PATH!", ex);
		}
		Auxiliar.consumirStream(process.getInputStream(), "STDOUT - ");
		Auxiliar.consumirStream(process.getErrorStream(), "STDERR - ");
		Auxiliar.conferirRetornoProcesso(process);
		
		LOGGER.info("Operação concluída!");
	}
}
