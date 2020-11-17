package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.AcumuladorExceptions;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ProgressoInterfaceGrafica;
import br.jus.trt4.justica_em_numeros_2016.dao.JPAUtil;
import br.jus.trt4.justica_em_numeros_2016.dao.ProcessoEnvioDao;
import br.jus.trt4.justica_em_numeros_2016.dao.RemessaDao;
import br.jus.trt4.justica_em_numeros_2016.dto.DadosBasicosProcessoDto;
import br.jus.trt4.justica_em_numeros_2016.entidades.ProcessoEnvio;
import br.jus.trt4.justica_em_numeros_2016.entidades.Remessa;
import br.jus.trt4.justica_em_numeros_2016.enums.BaseEmAnaliseEnum;
import br.jus.trt4.justica_em_numeros_2016.enums.OrigemProcessoEnum;
import br.jus.trt4.justica_em_numeros_2016.enums.Parametro;
import br.jus.trt4.justica_em_numeros_2016.enums.TipoRemessaEnum;
import br.jus.trt4.justica_em_numeros_2016.util.DataJudUtil;

/**
 * Monta uma lista de processos, conforme o parâmetro "tipo_carga_xml" do arquivo "config.properties".
 * 
 * @author felipe.giotto@trt4.jus.br
 */
public class Op_1_BaixaListaDeNumerosDeProcessos implements AutoCloseable {

	private static final Logger LOGGER = LogManager.getLogger(Op_1_BaixaListaDeNumerosDeProcessos.class);

	private static final RemessaDao remessaDAO = new RemessaDao();
	private static final ProcessoEnvioDao processoEnvioDAO = new ProcessoEnvioDao();

	private static final int BATCH_SIZE = Auxiliar.getParametroInteiroConfiguracao(Parametro.tamanho_batch);
	private static final int COMMIT_SIZE = Auxiliar.getParametroInteiroConfiguracao(Parametro.tamanho_lote_commit_operacao_1);

	private String tipoCarga;

	private boolean deveProcessarProcessosPje;
	private boolean deveProcessarProcessosSistemaLegadoNaoMigradosParaOPje;
	private boolean deveProcessarProcessosSistemaLegadoMigradosParaOPJe;

	private Connection conexaoBasePrincipal;
	private Connection conexaoBasePrincipalLegado;
	private Connection conexaoBaseStagingEGestao;

	public static void main(String[] args) throws Exception {
		Auxiliar.prepararPastaDeSaida();
		try (Op_1_BaixaListaDeNumerosDeProcessos baixaDados = new Op_1_BaixaListaDeNumerosDeProcessos()) {
			baixaDados.executarOperacao();
		}
	}

	public Op_1_BaixaListaDeNumerosDeProcessos() {
		this.deveProcessarProcessosPje = Auxiliar.deveProcessarProcessosPje();
		this.deveProcessarProcessosSistemaLegadoNaoMigradosParaOPje = Auxiliar
				.deveProcessarProcessosSistemaLegadoNaoMigradosParaOPje();
		this.deveProcessarProcessosSistemaLegadoMigradosParaOPJe = Auxiliar
				.deveProcessarProcessosSistemaLegadoMigradosParaOPJe();

		this.tipoCarga = Auxiliar.getParametroConfiguracao(Parametro.tipo_carga_xml, true);
	}
	
	public void executarOperacao() throws Exception {
		ProgressoInterfaceGrafica progresso = new ProgressoInterfaceGrafica(
				"(1/5) Baixa lista de números de processos");
		try {
			progresso.setMax(2);

			progresso.incrementProgress();

			this.baixarListaProcessos();

			progresso.incrementProgress();

			AcumuladorExceptions.instance().mostrarExceptionsAcumuladas();
			LOGGER.info("Fim!");
		} finally {
			progresso.close();
		}
	}
	
	public void baixarListaProcessos() throws IOException, SQLException {
		LocalDate dataCorte = DataJudUtil.getDataCorte();
		TipoRemessaEnum tipoRemessa = TipoRemessaEnum.criarApartirDoLabel(this.tipoCarga);
		
		if (tipoRemessa == null) {
			// TODO: implementar os ajustes necessários para que a aplicação funcione para os tipos de carga:
			// TODOS_COM_MOVIMENTACOES, TESTES e PROCESSO. Outra possibilidade é remover de vez essas cargas do código.
			throw new RuntimeException("Apenas os tipos de carga MENSAL e COMPLETA estão funcionando adequadamente.");
		}
		
		Remessa remessaAtual = this.obterRemessaAtual(dataCorte, tipoRemessa);
		if (remessaAtual.getId() == null) {
			this.salvarRemessa(remessaAtual, true);						
		}

		int [] graus = {1, 2};

		for (int grau : graus) {
			if (Auxiliar.deveProcessarGrau(grau)) {
				Map<String, DadosBasicosProcessoDto> mapDadosBasicosProcessos = this.baixarListaProcessos(grau);
				remessaAtual = this.gravarListaProcessosEmBanco(mapDadosBasicosProcessos, grau, remessaAtual);
				this.fecharConexoes();
			}
		}
	}

	private void fecharConexoes() {
		this.conexaoBasePrincipal = null;
		this.conexaoBasePrincipalLegado = null;
		this.conexaoBaseStagingEGestao = null;
	}
	
	private Remessa obterRemessaAtual(LocalDate dataCorte, TipoRemessaEnum tipoRemessa) {
		Remessa remessa = remessaDAO.getRemessa(dataCorte, tipoRemessa, true, false);

		if (remessa == null) {
			remessa = new Remessa();
			remessa.setDataCorte(dataCorte);
			remessa.setTipoRemessa(tipoRemessa);
		}
		return remessa;
	}

	private Remessa salvarRemessa(Remessa remessa, boolean isInsert) {
		Remessa retorno = null;
		try {
			JPAUtil.iniciarTransacao();

			if (isInsert) {
				remessaDAO.incluir(remessa);				
			} else {
				retorno = remessaDAO.alterar(remessa);
			}

			JPAUtil.commit();
		} catch (Exception e) {
			String origemOperacao = "Erro ao salvar remessa.";
			AcumuladorExceptions.instance().adicionarException(origemOperacao,
					"Erro ao salvar remessa: " + e.getLocalizedMessage(), e, true);
			JPAUtil.rollback();
		} finally {
			// JPAUtil.printEstatisticas();
			JPAUtil.close();
		}
		return retorno;
	}

	public Map<String, DadosBasicosProcessoDto> baixarListaProcessos(int grau) throws IOException, SQLException {
		LOGGER.info("Executando consulta " + this.tipoCarga + " para o " + grau + "G...");
		ResultSet rsConsultaProcessosPje = null;
		ResultSet rsConsultaProcessosMigradosLegado = null;
		ResultSet rsConsultaProcessosNaoMigradosLegado = null;
		String pastaIntermediariaPje = Auxiliar.getPastaResources(BaseEmAnaliseEnum.PJE, grau);
		String pastaIntermediariaLegado = Auxiliar.getPastaResources(BaseEmAnaliseEnum.LEGADO, grau);

		if ("TESTES".equals(this.tipoCarga)) {

			// Se usuário selecionou carga "TESTES" no parâmetro "tipo_carga_xml", pega um
			// lote qualquer de processos
			if (this.deveProcessarProcessosPje) {
				String sql = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/"
						+ pastaIntermediariaPje + "/carga_testes.sql");
				rsConsultaProcessosPje = getConexaoBasePrincipalPJe(grau).createStatement(ResultSet.TYPE_FORWARD_ONLY,
						ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD).executeQuery(sql);
			}
			if (this.deveProcessarProcessosSistemaLegadoMigradosParaOPJe) {
				String sqlMigradosLegado = Auxiliar
						.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/"
								+ pastaIntermediariaLegado + "/carga_testes_migrados.sql");
				rsConsultaProcessosMigradosLegado = getConexaoBasePrincipalLegado(grau)
						.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
								ResultSet.FETCH_FORWARD)
						.executeQuery(sqlMigradosLegado);
			}
			if (this.deveProcessarProcessosSistemaLegadoNaoMigradosParaOPje) {
				String sqlNaoMigradosLegado = Auxiliar
						.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/"
								+ pastaIntermediariaLegado + "/carga_testes_nao_migrados.sql");
				rsConsultaProcessosNaoMigradosLegado = getConexaoBasePrincipalLegado(grau)
						.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
								ResultSet.FETCH_FORWARD)
						.executeQuery(sqlNaoMigradosLegado);
			}

			LOGGER.warn(
					">>>>>>>>>> CUIDADO! Somente uma fração dos dados está sendo carregada, para testes! Atente ao parâmetro 'tipo_carga_xml', nas configurações!! <<<<<<<<<<");

		} else if (this.tipoCarga.startsWith("PROCESSO ")) {

			// Se usuário preencheu um número de processo no parâmetro "tipo_carga_xml",
			// carrega somente os dados dele
			String numeroProcesso = DataJudUtil.getNumeroProcessoCargaProcesso(this.tipoCarga);

			// Carrega o SQL do arquivo
			if (this.deveProcessarProcessosPje) {
				String sql = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/"
						+ pastaIntermediariaPje + "/carga_um_processo.sql");
				PreparedStatement ps = getConexaoBasePrincipalPJe(grau).prepareStatement(sql,
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);
				ps.setString(1, numeroProcesso);
				rsConsultaProcessosPje = ps.executeQuery();
			}
			if (this.deveProcessarProcessosSistemaLegadoMigradosParaOPJe) {
				String sqlMigradosLegado = Auxiliar
						.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/"
								+ pastaIntermediariaLegado + "/carga_um_processo_migrado.sql");
				PreparedStatement psMigradosLegado = getConexaoBasePrincipalLegado(grau).prepareStatement(
						sqlMigradosLegado, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
						ResultSet.FETCH_FORWARD);
				psMigradosLegado.setString(1, numeroProcesso);
				rsConsultaProcessosMigradosLegado = psMigradosLegado.executeQuery();
			}
			if (this.deveProcessarProcessosSistemaLegadoNaoMigradosParaOPje) {
				String sqlNaoMigradosLegado = Auxiliar
						.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/"
								+ pastaIntermediariaLegado + "/carga_um_processo_nao_migrado.sql");
				PreparedStatement psNaoMigradosLegado = getConexaoBasePrincipalLegado(grau).prepareStatement(
						sqlNaoMigradosLegado, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
						ResultSet.FETCH_FORWARD);
				psNaoMigradosLegado.setString(1, numeroProcesso);
				rsConsultaProcessosNaoMigradosLegado = psNaoMigradosLegado.executeQuery();
			}

			LOGGER.warn(">>>>>>>>>> CUIDADO! Somente estão sendo carregados os dados do processo " + numeroProcesso
					+ "! Atente ao parâmetro 'tipo_carga_xml', nas configurações!! <<<<<<<<<<");

		} else if ("COMPLETA".equals(this.tipoCarga)) {
			// Se usuário selecionou carga "COMPLETA" no parâmetro "tipo_carga_xml",
			// gera os XMLs de todos os processos que obedecerem às regras descritas no site
			// do CNJ
			if (this.deveProcessarProcessosPje) {
				String dataFinalComHoras = DataJudUtil.getDataPeriodoDeCorte(false);
				LOGGER.info(
						"* Considerando movimentações entre '01-01-2015 00:00:00.000' e '" + dataFinalComHoras + "'");

				String sql = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/"
						+ pastaIntermediariaPje + "/carga_completa_egestao_" + grau + "g.sql");
				getConexaoBaseStagingEGestao(grau).createStatement().execute("SET search_path TO pje_eg");
				PreparedStatement statement = getConexaoBaseStagingEGestao(grau).prepareStatement(sql,
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);
				statement.setDate(1, java.sql.Date.valueOf(DataJudUtil.getDataCorte()));
				statement.setDate(2, java.sql.Date.valueOf(DataJudUtil.getDataCorte()));
				statement.setString(3, dataFinalComHoras);
				statement.setFetchSize(100);
				rsConsultaProcessosPje = statement.executeQuery();
			}
			if (this.deveProcessarProcessosSistemaLegadoMigradosParaOPJe) {
				String sqlMigradosLegado = Auxiliar
						.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/"
								+ pastaIntermediariaLegado + "/carga_completa_migrados.sql");
				Statement statementMigradosLegado = getConexaoBasePrincipalLegado(grau).createStatement(
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);
				statementMigradosLegado.setFetchSize(100);
				rsConsultaProcessosMigradosLegado = statementMigradosLegado.executeQuery(sqlMigradosLegado);
			}
			if (this.deveProcessarProcessosSistemaLegadoNaoMigradosParaOPje) {
				String sqlNaoMigradosLegado = Auxiliar
						.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/"
								+ pastaIntermediariaLegado + "/carga_completa_nao_migrados.sql");
				Statement statementNaoMigradosLegado = getConexaoBasePrincipalLegado(grau).createStatement(
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);
				statementNaoMigradosLegado.setFetchSize(100);
				rsConsultaProcessosNaoMigradosLegado = statementNaoMigradosLegado.executeQuery(sqlNaoMigradosLegado);
			}

		} else if ("TODOS_COM_MOVIMENTACOES".equals(this.tipoCarga)) {

			// Se usuário selecionou carga "TODOS_COM_MOVIMENTACOES" no parâmetro "tipo_carga_xml",
			// gera os XMLs de todos os processos que tiveram qualquer movimentação processual na
			// tabela tb_processo_evento.

			String dataFinalComHoras = DataJudUtil.getDataPeriodoDeCorte(false);
			if (this.deveProcessarProcessosPje) {
				String sql = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/"
						+ pastaIntermediariaPje + "/carga_todos_com_movimentacoes.sql");
				PreparedStatement statement = getConexaoBasePrincipalPJe(grau).prepareStatement(sql,
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);
				statement.setString(1, dataFinalComHoras);
				statement.setFetchSize(100);
				rsConsultaProcessosPje = statement.executeQuery();
			}
			if (this.deveProcessarProcessosSistemaLegadoMigradosParaOPJe) {
				String sqlMigradosLegado = Auxiliar
						.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/"
								+ pastaIntermediariaLegado + "/carga_todos_com_movimentacoes_migrados.sql");
				Statement statementMigradosLegado = getConexaoBasePrincipalLegado(grau).createStatement(
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);
				statementMigradosLegado.setFetchSize(100);
				rsConsultaProcessosMigradosLegado = statementMigradosLegado.executeQuery(sqlMigradosLegado);
			}
			if (this.deveProcessarProcessosSistemaLegadoNaoMigradosParaOPje) {
				String sqlNaoMigradosLegado = Auxiliar
						.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/"
								+ pastaIntermediariaLegado + "/carga_todos_com_movimentacoes_nao_migrados.sql");
				Statement statementNaoMigradosLegado = getConexaoBasePrincipalLegado(grau).createStatement(
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);
				statementNaoMigradosLegado.setFetchSize(100);
				rsConsultaProcessosNaoMigradosLegado = statementNaoMigradosLegado.executeQuery(sqlNaoMigradosLegado);
			}

		} else if (this.tipoCarga.equals("MENSAL")) {
			// Identifica o início e o término do mês selecionado
			String dataInicial = DataJudUtil.getDataPeriodoDeCorte(true);
			String dataFinal = DataJudUtil.getDataPeriodoDeCorte(false);
			LOGGER.info("* Considerando movimentações entre '" + dataInicial + "' e '" + dataFinal + "'");
			if (this.deveProcessarProcessosPje) {
				// Se usuário selecionou carga "MENSAL" no parâmetro "tipo_carga_xml", utiliza
				// as regras definidas pelo CNJ

				String sql = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/"
						+ pastaIntermediariaPje + "/carga_mensal.sql");
				PreparedStatement statement = getConexaoBasePrincipalPJe(grau).prepareStatement(sql,
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);
				statement.setString(1, dataInicial);
				statement.setString(2, dataFinal);
				statement.setFetchSize(100);
				rsConsultaProcessosPje = statement.executeQuery();
			}
			if (this.deveProcessarProcessosSistemaLegadoMigradosParaOPJe) {
				String sqlMigradosLegado = Auxiliar
						.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/"
								+ pastaIntermediariaLegado + "/carga_mensal_migrados.sql");
				Statement statementMigradosLegado = getConexaoBasePrincipalLegado(grau).createStatement(
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);
				statementMigradosLegado.setFetchSize(100);
				rsConsultaProcessosMigradosLegado = statementMigradosLegado.executeQuery(sqlMigradosLegado);
			}
			if (this.deveProcessarProcessosSistemaLegadoNaoMigradosParaOPje) {
				String sqlNaoMigradosLegado = Auxiliar
						.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/"
								+ pastaIntermediariaLegado + "/carga_mensal_nao_migrados.sql");
				Statement statementNaoMigradosLegado = getConexaoBasePrincipalLegado(grau).createStatement(
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);
				statementNaoMigradosLegado.setFetchSize(100);
				rsConsultaProcessosNaoMigradosLegado = statementNaoMigradosLegado.executeQuery(sqlNaoMigradosLegado);
			}

		} else {
			throw new RuntimeException("Valor desconhecido para o parâmetro 'tipo_carga_xml': " + this.tipoCarga);
		}

		Map<String, DadosBasicosProcessoDto> mapProcessos = new HashMap<String, DadosBasicosProcessoDto>();
		if (rsConsultaProcessosPje != null) {
			mapProcessos.putAll(this.obterDadosBasicosProcessos(rsConsultaProcessosPje, OrigemProcessoEnum.PJE, grau));
		}
		if (rsConsultaProcessosMigradosLegado != null) {
			Map<String, DadosBasicosProcessoDto> mapProcessosHibridos = this.obterDadosBasicosProcessos(
					rsConsultaProcessosMigradosLegado, OrigemProcessoEnum.HIBRIDO, grau);

			for (String chave : mapProcessosHibridos.keySet()) {
				if (mapProcessos.containsKey(chave)) {
					mapProcessos.get(chave).setOrigemProcessoEnum(OrigemProcessoEnum.HIBRIDO);
				} else {
					DadosBasicosProcessoDto processoHibrido = mapProcessosHibridos.get(chave);
					LOGGER.error("O processo (grau: " + processoHibrido.getGrau() 
							+ "; número: " + processoHibrido.getNumeroProcesso()
							+ ") foi localizado no Sistema Judicial Legado "
							+ "como sendo um processo Híbrido, mas o mesmo não faz parte dos processos do PJe que serão enviados. "
							+ "Logo, tal processo não será enviado na Remessa atual.");
				}
			}
		}
		if (rsConsultaProcessosNaoMigradosLegado != null) {
			Map<String, DadosBasicosProcessoDto> mapProcessosLegados = this
					.obterDadosBasicosProcessos(rsConsultaProcessosNaoMigradosLegado, OrigemProcessoEnum.LEGADO, grau);

			for (String chave : mapProcessosLegados.keySet()) {
				DadosBasicosProcessoDto processoLegado = mapProcessosLegados.get(chave);
				if (mapProcessos.containsKey(chave)) {
					LOGGER.error("O processo (grau: " + processoLegado.getGrau() + "; número: "
							+ processoLegado.getNumeroProcesso()
							+ ") foi localizado no Sistema Judicial Legado como sendo um processo não migrado. "
							+ "No entanto, o mesmo faz parte dos processos do PJe que serão enviados. "
							+ "Este processo será enviado apenas com as informações presentes na base do PJe.");
				} else {
					mapProcessos.put(chave, processoLegado);
				}
			}
		}

		return mapProcessos;
	}

	/**
	 * Recupera as informações básicas dos processos que serão adicionados à Remessa atual.
	 * 
	 * @param rsConsultaProcessos
	 * @param origemProcesso
	 * @param grau
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 */
	public Map<String, DadosBasicosProcessoDto> obterDadosBasicosProcessos(ResultSet rsConsultaProcessos,
			OrigemProcessoEnum origemProcesso, int grau) throws IOException, SQLException {
		// Itera sobre os processos encontrados
		Map<String, DadosBasicosProcessoDto> mapDadosBasicosProcessos = new HashMap<String, DadosBasicosProcessoDto>();
		try {
			int qtdProcessos = 0;
			long tempo = System.currentTimeMillis();
			LOGGER.info("Iterando sobre a lista de processos. Origem: " + origemProcesso.getLabel() + ". Grau: " + 
					+ grau);
			
			while (rsConsultaProcessos.next()) {
				qtdProcessos++;

				String numProcesso = rsConsultaProcessos.getString("nr_processo");
				
				DadosBasicosProcessoDto dadosBasicosProcesso = new DadosBasicosProcessoDto(numProcesso, null,
						null, Integer.toString(grau), origemProcesso);

				mapDadosBasicosProcessos.put(numProcesso, dadosBasicosProcesso);

				// Mostra a quantidade de processos analisados a cada 15 segundos
				if (System.currentTimeMillis() - tempo > 15_000) {
					tempo = System.currentTimeMillis();
					LOGGER.info("Processos até agora: " + qtdProcessos);
				}
			}
		} catch (Exception ex) {
			String origemOperacao = "Erro na consulta de processos. Origem:  " + origemProcesso.getLabel() + ". Grau: "
					+ grau;
			AcumuladorExceptions.instance().adicionarException(origemOperacao,
					"Erro ao baixar lista de processos: " + ex.getLocalizedMessage(), ex, true);
		} finally {
			rsConsultaProcessos.close();
		}

		return mapDadosBasicosProcessos;
	}

	public Remessa gravarListaProcessosEmBanco(Map<String, DadosBasicosProcessoDto> mapChavesProcessos, int grau, Remessa remessa) {
		int qtdProcessosAEnviar = mapChavesProcessos.size();
		List<ProcessoEnvio> processosRemover = new ArrayList<ProcessoEnvio>();

		for (ProcessoEnvio processoEnvio : remessa.getProcessosEnvio()) {
			if (processoEnvio.getGrau().equals(Integer.toString(grau))) {
				String numProcesso = processoEnvio.getNumeroProcesso();
				DadosBasicosProcessoDto dadosBasicosProcesso = mapChavesProcessos.get(numProcesso);
				if (dadosBasicosProcesso != null) {
					if (!processoEnvio.getOrigem().equals(dadosBasicosProcesso.getOrigemProcessoEnum())) {
						processoEnvio.setOrigem(dadosBasicosProcesso.getOrigemProcessoEnum());
					}
					mapChavesProcessos.remove(numProcesso);
				} else {
					processosRemover.add(processoEnvio);
				}
			}
		}
		
		remessa.getProcessosEnvio().removeAll(processosRemover);
		
		remessa = this.salvarRemessa(remessa, false);


		int qtdLoteProcessosSalvos = 0;
		final AtomicInteger counter = new AtomicInteger();
		final Collection<List<DadosBasicosProcessoDto>> dadosBasicosProcessos = mapChavesProcessos.values().stream()
				.collect(Collectors.groupingBy(it -> counter.getAndIncrement() / COMMIT_SIZE)).values();

		for (List<DadosBasicosProcessoDto> loteProcessos : dadosBasicosProcessos) {
			try {
				JPAUtil.iniciarTransacao();
				for (DadosBasicosProcessoDto processo : loteProcessos) {
					ProcessoEnvio processoEnvio= new ProcessoEnvio();
					processoEnvio.setGrau(processo.getGrau());
					processoEnvio.setNumeroProcesso(processo.getNumeroProcesso());
					processoEnvio.setOrigem(processo.getOrigemProcessoEnum());
					processoEnvio.setRemessa(remessa);
					remessa.getProcessosEnvio().add(processoEnvio);
					
					processoEnvioDAO.incluir(processoEnvio);
					if (qtdLoteProcessosSalvos > 0 && qtdLoteProcessosSalvos % BATCH_SIZE == 0) {
						JPAUtil.flush();
						JPAUtil.clear();
					}
					qtdLoteProcessosSalvos++;
				}
				JPAUtil.commit();
			} catch (Exception e) {
				String origemOperacao = "Erro ao salvar lista de processos. Grau: " + grau;
				AcumuladorExceptions.instance().adicionarException(origemOperacao,
						"Erro ao salvar lista de processos: " + e.getLocalizedMessage(), e, true);
				JPAUtil.rollback();
			} finally {
				// JPAUtil.printEstatisticas();
				JPAUtil.close();
			}
		}

		LOGGER.info("Foram carregados " + qtdProcessosAEnviar + " processo(s) no " + grau + "º Grau.");
		return remessa;
	}

	public Connection getConexaoBasePrincipalPJe(int grau) throws SQLException {
		if (conexaoBasePrincipal == null) {
			conexaoBasePrincipal = Auxiliar.getConexao(grau, BaseEmAnaliseEnum.PJE);
			conexaoBasePrincipal.setAutoCommit(false);
		}
		return conexaoBasePrincipal;
	}

	public Connection getConexaoBasePrincipalLegado(int grau) throws SQLException {
		if (conexaoBasePrincipalLegado == null) {
			conexaoBasePrincipalLegado = Auxiliar.getConexao(grau, BaseEmAnaliseEnum.LEGADO);
			conexaoBasePrincipalLegado.setAutoCommit(false);
		}
		return conexaoBasePrincipalLegado;
	}

	public Connection getConexaoBaseStagingEGestao(int grau) throws SQLException {
		if (conexaoBaseStagingEGestao == null) {
			conexaoBaseStagingEGestao = Auxiliar.getConexaoStagingEGestao(grau);
			conexaoBaseStagingEGestao.setAutoCommit(false);
		}
		return conexaoBaseStagingEGestao;
	}

	/**
	 * Fecha conexão com o PJe e com o sistema judicial legado
	 */
	@Override
	public void close() {

		Auxiliar.fechar(conexaoBaseStagingEGestao);
		conexaoBaseStagingEGestao = null;

		Auxiliar.fechar(conexaoBasePrincipal);
		conexaoBasePrincipal = null;

		Auxiliar.fechar(conexaoBasePrincipalLegado);
		conexaoBasePrincipalLegado = null;
	}
}
