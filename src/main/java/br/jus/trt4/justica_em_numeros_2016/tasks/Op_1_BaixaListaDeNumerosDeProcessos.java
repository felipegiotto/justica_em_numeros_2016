package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;

/**
 * Monta uma lista de processos, conforme o parâmetro "tipo_carga_xml" do arquivo "config.properties",
 * e grava na pasta "output/Xg" (onde 'X' representa o número da instância - '1' ou '2'), 
 * em arquivos com nome "lista_processos.txt".
 * 
 * @author fgiotto
 */
public class Op_1_BaixaListaDeNumerosDeProcessos {

	private static final Logger LOGGER = LogManager.getLogger(Op_1_BaixaListaDeNumerosDeProcessos.class);
	private int grau;
	private final File arquivoSaida;
	private Connection conexaoBasePrincipal;
	private Connection conexaoBaseStagingEGestao;
	
	
	public static void main(String[] args) throws SQLException, IOException {
		Auxiliar.prepararPastaDeSaida();
		
		// Verifica se deve gerar XML para 2o Grau
		if (Auxiliar.getParametroBooleanConfiguracao("gerar_xml_2G")) {
			gerarListaProcessos(2);
		}
		
		// Verifica se deve gerar XML para 1o Grau
		if (Auxiliar.getParametroBooleanConfiguracao("gerar_xml_1G")) {
			gerarListaProcessos(1);
		}
		
		LOGGER.info("Fim!");
	}

	
	private static void gerarListaProcessos(int grau) throws SQLException, IOException {
		Op_1_BaixaListaDeNumerosDeProcessos baixaDados = new Op_1_BaixaListaDeNumerosDeProcessos(grau);
		try {

			// Executa consultas e grava arquivo XML
			baixaDados.baixarListaProcessos();
		} finally {
			baixaDados.close();
		}
	}
	
	
	public Op_1_BaixaListaDeNumerosDeProcessos(int grau) {
		this.grau = grau;
		this.arquivoSaida = Auxiliar.getArquivoListaProcessos(grau);
	}

	
	private void baixarListaProcessos() throws IOException, SQLException {
		
		Set<String> listaProcessos = new TreeSet<>();
		
		// Verifica quais os critérios selecionados pelo usuário, no arquivo "config.properties",
		// pra escolher os processos que serão analisados.
		String tipoCarga = Auxiliar.getParametroConfiguracao("tipo_carga_xml", true);
		LOGGER.info("Executando consulta " + tipoCarga + "...");
		ResultSet rsConsultaProcessos;
		if ("TESTES".equals(tipoCarga)) {
			
			// Se usuário selecionou carga "TESTES" no parâmetro "tipo_carga_xml", pega um lote qualquer
			// de 30 processos
			String sql = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/carga_testes.sql");
			rsConsultaProcessos = getConexaoBasePrincipalPJe().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD).executeQuery(sql);
			LOGGER.warn(">>>>>>>>>> CUIDADO! Somente uma fração dos dados está sendo carregada, para testes! Atente ao parâmetro 'tipo_carga_xml', nas configurações!! <<<<<<<<<<");
			
		} else if (tipoCarga.matches("\\d{7}\\-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}")) {
			
			// Se usuário preencheu um número de processo no parâmetro "tipo_carga_xml", carrega
			// somente os dados dele
			String sql = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/carga_um_processo.sql");
			PreparedStatement ps = getConexaoBasePrincipalPJe().prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);
			ps.setString(1, tipoCarga);
			rsConsultaProcessos = ps.executeQuery();
			LOGGER.warn(">>>>>>>>>> CUIDADO! Somente estão sendo carregados os dados do processo " + tipoCarga + "! Atente ao parâmetro 'tipo_carga_xml', nas configurações!! <<<<<<<<<<");
			
		} else if ("COMPLETA".equals(tipoCarga)) {
			
			// Se usuário selecionou carga "COMPLETA" no parâmetro "tipo_carga_xml", 
			// gera os XMLs de todos os processos que obedecerem às regras descritas no site do CNJ
			/*
			    1.1 Processos em tramitação
			        Saldo de processos em 31/07/2016. Trazer o histórico completo dos movimentos nos seguintes casos:
						a) Processos Pendentes de Baixa, conforme critérios de movimento de baixa estabelecidos na resolução CNJ nº 76.
						b) Cartas Precatórias e de  Ordem Pendentes de Devolução
						c) Recursos Internos Pendentes de Julgamento
						d) Processos de competência exclusiva da Presidência/Corregedoria  Pendentes de Decisão
						e) RPVs e Precatórios Pendentes de Quitação
			     1.2 Processos baixados
			        Todos os processos baixados de 01/01/2015 a 31/07/2016. Trazer o histórico completo dos movimentos nos seguintes casos:
						a) Processos Baixados , conforme critérios de movimento de baixa estabelecidos na resolução CNJ nº 76.
						b) Cartas Precatórias e de Ordem Devolvidas
						c) Recursos Internos Julgados
						d) Processos de competência exclusiva da Presidência/Corregedoria  Decididos
						e) RPvs e Precatórios Quitados/Cancelados
				Fonte: http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/2016-06-02-17-51-25
			*/
			String sql = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/carga_completa_egestao_" + grau + "g.sql");
			getConexaoBaseStagingEGestao().createStatement().execute("SET search_path TO pje_eg");
			Statement statement = getConexaoBaseStagingEGestao().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);
			statement.setFetchSize(100);
			rsConsultaProcessos = statement.executeQuery(sql);
			
		} else if ("TODOS_COM_MOVIMENTACOES".equals(tipoCarga)) {
			
			// Se usuário selecionou carga "TODOS_COM_MOVIMENTACOES" no parâmetro "tipo_carga_xml", 
			// gera os XMLs de todos os processos que tiveram qualquer movimentação processual na 
			// tabela tb_processo_evento. 
			String sql = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/carga_todos_com_movimentacoes.sql");
			Statement statement = getConexaoBasePrincipalPJe().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);
			statement.setFetchSize(100);
			rsConsultaProcessos = statement.executeQuery(sql);
			
		} else if ("MENSAL".equals(tipoCarga)) {
			
			// Se usuário selecionou carga "MENSAL" no parâmetro "tipo_carga_xml", utiliza as
			// regras definidas pelo CNJ:
			// Para a carga mensal devem ser transmitidos os processos que tiveram movimentação ou alguma atualização no mês
			// de agosto de 2016, com todos os dados e movimentos dos respectivos processos, de forma a evitar perda de
			String sql = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/carga_mensal.sql");
			Statement statement = getConexaoBasePrincipalPJe().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);
			statement.setFetchSize(100);
			rsConsultaProcessos = statement.executeQuery(sql);
			
		} else {
			throw new RuntimeException("Valor desconhecido para o parâmetro 'tipo_carga_xml': " + tipoCarga);
		}

		// Itera sobre os processos encontrados
		try {
			int qtdProcessos = 0;
			long tempo = System.currentTimeMillis();
			LOGGER.info("Iterando sobre a lista de processos...");
			while (rsConsultaProcessos.next()) {
				qtdProcessos++;
				String nrProcesso = rsConsultaProcessos.getString("nr_processo");
				listaProcessos.add(nrProcesso);
				
				// Mostra a quantidade de processos analisados a cada 15 segundos
				if (System.currentTimeMillis() - tempo > 15_000) {
					tempo = System.currentTimeMillis();
					LOGGER.info("Processos até agora: " + qtdProcessos);
				}
			}
		} finally {
			rsConsultaProcessos.close();
		}
		
		// Salva a lista de processos em um arquivo "properties"
		gravarListaProcessosEmArquivo(listaProcessos, arquivoSaida);
	}
	
	
	public Connection getConexaoBasePrincipalPJe() throws SQLException {
		if (conexaoBasePrincipal == null) {
			conexaoBasePrincipal = Auxiliar.getConexaoPJe(grau);
			conexaoBasePrincipal.setAutoCommit(false);
		}
		return conexaoBasePrincipal;
	}
	
	
	public Connection getConexaoBaseStagingEGestao() throws SQLException {
		if (conexaoBaseStagingEGestao == null) {
			conexaoBaseStagingEGestao = Auxiliar.getConexaoStagingEGestao(grau);
			conexaoBaseStagingEGestao.setAutoCommit(false);
		}
		return conexaoBaseStagingEGestao;
	}
	
	
	public static void gravarListaProcessosEmArquivo(Set<String> listaProcessos, File arquivoSaida) throws IOException {
		arquivoSaida.getParentFile().mkdirs();
		FileWriter fw = new FileWriter(arquivoSaida);
		try {
			for (String processo: listaProcessos) {
				fw.append(processo);
				fw.append("\r\n");
			}
		} finally {
			fw.close();
		}
		LOGGER.info("Arquivo gerado com lista de " + listaProcessos.size() + " processo(s): " + arquivoSaida);
	}

	
	/**
	 * Fecha conexão com o PJe
	 */
	public void close() {

		if (conexaoBaseStagingEGestao != null) {
			try {
				conexaoBaseStagingEGestao.close();
				conexaoBaseStagingEGestao = null;
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando conexaoBaseStagingEGestao: " + e.getLocalizedMessage(), e);
			}
		}
		
		if (conexaoBasePrincipal != null) {
			try {
				conexaoBasePrincipal.close();
				conexaoBasePrincipal = null;
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando conexaoBasePrincipal: " + e.getLocalizedMessage(), e);
			}
		}
	}
}
