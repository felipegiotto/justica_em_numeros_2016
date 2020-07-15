package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DadosInvalidosException;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ProgressoInterfaceGrafica;

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

	private ProgressoInterfaceGrafica progresso;
	
	// Enum com todas as operações que serão executadas.
	public enum ControleOperacoes {
	    
		OP_1_BAIXAR_LISTA                  (100),
		OP_2_GERAR_XMLS_INDIVIDUAIS        (200),
		OP_3_UNIFICA_ARQUIVOS_XML          (300), 
		OP_4_VALIDA_ENVIA_ARQUIVOS_CNJ     (400), 
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
		void run() throws Exception;
	}
	
	File pastaOutput;

	public static void main(String[] args) throws Exception {
		try {
			Op_X_OperacaoCompleta operacaoCompleta = new Op_X_OperacaoCompleta();
			try {
				operacaoCompleta.executarOperacaoCompleta();
			} finally {
				operacaoCompleta.close();
			}
		} catch (Exception ex) {
			LOGGER.error("Op_X_OperacaoCompleta abortada", ex);
		}
	}

	public Op_X_OperacaoCompleta() {
		this.pastaOutput = Auxiliar.prepararPastaDeSaida();
		this.progresso = new ProgressoInterfaceGrafica("Operação Completa");
		this.progresso.setMax(ControleOperacoes.values().length);
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
			public void run() throws SQLException, IOException {
				Op_1_BaixaListaDeNumerosDeProcessos.main(null);
			}
		});

		// CHECKLIST: 5. Execute a classe "Op_2_GeraXMLsIndividuais"
		executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes.OP_2_GERAR_XMLS_INDIVIDUAIS, new Operacao() {

			@Override
			public void run() throws SQLException, Exception {
				Op_2_GeraXMLsIndividuais.main(null);
			}
		});

		// CHECKLIST: 7. Execute a classe "Op_3_UnificaArquivosXML"
		executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes.OP_3_UNIFICA_ARQUIVOS_XML, new Operacao() {

			@Override
			public void run() throws Exception {
				Op_3_UnificaArquivosXML.main(null);
			}
		});

		// CHECKLIST: 9. Execute a classe "Op_4_ValidaEnviaArquivosCNJ", ...
		executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes.OP_4_VALIDA_ENVIA_ARQUIVOS_CNJ, new Operacao() {
			
			@Override
			public void run() throws Exception {
				
				// Envia os XMLs ao CNJ
				Op_4_ValidaEnviaArquivosCNJ.validarEnviarArquivosCNJ(false);
			}
		});
		
		// CHECKLIST: 12. Efetue backup dos seguintes dados, para referência futura: ...
		executaOperacaoSeAindaNaoFoiExecutada(ControleOperacoes.OP_9_ULTIMOS_BACKUPS, new Operacao() {
			
			@Override
			public void run() throws Exception {
				Op_6_BackupConfiguracoes.main(null);
			}
		});
		
		LOGGER.info("Operação completa realizada com sucesso!");
		LOGGER.info("Os dados referentes a este envio ao CNJ foram gravados na pasta '" + pastaOutput + "'.");
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
