package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cedarsoftware.util.SafeSimpleDateFormat;

import br.jus.trt4.justica_em_numeros_2016.tasks.Op_1_ParseArquivoXSD;

public class Auxiliar {

	private static final File arquivoConfiguracoes = new File("config.properties");

	private static final Logger LOGGER = LogManager.getLogger(Op_1_ParseArquivoXSD.class);
	private static Properties configs = null;

	public static Connection getConexaoPJe1G() throws SQLException {
		Connection connection = getConexaoDasConfiguracoes("url_jdbc_1g");
		return connection;
	}

	public static Connection getConexaoPJe2G() throws SQLException {
		Connection connection = getConexaoDasConfiguracoes("url_jdbc_2g");
		return connection;
	}
	
	public static Connection getConexaoDasConfiguracoes(String parametro) throws SQLException {
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		LOGGER.info("Abrindo conexão com o PJe");
		return DriverManager.getConnection(getParametroConfiguracao(parametro, true));
	}

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
	
	public static int getParametroInteiroConfiguracao(String parametro) {
		try {
			return Integer.parseInt(getParametroConfiguracao(parametro, true));
		} catch (NumberFormatException ex) {
			throw new RuntimeException("O parâmetro '" + parametro + "', no arquivo '" + arquivoConfiguracoes + "', deve ser numérico.");
		}
	}
	
	public static String getParametroConfiguracao(String parametro, boolean obrigatorio) {
		if (obrigatorio && !getConfigs().containsKey(parametro)) {
			throw new RuntimeException("Defina o parâmetro '" + parametro + "' no arquivo '" + arquivoConfiguracoes + "'");
		}
		return getConfigs().getProperty(parametro);
	}

	private static Properties getConfigs() {
		if (configs == null) {

			try {
				if (!arquivoConfiguracoes.exists()) {
					throw new RuntimeException("Crie o arquivo '" + arquivoConfiguracoes + "' com os parâmetros necessários! Utilize como modelo o arquivo 'config_modelo.properties'");
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

	public static void consumirStream(InputStream inputStream, String prefix) throws IOException {
		try (Scanner scanner = new Scanner(inputStream)) {
			while(scanner.hasNextLine()) {
				String line = scanner.nextLine();
				LOGGER.debug(prefix + line);
			}
		}
	}
	
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
	 * Lê um campo "int" de um ResultSet, verificando se ele é nulo.
	 */
	public static int getCampoIntNotNull(ResultSet rs, String fieldName) throws SQLException {
		int result = rs.getInt(fieldName);
		if (rs.wasNull()) {
			throw new RuntimeException("Campo '" + fieldName + "' do ResultSet é nulo!");
		}
		
		return result;
	}
	
	/**
	 * Lê um campo "string" de um ResultSet, verificando se ele é nulo.
	 */
	public static String getCampoStringNotNull(ResultSet rs, String fieldName) throws SQLException {
		String result = rs.getString(fieldName);
		if (result == null) {
			throw new RuntimeException("Campo '" + fieldName + "' do ResultSet é nulo!");
		}
		
		return result;
	}
}
