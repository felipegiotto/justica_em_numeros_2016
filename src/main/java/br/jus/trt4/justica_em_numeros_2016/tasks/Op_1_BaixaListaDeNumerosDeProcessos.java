package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
	private List<Integer> listaIdOrgaoJulgadorParaIgnorar;
	
	
	public static void main(String[] args) throws SQLException, IOException {
		
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

			// Abre conexões com o PJe e prepara consultas a serem realizadas
			baixaDados.prepararConexao();

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
		
		ArrayList<String> listaProcessos = new ArrayList<>();
		
		// Verifica quais os critérios selecionados pelo usuário, no arquivo "config.properties",
		// pra escolher os processos que serão analisados.
		String tipoCarga = Auxiliar.getParametroConfiguracao("tipo_carga_xml", true);
		LOGGER.info("Executando consulta " + tipoCarga + "...");
		ResultSet rsConsultaProcessos;
		if ("TESTES".equals(tipoCarga)) {
			
			// Se usuário selecionou carga "TESTES" no parâmetro "tipo_carga_xml", pega um lote qualquer
			// de 30 processos
			String sql = "SELECT nr_processo, ptrf.id_orgao_julgador " +
					"FROM tb_processo p " +
					"INNER JOIN tb_processo_trf ptrf ON (p.id_processo = ptrf.id_processo_trf) " +
					"WHERE nr_ano = 2016 " +
					"LIMIT 30";
			rsConsultaProcessos = conexaoBasePrincipal.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD).executeQuery(sql);
			LOGGER.warn(">>>>>>>>>> CUIDADO! Somente uma fração dos dados está sendo carregada, para testes! Atente ao parâmetro 'tipo_carga_xml', nas configurações!! <<<<<<<<<<");
			
		} else if (tipoCarga.matches("\\d{7}\\-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}")) {
			
			// Se usuário preencheu um número de processo no parâmetro "tipo_carga_xml", carrega
			// somente os dados dele
			String sql = "SELECT nr_processo, ptrf.id_orgao_julgador " +
					"FROM tb_processo p " +
					"INNER JOIN tb_processo_trf ptrf ON (p.id_processo = ptrf.id_processo_trf) " +
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
			String sql = "SELECT DISTINCT nr_processo, ptrf.id_orgao_julgador " +
					"FROM tb_processo p " +
					"INNER JOIN tb_processo_evento pe ON (p.id_processo = pe.id_processo) " +
					"INNER JOIN tb_processo_trf ptrf ON (p.id_processo = ptrf.id_processo_trf) " +
					"WHERE (pe.dt_atualizacao BETWEEN '2015-01-01 00:00:00.000' AND '2016-07-31 23:59:59.999')";
			rsConsultaProcessos = conexaoBasePrincipal.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD).executeQuery(sql);
			
		} else if ("MENSAL".equals(tipoCarga)) {
			
			// Se usuário selecionou carga "MENSAL" no parâmetro "tipo_carga_xml", utiliza as
			// regras definidas pelo CNJ:
			// Para a carga mensal devem ser transmitidos os processos que tiveram movimentação ou alguma atualização no mês
			// de agosto de 2016, com todos os dados e movimentos dos respectivos processos, de forma a evitar perda de
			String sql = "SELECT DISTINCT nr_processo, ptrf.id_orgao_julgador " +
					"FROM tb_processo p " +
					"INNER JOIN tb_processo_trf ptrf ON (p.id_processo = ptrf.id_processo_trf) " +
					"INNER JOIN tb_processo_evento pe ON (pe.id_processo = p.id_processo) " +
					"WHERE (pe.dt_atualizacao BETWEEN '2016-08-01 00:00:00.000' AND '2016-08-31 23:59:59.999')";
			rsConsultaProcessos = conexaoBasePrincipal.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD).executeQuery(sql);
			LOGGER.warn(">>>>>>>>>> CUIDADO! Somente uma fração dos dados está sendo carregada, para testes! Atente ao parâmetro 'tipo_carga_xml', nas configurações!! <<<<<<<<<<");
			
		} else {
			throw new RuntimeException("Valor desconhecido para o parâmetro 'tipo_carga_xml': " + tipoCarga);
		}

		// Itera sobre os processos encontrados
		try {
			while (rsConsultaProcessos.next()) {
				String nrProcesso = rsConsultaProcessos.getString("nr_processo");
				if (deveIgnorarIdOrgaoJulgador(rsConsultaProcessos.getInt("id_orgao_julgador"))) {
					LOGGER.info("XML do processo " + nrProcesso + " não será gerado pois pertence a um OJ que está sendo ignorado conforme parâmetro 'orgaos_julgadores_ignorados'");
				} else {
					listaProcessos.add(nrProcesso);
				}
			}
		} finally {
			rsConsultaProcessos.close();
		}
		
		// Salva a lista de processos em um arquivo "properties"
		gravarListaProcessosEmArquivo(listaProcessos, arquivoSaida);
	}
	
	
	/**
	 * Verifica se o órgão julgador do processo faz parte da lista que deve ser ignorada, 
	 * conforme parâmetro 'orgaos_julgadores_ignorados', nas configurações
	 */
	private boolean deveIgnorarIdOrgaoJulgador(int idOrgaoJulgador) throws SQLException {
		
		// Carrega uma vez a lista de IDs de órgãos julgadores cujos processos devem ser ignorados
		if (listaIdOrgaoJulgadorParaIgnorar == null) {
			
			// Essa configuração é opcional. Verifica se ela realmente precisará ser analisada
			List<Integer> lista = new ArrayList<>();
			String nomesOJsParaIgnorar = Auxiliar.getParametroConfiguracao("orgaos_julgadores_ignorados_" + grau + "G", false);
			if (nomesOJsParaIgnorar != null) {
			
				// Monta um SQL para consultar os IDs dos órgãos julgadores a serem ignorados
				LOGGER.info("Carregando lista de OJs que serão ignorados...");
				String[] nomesOJsParaIgnorarArray = StringUtils.split(nomesOJsParaIgnorar, ',');
				StringBuilder sbLocalizaOJs = new StringBuilder();
				sbLocalizaOJs.append("SELECT id_orgao_julgador, ds_orgao_julgador FROM tb_orgao_julgador WHERE upper(to_ascii(ds_orgao_julgador)) = ?");
				try (PreparedStatement psLocalizaOJs = conexaoBasePrincipal.prepareStatement(sbLocalizaOJs.toString())) {
					
					// Consulta todos os OJs informados no parâmetro
					for (String nomeOrgaoJulgador: nomesOJsParaIgnorarArray) {
						psLocalizaOJs.setString(1, nomeOrgaoJulgador);
						try (ResultSet rsLocalizaOJs = psLocalizaOJs.executeQuery()) {
							while (rsLocalizaOJs.next()) {
								int idOJ = rsLocalizaOJs.getInt("id_orgao_julgador");
								String nomeOJ = rsLocalizaOJs.getString("ds_orgao_julgador");
								lista.add(idOJ);
								LOGGER.warn("* Todos os processos do OJ '" + nomeOJ + "' (id=" + idOJ + ") serão ignorados!");
							}
						}
					}
				}
			}
			
			listaIdOrgaoJulgadorParaIgnorar = lista;
		}
		return listaIdOrgaoJulgadorParaIgnorar.contains(idOrgaoJulgador);
	}


	/**
	 * Abre conexão com o banco de dados do PJe
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	public void prepararConexao() throws SQLException {

		conexaoBasePrincipal = Auxiliar.getConexaoPJe(grau);
		conexaoBasePrincipal.setAutoCommit(false);
	}
	
	
	public static void gravarListaProcessosEmArquivo(List<String> listaProcessos, File arquivoSaida) throws IOException {
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
