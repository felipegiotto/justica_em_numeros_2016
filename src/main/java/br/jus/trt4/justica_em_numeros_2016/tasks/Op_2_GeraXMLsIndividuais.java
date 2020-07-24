package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.cnj.modeloDeTransferenciaDeDados.ModalidadePoloProcessual;
import br.jus.cnj.modeloDeTransferenciaDeDados.ModalidadeRepresentanteProcessual;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoAssuntoProcessual;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoCabecalhoProcesso;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoComplementoNacional;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoEndereco;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoMovimentoNacional;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoMovimentoProcessual;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoOrgaoJulgador;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoParametro;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoParte;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoPessoa;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoPoloProcessual;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoProcessoJudicial;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoQualificacaoPessoa;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoRelacaoIncidental;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoRepresentanteProcessual;
import br.jus.cnj.replicacao_nacional.Processos;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.AcumuladorExceptions;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DataJudException;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.IdentificaDocumentosPessoa;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.IdentificaGeneroPessoa;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.NamedParameterStatement;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Parametro;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ProgressoInterfaceGrafica;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ValidadorIntegridadeXMLCNJ;
import br.jus.trt4.justica_em_numeros_2016.dto.AssuntoDto;
import br.jus.trt4.justica_em_numeros_2016.dto.ClasseJudicialDto;
import br.jus.trt4.justica_em_numeros_2016.dto.ComplementoDto;
import br.jus.trt4.justica_em_numeros_2016.dto.DocumentoDto;
import br.jus.trt4.justica_em_numeros_2016.dto.DocumentoPessoaDto;
import br.jus.trt4.justica_em_numeros_2016.dto.EnderecoDto;
import br.jus.trt4.justica_em_numeros_2016.dto.HistoricoDeslocamentoOJDto;
import br.jus.trt4.justica_em_numeros_2016.dto.MovimentoDto;
import br.jus.trt4.justica_em_numeros_2016.dto.ParteProcessualDto;
import br.jus.trt4.justica_em_numeros_2016.dto.PoloDto;
import br.jus.trt4.justica_em_numeros_2016.dto.ProcessoDto;
import br.jus.trt4.justica_em_numeros_2016.enums.BaseEmAnaliseEnum;
import br.jus.trt4.justica_em_numeros_2016.tabelas_cnj.AnalisaAssuntosCNJ;
import br.jus.trt4.justica_em_numeros_2016.tabelas_cnj.AnalisaClassesProcessuaisCNJ;
import br.jus.trt4.justica_em_numeros_2016.tabelas_cnj.AnalisaMovimentosCNJ;
import br.jus.trt4.justica_em_numeros_2016.tabelas_cnj.AnalisaServentiasCNJ;
import br.jus.trt4.justica_em_numeros_2016.tabelas_cnj.ServentiaCNJ;

/**
 * Carrega as listas de processos geradas pela classe {@link Op_1_BaixaListaDeNumerosDeProcessos} e,
 * para cada processo, gera seu arquivo XML na pasta "output/.../Xg/xmls_individuais".
 * 
 * TODO: Tratar bug no PJe que faz com que haja mais de um processo com mesmo número. Sugestão: usar os IDs nas consultas SQL de partes, documentos, endereços, assuntos, movimentos, etc.
 *
 * TODO: Criar parâmetro para que somente movimentos existentes no "depara-jt-cnj" sejam inseridos no XML. Movimentos não reconhecidos pelo "depara-jt-cnj" ficariam, então, de fora.
 *
 * Fonte: http://www.mkyong.com/java/jaxb-hello-world-example/
 * 
 * @author felipe.giotto@trt4.jus.br
 */
public class Op_2_GeraXMLsIndividuais implements Closeable {

	private static final Logger LOGGER = LogManager.getLogger(Op_2_GeraXMLsIndividuais.class);
	private static ProgressoInterfaceGrafica progresso;
	private int grau;
	private BaseEmAnaliseEnum baseEmAnalise;
	private boolean deveProcessarProcessosSistemaLegadoMigradosParaOPJe;
	private Connection conexaoBasePrincipal;
	private Connection conexaoBaseLegadoMigrados;
	private NamedParameterStatement nsConsultaProcessos;
	private NamedParameterStatement nsPartes;
	private NamedParameterStatement nsDocumentos;
	private NamedParameterStatement nsEnderecos;
	private NamedParameterStatement nsAssuntos;
	private NamedParameterStatement nsMovimentos;
	private NamedParameterStatement nsComplementos;
	private NamedParameterStatement nsIncidentes;
	private NamedParameterStatement nsSentencasAcordaos;
	private NamedParameterStatement nsHistoricoDeslocamentoOJ;
	private NamedParameterStatement nsConsultaProcessosLegadosMigrados;
	private NamedParameterStatement nsMovimentosLegadosMigrados;
	private NamedParameterStatement nsComplementosLegadosMigrados;
	private NamedParameterStatement nsSentencasAcordaosLegadosMigrados;
	private int codigoMunicipioIBGETRT;
	private static AnalisaServentiasCNJ processaServentiasCNJ;
	private AnalisaAssuntosCNJ analisaAssuntosCNJ;
	private AnalisaClassesProcessuaisCNJ analisaClassesProcessuaisCNJ;
	private AnalisaMovimentosCNJ analisaMovimentosCNJ;
	private IdentificaGeneroPessoa identificaGeneroPessoa;
	private IdentificaDocumentosPessoa identificaDocumentosPessoa;
	private List<String> listaProcessos;
	private String statusString;
	private Map<String, String> mapaProcessosLegadoMigrados;
	
	// Objetos que armazenam os dados do PJe para poder trazer dados de processos em lote,
	// resultando em menos consultas ao banco de dados.
	private final Map<String, ProcessoDto> cacheProcessosDtos = new HashMap<>();
	
	// Objetos que armazenam os dados que foram migrados do Sistema Judicial Legado para o Pje, para poder trazer dados de processos em lote,
	// resultando em menos consultas ao banco de dados.
	private final Map<String, ProcessoDto> cacheProcessosLegadosMigradosDtos = new HashMap<>();

	/**
	 * @deprecated TODO: migrar, futuramente, para nova estrutura de controle "ProcessoFluxo" e centralizar o controle a partir da classe "Op_Y_OperacaoFluxoContinuo"
	 *
	 */
	@Deprecated
	private class OperacaoGeracaoXML {
		String numeroProcesso;
		File arquivoXML;
		File arquivoXMLTemporario;
		File arquivoXMLErro;
	}
	
	public static void main(String[] args) throws Exception {
		progresso = new ProgressoInterfaceGrafica("(2/6) Geração de XMLs individuais");
		Auxiliar.prepararPastaDeSaida();
		
		try {
			executarOperacaoGeracaoXML(true);

			AnalisaServentiasCNJ.mostrarWarningSeAlgumaServentiaNaoFoiEncontrada();
			AcumuladorExceptions.instance().mostrarExceptionsAcumuladas();
			LOGGER.info("Fim!");
		} finally {
			progresso.setInformacoes("");
			progresso.close();
			progresso = null;
		}
	}

	/**
	 * Gera todos os XMLs (1G e/ou 2G), conforme definido no arquivo "config.properties"
	 *
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	public static void executarOperacaoGeracaoXML(boolean aguardarCasoHajaProblemaComServentias) throws Exception {
		boolean deveProcessarProcessosPje = Auxiliar.deveProcessarProcessosPje();
		boolean deveProcessarProcessosSistemaLegadoNaoMigradosParaOPje = Auxiliar.deveProcessarProcessosSistemaLegadoNaoMigradosParaOPje();
		Op_2_GeraXMLsIndividuais baixaDados1g = null;
		Op_2_GeraXMLsIndividuais baixaDados2g = null;
		Op_2_GeraXMLsIndividuais baixaDadosLegado1g = null;
		Op_2_GeraXMLsIndividuais baixaDadosLegado2g = null;

		if (deveProcessarProcessosPje) {
			// Verifica se há alguma serventia inexistente. A análise de serventias só será realizada para o PJe,
			// pois as informações do Sistema Judicial Legado já estão corretas.
			AnalisaServentiasCNJ analisaServentiasCNJ = new AnalisaServentiasCNJ((BaseEmAnaliseEnum.PJE));
			boolean problemaComServentias = analisaServentiasCNJ.diagnosticarServentiasPjeInexistentes();
			if (problemaComServentias && aguardarCasoHajaProblemaComServentias) {
				Auxiliar.aguardaUsuarioApertarENTERComTimeout(1);
			}
			
			baixaDados1g = Auxiliar.deveProcessarPrimeiroGrau() ? new Op_2_GeraXMLsIndividuais(1, BaseEmAnaliseEnum.PJE) : null;
			baixaDados2g = Auxiliar.deveProcessarSegundoGrau()  ? new Op_2_GeraXMLsIndividuais(2, BaseEmAnaliseEnum.PJE) : null;	
		}
		
		if (deveProcessarProcessosSistemaLegadoNaoMigradosParaOPje) {
			baixaDadosLegado1g = Auxiliar.deveProcessarPrimeiroGrau() ? new Op_2_GeraXMLsIndividuais(1, BaseEmAnaliseEnum.LEGADO) : null;
			baixaDadosLegado2g = Auxiliar.deveProcessarSegundoGrau()  ? new Op_2_GeraXMLsIndividuais(2, BaseEmAnaliseEnum.LEGADO) : null;				
		}
		
		try {

			// Conta quantos processos serão baixados, para mostrar barra de progresso
			int qtdProcessos = 0;
			if (deveProcessarProcessosPje) {
				if (baixaDados1g != null) {
					qtdProcessos += baixaDados1g.carregarListaDeProcessos().size();
				}
				if (baixaDados2g != null) {
					qtdProcessos += baixaDados2g.carregarListaDeProcessos().size();
				}
			}
			
			if (deveProcessarProcessosSistemaLegadoNaoMigradosParaOPje) {
				if (baixaDadosLegado1g != null) {
					qtdProcessos += baixaDadosLegado1g.carregarListaDeProcessos().size();
				}
				if (baixaDadosLegado2g != null) {
					qtdProcessos += baixaDadosLegado2g.carregarListaDeProcessos().size();
				}
			}
			
			if (progresso != null) {
				progresso.setMax(qtdProcessos);
			}

			// Gera XMLs para cada processo
			if (deveProcessarProcessosPje) {
				if (baixaDados1g != null) {
					baixaDados1g.gerarXMLs();
				}
				if (baixaDados2g != null) {
					baixaDados2g.gerarXMLs();
				}
			}
			
			if (deveProcessarProcessosSistemaLegadoNaoMigradosParaOPje) {
				if (baixaDadosLegado1g != null) {
					baixaDadosLegado1g.gerarXMLs();
				}
				if (baixaDadosLegado2g != null) {
					baixaDadosLegado2g.gerarXMLs();
				}
			}
		} finally {
			IOUtils.closeQuietly(baixaDados1g);
			IOUtils.closeQuietly(baixaDados2g);
			IOUtils.closeQuietly(baixaDadosLegado1g);
			IOUtils.closeQuietly(baixaDadosLegado2g);
		}
	}
	
	private File getArquivoListaProcessos(int grau, BaseEmAnaliseEnum baseEmAnalise) {
		return baseEmAnalise.isBasePJe() ? Auxiliar.getArquivoListaProcessosPje(grau) : Auxiliar.getArquivoListaProcessosSistemaLegadoNaoMigradosParaOPje(grau);
	}

	private List<String> carregarListaDeProcessos() {
		listaProcessos = Auxiliar.carregarListaProcessosDoArquivo(this.getArquivoListaProcessos(this.grau, this.baseEmAnalise));
		return listaProcessos;
	}
	
	/**
	 * Recupera a lista de processos que foram migrados do sistema judicial legado para o pje.
	 * 
	 * @return
	 * @throws DadosInvalidosException
	 */
	private void carregarListaDeProcessosSistemaLegadoMigrados() {
		this.mapaProcessosLegadoMigrados = new HashMap<String, String>();

		List<String> processos = Auxiliar.carregarListaProcessosDoArquivo(Auxiliar.getArquivoListaProcessosSistemaLegadoMigradosParaOPJe(this.grau));

		for (String processo : processos) {
			this.mapaProcessosLegadoMigrados.put(processo, processo);
		}

	}

	private void gerarXMLs() throws Exception {

		// Abre conexões com o PJe e prepara consultas a serem realizadas
		prepararConexao();

		// Executa consultas e grava arquivo XML
		gerarXML();
	}


	public Op_2_GeraXMLsIndividuais(int grau, BaseEmAnaliseEnum baseEmAnalise) {
		this.grau = grau;
		this.baseEmAnalise = baseEmAnalise;
		this.deveProcessarProcessosSistemaLegadoMigradosParaOPJe = baseEmAnalise.isBasePJe() 
																   ? Auxiliar.deveProcessarProcessosSistemaLegadoMigradosParaOPJe()
																   : false;
	}

	private void gerarXML() throws SQLException, JAXBException, IOException, InterruptedException {

		statusString = "Criando lotes de operações do " + grau + "o Grau";
		LOGGER.info(statusString + "...");
		
		// Objetos auxiliares para gerar o XML a partir das classes Java
		JAXBContext context = JAXBContext.newInstance(Processos.class);
		Marshaller jaxbMarshaller = context.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		// Variáveis auxiliares para calcular o tempo estimado
		AtomicInteger qtdXMLGerados = new AtomicInteger();
		AtomicLong tempoGasto = new AtomicLong();

		// Pasta onde serão gerados os arquivos XML
		File pastaRaiz = Auxiliar.getPastaXMLsIndividuais(grau);
		pastaRaiz = new File(pastaRaiz, (this.baseEmAnalise.isBasePJe() ? "PJe" : "Legado"));

		// Carrega a lista de processos que precisará ser analisada
		List<String> listaProcessos = Auxiliar.carregarListaProcessosDoArquivo(this.getArquivoListaProcessos(this.grau, this.baseEmAnalise));
		
		// Monta uma lista de "operações" (cada operação é um processo a ser baixado)
		List<OperacaoGeracaoXML> operacoes = new ArrayList<>();
		for (String numeroProcesso: listaProcessos) {

			// Arquivo XML que conterá os dados do processo
			// Depois da geração do XML temporário, visando garantir a integridade do arquivo XML 
			// definitivo, o temporário só será excluído depois da gravação completa do definitivo.
			File arquivoXML = Auxiliar.gerarNomeArquivoIndividualParaProcesso(this.baseEmAnalise, grau, numeroProcesso);
			File arquivoXMLErro = Auxiliar.gerarNomeArquivoProcessoErro(arquivoXML);
			File arquivoXMLTemporario = new File(arquivoXML.getParentFile(), numeroProcesso + ".temp");

			// Se o script for abortado bem na hora da cópia do arquivo temporário para o definitivo, o definitivo
			// pode ficar vazio. Se isso ocorrer, apaga o XML vazio, para que um novo seja gerado.
			if (arquivoXML.exists() && arquivoXML.length() == 0) {
				arquivoXML.delete();
			}

			// Verifica se o XML do processo já foi gerado
			if (arquivoXML.exists()) {
				LOGGER.trace("O arquivo XML do processo " + numeroProcesso + " já existe e não será gerado novamente.");
				if (progresso != null) {
					progresso.incrementProgress();
				}
				
			} else {

				// Cadastra uma operação para ser executada posteriormente
				OperacaoGeracaoXML operacao = new OperacaoGeracaoXML();
				operacao.numeroProcesso = numeroProcesso;
				operacao.arquivoXMLTemporario = arquivoXMLTemporario;
				operacao.arquivoXML = arquivoXML;
				operacao.arquivoXMLErro = arquivoXMLErro;
				operacoes.add(operacao);
			}
		}

		// Agrupa os processos pendentes de geração em lotes para serem carregados do banco
		final int tamanhoLote = Math.max(Auxiliar.getParametroInteiroConfiguracao(Parametro.tamanho_lote_geracao_processos, 1), 1);
		final AtomicInteger counter = new AtomicInteger();
		final Collection<List<OperacaoGeracaoXML>> lotesOperacoes = operacoes.stream()
		    .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / tamanhoLote))
		    .values();
		
		int numeroThreads = Auxiliar.getParametroInteiroConfiguracao(Parametro.numero_threads_simultaneas, 1);
		
		statusString = "Gerando XMLs do " + grau + "o Grau";
		LOGGER.info(statusString + "...");
		AtomicInteger posicaoAtual = new AtomicInteger();
		for (List<OperacaoGeracaoXML> lote : lotesOperacoes) {
			List<String> processosPendentes = lote.stream().map(o -> o.numeroProcesso).collect(Collectors.toList());
			prepararCacheDadosProcessos(processosPendentes);
			ExecutorService threadPool = Executors.newFixedThreadPool(numeroThreads);
			for (OperacaoGeracaoXML operacao : lote) {
				
				// Cálculo do tempo restante
				long antes = System.currentTimeMillis();
				int i = posicaoAtual.incrementAndGet();
				threadPool.execute(() -> {
				
					Auxiliar.prepararThreadLog();
					// Calcula e mostra tempo restante
					// TODO: Sugerir calcular o ETA a partir do tempo de lote, já que fica difícil calcular individualmente, de forma precisa, em multi-thread. 
					int xmlsRestantes = operacoes.size() - i;
					long tempoRestante = 0;
					long mediaPorProcesso = 0;
					if (qtdXMLGerados.get() > 0) {
						mediaPorProcesso = tempoGasto.get() / qtdXMLGerados.get();
						tempoRestante = xmlsRestantes * mediaPorProcesso / numeroThreads;
					}
					String tempoRestanteStr = tempoRestante == 0 ? null : "ETA: " + DurationFormatUtils.formatDurationHMS(tempoRestante);
					LOGGER.debug("Gravando Processo " + operacao.numeroProcesso + " (" + i + "/" + operacoes.size() + " - " + i * 100 / operacoes.size() + "%" + (tempoRestanteStr == null ? "" : " - " + tempoRestanteStr) + (mediaPorProcesso == 0 ? "" : ", media de " + mediaPorProcesso + "ms/processo") + "). Arquivo de saída: " + operacao.arquivoXML + "...");
					if (tempoRestanteStr != null && progresso != null) {
						progresso.setInformacoes("G" + grau + " - " + tempoRestanteStr);
					}
		
					// Limpa arquivos que podem ter sido gerados anteriormente
					operacao.arquivoXML.getParentFile().mkdirs();
					operacao.arquivoXMLErro.delete();
					operacao.arquivoXMLTemporario.delete();
					String origemOperacao = operacao.numeroProcesso + ", base " + this.baseEmAnalise + ", grau " + this.grau;

					try {
						// Executa a consulta desse processo no banco de dados do PJe
						TipoProcessoJudicial processoJudicial = analisarProcessoJudicialCompleto(operacao.numeroProcesso);
			
						// Objeto que, de acordo com o padrão MNI, que contém uma lista de processos. 
						// Nesse caso, ele conterá somente UM processo. Posteriormente, os XMLs de cada
						// processo serão unificados, junto com os XMLs dos outros sistemas legados.
						Processos processos = new Processos();
						processos.getProcesso().add(processoJudicial);
		
						// Gera o arquivo XML temporário
						synchronized (jaxbMarshaller) {
							jaxbMarshaller.marshal(processos, operacao.arquivoXMLTemporario);
						}
		
						// OPCIONAL: Valida o arquivo XML com o "Programa validador de arquivos XML" do CNJ
						validarArquivoXML(operacao.arquivoXMLTemporario);
						
						// Geração ocorreu com sucesso!!
						// Move o XML temporário sobre o definitivo
						FileUtils.moveFile(operacao.arquivoXMLTemporario, operacao.arquivoXML);
						LOGGER.trace("Processo gravado com sucesso no arquivo " + operacao.arquivoXML);
						
						// Cálculo do tempo restante
						tempoGasto.addAndGet(System.currentTimeMillis() - antes);
						qtdXMLGerados.incrementAndGet();
			
						AcumuladorExceptions.instance().removerException(origemOperacao);
						
					} catch (Exception ex) {
						try {
							operacao.arquivoXMLErro.createNewFile();
						} catch (IOException e2) { 
						}
						AcumuladorExceptions.instance().adicionarException(origemOperacao, "Erro na geração do XML do processo: " + ex.getLocalizedMessage(), ex, true);
					}
					
					if (progresso != null) {
						progresso.incrementProgress();
					}
				});
			}
			
			threadPool.shutdown();
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		}
		LOGGER.info("Arquivos XML da base " + this.baseEmAnalise.getDescricao() + " - " + grau + "o Grau gerados!");
		this.statusString = null;
	}

	/**
	 * Valida um arquivo no "Programa validador de arquivos XML", conforme parâmetro "url_validador_cnj" das configurações
	 *
	 * TODO: Quando CNJ resolver o bug de concorrência no validador local, retirar o synchronized
	 *
	 * @param arquivoXML
	 * @throws DataJudException 
	 * @throws DadosInvalidosException
	 */
	private synchronized void validarArquivoXML(File arquivoXML) throws DataJudException {
		
		String url = Auxiliar.getParametroConfiguracao(Parametro.url_validador_cnj, false);
		if (url != null) {
			
			try {
				HttpPost post = new HttpPost(url);
				HttpEntity entity = MultipartEntityBuilder.create().addBinaryBody("arquivo", arquivoXML).build();
				post.setEntity(entity);
				
				HttpClient httpClient = HttpClients.createDefault();
				HttpResponse response = httpClient.execute(post);
				String json;
				try {
					HttpEntity result = response.getEntity();
					json = EntityUtils.toString(result, Charset.forName("UTF-8"));
				} finally {
					EntityUtils.consumeQuietly(response.getEntity());
				}
				
				// Grava o resultado do validador do CNJ, se solicitado
				if (Auxiliar.getParametroBooleanConfiguracao(Parametro.debug_gravar_relatorio_validador_cnj, false)) {
					File debugRelatorioCNJ = new File(arquivoXML.getParentFile(), FilenameUtils.getBaseName(arquivoXML.getName()) + "_validador_cnj.json");
					FileUtils.write(debugRelatorioCNJ, json != null ? json : "null", StandardCharsets.UTF_8);
				}
				
				// Verifica se o validador do CNJ apontou algum problema
				ValidadorIntegridadeXMLCNJ.buscarProblemasValidadorCNJ(json);
				
			} catch (Exception ex) {
				throw new DataJudException("Erro no validador local do CNJ: " + ex.getLocalizedMessage(), ex);
			}
		}
	}

	public void prepararCacheDadosProcessos(List<String> numerosProcessos) throws SQLException {
		
		LOGGER.info("Baixando cache de dados para " + numerosProcessos.size() + " processo(s)...");
		Array arrayNumerosProcessos = conexaoBasePrincipal.createArrayOf("varchar", numerosProcessos.toArray());
		this.cacheProcessosDtos.clear();
		this.cacheProcessosLegadosMigradosDtos.clear();
		
		// Carrega dados principais dos processos
		LOGGER.trace("* nsConsultaProcessos...");
		nsConsultaProcessos.setArray("numeros_processos", arrayNumerosProcessos);
		try (ResultSet rsProcessos = nsConsultaProcessos.executeQuery()) {
			while (rsProcessos.next()) {
				String nrProcesso = rsProcessos.getString("nr_processo");
				ProcessoDto processoDto = new ProcessoDto(rsProcessos, false);
				processoDto.setNumeroInstancia(grau);
				this.cacheProcessosDtos.put(nrProcesso, processoDto);
			}
		}
		
		// Consulta as partes de um determinado polo no processo
		LOGGER.trace("* nsPartes...");
		nsPartes.setArray("numeros_processos", arrayNumerosProcessos);
		Map<Integer, ParteProcessualDto> partesPorIdProcessoParte = new HashMap<>();
		Map<Integer, List<ParteProcessualDto>> partesPorIdPessoa = new HashMap<>();
		try (ResultSet rsPartes = nsPartes.executeQuery()) {
			while (rsPartes.next()) {
				String nrProcesso = rsPartes.getString("nr_processo");
				String inParticipacao = rsPartes.getString("in_participacao");
				ProcessoDto processoDto = cacheProcessosDtos.get(nrProcesso);
				
				// Busca o polo processual dentro do processo
				PoloDto polo;
				if (processoDto.getPolosPorTipoParticipacao().containsKey(inParticipacao)) {
					polo = processoDto.getPolosPorTipoParticipacao().get(inParticipacao);
				} else {
					polo = new PoloDto();
					polo.setInParticipacao(inParticipacao);
					processoDto.getPolosPorTipoParticipacao().put(inParticipacao, polo);
				}
				
				// Cadastra a parte dentro do polo
				ParteProcessualDto parte = new ParteProcessualDto(rsPartes);
				polo.getPartes().add(parte);
				
				// Variáveis auxiliares para carregar endereços e documentos das pessoas
				partesPorIdProcessoParte.put(parte.getIdProcessoParte(), parte);
				if (!partesPorIdPessoa.containsKey(parte.getIdPessoa())) {
					partesPorIdPessoa.put(parte.getIdPessoa(), new ArrayList<>());
				}
				partesPorIdPessoa.get(parte.getIdPessoa()).add(parte);
			}
		}

		// Consulta os endereços das partes
		LOGGER.trace("* nsEnderecos...");
		Array arrayIdProcessoParte = conexaoBasePrincipal.createArrayOf("int", partesPorIdProcessoParte.keySet().toArray());
		nsEnderecos.setArray("id_processo_parte", arrayIdProcessoParte);
		try (ResultSet rsEnderecos = nsEnderecos.executeQuery()) {
			while (rsEnderecos.next()) {
				int idProcessoParte = rsEnderecos.getInt("id_processo_parte");
				EnderecoDto endereco = new EnderecoDto(rsEnderecos);
				partesPorIdProcessoParte.get(idProcessoParte).getEnderecos().add(endereco);
			}
		}

		// Consulta todos os assuntos do processo
		LOGGER.trace("* nsAssuntos...");
		nsAssuntos.setArray("numeros_processos", arrayNumerosProcessos);
		try (ResultSet rsAssuntos = nsAssuntos.executeQuery()) {
			while (rsAssuntos.next()) {
				String nrProcesso = rsAssuntos.getString("nr_processo");
				AssuntoDto assunto = new AssuntoDto(rsAssuntos);
				cacheProcessosDtos.get(nrProcesso).getAssuntos().add(assunto);
			}
		}

		// Consulta todos os movimentos dos processos
		LOGGER.trace("* nsMovimentos...");
		Map<Integer, MovimentoDto> movimentosPorIdProcessoEvento = new HashMap<>();
		nsMovimentos.setArray("numeros_processos", arrayNumerosProcessos);
		try (ResultSet rsMovimentos = nsMovimentos.executeQuery()) {
			while (rsMovimentos.next()) {
				String nrProcesso = rsMovimentos.getString("nr_processo");
				MovimentoDto movimento = new MovimentoDto(rsMovimentos);
				cacheProcessosDtos.get(nrProcesso).getMovimentos().add(movimento);
				movimentosPorIdProcessoEvento.put(movimento.getIdProcessoEvento(), movimento);
			}
		}
		
		// Consulta os complementos desses movimentos processuais.
		// OBS: os complementos só existem no MovimentoNacional
		LOGGER.trace("* nsComplementos...");
		Array arrayIdProcessoEvento = conexaoBasePrincipal.createArrayOf("int", movimentosPorIdProcessoEvento.keySet().toArray());
		nsComplementos.setArray("id_movimento_processo", arrayIdProcessoEvento);
		try (ResultSet rsComplementos = nsComplementos.executeQuery()) {
			while (rsComplementos.next()) {
				int idMovimentoProcesso = rsComplementos.getInt("id_movimento_processo");
				ComplementoDto complemento = new ComplementoDto(rsComplementos);
				movimentosPorIdProcessoEvento.get(idMovimentoProcesso).getComplementos().add(complemento);
			}
		}
 
		// Consulta todos os documentos das pessoas, no banco de dados do PJe
		LOGGER.trace("* nsDocumentos...");
		Array arrayIdPessoa = conexaoBasePrincipal.createArrayOf("int", partesPorIdPessoa.keySet().toArray());
		nsDocumentos.setArray("ids_pessoas", arrayIdPessoa);
		try (ResultSet rsDocumentos = nsDocumentos.executeQuery()) {
			while (rsDocumentos.next()) {
				int idPessoa = rsDocumentos.getInt("id_pessoa");
				for (ParteProcessualDto parte : partesPorIdPessoa.get(idPessoa)) {
					DocumentoPessoaDto documentoDto = new DocumentoPessoaDto(rsDocumentos);
					parte.getDocumentos().add(documentoDto);
				}
			}
		}
		
		// Verifica se esse processo possui incidentes
		LOGGER.trace("* nsIncidentes...");
		nsIncidentes.setArray("numeros_processos", arrayNumerosProcessos);
		try (ResultSet rsIncidentes = nsIncidentes.executeQuery()) {
			while (rsIncidentes.next()) {
				String nrProcesso = rsIncidentes.getString("nr_processo_referencia");
				ProcessoDto incidente = new ProcessoDto(rsIncidentes, true);
				cacheProcessosDtos.get(nrProcesso).getIncidentes().add(incidente);
			}
		}
		
		// Baixa dados de sentenças e acórdãos dos processos, que auxiliarão na identificação do magistrado responsável
		LOGGER.trace("* nsSentencasAcordaos...");
		nsSentencasAcordaos.setArray("numeros_processos", arrayNumerosProcessos);
		try (ResultSet rsSentencasAcordaos = nsSentencasAcordaos.executeQuery()) {
			while (rsSentencasAcordaos.next()) {
				String nrProcesso = rsSentencasAcordaos.getString("nr_processo");
				DocumentoDto documentoDto = new DocumentoDto(rsSentencasAcordaos);
				cacheProcessosDtos.get(nrProcesso).getSentencasAcordaos().add(documentoDto);
			}
		}
		// Baixa dados de deslocamentos de OJ, que auxiliarão na identificação do OJ dos movimentos processuais
		if (this.baseEmAnalise.isBasePJe()) {
			//FIXME: no sistema judicial legado do TRT6 esse histórico não existe. O órgão julgador do movimento é o órgão julgador do processo.
			//Em alguns Regionais pode ser diferente.
			LOGGER.trace("* nsHistoricoDeslocamentoOJ...");
			nsHistoricoDeslocamentoOJ.setArray("numeros_processos", arrayNumerosProcessos);
			try (ResultSet rsHistoricoDeslocamentoOJ = nsHistoricoDeslocamentoOJ.executeQuery()) {
				while (rsHistoricoDeslocamentoOJ.next()) {
					String nrProcesso = rsHistoricoDeslocamentoOJ.getString("nr_processo");
					HistoricoDeslocamentoOJDto historico = new HistoricoDeslocamentoOJDto(rsHistoricoDeslocamentoOJ);
					cacheProcessosDtos.get(nrProcesso).getHistoricosDeslocamentoOJ().add(historico);
				}
			}
		}
		
		//Carregando dados de processos do sistema judicial legado que foram migrados para o PJe
		if (this.deveProcessarProcessosSistemaLegadoMigradosParaOPJe) {
			List<String> numerosProcessosLegadosMigrados = new ArrayList<String>();
			
			for (String numeroProcesso : numerosProcessos) {
				if (this.mapaProcessosLegadoMigrados.containsKey(numeroProcesso)) {
					numerosProcessosLegadosMigrados.add(numeroProcesso);
				}
			}

			if (!numerosProcessosLegadosMigrados.isEmpty()) {
				Array arrayNumerosProcessosLegadosMigrados = conexaoBaseLegadoMigrados.createArrayOf("varchar", numerosProcessosLegadosMigrados.toArray());

				// Carrega dados principais dos processos no sistema judicial legado
				LOGGER.trace("* nsConsultaProcessosLegadosMigrados...");
				nsConsultaProcessosLegadosMigrados.setArray("numeros_processos", arrayNumerosProcessosLegadosMigrados);
				try (ResultSet rsProcessos = nsConsultaProcessosLegadosMigrados.executeQuery()) {
					while (rsProcessos.next()) {
						String nrProcesso = rsProcessos.getString("nr_processo");
						ProcessoDto processoDto = new ProcessoDto(rsProcessos, false);
						processoDto.setNumeroInstancia(grau);
						this.cacheProcessosLegadosMigradosDtos.put(nrProcesso, processoDto);
						
					}
				}
				
				// Consulta todos os movimentos dos processos que foram migrados do sistema judicial legado para o pje
				LOGGER.trace("* nsMovimentosLegadosMigrados...");
				Map<Integer, MovimentoDto> movimentosPorIdProcessoEventoLegado = new HashMap<>();
				nsMovimentosLegadosMigrados.setArray("numeros_processos", arrayNumerosProcessosLegadosMigrados);
				try (ResultSet rsMovimentos = nsMovimentosLegadosMigrados.executeQuery()) {
					while (rsMovimentos.next()) {
						String nrProcesso = rsMovimentos.getString("nr_processo");
						MovimentoDto movimento = new MovimentoDto(rsMovimentos);
						cacheProcessosLegadosMigradosDtos.get(nrProcesso).getMovimentos().add(movimento);
						movimentosPorIdProcessoEventoLegado.put(movimento.getIdProcessoEvento(), movimento);
					}
				}
				
				// Consulta os complementos desses movimentos processuais.
				// OBS: os complementos só existem no MovimentoNacional
				LOGGER.trace("* nsComplementosLegadosMigrados...");
				Array arrayIdProcessoEventoLegado = conexaoBaseLegadoMigrados.createArrayOf("int", movimentosPorIdProcessoEventoLegado.keySet().toArray());
				nsComplementosLegadosMigrados.setArray("id_movimento_processo", arrayIdProcessoEventoLegado);
				try (ResultSet rsComplementos = nsComplementosLegadosMigrados.executeQuery()) {
					while (rsComplementos.next()) {
						int idMovimentoProcesso = rsComplementos.getInt("id_movimento_processo");
						ComplementoDto complemento = new ComplementoDto(rsComplementos);
						movimentosPorIdProcessoEventoLegado.get(idMovimentoProcesso).getComplementos().add(complemento);
					}
				}
				
				// Baixa dados de sentenças e acórdãos dos processos, que auxiliarão na identificação do magistrado responsável
				LOGGER.trace("* nsSentencasAcordaosLegadosMigrados...");
				nsSentencasAcordaosLegadosMigrados.setArray("numeros_processos", arrayNumerosProcessosLegadosMigrados);
				try (ResultSet rsSentencasAcordaos = nsSentencasAcordaosLegadosMigrados.executeQuery()) {
					while (rsSentencasAcordaos.next()) {
						String nrProcesso = rsSentencasAcordaos.getString("nr_processo");
						DocumentoDto documentoDto = new DocumentoDto(rsSentencasAcordaos);
						cacheProcessosLegadosMigradosDtos.get(nrProcesso).getSentencasAcordaos().add(documentoDto);
					}
				}
			}
		}
	}

	/**
	 * Consulta os dados do processo informado no banco de dados e gera um objeto da classe
	 * {@link TipoProcessoJudicial}
	 * 
	 * @param numeroProcesso
	 * @return
	 * @throws SQLException 
	 * @throws IOException
	 * @throws DataJudException 
	 */
	public TipoProcessoJudicial analisarProcessoJudicialCompleto(String numeroProcesso) throws SQLException, IOException, DataJudException {

		if (cacheProcessosDtos.containsKey(numeroProcesso)) {
			if (this.baseEmAnalise.isBasePJe()) {
				ProcessoDto processoLegado = null;
				if (cacheProcessosLegadosMigradosDtos.containsKey(numeroProcesso)) {
					//contém as informações do processo no sistema judicial legado,
					//necessárias para realização do merge de movimentos e complementos
					processoLegado = cacheProcessosLegadosMigradosDtos.get(numeroProcesso);
				}
				return analisarProcessoJudicialCompleto(cacheProcessosDtos.get(numeroProcesso), processoLegado);
			} else {
				return analisarProcessoJudicialCompleto(cacheProcessosDtos.get(numeroProcesso), null);				
			}
		}
			
		throw new DataJudException("Processo não encontrado");
	}


	/**
	 * Método criado com base no script recebido do TRT14
	 * para preencher os dados de um processo judicial dentro das classes que gerarão o XML.
	 * 
	 * @param processoLegadoMigrado contém as informações do processo no sistema judicial legado,
	 * necessárias para realização do merge de movimentos e complementos
	 * @param processoJudicial
	 * @param rsProcesso
	 * @throws SQLException 
	 * @throws DadosInvalidosException 
	 * @throws IOException 
	 * @throws DataJudException 
	 */
	public TipoProcessoJudicial analisarProcessoJudicialCompleto(ProcessoDto processo, ProcessoDto processoLegadoMigrado) throws SQLException, IOException, DataJudException {

		// Objeto que será retornado
		TipoProcessoJudicial processoJudicial = new TipoProcessoJudicial();

		// Cabeçalho com dados básicos do processo:
		TipoCabecalhoProcesso cabecalho = analisarCabecalhoProcesso(processo);
		processoJudicial.setDadosBasicos(cabecalho);

		// Movimentos processuais e complementos
		processoJudicial.getMovimento().addAll(analisarMovimentosProcesso(processo, processoLegadoMigrado, cabecalho.getOrgaoJulgador()));

		return processoJudicial;
	}


	private TipoCabecalhoProcesso analisarCabecalhoProcesso(ProcessoDto processo) throws SQLException, DataJudException {

		String numeroCompletoProcesso = processo.getNumeroProcesso();

		// Script TRT14:
		// raise notice '<dadosBasicos nivelSigilo="%" numero="%" classeProcessual="%" codigoLocalidade="%" dataAjuizamento="%">' 
		//  , proc.nivelSigilo, proc.nr_processo, proc.cd_classe_judicial, proc.id_municipio_ibge_origem, proc.dt_autuacao;
		TipoCabecalhoProcesso cabecalhoProcesso = new TipoCabecalhoProcesso();
		cabecalhoProcesso.setNivelSigilo(processo.isSegredoJustica() ? 5 : 0);
		cabecalhoProcesso.setNumero(processo.getNumeroProcessoSemSinais());
		cabecalhoProcesso.setSiglaTribunal(Auxiliar.getParametroConfiguracao(Parametro.sigla_tribunal, true));
		cabecalhoProcesso.setGrau("G" + grau);
		cabecalhoProcesso.setDscSistema(this.baseEmAnalise.isBasePJe() ? 1 : 8);// 1 = PJE; 8 = Outros
		
		// Informar se o processo tramita em sistema eletrônico ou em papel. São valores possíveis
		// 1: Sistema Eletrônico
		// 2: Sistema Físico
		cabecalhoProcesso.setProcEl(this.baseEmAnalise.isBasePJe() ? 1 : 2);
		
		// Grava a classe processual, conferindo se ela está na tabela nacional do CNJ
		analisaClassesProcessuaisCNJ.preencherClasseProcessualVerificandoTPU(cabecalhoProcesso, processo.getClasseJudicial(), numeroCompletoProcesso);

		cabecalhoProcesso.setDataAjuizamento(Auxiliar.formataDataMovimento(processo.getDataAutuacao()));
		cabecalhoProcesso.setValorCausa(processo.getValorCausa()); 
		if (grau == 1) {

			// Em 1G, pega como localidade o município do OJ do processo
			cabecalhoProcesso.setCodigoLocalidade(Integer.toString(processo.getOrgaoJulgador().getIdMunicipioIBGE()));
		} else {

			// Em 2G, pega como localidade o município do TRT, que está definido no arquivo de configurações
			cabecalhoProcesso.setCodigoLocalidade(Integer.toString(codigoMunicipioIBGETRT));
		}

		// Consulta todos os polos do processo
		cabecalhoProcesso.getPolo().addAll(analisarPolosProcesso(processo.getIdProcesso(), numeroCompletoProcesso));

		// Consulta todos os assuntos desse processo
		cabecalhoProcesso.getAssunto().addAll(analisarAssuntosProcesso(processo));

		// Preenche dados do órgão julgador do processo
		cabecalhoProcesso.setOrgaoJulgador(analisarOrgaoJulgadorProcesso(processo, this.baseEmAnalise));

		// Preenche dados de processos incidentais / principais
		analisarRelacaoIncidental(cabecalhoProcesso.getRelacaoIncidental(), processo);
		
		// Preenche um campo timestamp como "outroParametro", para evitar que arquivo seja negado como duplicado, 
		// garantindo que a última versão do processo será enviada ao CNJ
		TipoParametro parametro = new TipoParametro();
		parametro.setNome("timestamp");
		parametro.setValor(Long.toString(System.currentTimeMillis()));
		cabecalhoProcesso.getOutroParametro().add(parametro);
		
		return cabecalhoProcesso;
	}


	/**
	 * Preenche dados de processos incidentais / principais
	 * 
	 * @param relacaoIncidental : lista que será preenchida com os dados dos processos incidentais/principais
	 * @param rsProcesso : ResultSet que contém os dados do processo atual e do processo de referência
	 * @throws SQLException 
	 */
	private void analisarRelacaoIncidental(List<TipoRelacaoIncidental> relacaoIncidental, ProcessoDto processo) throws SQLException {
		
		// Verifica se esse processo é um incidente (possui um principal)
		if (processo.getProcessoReferencia() != null) {
			
			TipoRelacaoIncidental relacao = new TipoRelacaoIncidental();
			String numeroProcessoReferencia = processo.getProcessoReferencia().getNumeroProcesso();
			
			// De acordo com o validador do CNJ, deve gravar o número do processo de forma plana, sem sinais.
			// Erro reportado:
			// Caused by: org.xml.sax.SAXParseException; ... cvc-pattern-valid: Value '0020474-77.2016.5.04.0233' is not facet-valid with respect to pattern '\d{20}' for type 'tipoNumeroUnico'.
			// Linha do XML:
			// <relacaoIncidental numeroProcesso="0020474-77.2016.5.04.0233" tipoRelacao="PI" classeProcessual="1125"/>
			relacao.setNumeroProcesso(Auxiliar.removerPontuacaoNumeroProcesso(numeroProcessoReferencia));
			
			// Indicar se o processo é principal ou incidental.
			// Podem ser classificados como:
			// 'PP': processos principal
			// 'PI': processos incidental
			relacao.setTipoRelacao("PP");
			
			int idClasseJudicial = processo.getProcessoReferencia().getClasseJudicial().getCodigo();
			relacao.setClasseProcessual(idClasseJudicial);
			analisaClassesProcessuaisCNJ.validarClasseProcessualCNJ(idClasseJudicial, numeroProcessoReferencia);
			
			relacaoIncidental.add(relacao);
		}
		
		for (ProcessoDto incidenteDto : processo.getIncidentes()) {
			TipoRelacaoIncidental relacao = new TipoRelacaoIncidental();
			String nrProcesso = incidenteDto.getNumeroProcesso();
			
			// De acordo com o validador do CNJ, deve gravar o número do processo de forma plana, sem sinais.
			// Erro reportado:
			// Caused by: org.xml.sax.SAXParseException; ... cvc-pattern-valid: Value '0020474-77.2016.5.04.0233' is not facet-valid with respect to pattern '\d{20}' for type 'tipoNumeroUnico'.
			// Linha do XML:
			// <relacaoIncidental numeroProcesso="0020474-77.2016.5.04.0233" tipoRelacao="PI" classeProcessual="1125"/>
			relacao.setNumeroProcesso(Auxiliar.removerPontuacaoNumeroProcesso(nrProcesso));
			
			// Indicar se o processo é principal ou incidental.
			// Podem ser classificados como:
			// 'PP': processos principal
			// 'PI': processos incidental
			relacao.setTipoRelacao("PI");
			
			int idClasseJudicial = incidenteDto.getClasseJudicial().getCodigo();
			relacao.setClasseProcessual(idClasseJudicial);
			analisaClassesProcessuaisCNJ.validarClasseProcessualCNJ(idClasseJudicial, nrProcesso);
			
			relacaoIncidental.add(relacao);
		}
	}

	private List<TipoPoloProcessual> analisarPolosProcesso(int idProcesso, String numeroProcesso) throws SQLException, DataJudException {

		// Itera sobre os polos processuais
		Collection<PoloDto> polosDtos = cacheProcessosDtos.get(numeroProcesso).getPolosPorTipoParticipacao().values();
		List<TipoPoloProcessual> polos = new ArrayList<>();
		for (PoloDto poloDto : polosDtos) {
			// Script TRT14:
			// raise notice '<polo polo="%">', polo.in_polo_participacao;
			TipoPoloProcessual polo = new TipoPoloProcessual();
			String tipoPoloPJe = poloDto.getInParticipacao();
			if ("A".equals(tipoPoloPJe)) {
				polo.setPolo(ModalidadePoloProcessual.AT); // AT: polo ativo
			} else if ("P".equals(tipoPoloPJe)) {
				polo.setPolo(ModalidadePoloProcessual.PA); // PA: polo passivo
			} else if ("T".equals(tipoPoloPJe)) {
				polo.setPolo(ModalidadePoloProcessual.TC); // TC: terceiro
			} else {
				throw new DataJudException("Tipo de polo não reconhecido: " + tipoPoloPJe);
			}
			polos.add(polo);

			// O PJe considera, como partes, tanto os autores e réus quanto seus advogados,
			// procuradores, tutores, curadores, assistentes, etc.
			// Por isso, a identificação das partes será feita em etapas:
			// 1. Todas as partes e advogados serão identificados e gravados no HashMap
			//    "partesPorIdParte"
			// 2. As partes representadas e seus representantes serão gravados no HashMap
			//    "partesERepresentantes"
			// 2. As partes identificadas que forem representantes de alguma outra parte 
			//    serão removidas de "partesPorIdParte" e registradas efetivamente como 
			//    representantes no atributo "advogado" da classe TipoParte
			// 3. As partes que "restarem" no HashMap, então, serão inseridas no XML.
			HashMap<Integer, TipoParte> partesPorIdParte = new HashMap<>(); // id_processo_parte -> TipoParte
			HashMap<Integer, List<Integer>> partesERepresentantes = new HashMap<>(); // id_processo_parte -> lista dos id_processo_parte dos seus representantes
			HashMap<Integer, ModalidadeRepresentanteProcessual> tiposRepresentantes = new HashMap<>(); // id_processo_parte -> Modalidade do representante (advogado, procurador, etc).
			for (ParteProcessualDto parteProcessual : poloDto.getPartes()) {

				String nomeParte = parteProcessual.getNomeParte();

				// Script TRT14:
				// raise notice '<parte>';
				TipoParte parte = new TipoParte();
				int idProcessoParte = parteProcessual.getIdProcessoParte();
				Integer idProcessoParteRepresentante = parteProcessual.getIdProcessoParteRepresentante();
				partesPorIdParte.put(idProcessoParte, parte);

				// Verifica se, no PJe, essa parte possui representante (advogado, procurador, tutor, curador, etc)
				if (idProcessoParteRepresentante != null) {
					if (!partesERepresentantes.containsKey(idProcessoParte)) {
						partesERepresentantes.put(idProcessoParte, new ArrayList<Integer>());
					}
					partesERepresentantes.get(idProcessoParte).add(idProcessoParteRepresentante);

					String tipoParteRepresentante = parteProcessual.getTipoParteRepresentante();
					if ("ADVOGADO".equals(tipoParteRepresentante)) {
						tiposRepresentantes.put(idProcessoParteRepresentante, ModalidadeRepresentanteProcessual.A);

					} else if ("PROCURADOR".equals(tipoParteRepresentante)) {
						tiposRepresentantes.put(idProcessoParteRepresentante, ModalidadeRepresentanteProcessual.P);

					} else if ("ADMINISTRADOR".equals(tipoParteRepresentante)
							|| "ASSISTENTE".equals(tipoParteRepresentante)
							|| "ASSISTENTE TÉCNICO".equals(tipoParteRepresentante)
							|| "CURADOR".equals(tipoParteRepresentante)
							|| "INVENTARIANTE".equals(tipoParteRepresentante)
							|| "REPRESENTANTE".equals(tipoParteRepresentante)
							|| "TERCEIRO INTERESSADO".equals(tipoParteRepresentante)
							|| "TUTOR".equals(tipoParteRepresentante)) {
						// Não fazer nada, pois esses tipos de parte (PJe), apesar de estarem descritos
						// no arquivo "intercomunicacao-2.2.2", não estão sendo enviados ao CNJ
						// pela ferramenta "replicacao-client".

					} else {
						LOGGER.warn("O representante da parte '" + nomeParte + "' (id_processo_parte=" + idProcessoParte + ") possui um tipo de parte que ainda não foi tratado: " + tipoParteRepresentante);
					}
				}

				// Pessoa
				TipoPessoa pessoa = new TipoPessoa();
				parte.setPessoa(pessoa);
				pessoa.setNome(nomeParte);

				// Tipo de pessoa (física / jurídica / outros)
				// Pergunta feita à ASSTECO: no e-mail com assunto "Dúvidas sobre envio de dados para Justiça em Números", em "03/07/2017 14:15":
				//   Estou gerando os dados para o Selo Justiça em Números, do CNJ. Ocorre que o MNI 2.2, utilizado como referência, permite somente o envio de dados de pessoas dos tipos "Física", "Jurídica", "Autoridade" e "Órgão de Representação".
				//   No PJe, os tipos são parecidos, mas ligeiramente diferentes: "Física", "Jurídica", "Autoridade", "MPT" e "Órgão Público". Quanto aos três primeiros, vejo que eles tem correspondência direta, mas fiquei na dúvida sobre como tratar os outros dois! Preciso, para cada pessoa do "lado" do PJe, encontrar um correspondente do "lado" do MNI.
				//   Vocês saberiam me informar qual o tipo correto, no padrão MNI, para enviar partes dos tipos "MPT" e "Órgão Público"?
				// Resposta do CNJ no e-mail em "28/07/2017 18:10":
				//   Sugerimos enquadrar tanto o MPT como os órgãos públicos sem personalidade jurídica própria como "Órgãos de Representação".
				String tipoPessoaPJe = parteProcessual.getTipoPessoa();
				if ("F".equals(tipoPessoaPJe)) {
					pessoa.setTipoPessoa(TipoQualificacaoPessoa.FISICA);
				} else if ("J".equals(tipoPessoaPJe)) {
					pessoa.setTipoPessoa(TipoQualificacaoPessoa.JURIDICA);
				} else if ("A".equals(tipoPessoaPJe)) {
					pessoa.setTipoPessoa(TipoQualificacaoPessoa.AUTORIDADE);
				} else if ("M".equals(tipoPessoaPJe)) {
					pessoa.setTipoPessoa(TipoQualificacaoPessoa.ORGAOREPRESENTACAO);
				} else if ("O".equals(tipoPessoaPJe)) {
					pessoa.setTipoPessoa(TipoQualificacaoPessoa.ORGAOREPRESENTACAO);
				} else {
					throw new DataJudException("Tipo de pessoa desconhecido: " + tipoPessoaPJe + "(" + nomeParte + ")");
				}

				// Consulta os documentos da parte
				identificaDocumentosPessoa.preencherDocumentosPessoa(pessoa, parteProcessual.getDocumentos());

				// Identifica o gênero (sexo) da pessoa (pode ser necessário consultar na outra instância)
				identificaGeneroPessoa.preencherSexoPessoa(pessoa, parteProcessual.getSexoPessoa(), parteProcessual.getNomeConsultaParte(), this.baseEmAnalise);

				// Identifica os endereços da parte
				for (EnderecoDto enderecoDto : parteProcessual.getEnderecos()) {
					
					// intercomunicacao-2.2.2: Atributo indicador do código de endereçamento 
					// postal do endereço no diretório nacional de endereços da ECT. O 
					// valor deverá ser uma sequência de 8 dígitos, sem qualquer separador. 
					// O atributo é opcional para permitir a apresentação de endereços 
					// desprovidos de CEP e de endereços internacionais.
					// <restriction base="string"><pattern value="\d{8}"></pattern></restriction>
					String cep = enderecoDto.getCep();
					if (!StringUtils.isBlank(cep)) {
						cep = cep.replaceAll("[^0-9]", "");
					}
					
					// No PJe 2.5.1, endereços internacionais aparecem no banco de dados assim:
					// nr_cep='0', cd_estado='X1', ds_municipio='Município Estrangeiro'
					boolean isCepInternacional = "0".equals(cep);

					if (!StringUtils.isBlank(cep)) {
						TipoEndereco endereco = new TipoEndereco();
						
						if (!isCepInternacional) {
							endereco.setCep(cep);
						}

						// intercomunicacao-2.2.2: O logradouro pertinente a este endereço, 
						// tais como rua, praça, quadra etc. O elemento é opcional para permitir 
						// que as implementações acatem a indicação de endereço exclusivamente 
						// pelo CEP, quando o CEP já encerrar o dado respectivo.
						endereco.setLogradouro(enderecoDto.getLogradouro());

						// intercomunicacao-2.2.2: O número vinculado a este endereço. O elemento 
						// é opcional para permitir que as implementações acatem a indicação de 
						// endereço exclusivamente pelo CEP, quando o CEP já encerrar o dado respectivo.
						endereco.setNumero(enderecoDto.getNumero());

						// intercomunicacao-2.2.2: O complemento vinculado a este endereço. 
						// O elemento é opcional em razão de sua própria natureza.
						endereco.setComplemento(enderecoDto.getComplemento());

						// intercomunicacao-2.2.2: O bairro vinculado a este endereço. O elemento 
						// é opcional para permitir que as implementações acatem a indicação 
						// de endereço exclusivamente pelo CEP, quando o CEP já encerrar o dado respectivo.
						endereco.setBairro(enderecoDto.getBairro());

						// intercomunicacao-2.2.2: A cidade vinculada a este endereço. O elemento 
						// é opcional para permitir que as implementações acatem a indicação 
						// de endereço exclusivamente pelo CEP, quando o CEP já encerrar o dado respectivo.
						endereco.setCidade(enderecoDto.getMunicipio());
						
						// modelo-de-transferencia-de-dados-1.0.xsd:
						// código do município IBGE com sete dígitos, referente ao campo “cidade”.
						endereco.setCodCidade(enderecoDto.getIdMunicipioIBGE());

						pessoa.getEndereco().add(endereco);
					}
				}

				// Outras dados da pessoa
				if (parteProcessual.getDataNascimento() != null) {
					pessoa.setDataNascimento(Auxiliar.formataDataAAAAMMDD(parteProcessual.getDataNascimento()));
				}
				if (parteProcessual.getDataObito() != null) {
					pessoa.setDataObito(Auxiliar.formataDataAAAAMMDD(parteProcessual.getDataObito()));
				}
				pessoa.setNomeGenitor(parteProcessual.getNomeGenitor());
				pessoa.setNomeGenitora(parteProcessual.getNomeGenitora());
			}

			// Para cada parte identificada, localiza todos os seus representantes
			for (int idProcessoParte: partesERepresentantes.keySet()) {
				for (int idProcessoParteRepresentante : partesERepresentantes.get(idProcessoParte)) {

					// Isola a parte e seu representante
					TipoParte parte = partesPorIdParte.get(idProcessoParte);
					TipoParte representante = partesPorIdParte.get(idProcessoParteRepresentante);

					// Pode ocorrer de a parte estar ATIVA e possuir um advogado cujo registro 
					// em tb_processo_parte está INATIVO. Nesse caso, não insere o advogado
					// como representante.
					if (representante != null) {

						// Cria um objeto TipoRepresentanteProcessual a partir dos dados do representante
						// OBS: somente se o tipo do representante foi identificado, pois
						//      alguns tipos (como tutores e curadores) não são processados
						//      pelo "replicacao-client", do CNJ, e não serão incluídos no
						//      arquivo XML.
						if (tiposRepresentantes.containsKey(idProcessoParteRepresentante)) {
							TipoRepresentanteProcessual representanteProcessual = new TipoRepresentanteProcessual();
							parte.getAdvogado().add(representanteProcessual);
							representanteProcessual.setNome(representante.getPessoa().getNome());
							representanteProcessual.setIntimacao(true); // intercomunicacao-2.2.2: Indicativo verdadeiro (true) ou falso (false) relativo à escolha de o advogado, escritório ou órgão de representação ser o(s) preferencial(is) para a realização de intimações.
							representanteProcessual.setNumeroDocumentoPrincipal(representante.getPessoa().getNumeroDocumentoPrincipal());
							representanteProcessual.setTipoRepresentante(tiposRepresentantes.get(idProcessoParteRepresentante));
							representanteProcessual.getEndereco().addAll(representante.getPessoa().getEndereco());
						}
					}
				}
			}

			// Retira, da lista de partes do processo, as partes que são representantes
			// (e, por isso, já estão constando dentro das suas partes representadas)
			for (List<Integer> idProcessoParteRepresentantes: partesERepresentantes.values()) {
				for (int idProcessoParteRepresentante: idProcessoParteRepresentantes) {
					partesPorIdParte.remove(idProcessoParteRepresentante);
				}
			}

			// Insere as partes "restantes" no XML
			for (TipoParte parte: partesPorIdParte.values()) {
				polo.getParte().add(parte);
			}

			if (polo.getParte().isEmpty()) {
				throw new DataJudException("O polo " + polo.getPolo() + " do processo não contém nenhuma parte no XML gerado!");
			}
		}
		return polos;
	}


	private List<TipoAssuntoProcessual> analisarAssuntosProcesso(ProcessoDto processo) throws DataJudException, SQLException {

		List<TipoAssuntoProcessual> assuntos = new ArrayList<>();

		boolean encontrouAlgumAssunto = false;
		boolean encontrouAssuntoPrincipal = false;
		for (AssuntoDto assuntoDto : processo.getAssuntos()) {

			// Script TRT14:
			// raise notice '<assunto>'; -- principal="%">', assunto.in_assunto_principal;
			// raise notice '<codigoNacional>%</codigoNacional>', assunto.cd_assunto_trf;
			// raise notice '</assunto>';

			// Analisa o assunto, que pode ou não estar nas tabelas processuais unificadas do CNJ.
			int codigo = assuntoDto.getCodigo();
			TipoAssuntoProcessual assunto = analisaAssuntosCNJ.getAssunto(codigo, this.baseEmAnalise);
			if (assunto != null) {
				assuntos.add(assunto);
				encontrouAlgumAssunto = true;
	
				// Trata o campo "assunto principal", verificando também se há mais de um assunto principal no processo.
				boolean assuntoPrincipal = assuntoDto.isPrincipal();
				assunto.setPrincipal(assuntoPrincipal);
				if (assuntoPrincipal) {
					if (encontrouAssuntoPrincipal) {
						LOGGER.warn("Processo possui mais de um assunto principal: " + processo.getNumeroProcesso());
					} else {
						encontrouAssuntoPrincipal = true;
					}
				}
			}
		}

		if (!encontrouAlgumAssunto) {

			// Se não há nenhum assunto no processo, verifica se deve ser utilizando um assunto
			// padrão, conforme arquivo de configuração.
			TipoAssuntoProcessual assuntoPadrao = analisaAssuntosCNJ.getAssuntoProcessualPadrao();
			if (assuntoPadrao != null) {
				assuntos.add(assuntoPadrao);

			} else {
				throw new DataJudException("Processo sem assunto cadastrado");
			}

		} else if (!encontrouAssuntoPrincipal) {
			LOGGER.info("Processo sem assunto principal: " + processo.getNumeroProcesso() + ". O primeiro assunto será marcado como principal.");
			assuntos.get(0).setPrincipal(true);
		}
		return assuntos;
	}


	private TipoOrgaoJulgador analisarOrgaoJulgadorProcesso(ProcessoDto processo, BaseEmAnaliseEnum baseEmAnalise) throws SQLException, DataJudException {
		return analisarOrgaoJulgadorProcesso(processo.getClasseJudicial(), processo.getOrgaoJulgador().getNomeNormalizado(), processo.getOrgaoJulgador().getCodigoServentiaJudiciariaLegado(), processo.getOrgaoJulgador().getIdMunicipioIBGE(), baseEmAnalise);
	}
	
	/**
	 * Retorna um órgão julgador com os dados das serventias do CNJ.
	 * 
	 * @param nomeOrgaoJulgadorProcesso : nome do órgão julgador conforme campo "ds_orgao_julgador" da tabela "tb_orgao_julgador".
	 * @param codigoOrgaoJulgadorLegado: codigo do órgão julgador que é retornado apenas pelo sistema judicial legado (para composição da serventia)
	 * @param idMunicipioIBGEOrgaoJulgador : código IBGE do município do órgão julgador. Se estiver gerando dados do segundo grau, 
	 * 		esse parâmetro será ignorado e, em vez dele, será sempre preenchido o conteúdo do parâmetro "codigo_municipio_ibge_trt".
	 * @return
	 * @throws SQLException
	 * @throws DataJudException 
	 */
	private TipoOrgaoJulgador analisarOrgaoJulgadorProcesso(ClasseJudicialDto classe, String nomeOrgaoJulgadorProcesso, int codigoOrgaoJulgadorLegado, int idMunicipioIBGE, BaseEmAnaliseEnum baseEmAnalise) throws SQLException, DataJudException {
		/*
		 * Órgãos Julgadores
				Para envio do elemento <orgaoJulgador >, pede-se os atributos <codigoOrgao> e <nomeOrgao>, conforme definido em <tipoOrgaoJulgador>. 
				Em <codigoOrgao> deverão ser informados os mesmos códigos das serventias judiciárias cadastradas no Módulo de Produtividade Mensal (Resolução CNJ nº 76/2009).
				Em <nomeOrgao> deverão ser informados os mesmos descritivos das serventias judiciárias cadastradas no Módulo de Produtividade Mensal (Resolução CNJ nº 76/2009)
			Fonte: http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/2016-06-02-17-51-25
		 */
		/*
			10. Como preencher o campo “Órgão Julgador”? 
			    R: Os Tribunais deverão seguir os mesmos códigos e descrições utilizadas no módulo de produtividade.
			Fonte: http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/perguntas-frequentes
		 */
		// Conversando com Clara, decidimos utilizar sempre a serventia do OJ do processo
		ServentiaCNJ serventiaCNJ = processaServentiasCNJ.getServentiaByOJ(nomeOrgaoJulgadorProcesso, codigoOrgaoJulgadorLegado, baseEmAnalise);
		if (serventiaCNJ == null) {
			throw new DataJudException("Falta mapear serventia no arquivo " + AnalisaServentiasCNJ.getArquivoServentias());
		}

		TipoOrgaoJulgador orgaoJulgador = new TipoOrgaoJulgador();
		orgaoJulgador.setCodigoOrgao(serventiaCNJ.getCodigo());
		orgaoJulgador.setNomeOrgao(serventiaCNJ.getNome());
		if (grau == 1) {

			// Em 1G, pega como localidade do OJ o município do OJ do processo
			orgaoJulgador.setCodigoMunicipioIBGE(idMunicipioIBGE);

			// Em 1G, instância será sempre originária
			orgaoJulgador.setInstancia("ORIG");
		} else {

			// Em 2G, pega como localidade do OJ o município do TRT, que está definido no arquivo de configurações
			orgaoJulgador.setCodigoMunicipioIBGE(codigoMunicipioIBGETRT);

			// Em 2G, instância poderá ser originária ou recursal
			orgaoJulgador.setInstancia(classe.isRecursal() ? "REV" : "ORIG");
		}

		return orgaoJulgador;
	}

	private List<TipoMovimentoProcessual> analisarMovimentosProcesso(ProcessoDto processo, ProcessoDto processoLegadoMigrado, TipoOrgaoJulgador orgaoJulgadorProcessoCabecalho) throws SQLException, IOException, DataJudException {

		List<TipoMovimentoProcessual> movimentos = new ArrayList<>();
		if (this.baseEmAnalise.isBasePJe()) {
			movimentos.addAll(this.getMovimentosProcesso(processo, orgaoJulgadorProcessoCabecalho, this.baseEmAnalise));
			if (processoLegadoMigrado != null) {
				movimentos.addAll(this.getMovimentosProcesso(processoLegadoMigrado, analisarOrgaoJulgadorProcesso(processoLegadoMigrado, BaseEmAnaliseEnum.LEGADO), BaseEmAnaliseEnum.LEGADO));				
			}
		} else {
			movimentos.addAll(this.getMovimentosProcesso(processo, orgaoJulgadorProcessoCabecalho, this.baseEmAnalise));
		}

		return movimentos;
	}

	private List<TipoMovimentoProcessual> getMovimentosProcesso(ProcessoDto processo, TipoOrgaoJulgador orgaoJulgadorProcesso, BaseEmAnaliseEnum baseEmAnalise) throws SQLException, DataJudException {

		List<TipoMovimentoProcessual> movimentos = new ArrayList<>();
		
		for (MovimentoDto movimentoDto : processo.getMovimentos()) {
			
			// Movimento processual
			TipoMovimentoProcessual movimento = new TipoMovimentoProcessual();
			movimento.setDataHora(Auxiliar.formataDataMovimento(movimentoDto.getDataAtualizacao()));
			movimento.setNivelSigilo(movimentoDto.isVisibilidadeExterna() ? 0 : 5);
			movimento.setIdentificadorMovimento(Integer.toString(movimentoDto.getIdProcessoEvento()));
			
			// TRT4 possui um usuário utilizado pelo sistema e-Jus², cujo CPF está registrado como "ejus2".
			// Então, preenche CPF somente quando for válido
			if (movimentoDto.getCPFUsuarioMovimento() != null && movimentoDto.getCPFUsuarioMovimento().length() == 11) {
				movimento.setResponsavelMovimento(movimentoDto.getCPFUsuarioMovimento());
			}
			
			// tipoResponsavelMovimento: Identificação do responsável pelo movimento: Servidor=0; Magistrado=1;
			movimento.setTipoResponsavelMovimento(movimentoDto.isUsuarioMagistrado() ? 1 : 0);
			
			analisaMovimentosCNJ.preencheDadosMovimentoCNJ(processo, movimento, movimentoDto, baseEmAnalise);
			
			LocalDateTime dataMovimento = movimentoDto.getDataAtualizacao();

			// Consulta os complementos desse movimento processual.
			// OBS: os complementos só existem no MovimentoNacional
			TipoMovimentoNacional movimentoNacional = movimento.getMovimentoNacional();
			
			// Se o parâmetro descartar_movimentos_ausentes_de_para_cnj tiver o valor SIM, apenas movimentos mapeados no DE-PARA do CNJ serão mantidos
			boolean descartarMovimentosAusentesDeParaCNJ = Auxiliar.getParametroBooleanConfiguracao(Parametro.descartar_movimentos_ausentes_de_para_cnj, false);
			if (!descartarMovimentosAusentesDeParaCNJ || (movimentoNacional != null)) {
				movimentos.add(movimento);
			} 

			if (movimentos.contains(movimento)) {
				if (movimentoNacional != null) {
					for (ComplementoDto complementoDto : movimentoDto.getComplementos()) {
						
						/*
						Se o complemento for do tipo TABELADO, inserir somente o seu código tabelado (não precisa do valor). Ex:
						
						<movimento dataHora="20150206110014" identificadorMovimento="4">
						  <movimentoNacional codigoNacional="123">
						    <complemento>18:motivo_da_remessa:38</complemento>
						  </movimentoNacional>
						  <complementoNacional codComplemento="18" descricaoComplemento="motivo_da_remessa" codComplementoTabelado="38"/>
						</movimento>
						
						Fonte: https://www.cnj.jus.br/wp-content/uploads/2020/07/documento_XML_exemplo_DataJud_06042020.pdf
						 */
						Integer codComplementoTabelado = (complementoDto.isComplementoTipoTabelado() && complementoDto.getCodigoComplemento() != null) ? Integer.parseInt(complementoDto.getCodigoComplemento()) : null;
						
						StringBuilder sb = new StringBuilder();
						sb.append(complementoDto.getCodigoTipoComplemento());
						sb.append(":");
						sb.append(complementoDto.getNome());
						String codigoComplemento = complementoDto.getCodigoComplemento();
						if (!StringUtils.isBlank(codigoComplemento)) {
							sb.append(":");
							sb.append(codigoComplemento);
						}
						if (codComplementoTabelado == null) {
							sb.append(":");
							sb.append(complementoDto.getValor());
						}
						movimentoNacional.getComplemento().add(sb.toString());
						
						TipoComplementoNacional complemento = new TipoComplementoNacional();
						movimento.getComplementoNacional().add(complemento);
						complemento.setCodComplemento(complementoDto.getCodigoTipoComplemento());
						complemento.setDescricaoComplemento(complementoDto.getNome());
						
						if (codComplementoTabelado != null) {
							complemento.setCodComplementoTabelado(codComplementoTabelado);
						}
					}
				}
				
				// Se for um movimento de JULGAMENTO de um MAGISTRADO, precisa identificar o CPF do prolator
				if (movimentoDto.isMovimentoMagistradoJulgamento()) {
					
					// Analisa a lista de sentenças e acórdãos do processo, para tentar encontrar qual o 
					// magistrado responsável pelo movimento de julgamento
					DocumentoDto documentoRelacionado = processo.getSentencasAcordaos().stream()
							
						// Procura uma sentença ou acórdão ANTERIOR ao movimento para saber qual o magistrado prolator.
						.filter(d -> d.getDataJuntada().isBefore(dataMovimento))
						
						// Volta no máximo uma semana, para evitar pegar um documento muito antigo
						.filter(d -> d.getDataJuntada().isAfter(dataMovimento.minusDays(7)))
						.findFirst().orElse(null);
					
					// Se encontrou, preenche CPF do magistrado prolator.
					if (documentoRelacionado != null) {
						movimento.getMagistradoProlator().add(documentoRelacionado.getCpfUsuarioAssinou());
					}
				}
	
				// Identifica o OJ do processo no instante em que o movimento foi lançado, baseado no histórico de deslocamento.
				// Se não há nenhum deslocamento de OJ no período, considera o mesmo OJ do processo.
				//FIXME:  no sistema judicial legado do TRT6 esse histórico não existe. O órgão julgador do movimento é o órgão julgador do processo.
				//Em alguns Regionais pode ser diferente.
				if (baseEmAnalise.isBasePJe()) {
					for (HistoricoDeslocamentoOJDto historico : processo.getHistoricosDeslocamentoOJ()) {
						LocalDateTime dataDeslocamento = historico.getDataDeslocamento();
						LocalDateTime dataRetorno = historico.getDataRetorno();
						
						// TODO: Criar um parâmetro para definir o que fazer com movimentos gerados em órgãos julgadores que não possuem serventia, ex: "movimentos_sem_serventia_cnj",
						//       para evitar a ocorrência do erro "Falta mapear serventia no arquivo..." em "analisarOrgaoJulgadorProcesso"
						/*
						 * (1) DESCARTAR_PROCESSO (comportamento atual do sistema): não gera o XML desse processo
						 * (2) DESCARTAR_MOVIMENTO: gera o XML do processo, mas sem o movimento cujo OJ não possui serventia
						 * (3) SEM_SERVENTIA: envia o movimento de qualquer forma, mas sem informar serventia
						 * (4) SERVENTIA_OJ_PRINCIPAL: envia o movimento com o código da serventia do órgão julgador atual do processo.
						 */
						if (dataDeslocamento.isAfter(dataMovimento)) {
							TipoOrgaoJulgador orgaoJulgador = analisarOrgaoJulgadorProcesso(processo.getClasseJudicial(), historico.getNomeOrgaoJulgadorOrigem(), 0, historico.getIdMunicipioOrigem(), baseEmAnalise);
							movimento.setOrgaoJulgador(orgaoJulgador);
							break;
	
						} else if (dataDeslocamento.isBefore(dataMovimento) && dataRetorno.isAfter(dataMovimento)) {
							TipoOrgaoJulgador orgaoJulgador = analisarOrgaoJulgadorProcesso(processo.getClasseJudicial(), historico.getNomeOrgaoJulgadorDestino(), 0, historico.getIdMunicipioDestino(), baseEmAnalise);
							movimento.setOrgaoJulgador(orgaoJulgador);
							break;
						}
					}
				}
				if (movimento.getOrgaoJulgador() == null) {
					movimento.setOrgaoJulgador(orgaoJulgadorProcesso);
				}
			}
		}

		return movimentos;
	}


	public void prepararConexao() throws SQLException, IOException, InterruptedException {

		LOGGER.info("Preparando informações para gerar XMLs da base " + this.baseEmAnalise.getDescricao() + " - " + grau + "o Grau...");
		this.statusString = "Preparando dados";

		// Objeto que fará o de/para dos OJ e OJC do PJe para os do CNJ
		if (processaServentiasCNJ == null) {
			processaServentiasCNJ = new AnalisaServentiasCNJ(this.baseEmAnalise);
		}

		// Abre conexão com o banco de dados do PJe ou Legado
		conexaoBasePrincipal = Auxiliar.getConexao(this.grau, this.baseEmAnalise);
		conexaoBasePrincipal.setAutoCommit(false);

		// Objeto que auxiliará na identificação do sexo das pessoas na OUTRA INSTANCIA, quando 
		// essa informação estiver ausente na instância atual.
		int outraInstancia = grau == 1 ? 2 : 1;
		identificaGeneroPessoa = new IdentificaGeneroPessoa(outraInstancia, this.baseEmAnalise);

		// Objeto que auxiliará na identificação dos documentos de identificação das pessoas
		identificaDocumentosPessoa = new IdentificaDocumentosPessoa(conexaoBasePrincipal, this.baseEmAnalise);

		String pastaIntermediaria = Auxiliar.getPastaResources(this.baseEmAnalise);

		// SQL que fará a consulta de um processo
		String sqlConsultaProcessos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/" + pastaIntermediaria + "/01_consulta_processo.sql");
		nsConsultaProcessos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaProcessos, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);

		// SQL que fará a consulta das partes
		String sqlConsultaPartes = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/" + pastaIntermediaria + "/03_consulta_partes.sql");
		nsPartes = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaPartes);

		// SQL que fará a consulta dos endereços da parte
		String sqlConsultaDocumentos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/" + pastaIntermediaria + "/04_consulta_documentos_pessoa.sql");
		nsDocumentos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaDocumentos);
		
		// SQL que fará a consulta dos endereços da parte
		String sqlConsultaEnderecos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/" + pastaIntermediaria + "/04_consulta_enderecos_pessoa.sql");
		nsEnderecos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaEnderecos);

		// SQL que fará a consulta dos assuntos do processo
		String sqlConsultaAssuntos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/" + pastaIntermediaria + "/05_consulta_assuntos.sql");
		nsAssuntos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaAssuntos);

		// SQL que fará a consulta dos movimentos processuais
		String sqlConsultaMovimentos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/" + pastaIntermediaria + "/06_consulta_movimentos.sql");
		nsMovimentos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaMovimentos);

		// Le o SQL que fará a consulta dos complementos dos movimentos processuais
		String sqlConsultaComplementos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/" + pastaIntermediaria + "/07_consulta_complementos.sql");
		nsComplementos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaComplementos);

		// Le o SQL que fará a consulta dos processos incidentais
		String sqlConsultaIncidentes = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/" + pastaIntermediaria + "/08_consulta_incidentes.sql");
		nsIncidentes = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaIncidentes);
		
		// Le o SQL que fará a consulta das sentenças e acórdãos
		String sqlConsultaSentencasAcordaos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/" + pastaIntermediaria + "/09_consulta_sentencas_acordaos.sql");
		nsSentencasAcordaos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaSentencasAcordaos);
		
		// Le o SQL que fará a consulta do histórico de deslocamento
		if (this.baseEmAnalise.isBasePJe()) {
			//FIXME:  no sistema judicial legado do TRT6 esse histórico não existe. O órgão julgador do movimento é o órgão julgador do processo.
			//Em alguns Regionais pode ser diferente.
			String sqlConsultaHistoricoDeslocamentoOJ = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/" + pastaIntermediaria + "/10_consulta_deslocamento_oj.sql");
			nsHistoricoDeslocamentoOJ = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaHistoricoDeslocamentoOJ);			
		}
		
		//Se a base em análise é a do pje, será preciso recuperar, para processos que foram migrados do sistema judicial legado,
		// os respectivos movimentos e complementos
		if (this.deveProcessarProcessosSistemaLegadoMigradosParaOPJe) {
			this.carregarListaDeProcessosSistemaLegadoMigrados();
			conexaoBaseLegadoMigrados = Auxiliar.getConexao(this.grau, BaseEmAnaliseEnum.LEGADO);
			conexaoBaseLegadoMigrados.setAutoCommit(false);

			String sqlConsultaProcessosLegadoMigrados = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/legado/01_consulta_processo.sql");
			nsConsultaProcessosLegadosMigrados = new NamedParameterStatement(conexaoBaseLegadoMigrados, sqlConsultaProcessosLegadoMigrados, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);

			// SQL que fará a consulta dos movimentos processuais
			String sqlConsultaMovimentosLegadoMigrados = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/legado/06_consulta_movimentos.sql");
			nsMovimentosLegadosMigrados = new NamedParameterStatement(conexaoBaseLegadoMigrados, sqlConsultaMovimentosLegadoMigrados);

			// Le o SQL que fará a consulta dos complementos dos movimentos processuais
			String sqlConsultaComplementosLegadoMigrados = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/legado/07_consulta_complementos.sql");
			nsComplementosLegadosMigrados = new NamedParameterStatement(conexaoBaseLegadoMigrados, sqlConsultaComplementosLegadoMigrados);

			// Le o SQL que fará a consulta das sentenças e acórdãos
			String sqlConsultaSentencasAcordaosLegadoMigrados = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/legado/09_consulta_sentencas_acordaos.sql");
			nsSentencasAcordaosLegadosMigrados = new NamedParameterStatement(conexaoBaseLegadoMigrados, sqlConsultaSentencasAcordaosLegadoMigrados);
		}

		// O código IBGE do município onde fica o TRT vem do arquivo de configuração, já que será diferente para cada regional
		codigoMunicipioIBGETRT = Auxiliar.getParametroInteiroConfiguracao(Parametro.codigo_municipio_ibge_trt);

		// Objeto que identificará os assuntos das tabelas nacionais do CNJ
		String origem = "Mapeamento de assuntos processuais, base " + baseEmAnalise + ", grau " + grau;
		try {
			analisaAssuntosCNJ = new AnalisaAssuntosCNJ(grau, conexaoBasePrincipal, true, baseEmAnalise);
			AcumuladorExceptions.instance().removerException(origem);
		} catch (Exception ex) {
			AcumuladorExceptions.instance().adicionarException(origem, ex.getLocalizedMessage(), ex, true);
		}
		
		// Objeto que identificará os movimentos processuais das tabelas nacionais do CNJ
		analisaMovimentosCNJ = new AnalisaMovimentosCNJ(baseEmAnalise, conexaoBasePrincipal);
		analisaClassesProcessuaisCNJ = new AnalisaClassesProcessuaisCNJ();
		
		this.statusString = null;
	}


	public void close() {

		// Fecha objetos que auxiliam a carga de dados do PJe
		analisaMovimentosCNJ = null;
		Auxiliar.fechar(analisaAssuntosCNJ);
		analisaAssuntosCNJ = null;
		Auxiliar.fechar(identificaGeneroPessoa);
		identificaGeneroPessoa = null;
		Auxiliar.fechar(identificaDocumentosPessoa);
		identificaDocumentosPessoa = null;

		// Fecha PreparedStatements
		Auxiliar.fechar(nsConsultaProcessos);
		nsConsultaProcessos = null;
		Auxiliar.fechar(nsPartes);
		nsPartes = null;
		Auxiliar.fechar(nsDocumentos);
		nsDocumentos = null;
		Auxiliar.fechar(nsEnderecos);
		nsEnderecos = null;
		Auxiliar.fechar(nsAssuntos);
		nsAssuntos = null;
		Auxiliar.fechar(nsMovimentos);
		nsMovimentos = null;
		Auxiliar.fechar(nsComplementos);
		nsComplementos = null;
		Auxiliar.fechar(nsIncidentes);
		nsIncidentes = null;
		Auxiliar.fechar(nsSentencasAcordaos);
		nsSentencasAcordaos = null;
		Auxiliar.fechar(nsHistoricoDeslocamentoOJ);
		nsHistoricoDeslocamentoOJ = null;			

		Auxiliar.fechar(nsConsultaProcessosLegadosMigrados);
		nsConsultaProcessosLegadosMigrados = null;
		Auxiliar.fechar(nsMovimentosLegadosMigrados);
		nsMovimentosLegadosMigrados = null;
		Auxiliar.fechar(nsComplementosLegadosMigrados);
		nsComplementosLegadosMigrados = null;
		Auxiliar.fechar(nsSentencasAcordaosLegadosMigrados);
		nsSentencasAcordaosLegadosMigrados = null;
		
		Auxiliar.fechar(conexaoBasePrincipal);
		conexaoBasePrincipal = null;
		Auxiliar.fechar(conexaoBaseLegadoMigrados);
		conexaoBaseLegadoMigrados = null;
	}
}
