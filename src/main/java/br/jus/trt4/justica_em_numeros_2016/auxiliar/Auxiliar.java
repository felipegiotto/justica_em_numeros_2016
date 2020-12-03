package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import br.jus.trt4.justica_em_numeros_2016.dao.ProcessoEnvioDao;
import br.jus.trt4.justica_em_numeros_2016.entidades.ProcessoEnvio;
import br.jus.trt4.justica_em_numeros_2016.enums.BaseEmAnaliseEnum;
import br.jus.trt4.justica_em_numeros_2016.enums.OrigemProcessoEnum;
import br.jus.trt4.justica_em_numeros_2016.enums.Parametro;
import br.jus.trt4.justica_em_numeros_2016.enums.TipoRemessaEnum;
import br.jus.trt4.justica_em_numeros_2016.enums.TipoSistemaJudicialEnum;
import br.jus.trt4.justica_em_numeros_2016.util.DataJudUtil;

/**
 * Classe que contém métodos auxiliares utilizados nesse projeto.
 * @author fgiotto
 *
 */
public class Auxiliar {

	private static final File arquivoConfiguracoes = new File("config.properties");

	private static final Logger LOGGER = LogManager.getLogger(Auxiliar.class);
	private static Properties configs = null;
	private static boolean permitirAguardarUsuarioApertarENTER = true;
	private static final SimpleDateFormat dfDataNascimento = new SimpleDateFormat("yyyyMMdd");
	private static final SimpleDateFormat dfDataMovimentoProcessual = new SimpleDateFormat("yyyyMMddHHmmss");
	private static final DateTimeFormatter dfDataDiaMesAno = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static File pastaSaida = null;
	
	public static final String SUFIXO_ARQUIVO_ENVIADO = ".enviado";
	public static final String SUFIXO_PROTOCOLO = ".protocolo";
	public static final String SUFIXO_PROTOCOLO_SUCESSO = ".sucesso";
	public static final String SUFIXO_PROTOCOLO_ERRO = ".erro";
	
	public static String TIPO_SISTEMA = Auxiliar.getParametroConfiguracaoComValorPadrao(Parametro.sistema_judicial,
			TipoSistemaJudicialEnum.APENAS_PJE.getCodigo());
	
	public static boolean MESCLAR_MOVIMENTOS_LEGADO_MIGRADO_VIA_XML = Auxiliar
			.getParametroBooleanConfiguracaoComValorPadrao(Parametro.mesclar_movimentos_legado_migrado_via_xml, false);

	private static final ProcessoEnvioDao processoEnvioDAO = new ProcessoEnvioDao();

	/**
	 * Recupera a pasta em que se encontram os arquivos sql dos diretórios 'op_1_baixa_lista_processos '
	 * e 'op_2_gera_xmls' de acordo com o grau pesquisado e a base em análise. 
	 * 
	 * @param grau
	 * @param baseEmAnaliseEnum
	 * @return
	 */
	public static String getPastaResources(BaseEmAnaliseEnum baseEmAnaliseEnum, int grau) {
		return baseEmAnaliseEnum.isBasePJe() ? "pje" : "legado/G" + grau;
	}

	/**
	 * Cria uma conexão com o banco de dados do Pje ou do sistema legado, conforme a instância selecionada (1 ou 2)
	 * e a indicacao da base em análise, lendo os dados dos parâmetros do arquivo "config.properties"
	 * @throws SQLException
	 */
	public static Connection getConexao(int grau, BaseEmAnaliseEnum baseEmAnalise) throws SQLException {
		return baseEmAnalise.isBasePJe() ? Auxiliar.getConexaoPJe(grau) : Auxiliar.getConexaoSistemaLegado(grau);
	}

	/**
	 * Cria uma conexão com o banco de dados de outro sistema judicial legado, conforme a instância selecionada (1 ou 2),
	 * lendo os dados dos parâmetros do arquivo "config.properties"
	 * @throws SQLException
	 */
	private static Connection getConexaoSistemaLegado(int grau) throws SQLException {
		LOGGER.info("Abrindo conexão com Sistema Legado " + grau + "G");
		if (grau == 1) {
			return getConexaoDasConfiguracoes(Parametro.url_legado_1g);
		} else if (grau == 2) {
			return getConexaoDasConfiguracoes(Parametro.url_legado_2g);
		} else {
			throw new SQLException("Grau inválido: " + grau);
		}
	}
	
	/**
	 * Cria uma conexão com o banco de dados do PJe, conforme a instância selecionada (1 ou 2),
	 * lendo os dados dos parâmetros "url_jdbc_1g" ou "url_jdbc_2g"
	 * @throws SQLException
	 */
	private static Connection getConexaoPJe(int grau) throws SQLException {
		LOGGER.info("Abrindo conexão com o PJe " + grau + "G");
		if (grau == 1) {
			return getConexaoDasConfiguracoes(Parametro.url_jdbc_1g);
		} else if (grau == 2) {
			return getConexaoDasConfiguracoes(Parametro.url_jdbc_2g);
		} else {
			throw new SQLException("Grau inválido: " + grau);
		}
	}
	
	
	/**
	 * Cria uma conexão com o banco de dados de staging do e-Gestão, conforme a instância selecionada (1 ou 2),
	 * lendo os dados dos parâmetros "url_jdbc_egestao_1g" ou "url_jdbc_egestao_2g"
	 * @throws SQLException
	 */
	public static Connection getConexaoStagingEGestao(int grau) throws SQLException {
		LOGGER.info("Abrindo conexão com o Staging do e-Gestão " + grau + "G");
		if (grau == 1) {
			return getConexaoDasConfiguracoes(Parametro.url_jdbc_egestao_1g);
		} else if (grau == 2) {
			return getConexaoDasConfiguracoes(Parametro.url_jdbc_egestao_2g);
		} else {
			throw new SQLException("Grau inválido: " + grau);
		}
	}
	
	
	/**
	 * Cria uma nova conexão com o banco de dados do PJe, recebendo por parâmetro o nome da chave do 
	 * arquivo "config.properties" que contém a String JDBC que deverá ser utilizada para conexão.
	 * 
	 * @param parametro
	 * @return
	 * @throws SQLException
	 */
	private static Connection getConexaoDasConfiguracoes(Parametro parametro) throws SQLException {
		try {
			Class.forName("org.postgresql.Driver");
			return DriverManager.getConnection(getParametroConfiguracao(parametro, true));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Carrega um parâmetro booleano do arquivo "config.properties". Se o parâmetro não existir ou não for "SIM" ou "NAO", lança uma exceção.
	 * 
	 * @param parametro
	 * @return
	 */
	public static boolean getParametroBooleanConfiguracao(Parametro parametro) {
		String valor = getParametroConfiguracao(parametro, false);
		if ("SIM".equals(valor)) {
			return true;
		}
		if ("NAO".equals(valor) || "NÃO".equals(valor)) {
			return false;
		}
		throw new RuntimeException("Defina o parâmetro '" + parametro + "' no arquivo '" + arquivoConfiguracoes + "' com os valores SIM ou NAO");
	}
	
	
	/**
	 * Carrega um parâmetro booleano do arquivo "config.properties". Se o parâmetro não existir ou não for "SIM" ou "NAO", retorna um valor padrão
	 * 
	 * @param parametro
	 * @return
	 */
	public static boolean getParametroBooleanConfiguracaoComValorPadrao(Parametro parametro, boolean valorPadrao) {
		String valor = getParametroConfiguracao(parametro, false);
		if ("SIM".equals(valor)) {
			return true;
		}
		if ("NAO".equals(valor)) {
			return false;
		}
		return valorPadrao;
	}
	
	
	/**
	 * Carrega um parâmetro numérico do arquivo "config.properties". Se o parâmetro não existir ou não for numérico, lança uma exceção.
	 * 
	 * @param parametro
	 * @return
	 */
	public static int getParametroInteiroConfiguracao(Parametro parametro) {
		try {
			return Integer.parseInt(getParametroConfiguracao(parametro, true));
		} catch (NumberFormatException ex) {
			throw new RuntimeException("O parâmetro '" + parametro + "', no arquivo '" + arquivoConfiguracoes + "', deve ser numérico.");
		}
	}
	
	
	/**
	 * Carrega um parâmetro numérico do arquivo "config.properties". Se o parâmetro não existir ou não for numérico, retorna o valor padrão
	 * 
	 * @param parametro
	 * @return
	 */
	public static int getParametroInteiroConfiguracaoComValorPadrao(Parametro parametro, int valorPadrao) {
		try {
			return Integer.parseInt(getParametroConfiguracao(parametro, true));
		} catch (NumberFormatException ex) {
			return valorPadrao;
		}
	}
	
	
	/**
	 * Retorna o conteúdo de um determinado parâmetro definido no arquivo "config.properties".
	 * 
	 * Se o parâmetro for obrigatório e não existir, será lançada uma exceção, solicitando que o usuário preencha o referido parâmetro.
	 * 
	 * @param parametro
	 * @param obrigatorio: indica se o parâmetro é obrigatório
	 * @return se o parâmetro existir, retorna seu valor. Se o parâmetro não existir e for obrigatório, será lançada uma exceção.
	 * Se o parâmetro não existir mas não for obrigatório, retornará "null".
	 */
	public static String getParametroConfiguracao(Parametro parametro, boolean obrigatorio) {
		if (obrigatorio && !getConfigs().containsKey(parametro.toString())) {
			throw new RuntimeException("Defina o parâmetro '" + parametro + "' no arquivo '" + arquivoConfiguracoes + "'");
		}
		return getConfigs().getProperty(parametro.toString());
	}

	
	/**
	 * Retorna o conteúdo de um determinado parâmetro definido no arquivo "config.properties".
	 * 
	 * Se o parâmetro não existir, será retornado o valor padrão.
	 */
	public static String getParametroConfiguracaoComValorPadrao(Parametro parametro, String valorPadrao) {
		if (getConfigs().containsKey(parametro.toString())) {
			return getConfigs().getProperty(parametro.toString());
		} else {
			return valorPadrao;
		}
	}

	
	/**
	 * Carrega e mantém em cache as configurações definidas no arquivo "config.properties"
	 */
	private static Properties getConfigs() {
		if (configs == null) {

			try {
				configs = carregarPropertiesDoArquivo(arquivoConfiguracoes);
				
				// Confere se há algum atributo não reconhecido, conforme enum "Parametro"
				for (Object key: configs.keySet()) {
					try {
						Parametro.valueOf(key.toString());
					} catch (IllegalArgumentException ex) {
						
						// Nesse momento, é melhor não utilizar o LOGGER, pois ele ainda não está configurado para gravar na pasta correta (dentro de "output")
						System.out.println("Há um atributo não reconhecido (" + key + ") no arquivo de configurações (" + arquivoConfiguracoes.getAbsolutePath() + "). Este atributo será ignorado!");
					}
				}
			} catch (IOException ex) {
				throw new RuntimeException("Erro ao ler arquivo de configurações (" + arquivoConfiguracoes + "): " + ex.getLocalizedMessage(), ex);
			}
		}
		return configs;
	}


	/**
	 * Carrega os dados de um arquivo ".properties"
	 *  
	 * @throws IOException
	 */
	public static Properties carregarPropertiesDoArquivo(File arquivo) throws IOException {
		if (!arquivo.exists()) {
			throw new RuntimeException("O arquivo '" + arquivo + "' não existe!");
		}
		Properties p = new Properties();

		FileInputStream fis = new FileInputStream(arquivo);
		try {
			p.load(fis);
		} finally {
			fis.close();
		}
		return p;
	}
	
	
	/**
	 *  Carrega os dados de um arquivo ".properties", possibilitando que o parâmetro tipo_carga_xml seja
	 *  passado como parâmetro da função, desconsiderando o que está no arquivo de propriedades.
	 *  Esse método visa possibilitar a execução automatizada do extrator por linha de comando, evitando que seja necessário
	 *  alterar o arquivo de propriedades indicando a competência de cada remessa mensal.

	 * @param tipoCargaXml Tipo de carga em que os processos serão extraídos, podendo ser COMPLETA, MENSAL, 
	 * 					   TODOS_COM_MOVIMENTACOES, TESTES e PROCESSO.
	 */
	public static void carregarPropertiesDoArquivo(String tipoCargaXml) {
		
		final String ParametroTipoCargaXml = "tipo_carga_xml";
		
		if (configs == null) {
			configs = getConfigs();
		}
		
		if(tipoCargaXml != null && tipoCargaXml.length() > 0) {
			configs.setProperty(ParametroTipoCargaXml, tipoCargaXml);			
		}
		
	}

	
	/**
	 * Consome um stream, mostrando seu resultado no console
	 * 
	 * @param inputStream: stream que será lido
	 * @param prefix: prefixo para escrever no console antes de cada linha lida do stream
	 * @throws IOException
	 */
	public static void consumirStream(InputStream inputStream, String prefix) throws IOException {
		try (Scanner scanner = new Scanner(inputStream, "LATIN1")) {
			while(scanner.hasNextLine()) {
				String line = scanner.nextLine();
				LOGGER.debug(prefix + line);
			}
		}
	}
	
	
	/**
	 * Aguarda o término da execução de um processo e confere se ele retornou código "0",
	 * que é o código padrão para um processo que retornou sem erros.
	 * 
	 * @param process
	 * @throws InterruptedException
	 */
	public static void conferirRetornoProcesso(Process process) throws InterruptedException {
		process.waitFor();
		
		// Confere se o processo retornou "0", que significa "OK".
		if (process.exitValue() != 0) {
			throw new RuntimeException("Processo retornou " + process.exitValue());
		}
	}
	
	
	/**
	 * Consome um determinado stream, gravando linha por linha nos logs do LOG4J.
	 * 
	 * Esse método é um pouco mais complexo do que um simples "while(hasNextLine) { log(nextLine) }", 
	 * pois é utilizado para ler a STDOUT e a STDERR da JAR "replicacao-client", do CNJ.
	 * 
	 * Essa JAR, em diversas vezes, utiliza "print" em vez de "println", ou seja, não envia o caractere de
	 * fim de linha, o que faz com que o "hasNextLine" fique aguardando indefinidamente.
	 * 
	 * Por isso, o método "consumirStreamAssincrono" lê todos os bytes disponíveis, mesmo que não exista
	 * uma quebra de linha.
	 * 
	 * @param inputStream
	 * @param prefix
	 * @param logLevel
	 * @throws IOException
	 */
	public static void consumirStreamAssincrono(final InputStream inputStream, final String prefix, final Level logLevel) throws IOException {
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				prepararPastaDeSaida();
				
				try {
					while(true) {
						leBytesDisponiveis(inputStream, logLevel);
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							leBytesDisponiveis(inputStream, logLevel);
						}
					}
					
				} catch (IOException e) {
					LOGGER.log(logLevel, "Leitura do stream finalizada: " + inputStream);
				}
			}

			private void leBytesDisponiveis(final InputStream inputStream, final Level logLevel) throws IOException {
				int qtdBytes = inputStream.available();
				if (qtdBytes > 0) {
					byte[] bytes = new byte[qtdBytes];
					inputStream.read(bytes);
					String str = new String(bytes);
					for (String linha: str.split("\n")) {
						LOGGER.log(logLevel, linha);
					}
				}
			}
		});
		t.setDaemon(true);
		t.start();
	}
	
	
	/**
	 * Lê todo o conteúdo de um arquivo UTF-8 e retorna em uma String
	 * 
	 * @param arquivo
	 * @throws IOException
	 */
	public static String lerConteudoDeArquivo(String arquivo) throws IOException {
		return FileUtils.readFileToString(new File(arquivo), "UTF-8");
	}
	
	
	/**
	 * Lê um campo "int" de um ResultSet, lançando uma exceção se ele for nulo
	 */
	public static int getCampoIntNotNull(ResultSet rs, String fieldName) throws SQLException {
		int result = rs.getInt(fieldName);
		if (rs.wasNull()) {
			throw new RuntimeException("Campo '" + fieldName + "' do ResultSet é nulo!");
		}
		
		return result;
	}
	
	
	/**
	 * Lê um campo "string" de um ResultSet, lançando uma exceção se ele for nulo
	 */
	public static String getCampoStringNotNull(ResultSet rs, String fieldName) throws SQLException {
		String result = rs.getString(fieldName);
		if (result == null) {
			throw new RuntimeException("Campo '" + fieldName + "' do ResultSet é nulo!");
		}
		
		return result;
	}
	

	/**
	 * Lê um campo "double" de um ResultSet, retornando NULL se estiver em branco no banco de dados.
	 */
	public static Double getCampoDoubleOrNull(ResultSet rs, String fieldName) throws SQLException {
		double result = rs.getDouble(fieldName);
		if (rs.wasNull()) {
			return null;
		} else {
			return result;
		}
	}
	
	/**
	 * Lê um campo "int" de um ResultSet, retornando NULL se estiver em branco no banco de dados.
	 */
	public static Integer getCampoIntOrNull(ResultSet rs, String fieldName) throws SQLException {
		int result = rs.getInt(fieldName);
		if (rs.wasNull()) {
			return null;
		} else {
			return result;
		}
	}

	/**
	 * Lê um campo "timestamp" de um ResultSet, retornando seu LocalDateTime, ou null se estiver em branco
	 * @param rs
	 * @param fieldName
	 * @return
	 * @throws SQLException 
	 */
	public static LocalDateTime getCampoLocalDateTimeOrNull(ResultSet rs, String fieldName) throws SQLException {
		Timestamp dtAutuacao = rs.getTimestamp("dt_autuacao");
		return dtAutuacao != null ? dtAutuacao.toLocalDateTime() : null;
	}
	
	/**
	 * Retorna o arquivo onde deve ser gravada e lida a lista de processos de uma determinada
	 * instância do PJe.
	 */
	public static File getArquivoListaProcessosPje(int grau) {
		return new File(prepararPastaDeSaida(), "G" + grau + "/PJe_lista_processos.txt");
	}
	
	/**
	 * Retorna o arquivo onde deve ser gravada e lida a lista de processos migrados para o PJE de uma determinada
	 * instância do sistema legado.
	 */
	public static File getArquivoListaProcessosSistemaLegadoMigradosParaOPJe(int grau) {
		return new File(prepararPastaDeSaida(), "G" + grau + "/Legado_lista_processos_migrados.txt");
	}

	/**
	 * Retorna o arquivo onde deve ser gravada e lida a lista de processos não migrados para o PJE de uma determinada
	 * instância do sistema legado.
	 */
	public static File getArquivoListaProcessosSistemaLegadoNaoMigradosParaOPje(int grau) {
		return new File(prepararPastaDeSaida(), "G" + grau + "/Legado_lista_processos_nao_migrados.txt");
	}
	
	/**
	 * Retorna o arquivo onde deve ser gravada e lida a última página consultada do serviço 
	 * que busca os protocolos do CNJ
	 * 
	 * @return Arquivo com a última página consultada do serviço que busca os protocolos com erro do CNJ
	 */
	public static File getArquivoUltimaPaginaConsultaCNJ() {
		return new File(prepararPastaDeSaida(), "/CNJ_ultima_pagina_consulta_protocolos.txt");
	}
	
	/**
	 * Retorna o arquivo onde deve ser gravada e lida a lista de processos e as meta-informações retornadas pela
	 * consulta de conferência de protocolos, realizada via serviço do CNJ, de uma determinada instância.
	 * 
	 * @return Arquivo com a última página consultada do serviço que busca os protocolos com erro do CNJ
	 */
	public static File getArquivoListaProcessosProtocolo(int grau) {
		return new File(prepararPastaDeSaida(), "G" + grau + "/Lista_processos_protocolo.txt");
	}
	
	
	/**
	 * Retorna a pasta raiz onde serão gravados e lidos os arquivos XML de uma determinada
	 * instância do PJe.
	 */
	public static File getPastaXMLsIndividuais(int grau) {
		return new File(prepararPastaDeSaida(), "G" + grau + "/xmls_individuais");
	}
        
        /**
         * Retorna a pasta onde serão ligos os arquivos XML do legado
         * @param grau
         * @return 
         */
        public static File getPastaXMLsLegado(int grau) {
            return new File(Auxiliar.getPastaXMLsIndividuais(grau), "Legado");
        }


	/**
	 * Formata uma data conforme estabelecido no arquivo de intercomunicação: AAAAMMDD
	 */
	public static String formataDataAAAAMMDD(Date data) {
		return dfDataNascimento.format(data);
	}
	
	/**
	 * Formata uma data conforme estabelecido no arquivo de intercomunicação: AAAAMMDD
	 */
	public static String formataDataAAAAMMDD(LocalDate data) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
		return formatter.format(data);
	}
	
	/**
	 * Formata uma data para preencher nos movimentos processuais.
	 * 
	 * @param data
	 * @return
	 */
	public static String formataDataMovimento(Date data) {
		return dfDataMovimentoProcessual.format(data);
	}
	
	public static String formataDataMovimento(LocalDateTime data) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		return formatter.format(data);
	}
	
	public static String formataDataDiaMesAno(LocalDate data) {
		return dfDataDiaMesAno.format(data);
	}
	
	/**
	 * Retorna a pasta padrão onde os arquivos TXT e XML serão gerados pela ferramenta.
	 * 
	 * Direciona, também, os arquivos de log para esta pasta. É necessário direcionar os logs para
	 * cada thread que for utilizada!
	 * Fonte: http://stackoverflow.com/questions/25114526/log4j2-how-to-write-logs-to-separate-files-for-each-user
	 */
	public static File prepararPastaDeSaida() {
		if (pastaSaida == null) {
			File pastaOutputRaiz = getPastaOutputRaiz();
			String tipoCarga = DataJudUtil.TIPO_CARGA + " " + DataJudUtil.MES_ANO_CORTE;

			File pastaOutputCarga = new File(pastaOutputRaiz, tipoCarga);
			pastaSaida = pastaOutputCarga;
			
			// Mapeia STDOUT e STDERR para os arquivos de log
			System.setErr(new PrintStream(new LoggingOutputStream(LogManager.getRootLogger(), Level.ERROR), true));
			System.setOut(new PrintStream(new LoggingOutputStream(LogManager.getRootLogger(), Level.INFO), true));
		}
		
		prepararThreadLog();
		
		return pastaSaida;
	}

	public static void prepararThreadLog() {
		// TODO: Resolver problema reportado pelo Wiler: quando número de threads é maior que número de processos a gerar, dá um erro nos logs
		ThreadContext.put("logFolder", pastaSaida.getAbsolutePath());
	}

	/**
	 * Retorna o caminho da pasta "output raiz", conforme definido pelo parâmetro "pasta_saida_padrao" no arquivo de configurações.
	 * 
	 * Se a configuração não existir, utiliza a pasta padrão ("output").
	 * 
	 * @return
	 */
	public static File getPastaOutputRaiz() {
		return new File(Auxiliar.getParametroConfiguracaoComValorPadrao(Parametro.pasta_saida_padrao, "output"));
	}
	
	/**
	 * Solicita que o usuário responda uma pergunta no console.
	 * 
	 * @param pergunta
	 * @return
	 */
	public static String pedirParaUsuarioDigitarSenha(String pergunta) {
		LOGGER.info(pergunta);
		
		// O objeto "Console" não existirá ao rodar o projeto, por exemplo, de dentro do Eclipse.
		Console console = System.console();
		if (console != null) {
			char[] passwordChars = console.readPassword(pergunta + ": ");
			return new String(passwordChars);
			
		} else {
			System.out.println(pergunta + " (CUIDADO: AS INFORMAÇÕES DIGITADAS SERÃO EXIBIDAS EM TEXTO PLANO!)");
			String senha = Auxiliar.readStdin();
			System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
			return senha;
		}
	}
	
	/**
	 * Lê na entrada padrão um comando do usuário e retorna em uma String
	 */
	public static String readStdin() {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		try {
			String valorDigitado = in.readLine();
			return valorDigitado;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Compacta uma pasta inteira em um arquivo ZIP
	 * 
	 * Fonte: http://stackoverflow.com/questions/23318383/compress-directory-into-a-zipfile-with-commons-io
	 */
	@SuppressWarnings("deprecation")
	public static void compressZipfile(String sourceDir, String outputFile) throws IOException {
	    ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(outputFile));
	    zipFile.setLevel(Deflater.BEST_COMPRESSION);
	    compressDirectoryToZipfile(sourceDir, sourceDir, zipFile);
	    IOUtils.closeQuietly(zipFile);
	}

	/**
	 * Fonte: http://stackoverflow.com/questions/23318383/compress-directory-into-a-zipfile-with-commons-io
	 */
	@SuppressWarnings("deprecation")
	private static void compressDirectoryToZipfile(String rootDir, String sourceDir, ZipOutputStream out) throws IOException {
		
	    File[] files = new File(sourceDir).listFiles();
		LOGGER.info("Compactando pasta " + sourceDir + " com " + files.length + " arquivo(s)...");
		for (File file : files) {
	        if (file.isDirectory()) {
	            compressDirectoryToZipfile(rootDir, sourceDir + File.separator + file.getName(), out);
	        } else {
	            String zipEntryName = FilenameUtils.normalize(sourceDir.replace(rootDir, "") + File.separator + file.getName());
				ZipEntry entry = new ZipEntry(zipEntryName);
	            out.putNextEntry(entry);

	            FileInputStream in = new FileInputStream(sourceDir + File.separator + file.getName());
	            IOUtils.copy(in, out);
	            IOUtils.closeQuietly(in);
	        }
	    }
	}
	
	/**
	 * Recupera a lista de processos do PJe que serão enviados na remessa atual.
	 * 
	 * @param grau
	 * @return
	 */
	public static List<String> carregarListaProcessosPJe(String grau) {
		try {
			LocalDate dataCorte = DataJudUtil.getDataCorte();
			TipoRemessaEnum tipoRemessa = DataJudUtil.getTipoRemessa();
			List<OrigemProcessoEnum> origens = new ArrayList<OrigemProcessoEnum>();
			origens.add(OrigemProcessoEnum.PJE);
			origens.add(OrigemProcessoEnum.HIBRIDO);

			List<ProcessoEnvio> processosEnvio = processoEnvioDAO.getProcessosRemessa(dataCorte, tipoRemessa, grau, origens);
			List<String> listaProcessos = processosEnvio.stream().map(o -> o.getNumeroProcesso()).collect(Collectors.toList());


			LOGGER.info("Foram carregados " + listaProcessos.size() + " processo(s). Grau: " + grau +". Base: PJe");
			return listaProcessos;
			
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Informa onde deve ser gravado o arquivo XML de um determinado processo
	 * @param grau
	 * @param numeroProcesso
	 * @return
	 */
	public static File gerarNomeArquivoJson(int grau, String numeroProcesso, OrigemProcessoEnum origemProcessoEnum) {
		File pastaRaiz = Auxiliar.getPastaXMLsIndividuais(grau);
		String diretorio = origemProcessoEnum.equals(OrigemProcessoEnum.LEGADO) 
						? "Legado" 
						: (origemProcessoEnum.equals(OrigemProcessoEnum.HIBRIDO) 
								? "PJe/Hibrido"
								: "PJe"
						  );
		File pastaRaizSistemaJudicial = new File(pastaRaiz, diretorio);
		return new File(pastaRaizSistemaJudicial, numeroProcesso + "_validador_cnj.json");
	}

	/**
	 * Informa onde deve ser gravado o arquivo XML de um determinado processo
	 * @param grau
	 * @param numeroProcesso
	 * @return
	 */
	public static File gerarNomeArquivoIndividualParaProcesso(BaseEmAnaliseEnum base, int grau, String numeroProcesso) {
		File pastaRaiz = Auxiliar.getPastaXMLsIndividuais(grau);
		File pastaRaizSistemaJudicial = new File(pastaRaiz, base.isBasePJe() ? "PJe" : "Legado");
		return new File(pastaRaizSistemaJudicial, numeroProcesso + ".xml");
	}
	
	/**
	 * Informa onde deve ser gravado o arquivo com o número do protocolo de envio de um determinado processo do PJe
	 * 
	 * @return
	 */
	public static File gerarNomeArquivoProtocoloProcessoEnviado(File arquivoXMLProcesso) {
		return new File(arquivoXMLProcesso.getAbsolutePath() + SUFIXO_PROTOCOLO);
	}
	
	/**
	 * Informa onde deve ser gravado o arquivo que indica a ocorrência de erro na geração de um XML
	 * 
	 * @return
	 */
	public static File gerarNomeArquivoProcessoErro(File arquivoXMLProcesso) {
		return new File(arquivoXMLProcesso.getAbsolutePath() + SUFIXO_PROTOCOLO_ERRO);
	}
	
	/**
	 * Informa onde deve ser gravado o arquivo que indica que o processo foi ACEITO com sucesso no CNJ
	 * 
	 * @return
	 */
	public static File gerarNomeArquivoProcessoSucesso(File arquivoProtocolo) {
		return new File(arquivoProtocolo.getAbsolutePath() + SUFIXO_PROTOCOLO_SUCESSO);
	}

	/**
	 * Informa onde deve ser gravado o arquivo que indica que o processo foi NEGADO no CNJ
	 * 
	 * @return
	 */
	public static File gerarNomeArquivoProcessoNegado(File arquivoProtocolo) {
		return new File(arquivoProtocolo.getAbsolutePath() + SUFIXO_PROTOCOLO_ERRO);
	}
	
	public static void aguardaUsuarioApertarENTERComTimeout(int minutos) throws InterruptedException, IOException {
		
		if (!permitirAguardarUsuarioApertarENTER) {
			return;
		}
		
		LOGGER.warn("Pressione ENTER ou aguarde " + minutos + " minuto(s) para continuar. Se preferir, aborte este script para resolver o problema.");

		// Aguarda um tempo ou até usuário digite alguma coisa na STDIN
		int segundos = 60 * minutos;
		for (int i=0; i<segundos; i++) {
			Thread.sleep(1000);
			if (System.in.available() > 0) {
				Auxiliar.readStdin();
				break;
			}
		}
	}

	public static boolean deveProcessarSegundoGrau() {
		return getParametroBooleanConfiguracao(Parametro.gerar_xml_2G);
	}
	
	public static boolean deveProcessarPrimeiroGrau() {
		return getParametroBooleanConfiguracao(Parametro.gerar_xml_1G);
	}
	
	public static boolean deveProcessarGrau(int grau) {
		if (grau == 1) {
			return getParametroBooleanConfiguracao(Parametro.gerar_xml_1G);
		} else if (grau == 2) {
			return getParametroBooleanConfiguracao(Parametro.gerar_xml_2G);
		}
		return false;
	}
	
	public static boolean deveProcessarProcessosPje() {
		return MESCLAR_MOVIMENTOS_LEGADO_MIGRADO_VIA_XML || TIPO_SISTEMA.equals(TipoSistemaJudicialEnum.APENAS_PJE.getCodigo())
				|| TIPO_SISTEMA.equals(TipoSistemaJudicialEnum.APENAS_PJE_COM_MIGRADOS_LEGADO.getCodigo())
				|| TIPO_SISTEMA.equals(TipoSistemaJudicialEnum.TODOS.getCodigo());
	}

	public static boolean deveProcessarProcessosSistemaLegadoNaoMigradosParaOPjeViaStaging() {
		return !MESCLAR_MOVIMENTOS_LEGADO_MIGRADO_VIA_XML 
				&& (TIPO_SISTEMA.equals(TipoSistemaJudicialEnum.APENAS_LEGADO.getCodigo())
				|| TIPO_SISTEMA.equals(TipoSistemaJudicialEnum.TODOS.getCodigo()));
	}

	public static boolean deveProcessarProcessosSistemaLegadoMigradosParaOPJeViaStaging() {
		return !MESCLAR_MOVIMENTOS_LEGADO_MIGRADO_VIA_XML 
				&& (TIPO_SISTEMA.equals(TipoSistemaJudicialEnum.APENAS_PJE_COM_MIGRADOS_LEGADO.getCodigo())
				|| TIPO_SISTEMA.equals(TipoSistemaJudicialEnum.TODOS.getCodigo()));
	}
	
	public static void validarTipoRemessaAtual(TipoRemessaEnum tipoRemessaAtual) {
		if (tipoRemessaAtual == null) {
			throw new RuntimeException("A remessa indicada no arquivo config.properties não é válida.");
		}
	}
	
	
	public static File getArquivoconfiguracoes() {
		return arquivoConfiguracoes;
	}
	
	public static void setPermitirAguardarUsuarioApertarENTER(boolean permitirAguardarUsuarioApertarENTER) {
		Auxiliar.permitirAguardarUsuarioApertarENTER = permitirAguardarUsuarioApertarENTER;
	}


	public static void fechar(AutoCloseable objeto) {
		if (objeto != null) {
			try {
				objeto.close();
			} catch (Exception e) {
				LOGGER.error("Erro fechando " + objeto, e);
			}
		}
	}
	
	public static String removerPontuacaoNumeroProcesso(String numeroProcesso) {
		return numeroProcesso.replaceAll("[^0-9]", "");
	}
	
	public static void safeSleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
