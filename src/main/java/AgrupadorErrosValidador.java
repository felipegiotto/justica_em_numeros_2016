
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.AcumuladorExceptions;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.util.ProcessoUtil;

/**
 * 
 * Essa Classe pode ser utilizada agrupar os erros apresentados pelo validador. 
 * 
 * Os erros apontados nos processos do primeiro grau serão agrupados no arquivo ../G1/erros_validador_G1.txt  
 * Os erros apontados nos processos do segundo grau serão agrupados no arquivo ../G2/erros_validador_G2.txt  
 * 
 * 
 * @author ivan.franca@trt6.jus.br
 *
 */
public class AgrupadorErrosValidador {

	private static final Logger LOGGER = LogManager.getLogger(AgrupadorErrosValidador.class);

	private static void localizarArquivos(int grau, String sufixo, List<File> arquivosParaEnviar) {
		File pastaXMLsParaEnvio = Auxiliar.getPastaXMLsIndividuais(grau);
		localizarArquivosRecursivamente(pastaXMLsParaEnvio, grau, sufixo, arquivosParaEnviar);
	}

	private static void localizarArquivosRecursivamente(File pasta, int grau, String sufixo,
			List<File> arquivosParaEnviar) {

		LOGGER.trace("Localizando arquivos na pasta '" + pasta.getAbsolutePath() + "'...");
		if (!pasta.isDirectory()) {
			return;
		}
		FileFilter aceitarPastasOuXMLs = new FileFilter() {

			@Override
			public boolean accept(File file) {
				return file.isDirectory() || file.getName().toUpperCase().endsWith(sufixo);
			}
		};

		// Localiza todos os arquivos XML da pasta
		File[] arquivos = pasta.listFiles(aceitarPastasOuXMLs);
		for (File filho : arquivos) {
			if (filho.isDirectory()) {
				localizarArquivosRecursivamente(filho, grau, sufixo, arquivosParaEnviar);
			} else {
				try (BufferedReader br = new BufferedReader(new FileReader(filho))) {
					arquivosParaEnviar.add(filho);
				} catch (Exception e) {
					LOGGER.error("* Nãoo foi possível ler o arquivo " + filho.toString());
				}
			}
		}
	}

	public static void main(String[] args) {
		Auxiliar.prepararPastaDeSaida();

		List<File> arquivos = new ArrayList<>();

		int[] grausAnalisados = { 1, 2 };
		int count = 0;
		try {
			for (int grau : grausAnalisados) {
				arquivos = new ArrayList<>();
				localizarArquivos(grau, ".JSON", arquivos);
				Map<String, List<String>> mapaErros = new HashMap<String, List<String>>();
				for (File file : arquivos) {
					count = count + 1;
					LOGGER.info("Arquivo " + count + ": " + file.getName());
					String jsonRespostaValidador = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())),
							StandardCharsets.UTF_8);
					JsonObject rootObject = JsonParser.parseString(jsonRespostaValidador).getAsJsonObject();
					JsonObject errosPorProcesso = rootObject.get("errosPorProcesso").getAsJsonObject();
	
					if (!errosPorProcesso.keySet().isEmpty()) {
						for (String key : errosPorProcesso.keySet()) {
							JsonArray arrayErrosPorProcesso = errosPorProcesso.get(key).getAsJsonArray();
							for (JsonElement jsonElement : arrayErrosPorProcesso) {
								JsonObject jsonErro = jsonElement.getAsJsonObject();
								String chaveMapa = jsonErro.get("id").getAsString();
	
								if (!mapaErros.containsKey(chaveMapa)) {
									mapaErros.put(chaveMapa, new ArrayList<String>());
								}
								mapaErros.get(chaveMapa).add(ProcessoUtil.formatarNumeroProcesso(key) 
										+ "(" + file.getParentFile().getName() + "): " 
										+ jsonErro.get("descricao").getAsString());
							}
						}
					}
				}
	
				File pastaXMLsParaEnvio = new File(Auxiliar.prepararPastaDeSaida(), "G" + grau);
				File arquivoRelatorioErros = new File(pastaXMLsParaEnvio.getAbsolutePath(), "/erros_validador_G" + grau + ".txt");
				List<String> erros = new ArrayList<String>();
				for (String tipoErro : mapaErros.keySet()) {
					erros.add(tipoErro);
					for (String numeroProcesso : mapaErros.get(tipoErro)) {
						erros.add("    " + numeroProcesso);
					}
					erros.add(" ");
				}
				FileUtils.writeLines(arquivoRelatorioErros, erros);
			}
		} catch (Exception e) {
			String origemOperacao = "Erro ao agrupar erros apontados pelo validador.";
			AcumuladorExceptions.instance().adicionarException(origemOperacao,
					"Erro ao agrupar erros apontados pelo validador: "
							+ e.getLocalizedMessage(), e, true);
		}
	}
}
