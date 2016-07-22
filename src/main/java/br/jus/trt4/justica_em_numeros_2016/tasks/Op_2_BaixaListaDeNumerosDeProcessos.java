package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;

/**
 * Monta uma lista de processos e grava em um arquivo "properties", para que posteriormente seus 
 * XMLs possam ser baixados do PJe.
 * 
 * @author fgiotto
 */
public class Op_2_BaixaListaDeNumerosDeProcessos {

	private static final Logger LOGGER = LogManager.getLogger(Op_2_BaixaListaDeNumerosDeProcessos.class);
	private int grau;
	private final File arquivoSaida;
	private Connection conexaoBasePrincipal;
	
	public static void main(String[] args) throws SQLException, IOException {
		
		// Verifica se deve gerar XML para 1o Grau
		if (Auxiliar.getParametroBooleanConfiguracao("gerar_xml_1G")) {
			gerarListaProcessos(1);
		}
		
		// Verifica se deve gerar XML para 2o Grau
		if (Auxiliar.getParametroBooleanConfiguracao("gerar_xml_2G")) {
			gerarListaProcessos(2);
		}
		
		LOGGER.info("Fim!");
	}

	private static void gerarListaProcessos(int grau) throws SQLException, IOException {
		Op_2_BaixaListaDeNumerosDeProcessos baixaDados = new Op_2_BaixaListaDeNumerosDeProcessos(grau);
		try {

			// Abre conexões com o PJe e prepara consultas a serem realizadas
			baixaDados.prepararConexao();

			// Executa consultas e grava arquivo XML
			baixaDados.baixarListaProcessos();
		} finally {
			baixaDados.close();
		}
	}
	
	public Op_2_BaixaListaDeNumerosDeProcessos(int grau) {
		this.grau = grau;
		this.arquivoSaida = Auxiliar.getArquivoListaProcessos(grau);
	}

	private void baixarListaProcessos() throws IOException, SQLException {
		
		ArrayList<String> listaProcessos = new ArrayList<>();
		
		// Verifica quais os critérios selecionados pelo usuário, no arquivo "config.properties",
		// pra escolher os processos que serão analisados.
		String tipoCarga = Auxiliar.getParametroConfiguracao("tipo_carga_xml", true);
		ResultSet rsConsultaProcessos;
		
		if ("TESTES".equals(tipoCarga)) {
			
			// Se usuário selecionou carga "TESTES" no parâmetro "tipo_carga_xml", pega um lote qualquer
			// de 30 processos
			String sql = "SELECT nr_processo " +
					"FROM tb_processo p " +
					"INNER JOIN tb_processo_trf ptrf ON (p.id_processo = ptrf.id_processo_trf) " +
					"WHERE nr_ano = 2016 " +
					"LIMIT 30";
			rsConsultaProcessos = conexaoBasePrincipal.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD).executeQuery(sql);
			LOGGER.warn(">>>>>>>>>> CUIDADO! Somente uma fração dos dados está sendo carregada, para testes! Atente ao parâmetro 'tipo_carga_xml', nas configurações!! <<<<<<<<<<");
			
		} else if (tipoCarga.matches("\\d{7}\\-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}")) {
			
			// Se usuário preencheu um número de processo no parâmetro "tipo_carga_xml", carrega
			// somente os dados dele
			String sql = "SELECT nr_processo " +
					"FROM tb_processo p " +
					"WHERE nr_processo = ?";
			PreparedStatement ps = conexaoBasePrincipal.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);
			ps.setString(1, tipoCarga);
			rsConsultaProcessos = ps.executeQuery();
			LOGGER.warn(">>>>>>>>>> CUIDADO! Somente estão sendo carregados os dados do processo " + tipoCarga + "! Atente ao parâmetro 'tipo_carga_xml', nas configurações!! <<<<<<<<<<");
			
		} else if ("COMPLETA".equals(tipoCarga)) {
			
			// Se usuário selecionou carga "COMPLETA" no parâmetro "tipo_carga_xml", utiliza as
			// regras definidas pelo CNJ:
			// Para a carga completa devem ser encaminhados a totalidade dos processos em tramitação em 31 de julho de 2016, 
			// bem como daqueles que foram baixados de 1° de janeiro de 2015 até 31 de julho de 2016. 
			throw new RuntimeException("Falta implementar!!!!");
			
		} else if ("MENSAL".equals(tipoCarga)) {
			
			// Se usuário selecionou carga "MENSAL" no parâmetro "tipo_carga_xml", utiliza as
			// regras definidas pelo CNJ:
			// Para a carga mensal devem ser transmitidos os processos que tiveram movimentação ou alguma atualização no mês
			// de agosto de 2016, com todos os dados e movimentos dos respectivos processos, de forma a evitar perda de
			throw new RuntimeException("Falta implementar!!!!");
			
		} else {
			throw new RuntimeException("Valor desconhecido para o parâmetro 'tipo_carga_xml': " + tipoCarga);
		}
		
//		// Preenche os parâmetros referentes ao período de movimentação dos processos
//		// TODO: Conferir, com área de negócio, qual o critério exato para selecionar os processos conforme regras do CNJ
//		Calendar dataInicial = Calendar.getInstance();
//		Calendar dataFinal = Calendar.getInstance();
//		if ("COMPLETA".equals(tipoCarga)) {
//			// CNJ: Para a carga completa devem ser encaminhados a totalidade dos processos em tramitação em 31 de julho de 2016, 
//			// bem como daqueles que foram baixados de 1° de janeiro de 2015 até 31 de julho de 2016. 
//			dataInicial.set(2015, Calendar.JANUARY, 1, 0, 0, 0);
//			dataFinal.set(2016, Calendar.JULY, 31, 23, 59, 59);
//			nsConsultaProcessos.setInt("filtrar_por_movimentacoes", 1);
//			
//		} else if ("MENSAL".equals(tipoCarga)) {
//			// CNJ: Para a carga mensal devem ser transmitidos os processos que tiveram movimentação ou alguma atualização no mês
//			// de agosto de 2016, com todos os dados e movimentos dos respectivos processos, de forma a evitar perda de
//			// algum tipo de informação.
//			dataInicial.set(2016, Calendar.AUGUST, 1, 0, 0, 0);
//			dataFinal.set(2016, Calendar.AUGUST, 31, 23, 59, 59);
//			nsConsultaProcessos.setInt("filtrar_por_movimentacoes", 1);
//			
//		} else {
//			// Para outros filtros, não considera as datas das movimentações
//			nsConsultaProcessos.setInt("filtrar_por_movimentacoes", 0);
//		}
//		dataInicial.set(Calendar.MILLISECOND, 0);
//		dataFinal.set(Calendar.MILLISECOND, 999);
//		nsConsultaProcessos.setDate("dt_inicio_periodo", new Date(dataInicial.getTimeInMillis()));
//		nsConsultaProcessos.setDate("dt_fim_periodo", new Date(dataFinal.getTimeInMillis()));

		// Itera sobre os processos encontrados
		try {
			while (rsConsultaProcessos.next()) {
				listaProcessos.add(rsConsultaProcessos.getString("nr_processo"));
			}
		} finally {
			rsConsultaProcessos.close();
		}
		
		// Salva a lista de processos em um arquivo "properties"
		Auxiliar.gravarListaProcessosEmArquivo(listaProcessos, arquivoSaida);
	}
	
	public void prepararConexao() throws SQLException, IOException {

		// Abre conexão com o banco de dados do PJe
		conexaoBasePrincipal = Auxiliar.getConexaoPJe(grau);
		conexaoBasePrincipal.setAutoCommit(false);
	}
	
	public void close() {

		// Fecha conexão com o PJe
		if (conexaoBasePrincipal != null) {
			try {
				conexaoBasePrincipal.close();
				conexaoBasePrincipal = null;
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando conexão com o PJe: " + e.getLocalizedMessage(), e);
			}
		}
	}
}
