package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Classe que contém métodos auxiliares utilizados nesse projeto.
 * @author fgiotto
 *
 */
public class Auxiliar {

	private static final File arquivoConfiguracoes = new File("config.properties");
	private static final File arquivoModeloConfiguracoes = new File("config_modelo.properties");

	private static final Logger LOGGER = LogManager.getLogger(Auxiliar.class);
	private static Properties configs = null;

	/**
	 * Cria uma conexão com o banco de dados de 1o Grau do PJe, a partir do parâmetro "url_jdbc_1g"
	 * do arquivo "config.properties"
	 * 
	 * @return
	 * @throws SQLException
	 */
	public static Connection getConexaoPJe1G() throws SQLException {
		Connection connection = getConexaoDasConfiguracoes("url_jdbc_1g");
		return connection;
	}

	
	/**
	 * Cria uma conexão com o banco de dados de 2o Grau do PJe, a partir do parâmetro "url_jdbc_2g"
	 * do arquivo "config.properties"
	 * 
	 * @return
	 * @throws SQLException
	 */
	public static Connection getConexaoPJe2G() throws SQLException {
		Connection connection = getConexaoDasConfiguracoes("url_jdbc_2g");
		return connection;
	}
	
	
	/**
	 * Cria uma nova conexão com o banco de dados do PJe, recebendo por parâmetro o nome da chave do 
	 * arquivo "config.properties" que contém a String JDBC que deverá ser utilizada para conexão.
	 * 
	 * @param parametro
	 * @return
	 * @throws SQLException
	 */
	private static Connection getConexaoDasConfiguracoes(String parametro) throws SQLException {
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		LOGGER.info("Abrindo conexão com o PJe");
		return DriverManager.getConnection(getParametroConfiguracao(parametro, true));
	}

	/**
	 * Carrega um parâmetro booleano do arquivo "config.properties". Se o parâmetro não existir ou não for "SIM" ou "NAO", lança uma exceção.
	 * 
	 * @param parametro
	 * @return
	 */
	public static boolean getParametroBooleanConfiguracao(String parametro) {
		String valor = getParametroConfiguracao(parametro, false);
		if ("SIM".equals(valor)) {
			return true;
		}
		if ("NAO".equals(valor)) {
			return false;
		}
		throw new RuntimeException("Defina o parâmetro '" + parametro + "' no arquivo '" + arquivoConfiguracoes + "' com os valores SIM ou NAO");
	}
	
	
	/**
	 * Carrega um parâmetro numérico do arquivo "config.properties". Se o parâmetro não existir ou não for numérico, lança uma exceção.
	 * 
	 * @param parametro
	 * @return
	 */
	public static int getParametroInteiroConfiguracao(String parametro) {
		try {
			return Integer.parseInt(getParametroConfiguracao(parametro, true));
		} catch (NumberFormatException ex) {
			throw new RuntimeException("O parâmetro '" + parametro + "', no arquivo '" + arquivoConfiguracoes + "', deve ser numérico.");
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
	public static String getParametroConfiguracao(String parametro, boolean obrigatorio) {
		if (obrigatorio && !getConfigs().containsKey(parametro)) {
			throw new RuntimeException("Defina o parâmetro '" + parametro + "' no arquivo '" + arquivoConfiguracoes + "'");
		}
		return getConfigs().getProperty(parametro);
	}

	
	/**
	 * Carrega e mantém em cache as configurações definidas no arquivo "config.properties"
	 * 
	 * @return
	 */
	private static Properties getConfigs() {
		if (configs == null) {

			try {
				if (!arquivoConfiguracoes.exists()) {
					throw new RuntimeException("Crie o arquivo '" + arquivoConfiguracoes + "' com os parâmetros necessários! Utilize como modelo o arquivo '" + arquivoModeloConfiguracoes + "'");
				}
				Properties p = new Properties();

				FileInputStream fis = new FileInputStream(arquivoConfiguracoes);
				try {
					p.load(fis);
				} finally {
					fis.close();
				}
				configs = p;
			} catch (IOException ex) {
				throw new RuntimeException("Erro ao ler arquivo de configurações (" + arquivoConfiguracoes + "): " + ex.getLocalizedMessage(), ex);
			}
		}
		return configs;
	}

	
	/**
	 * Consome um stream, mostrando seu resultado no console
	 * 
	 * @param inputStream: stream que será lido
	 * @param prefix: prefixo para escrever no console antes de cada linha lida do stream
	 * @throws IOException
	 */
	public static void consumirStream(InputStream inputStream, String prefix) throws IOException {
		try (Scanner scanner = new Scanner(inputStream)) {
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
	
}
