package br.jus.trt4.justica_em_numeros_2016.auxiliar;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ValidadorIntegridadeXMLCNJ {

	public static void buscarProblemasValidadorCNJ(String jsonRespostaValidador) throws Exception {
		
		elementoJSONDeveExistir(jsonRespostaValidador, "Validador do CNJ retornou 'null'");
		
		JsonObject rootObject = JsonParser.parseString(jsonRespostaValidador).getAsJsonObject();
		elementoJSONDeveExistir(rootObject, "Validador do CNJ não retornou um JSON válido");

		JsonObject errosPorProcesso = rootObject.get("errosPorProcesso").getAsJsonObject();
		JsonObject errosPorIdentificador = rootObject.get("errosPorIdentificador").getAsJsonObject();
		int qtdErros = rootObject.get("qtdErros").getAsInt();
		
		List<String> problemas = new ArrayList<>();

		if (!errosPorProcesso.keySet().isEmpty() || !errosPorIdentificador.keySet().isEmpty() || qtdErros > 0) {
			for (String key : errosPorProcesso.keySet()) {
				JsonArray arrayErrosPorProcesso = errosPorProcesso.get(key).getAsJsonArray();
				for (JsonElement jsonElement : arrayErrosPorProcesso) {
					JsonObject jsonErro = jsonElement.getAsJsonObject();
					problemas.add(jsonErro.get("id").getAsString() + ": " + jsonErro.get("descricao").getAsString());
				}
			}
			
			String errosApontados = "";
			
			for (String key : errosPorIdentificador.keySet()) {
				errosApontados = errosApontados + key + ";";			
			}
			
			if (!errosApontados.equals("")) {
				problemas.add("Resumo dos Erros Apontados: " + errosApontados.substring(0, errosApontados.length() - 1));			
			}
 
			problemas.add("Quantidade de Erros Reportados Pelo Validador: " + qtdErros);
		}
		
		if (!problemas.isEmpty()) {
			throw new IOException(StringUtils.join(problemas, ", "));
		}
	}

	private static void elementoJSONDeveExistir(Object element, String mensagem) throws IOException {
		if (element == null) {
			throw new IOException(mensagem);
		}
	}
	
	public static void main(String[] args) throws Exception {
		String filename = "0001085-22.2014.5.06.0201_validador_cnj.json";
		File arquivo = new File(filename);
		String json = FileUtils.readFileToString(arquivo, StandardCharsets.UTF_8);
		buscarProblemasValidadorCNJ(json);
	}
}
