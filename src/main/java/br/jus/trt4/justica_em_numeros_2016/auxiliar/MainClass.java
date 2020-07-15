package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import br.jus.trt4.justica_em_numeros_2016.tasks.Op_1_BaixaListaDeNumerosDeProcessos;
import br.jus.trt4.justica_em_numeros_2016.tasks.Op_2_GeraXMLsIndividuais;
import br.jus.trt4.justica_em_numeros_2016.tasks.Op_3_UnificaArquivosXML;
import br.jus.trt4.justica_em_numeros_2016.tasks.Op_4_ValidaEnviaArquivosCNJ;
import br.jus.trt4.justica_em_numeros_2016.tasks.Op_6_BackupConfiguracoes;
import br.jus.trt4.justica_em_numeros_2016.tasks.Op_X_OperacaoCompleta;

public class MainClass {

	public static void main(String[] args) throws Exception {
		System.out.println("Digite código da operação a ser executada:");
		System.out.println("1: BaixaListaDeNumerosDeProcessos");
		System.out.println("2: GeraXMLsIndividuais");
		System.out.println("3: UnificaArquivosXML");
		System.out.println("4: ValidaEnviaArquivosCNJ");
		System.out.println("6: BackupConfiguracoes");
		System.out.println("X: OperacaoCompleta");
		switch (Auxiliar.readStdin().toUpperCase()) {
		case "1": 
			Op_1_BaixaListaDeNumerosDeProcessos.main(null);
			break;
		case "2": 
			Op_2_GeraXMLsIndividuais.main(null);
			break;
		case "3": 
			Op_3_UnificaArquivosXML.main(null);
			break;
		case "4": 
			Op_4_ValidaEnviaArquivosCNJ.main(null);
			break;
		case "6": 
			Op_6_BackupConfiguracoes.main(null);
			break;
		case "X": 
			Op_X_OperacaoCompleta.main(null);
			break;
		}
	}
}
