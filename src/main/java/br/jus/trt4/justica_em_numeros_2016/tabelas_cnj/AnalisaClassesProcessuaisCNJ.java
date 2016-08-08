package br.jus.trt4.justica_em_numeros_2016.tabelas_cnj;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.cnj.intercomunicacao_2_2.TipoCabecalhoProcesso;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;

public class AnalisaClassesProcessuaisCNJ {

	private static final Logger LOGGER = LogManager.getLogger(AnalisaClassesProcessuaisCNJ.class);
	private List<Integer> classesProcessuaisCNJ;

	public AnalisaClassesProcessuaisCNJ(int grau) throws IOException {
		
		File arquivoClasses = new File("src/main/resources/tabelas_cnj", getNomeArquivoClasses(grau));
		LOGGER.info("Carregando lista de classes CNJ do arquivo " + arquivoClasses + "...");
		
		// Lista de classes processuais do CNJ. Essa lista servirá para garantir que as classes
		// informadas no arquivo XML sejam válidas.
		// Fonte: http://www.cnj.jus.br/sgt/versoes.php?tipo_tabela=C
		this.classesProcessuaisCNJ = new ArrayList<>();
		for (String classeString: FileUtils.readLines(arquivoClasses, "UTF-8")) {
			classesProcessuaisCNJ.add(Integer.parseInt(classeString));
		}
	}
	
	
	/**
	 * Identifica se o usuário quer utilizar a lista de classes da JT ou a lista completa do CNJ
	 */
	private String getNomeArquivoClasses(int grau) {
		String tabelaAssuntosNacionais = Auxiliar.getParametroConfiguracao("tabela_de_classes_nacionais", "CNJ-JT");
		
		if ("CNJ-JT".equals(tabelaAssuntosNacionais)) {
			return "classes_jt_" + grau + "g.csv";
		} else if ("CNJ-GLOBAL".equals(tabelaAssuntosNacionais)) {
			return "classes_global.csv";
		} else {
			throw new RuntimeException("Valor inválido para o parâmetro tabela_de_classes_nacionais: '" + tabelaAssuntosNacionais + "'. Verifique o arquivo de configurações.");
		}
	}
	
	
	public void preencherClasseProcessualVerificandoTPU(TipoCabecalhoProcesso cabecalhoProcesso, int codigoClasseProcessual, String descricaoClasseProcessual, String numeroProcesso) {
		cabecalhoProcesso.setClasseProcessual(codigoClasseProcessual);
		if (!classesProcessuaisCNJ.contains(codigoClasseProcessual)) {
			LOGGER.warn("Processo '" + numeroProcesso + "' possui uma classe processual que não existe nas tabelas do CNJ: " + codigoClasseProcessual + " - " + descricaoClasseProcessual);
		}
	}
}
