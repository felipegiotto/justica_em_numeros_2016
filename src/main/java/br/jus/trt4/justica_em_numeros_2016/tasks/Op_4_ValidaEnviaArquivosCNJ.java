package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Parametro;

/**
 * Chama a JAR oficial do CNJ (replicacao-client), conforme parâmetro jar_replicacao_nacional_cnj 
 * das configurações.
 * 
 * Essa JAR fará validação dos arquivos XML gerados pela classe {@link Op_3_UnificaArquivosXML},
 * conversão para formato JSON, compactação em ZIP e envio ao FTP do CNJ.
 * 
 * OBS: A JAR do CNJ executa os passos acima e, depois de cada passo, move ou exclui os arquivos
 * anteriores, ex: 
 * 1. depois de validar todos os arquivos XML, eles são MOVIDOS para a pasta "validados"
 * 2. depois de converter os arquivos da pasta "validados" para JSON na pasta "convertidos", 
 *    os arquivos da pasta "validados" são EXCLUÍDOS.
 * 3. depois de zipar os arquivos da pasta "convertidos" e enviar ao FTP, os arquivos ZIP são gravados
 *    na pasta "enviados" e os arquivos antigos, da pasta "convertidos", são EXCLUÍDOS.
 * Por isso, RECOMENDO que, depois de disparar essa classe (Op_4), seguir com atenção as instruções 
 * informadas na tela e fazer backup dos arquivos da pasta "output" antes que sejam excluídos, para 
 * caso seja necessário fazer alguma análise futura (já que o funcionamento da JAR do CNJ pode não 
 * ser tão confiável).
 * 
 * Para maiores detalhes sobre o funcionamento do "replicacao-client", consultar o manual do CNJ,
 * disponível no mesmo endereço onde o JAR é baixado (ver atributo "jar_replicacao_nacional_cnj" no 
 * arquivo de configurações).
 * 
 * OBS 2: Se essa classe não funcionar por algum problema de proxy ou de caminho do JAVA, é possível
 * chamá-la manualmente, seguindo as instruções abaixo:
 * 
 * 1. Baixar a versão mais recente do arquivo "replicacao-client-X.Y.Z.jar" (ver atributo 
 *    "jar_replicacao_nacional_cnj" no arquivo de configurações) e gravar na pasta "lib/replicacaoNacional"
 * 
 * 2. Iniciar um terminal (cmd.exe) e acessar a pasta raiz desse projeto (justica_em_numeros_2016).
 *
 * 3. Executar o comando abaixo para validar e enviar os arquivos XML unificados (gerados pela operação "Op_3") ao CNJ.
 *    Lembrar de substituir os tokens "SIGLA_REGIONAL" (ex: "TRT0") e "TIPO_CARGA_XML" (ex: "MENSAL 2017-02") 
 *    e a versão correta da biblioteca do CNJ (replicacao-client)
 *    Ambiente Windows: java -jar lib\replicacaoNacional\replicacao-client-2.2.6.jar SIGLA_REGIONAL output\TIPO_CARGA_XML\xmls_unificados
 *    Ambiente Linux:   java -jar lib/replicacaoNacional/replicacao-client-2.2.6.jar SIGLA_REGIONAL output/TIPO_CARGA_XML/xmls_unificados
 *
 * OBS: caso tenha problemas com proxy, adicionar os parâmetros abaixo ao final dos comandos sugeridos:
 *      -Dhttp.proxyHost=HOST -Dhttp.proxyPort=PORTA -Dhttp.proxyUser=USUARIO -Dhttp.proxyPassword=SENHA
 *      
 * @author fgiotto
 */
public class Op_4_ValidaEnviaArquivosCNJ {
	
	private static final Logger LOGGER = LogManager.getLogger(Op_4_ValidaEnviaArquivosCNJ.class);
	
	public static void main(String[] args) throws Exception {
		Auxiliar.prepararPastaDeSaida();
		
		File pastaXMLsUnificados = new File(Auxiliar.prepararPastaDeSaida(), "xmls_unificados");
		
		LOGGER.info("Processando todos os arquivos da pasta '" + pastaXMLsUnificados.getAbsolutePath() + "' com a ferramenta 'replicacao-client', do CNJ.");
		LOGGER.info("*** Executando JAR 'replicacaoClient' do CNJ. Os dados retornados pela 'replicacao-client' serão exibidos, nos arquivos de log, com o prefixo 'DEBUG' ou 'ERROR'.");
		
		// Prepara para disparar o "replicacao-client" do CNJ
		ProcessBuilder pb = prepararReplicacaoNacional(pastaXMLsUnificados);
		
		// Envia comandos do teclado (STDIN) no processo pai (esta classe) para o processo filho (replicacaoNacional)
		pb.redirectInput(Redirect.INHERIT); 
		
		// Chama o "replicacao-client".
		Process p = pb.start();
		
		// Redireciona a STDOUT e STDERR do processo filho (replicacao-client) para os logs em arquivo, para referência futura.
		Auxiliar.consumirStreamAssincrono(p.getInputStream(), "", Level.DEBUG);
		Auxiliar.consumirStreamAssincrono(p.getErrorStream(), "", Level.ERROR);
		
		// Aguarda até que o processo termine.
		p.waitFor();
		Thread.sleep(1000);
		
		// Confere se o processo retornou sucesso
		if (p.exitValue() != 0) {
			throw new Exception("O envio ao CNJ deveria ter retornado código 0, mas retornou " + p.exitValue());
		}
		
		LOGGER.info("Fim!");
	}
	
	public static ProcessBuilder prepararReplicacaoNacional(File pastaParaProcessamento) {
		
		// Lê os parâmetros necessários
		String caminhoJar = Auxiliar.getParametroConfiguracao(Parametro.jar_replicacao_nacional_cnj, true);
		String siglaTribunal = Auxiliar.getParametroConfiguracao(Parametro.sigla_tribunal, true);
		return new ProcessBuilder("java", "-jar", caminhoJar, siglaTribunal, pastaParaProcessamento.getAbsolutePath());
	}

	public static boolean verificarVersaoDesatualizadaJarCNJ() throws InterruptedException {
		
		try {
			// Cria uma pasta temporária para executar o JAR do CNJ
			File pastaTemporaria = new File("tmp/versao_jar_cnj");
			try {
				FileUtils.deleteDirectory(pastaTemporaria);
				pastaTemporaria.mkdirs();
				
				ProcessBuilder pb = prepararReplicacaoNacional(pastaTemporaria);
				
				// Chama o "replicacao-client".
				Process p = pb.start();
				
				// Analisa a saída da JAR do CNJ
				StringBuilder saida = new StringBuilder();
				saida.append(IOUtils.toString(p.getInputStream()));
				saida.append(IOUtils.toString(p.getErrorStream()));
	
				// Aguarda até que o processo termine.
				p.waitFor();
				
				// Verifica se JAR indicou estar desatualizada ou se houve problemas na execução
				boolean houveProblema = false;
				if (saida.toString().contains("desatualizada")) { // Ex: Sua versão [2.3.0] está desatualizada.
					LOGGER.warn("");
					for (String linha: saida.toString().split("\n")) {
						LOGGER.warn("JAR CNJ: " + linha);
					}
					houveProblema = true;
				}
				if (p.exitValue() != 0) {
					LOGGER.warn("JAR CNJ retornou código " + p.exitValue() + " (deveria ser zero)");
					houveProblema = true;
				}
				return houveProblema;
				
			} finally {
				FileUtils.deleteDirectory(pastaTemporaria);
			}
		} catch (IOException ex) {
			LOGGER.error("Erro verificando se JAR do CNJ está desatualizada: " + ex.getLocalizedMessage(), ex);
			return true;
		}
	}
}
