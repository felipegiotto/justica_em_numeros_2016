package br.jus.trt4.justica_em_numeros_2016.tabelas_cnj;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.cnj.modeloDeTransferenciaDeDados.TipoCabecalhoProcesso;
import br.jus.trt4.justica_em_numeros_2016.dto.ClasseJudicialDto;

public class AnalisaClassesProcessuaisCNJ {

	private static final Logger LOGGER = LogManager.getLogger(AnalisaClassesProcessuaisCNJ.class);
	private List<Integer> classesProcessuaisCNJ;

	public AnalisaClassesProcessuaisCNJ(int grau) throws IOException {
		
		File arquivoClasses = new File("src/main/resources/tabelas_cnj/classes_cnj.csv");
		LOGGER.info("Carregando lista de classes CNJ do arquivo " + arquivoClasses + "...");
		
		// Lista de classes processuais do CNJ. Essa lista servirá para garantir que as classes
		// informadas no arquivo XML sejam válidas.
		// Fonte: http://www.cnj.jus.br/sgt/versoes.php?tipo_tabela=C
		this.classesProcessuaisCNJ = new ArrayList<>();
		for (String classeString: FileUtils.readLines(arquivoClasses, "UTF-8")) {
			classesProcessuaisCNJ.add(Integer.parseInt(classeString));
		}
	}
	
	
	public void validarClasseProcessualCNJ(int codigoClasseProcessual, String numeroProcesso) {
		if (!classesProcessuaisCNJ.contains(codigoClasseProcessual)) {
			LOGGER.warn("Processo '" + numeroProcesso + "' referencia uma classe processual que não existe nas tabelas do CNJ: " + codigoClasseProcessual);
		}
	}
	
	public void preencherClasseProcessualVerificandoTPU(TipoCabecalhoProcesso cabecalhoProcesso, ClasseJudicialDto classe, String numeroProcesso) {
		cabecalhoProcesso.setClasseProcessual(classe.getCodigo());
		if (!classesProcessuaisCNJ.contains(classe.getCodigo())) {
			LOGGER.warn("Processo '" + numeroProcesso + "' possui uma classe processual que não existe nas tabelas do CNJ: " + classe.getCodigo() + " - " + classe.getDescricao());
		}
	}
}
