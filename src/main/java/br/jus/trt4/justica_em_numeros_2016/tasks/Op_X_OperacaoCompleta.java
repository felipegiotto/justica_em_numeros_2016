package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.AcumuladorExceptions;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ProgressoInterfaceGrafica;
import br.jus.trt4.justica_em_numeros_2016.tabelas_cnj.AnalisaServentiasCNJ;

/**
 * Prototipo de rotina de envio completo dos dados ao CNJ
 * 
 * Esta classe seguirá o mais próximo possível das instruções descritas no arquivo CHECKLIST_RESUMO.txt,
 * executando as operações necessárias em sequência.
 * 
 * Ela recebe OPCIONAMENTE como parâmetro um caractere ('S' ou 'N') indicando se as operações Op_4_ValidaEnviaArquivosCNJ e 
 * Op_5_ConfereProtocolosCNJ serão reiniciadas caso aconteça algum erro.
 * 
 * Se alguma exceção capturada ocorrer, a operação não será abortada. Caso a execução termine inesperadamente, 
 * é possível continuar da etapa em que parou, reexecutando a classe. O controle das operações já executadas ficará 
 * no arquivo "operacao_atual.dat", dentro da pasta "output/<TIPO_CARGA_XML>_backup_operacao_completa"
 * 
 * TODO: Tentar fazer uma nova operação completa, com processamento paralelo (enquanto alguns estão gerando, outros estão enviando, outros estão conferindo protocolo), como uma linha de produção.
 * Para acompanhar, gerar constantemente um arquivo HTML que, ao ser aberto no navegador, mostra o status de cada uma das fases, os totais, os erros que precisam ser resolvidos,
 * uma barra de progresso com várias cores (cada fase), instruções sobre como resolver os problemas encontrados. Esse HTML pode fazer refresh sozinho, para dar impressão de ser dinâmico.
 * Ou então abrir uma porta TCP, acessível pelo navegador, onde dê para acompanhar.
 *
 * @author felipe.giotto@trt4.jus.br
 */
public class Op_X_OperacaoCompleta {

	private ProgressoInterfaceGrafica progresso;
	
	// Enum com todas as operações que serão executadas.
	public enum ControleOperacoes {
	    
		OP_1_BAIXAR_LISTA                  (100),
		OP_2_GERAR_XMLS_INDIVIDUAIS        (200),
		OP_4_VALIDA_ENVIA_ARQUIVOS_CNJ     (400), 
		OP_5_CONFERE_PROTOCOLOS_CNJ        (500), 
		OP_9_ULTIMOS_BACKUPS               (900);
		
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
		void run(boolean reiniciarEmCasoErro) throws Exception;
	}
	
	File pastaOutput;
	
	private boolean reiniciarEmCasoErro = true;

	public static void main(String[] args) throws Exception {
		
		boolean reiniciarEmCasoErro = true;
		
		if(args != null && args.length > 0) {
			reiniciarEmCasoErro = "S".equals(args[0]);
		}			
		
		try {
			Op_X_OperacaoCompleta operacaoCompleta = new Op_X_OperacaoCompleta(reiniciarEmCasoErro);
			try {
				operacaoCompleta.executarOperacaoCompleta();
			} finally {
				operacaoCompleta.close();
			}
		} catch (Exception ex) {
			LOGGER.error("Op_X_OperacaoCompleta abortada", ex);
		}
	}

	public Op_X_OperacaoCompleta(boolean reiniciarEmCasoErro) {
		this.pastaOutput = Auxiliar.prepararPastaDeSaida();
		this.progresso = new ProgressoInterfaceGrafica("Operação Completa");
		this.progresso.setMax(ControleOperacoes.values().length);
		this.reiniciarEmCasoErro = reiniciarEmCasoErro;
	}

	public void close() {
		this.progresso.close();
		this.progresso = null;
	}
	
	/**
	 * Método que executa todas as operações que ainda estão pendentes.
	 * 
	 * @throws Exception
	 */
	private void executarOperacaoCompleta() throws Exception {

		// CHECKLIST: 4. Execute a classe "Op_1_BaixaListaDeNumerosDeProcessos".
		executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes.OP_1_BAIXAR_LISTA, new Operacao() {
			
			@Override
			public void run(boolean reiniciarEmCasoErro) throws SQLException, IOException {
				Op_1_BaixaListaDeNumerosDeProcessos.main(null);
			}
		}, this.reiniciarEmCasoErro);

		// CHECKLIST: 5. Execute a classe "Op_2_GeraXMLsIndividuais"
		executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes.OP_2_GERAR_XMLS_INDIVIDUAIS, new Operacao() {

			@Override
			public void run(boolean reiniciarEmCasoErro) throws SQLException, Exception {
				Op_2_GeraXMLsIndividuais.main(null);
				
				//Remove essa exceção caso exista para que não interfira nas demais fases.
				String agrupadorErro = "Órgãos julgadores sem serventia cadastrada no arquivo " + AnalisaServentiasCNJ.getArquivoServentias();
				AcumuladorExceptions.instance().removerExceptionsDoAgrupador(agrupadorErro);
			}
		}, this.reiniciarEmCasoErro);

		// CHECKLIST: 9. Execute a classe "Op_4_ValidaEnviaArquivosCNJ", ...
		executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes.OP_4_VALIDA_ENVIA_ARQUIVOS_CNJ, new Operacao() {
			
			@Override
			public void run(boolean reiniciarEmCasoErro) throws Exception {
				
				// Envia os XMLs ao CNJ
				Op_4_ValidaEnviaArquivosCNJ.validarEnviarArquivosCNJ(reiniciarEmCasoErro);
			}
		}, this.reiniciarEmCasoErro);
		
		// CHECKLIST: TODO
		executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes.OP_5_CONFERE_PROTOCOLOS_CNJ, new Operacao() {

			@Override
			public void run(boolean reiniciarEmCasoErro) throws Exception {
				Op_5_ConfereProtocolosCNJ.executarOperacaoConfereProtocolosCNJ(reiniciarEmCasoErro);
			}
		}, this.reiniciarEmCasoErro);

		// CHECKLIST: 12. Efetue backup dos seguintes dados, para referência futura: ...
		executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes.OP_9_ULTIMOS_BACKUPS, new Operacao() {
			
			@Override
			public void run(boolean reiniciarEmCasoErro) throws Exception {
				Op_6_BackupConfiguracoes.main(null);
			}
		}, this.reiniciarEmCasoErro);
		
		if (AcumuladorExceptions.instance().isExisteExceptionRegistrada()) {
			LOGGER.info("Operação completa realizada com alguns erros!");
			LOGGER.info("Os dados referentes a este envio ao CNJ foram gravados na pasta '" + pastaOutput + "'.");
			AcumuladorExceptions.instance().mostrarExceptionsAcumuladas();			
		} else {
			LOGGER.info("Operação completa realizada com sucesso!");
			LOGGER.info("Os dados referentes a este envio ao CNJ foram gravados na pasta '" + pastaOutput + "'.");		
		}
		
		// Apaga o arquivo com a última operação executada. Como todas as operações foram executadas 
		// não tem sentido mantê-lo.
		File arquivoOperacaoAtual = getArquivoOperacaoAtual(pastaOutput);

		if (arquivoOperacaoAtual.exists()) {
			arquivoOperacaoAtual.delete();
		}

		
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
	private void executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes controleOperacoes, Operacao operacao, boolean reiniciarEmCasoErro) throws Exception {

		String descricaoOperacao = controleOperacoes + " (" + controleOperacoes.getOrdem() + ")";
		if (getUltimaOperacaoExecutada() < controleOperacoes.getOrdem()) {

			LOGGER.info("Iniciando operação " + descricaoOperacao + "...");
			
			try {
				operacao.run(reiniciarEmCasoErro);
			} catch (Exception ex) {
				LOGGER.error("Erro na operação " + descricaoOperacao + ": " + ex.getLocalizedMessage(), ex);
				throw ex;
			}
			LOGGER.info("Operação " + descricaoOperacao + " concluída!");

			//Não aborta a execução das demais operações caso tenha acontecido algum erro.
			setUltimaOperacaoExecutada(controleOperacoes.getOrdem());

		} else {
			LOGGER.info("Operação " + descricaoOperacao + " já foi executada!");
		}
		
		this.progresso.incrementProgress();
	}

	/**
	 * Lê, do arquivo "operacao_atual.dat", qual a última operação executada.
	 * 
	 * @return
	 */
	private int getUltimaOperacaoExecutada() {
		File arquivoOperacaoAtual = getArquivoOperacaoAtual(pastaOutput);
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
		File arquivoOperacaoAtual = getArquivoOperacaoAtual(pastaOutput);
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
}
