package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.AcumuladorExceptions;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ProgressoInterfaceGrafica;
import br.jus.trt4.justica_em_numeros_2016.dao.ChaveProcessoCNJDao;
import br.jus.trt4.justica_em_numeros_2016.dao.JPAUtil;
import br.jus.trt4.justica_em_numeros_2016.dao.LoteDao;
import br.jus.trt4.justica_em_numeros_2016.dao.LoteProcessoDao;
import br.jus.trt4.justica_em_numeros_2016.dao.RemessaDao;
import br.jus.trt4.justica_em_numeros_2016.dto.ChaveProcessoCNJDto;
import br.jus.trt4.justica_em_numeros_2016.entidades.ChaveProcessoCNJ;
import br.jus.trt4.justica_em_numeros_2016.entidades.Lote;
import br.jus.trt4.justica_em_numeros_2016.entidades.LoteProcesso;
import br.jus.trt4.justica_em_numeros_2016.entidades.Remessa;
import br.jus.trt4.justica_em_numeros_2016.enums.BaseEmAnaliseEnum;
import br.jus.trt4.justica_em_numeros_2016.enums.OrigemProcessoEnum;
import br.jus.trt4.justica_em_numeros_2016.enums.Parametro;
import br.jus.trt4.justica_em_numeros_2016.enums.SituacaoLoteEnum;
import br.jus.trt4.justica_em_numeros_2016.enums.SituacaoLoteProcessoEnum;
import br.jus.trt4.justica_em_numeros_2016.enums.TipoRemessaEnum;

/**
 * Monta uma lista de processos, conforme o parâmetro "tipo_carga_xml" do arquivo "config.properties".
 * 
 * @author felipe.giotto@trt4.jus.br
 */
public class Op_1_BaixaListaDeNumerosDeProcessos implements AutoCloseable {

	private static final Logger LOGGER = LogManager.getLogger(Op_1_BaixaListaDeNumerosDeProcessos.class);

	private static final RemessaDao remessaDAO = new RemessaDao();
	private static final ChaveProcessoCNJDao chaveProcessoCNJDAO = new ChaveProcessoCNJDao();
	private static final LoteDao loteDAO = new LoteDao();
	private static final LoteProcessoDao loteProcessosDAO = new LoteProcessoDao();

	// O valor do BATCH_SIZE deve ser igual à propriedade hibernate.jdbc.batch_size no persistence.xml
	private static final int BATCH_SIZE = 50;
	private static final int COMMIT_SIZE = Auxiliar.getParametroInteiroConfiguracao(Parametro.tamanho_lote_commit_operacao_1);
	
	private String tipoCarga;
	private int mesCorte;
	private int anoCorte;

	private boolean deveProcessarProcessosPje;
	private boolean deveProcessarProcessosSistemaLegadoNaoMigradosParaOPje;
	private boolean deveProcessarProcessosSistemaLegadoMigradosParaOPJe;

	private Connection conexaoBasePrincipal;
	private Connection conexaoBasePrincipalLegado;
	private Connection conexaoBaseStagingEGestao;
	private static final Pattern pCargaProcesso = Pattern
			.compile("^PROCESSO (\\d{7}\\-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4})$");
	private static final Pattern pMesAnoCorte = Pattern.compile("^(\\d+)-(\\d+)$");

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

		String mesAnoCorte = Auxiliar.getParametroConfiguracao(Parametro.mes_ano_corte, true);
		Matcher matcher = pMesAnoCorte.matcher(mesAnoCorte);
		if (!matcher.find()) {
			throw new RuntimeException(
					"Parâmetro 'mes_ano_corte' não especifica corretamente o ano e o mês que precisam ser baixados! Verifique o arquivo 'config.properties'");
		}

		this.mesCorte = this.getMesCorte(matcher);
		this.anoCorte = this.getAnoCorte(matcher);
	}
	
	public void executarOperacao() throws Exception {
		ProgressoInterfaceGrafica progresso = new ProgressoInterfaceGrafica(
				"(1/6) Baixa lista de números de processos");
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
		LocalDate dataCorte = LocalDate.now().withMonth(this.mesCorte).withYear(this.anoCorte);
		dataCorte = dataCorte.withDayOfMonth(dataCorte.lengthOfMonth());
		TipoRemessaEnum tipoRemessa = TipoRemessaEnum.criarApartirDoLabel(this.tipoCarga);

		Lote loteAtual = this.obterLoteAtual(dataCorte, tipoRemessa);
		
		this.salvarRemessa(loteAtual.getRemessa());
		
		int [] graus = {1, 2};

		for (int grau : graus) {
			if (Auxiliar.deveProcessarGrau(grau)) {
				Map<String, ChaveProcessoCNJDto> mapChavesProcessos = this.baixarListaProcessos(grau, loteAtual);
				this.gravarListaProcessosEmBanco(mapChavesProcessos, grau, loteAtual);
			}
		}

		this.atualizarSituacaoLoteCriado(loteAtual);
	}

	public Lote obterLoteAtual(LocalDate dataCorte, TipoRemessaEnum tipoRemessa) {
		if (tipoRemessa == null) {
			// TODO: implementar os ajustes necessários para que a aplicação funcione para os tipos de carga:
			// TODOS_COM_MOVIMENTACOES, TESTES e PROCESSO. Outra possibilidade é remover de vez essas cargas do código.
			throw new RuntimeException("Apenas os tipos de carga MENSAL e COMPLETA estão funcionando adequadamente.");
		}

		Lote ultimoLoteRemessa = loteDAO.getUltimoLoteDeUmaRemessa(dataCorte, tipoRemessa, false);

		Remessa remessa = null;
		Lote loteAtual = new Lote();
		if (ultimoLoteRemessa == null) {
			// Gerando o primeiro lote de uma remessa
			remessa = new Remessa();
			remessa.setDataCorte(dataCorte);
			remessa.setTipoRemessa(tipoRemessa);

			loteAtual.setRemessa(remessa);
			loteAtual.setSituacao(SituacaoLoteEnum.CRIADO_PARCIALMENTE);
			loteAtual.setNumero("1");

			remessa.getLotes().add(loteAtual);
		} else {
			remessa = ultimoLoteRemessa.getRemessa();
			if (ultimoLoteRemessa.getSituacao().equals(SituacaoLoteEnum.CRIADO)) {
				// Criando um novo lote para uma remessa existente
				Integer numeroProximoLote = new Integer(ultimoLoteRemessa.getNumero()) + 1;

				loteAtual.setRemessa(remessa);
				loteAtual.setSituacao(SituacaoLoteEnum.CRIADO_PARCIALMENTE);
				loteAtual.setNumero(numeroProximoLote.toString());

				remessa.getLotes().add(loteAtual);
			} else if (ultimoLoteRemessa.getSituacao().equals(SituacaoLoteEnum.CRIADO_PARCIALMENTE)) {
				// Atualizando o último lote da remessa que foi criado parcialmente. As listas serão carregadas.
				loteAtual = loteDAO.getUltimoLoteDeUmaRemessa(dataCorte, tipoRemessa, true);
			}
		}
		
		return loteAtual;
	}

	private void salvarRemessa(Remessa remessa) {
		try {
			JPAUtil.iniciarTransacao();

			remessaDAO.incluir(remessa);

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
	}
	
	private void atualizarSituacaoLoteCriado(Lote lote) {
		try {
			JPAUtil.iniciarTransacao();

			lote.setSituacao(SituacaoLoteEnum.CRIADO);

			loteDAO.alterar(lote);

			JPAUtil.commit();
		} catch (Exception e) {
			String origemOperacao = "Erro ao salvar situação do lote.";
			AcumuladorExceptions.instance().adicionarException(origemOperacao,
					"Erro ao salvar situação do lote: " + e.getLocalizedMessage(), e, true);
			JPAUtil.rollback();
		} finally {
			// JPAUtil.printEstatisticas();
			JPAUtil.close();
		}
	}

	public Map<String, ChaveProcessoCNJDto> baixarListaProcessos(int grau, Lote lote) throws IOException, SQLException {
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
			// carrega
			// somente os dados dele
			Matcher m = pCargaProcesso.matcher(this.tipoCarga);
			if (!m.find()) {
				throw new RuntimeException(
						"Parâmetro 'tipo_carga_xml' não especifica corretamente o processo que precisa ser baixado! Verifique o arquivo 'config.properties'");
			}
			String numeroProcesso = m.group(1);

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
				String dataFinalComHoras = this.getDataCorte(false, true);
				String dataFinalSemHoras = this.getDataCorte(false, false);
				LOGGER.info(
						"* Considerando movimentações entre '01-01-2015 00:00:00.000' e '" + dataFinalComHoras + "'");

				String sql = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/"
						+ pastaIntermediariaPje + "/carga_completa_egestao_" + grau + "g.sql");
				getConexaoBaseStagingEGestao(grau).createStatement().execute("SET search_path TO pje_eg");
				PreparedStatement statement = getConexaoBaseStagingEGestao(grau).prepareStatement(sql,
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);
				statement.setDate(1, java.sql.Date.valueOf(dataFinalSemHoras));
				statement.setDate(2, java.sql.Date.valueOf(dataFinalSemHoras));
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

			String dataFinalComHoras = this.getDataCorte(false, true);
			if (this.deveProcessarProcessosPje) {
				String sql = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_1_baixa_lista_processos/"
						+ pastaIntermediariaPje + "/carga_todos_com_movimentacoes.sql");
				PreparedStatement statement = getConexaoBasePrincipalPJe(grau).prepareStatement(sql,
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);
				statement.setDate(1, java.sql.Date.valueOf(dataFinalComHoras));
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

			if (this.deveProcessarProcessosPje) {
				// Se usuário selecionou carga "MENSAL" no parâmetro "tipo_carga_xml", utiliza
				// as regras definidas pelo CNJ

				// Identifica o início e o término do mês selecionado
				String dataInicial = this.getDataCorte(true, true);
				String dataFinal = this.getDataCorte(false, true);
				LOGGER.info("* Considerando movimentações entre '" + dataInicial + "' e '" + dataFinal + "'");

				// Carrega o SQL do arquivo
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

		Map<String, ChaveProcessoCNJDto> mapChavesProcessos = new HashMap<String, ChaveProcessoCNJDto>();
		if (rsConsultaProcessosPje != null) {
			mapChavesProcessos.putAll(obterChavesProcessos(rsConsultaProcessosPje, OrigemProcessoEnum.PJE, grau, lote));
			if (rsConsultaProcessosMigradosLegado != null) {
				Map<String, ChaveProcessoCNJDto> mapChavesProcessosHibridos = obterChavesProcessos(
						rsConsultaProcessosMigradosLegado, OrigemProcessoEnum.HIBRIDO, grau, lote);

				for (String chave : mapChavesProcessosHibridos.keySet()) {
					if (mapChavesProcessos.containsKey(chave)) {
						mapChavesProcessos.get(chave).setOrigemProcessoEnum(OrigemProcessoEnum.HIBRIDO);
					} else {
						ChaveProcessoCNJDto chaveProcessoHibrido = mapChavesProcessosHibridos.get(chave);
						LOGGER.warn("O processo (grau: " + chaveProcessoHibrido.getGrau() + "; número: "
								+ chaveProcessoHibrido.getNumeroProcesso() + "; código classe: "
								+ chaveProcessoHibrido.getCodigoClasseJudicial() + "; código órgão julgador: "
								+ chaveProcessoHibrido.getCodigoOrgaoJulgador()
								+ " foi localizado no Sistema Judicial Legado "
								+ "como sendo um processo Híbrido, mas o mesmo não foi localizado na base do PJE. Tal processo não será enviado.");
					}
				}
			}
		}
		if (rsConsultaProcessosNaoMigradosLegado != null) {
			mapChavesProcessos.putAll(
					obterChavesProcessos(rsConsultaProcessosNaoMigradosLegado, OrigemProcessoEnum.LEGADO, grau, lote));
		}

		return mapChavesProcessos;

	}

	/**
	 * Recupera as informações básicas dos processos que serão adicionados ao novo lote da Remessa atual.
	 * 
	 * @param rsConsultaProcessos
	 * @param origemProcesso
	 * @param grau
	 * @param lote
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 */
	public Map<String, ChaveProcessoCNJDto> obterChavesProcessos(ResultSet rsConsultaProcessos,
			OrigemProcessoEnum origemProcesso, int grau, Lote lote) throws IOException, SQLException {
		// Itera sobre os processos encontrados
		Map<String, ChaveProcessoCNJDto> mapChavesProcessos = new HashMap<String, ChaveProcessoCNJDto>();
		try {
			int qtdProcessos = 0;
			long tempo = System.currentTimeMillis();
			LOGGER.info("Iterando sobre a lista de processos...");
			while (rsConsultaProcessos.next()) {
				qtdProcessos++;

				String numProcesso = rsConsultaProcessos.getString("nr_processo");
				String codClasse = rsConsultaProcessos.getString("cd_classe_judicial");
				Long codOrgaoJulgador = rsConsultaProcessos.getLong("cd_orgao_julgador");

				ChaveProcessoCNJDto chaveProcessoCNJDto = new ChaveProcessoCNJDto(numProcesso, codClasse,
						codOrgaoJulgador, Integer.toString(grau), origemProcesso);

				mapChavesProcessos.put(
						this.getChaveMapa(Integer.toString(grau), numProcesso, codClasse, codOrgaoJulgador),
						chaveProcessoCNJDto);

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

		return mapChavesProcessos;
	}

	public void gravarListaProcessosEmBanco(Map<String, ChaveProcessoCNJDto> mapChavesProcessos, int grau, Lote lote) {

		// Quando a inserção é realizada em um lote cadastrado parcialmente, só será necessário adicionar
		// no lote os processos que ainda não foram inseridos.
		for (LoteProcesso loteProcesso : lote.getLotesProcessos()) {
			ChaveProcessoCNJ chaveProcessoCNJ = loteProcesso.getChaveProcessoCNJ();
			String chaveMapa = this.getChaveMapa(chaveProcessoCNJ.getGrau(), chaveProcessoCNJ.getNumeroProcesso(),
					chaveProcessoCNJ.getCodigoClasseJudicial(), chaveProcessoCNJ.getCodigoOrgaoJulgador());
			if (mapChavesProcessos.containsKey(chaveMapa)) {
				mapChavesProcessos.remove(chaveMapa);
			}
		}

		int qtdLoteProcessosSalvos = 0;

		final AtomicInteger counter = new AtomicInteger();
		final Collection<List<ChaveProcessoCNJDto>> chavesProcessos = mapChavesProcessos.values().stream()
				.collect(Collectors.groupingBy(it -> counter.getAndIncrement() / COMMIT_SIZE)).values();

		for (List<ChaveProcessoCNJDto> loteChavesProcessos : chavesProcessos) {
			try {
				JPAUtil.iniciarTransacao();
				for (ChaveProcessoCNJDto chaveProcesso : loteChavesProcessos) {
					ChaveProcessoCNJ chaveProcessoCNJPersistida = chaveProcessoCNJDAO.getChaveProcessoCNJ(
							chaveProcesso.getNumeroProcesso(), chaveProcesso.getCodigoClasseJudicial(),
							chaveProcesso.getCodigoOrgaoJulgador(), Integer.toString(grau));
					ChaveProcessoCNJ chaveProcessoCNJ = new ChaveProcessoCNJ();
					if (chaveProcessoCNJPersistida == null) {
						chaveProcessoCNJ.setCodigoOrgaoJulgador(chaveProcesso.getCodigoOrgaoJulgador());
						chaveProcessoCNJ.setCodigoClasseJudicial(chaveProcesso.getCodigoClasseJudicial());
						chaveProcessoCNJ.setNumeroProcesso(chaveProcesso.getNumeroProcesso());
						chaveProcessoCNJ.setGrau(Integer.toString(grau));
					} else {
						chaveProcessoCNJ = chaveProcessoCNJPersistida;
					}

					LoteProcesso loteProcesso = new LoteProcesso();
					loteProcesso.setChaveProcessoCNJ(chaveProcessoCNJ);
					loteProcesso.setLote(lote);
					loteProcesso.setOrigem(chaveProcesso.getOrigemProcessoEnum());
					loteProcesso.setSituacao(SituacaoLoteProcessoEnum.AGUARDANDO_GERACAO_XML);

					loteProcessosDAO.incluir(loteProcesso);
					if (qtdLoteProcessosSalvos > 0 && qtdLoteProcessosSalvos % BATCH_SIZE == 0) {
						loteProcessosDAO.flush();
						loteProcessosDAO.clear();
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

		LOGGER.info("Foram carregados " + mapChavesProcessos.size() + " processo(s) no " + grau + "º Grau.");
	}

	private String getChaveMapa(String grau, String numProcesso, String codClasse, Long codOrgaoJulgador) {
		return (grau + "_" + codClasse + "_" + numProcesso + "_" + codOrgaoJulgador);
	}

	private int getAnoCorte(Matcher matcher) {
		return Integer.parseInt(matcher.group(1));
	}

	private int getMesCorte(Matcher matcher) {
		return Integer.parseInt(matcher.group(2));
	}

	private String getDataCorte(boolean returnarDataInicio, boolean informarHoras) {
		String data = null;
		if (returnarDataInicio) {
			data = this.anoCorte + "-" + this.mesCorte + (informarHoras ? "-1 00:00:00.000" : "");
		} else {
			int maiorDiaNoMes = new GregorianCalendar(this.anoCorte, (this.mesCorte - 1), 1)
					.getActualMaximum(Calendar.DAY_OF_MONTH);
			data = this.anoCorte + "-" + this.mesCorte + "-" + maiorDiaNoMes + (informarHoras ? " 23:59:59.999" : "");
		}
		return data;
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
