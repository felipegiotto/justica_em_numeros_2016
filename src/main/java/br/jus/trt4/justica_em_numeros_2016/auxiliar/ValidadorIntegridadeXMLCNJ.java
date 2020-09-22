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

		JsonArray processos = rootObject.get("processos").getAsJsonArray();
		elementoJSONDeveExistir(processos, "Não foi possível identificar o array 'processos' dentro do JSON");

		JsonObject processo = processos.get(0).getAsJsonObject();
		elementoJSONDeveExistir(processo, "Não foi possível identificar o processo dentro do JSON");

		JsonObject erros = rootObject.get("erros").getAsJsonObject();

		List<String> problemas = new ArrayList<>();
		
		// O objeto "processo" conterá diversos campos, entre eles os campos "enriquecidos" pelo validador do CNJ.
		// Os que serão analisados aqui são os que iniciam com "dpj_", que possuem as informações úteis para a validação:
		// "dpj_totalDatasMovimentosInvalidas", "dpj_totalAssuntosInvalidos", "dpj_assuntosInvalidos", "dpj_totalMovimentosInvalidos", "dpj_movimentosInvalidos".
		// Os campos cuja chave contém "total" são numéricos. Os demais são arrays.
		for (String key: processo.keySet()) {
			if (key.contains("dpj_") && key.contains("Invalid")) {
				
				// Verifica se é um total de campos inválidos (que deve ser igual a zero)
				// ou se é uma lista de valores inválidos (que deve ser uma lista vazia)
				if (key.contains("total")) {
					int totalInvalidos = processo.get(key).getAsInt();
					if (totalInvalidos > 0) {
						problemas.add(key + ": " + totalInvalidos);
					}
					
				} else {
					JsonArray itensInvalidos = processo.get(key).getAsJsonArray();
					if (itensInvalidos.size() > 0) {
						problemas.add(key + ": " + itensInvalidos);
					}
				}
			}
		}
		
		for (String key : erros.keySet()) {
			JsonArray errosJsonArray = erros.get(key).getAsJsonArray();
			for (int i = 0; i < errosJsonArray.size(); i++) {
				JsonObject erro = errosJsonArray.get(0).getAsJsonObject();
				problemas.add(erro.get("id").getAsString() + ": " + erro.get("descricao").getAsString());
			}
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
//		String filename = "teste.json";
		String filename = "teste_invalido.json";
		File arquivo = new File(filename);
		String json = FileUtils.readFileToString(arquivo, StandardCharsets.UTF_8);
		buscarProblemasValidadorCNJ(json);
	}
}
