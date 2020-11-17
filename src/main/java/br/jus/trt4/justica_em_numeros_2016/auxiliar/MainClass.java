package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import br.jus.trt4.justica_em_numeros_2016.tasks.Op_1_BaixaListaDeNumerosDeProcessos;
import br.jus.trt4.justica_em_numeros_2016.tasks.Op_2_GeraEValidaXMLsIndividuais;
import br.jus.trt4.justica_em_numeros_2016.tasks.Op_3_EnviaArquivosCNJ;
import br.jus.trt4.justica_em_numeros_2016.tasks.Op_4_ConfereProtocolosCNJ;
import br.jus.trt4.justica_em_numeros_2016.tasks.Op_5_BackupConfiguracoes;
import br.jus.trt4.justica_em_numeros_2016.tasks.Op_X_OperacaoCompleta;
import br.jus.trt4.justica_em_numeros_2016.tasks.Op_Y_OperacaoFluxoContinuo;

/**
 * Classe disparada ao se executar diretamente o jar da aplicação.
 * 
 * OPCIONALMENTE pode receber até três argumentos de execucação: 
 * 		1) Código da operação a ser executada
 * 		2) Tipo de carga XML , podendo ser COMPLETA, MENSAL, TODOS_COM_MOVIMENTACOES, TESTES e PROCESSO.  
 *		3) Caractere (S, N) indicando se as operações Op_3_EnviaArquivosCNJ e Op_4_ConfereProtocolosCNJ
 *		   serão reiniciadas caso aconteça algum erro. Valor padrão: S.
 *		4) Caractere (S, N) indicando se a operação Op_X_OperacaoCompleta deve continuar caso aconteça algum erro em quaisquer 
 *		   das operações. As operações 4 e 5 podem ser reiniciadas caso aconteça algum erro, porém os erros das operações anteriores
 *		   são considerados e farão com que essas operações nunca terminem, mesmo que não aconteça nenhum erro nelas.
 *		   Valor padrão: N.    
 * @see br.jus.trt4.justica_em_numeros_2016.auxiliar#carregarPropertiesDoArquivo(String)
 * 
 * @author felipe.giotto@trt4.jus.br
 */
public class MainClass {

	public static void main(String[] args) throws Exception {
		String opcao;
		String tipoCargaXml = null;
		String reiniciarOperacaoEmCasoErro = "S";
		String continuarOperacaoCompletaEmCasoErro = "N";

		if (args.length > 0) {
			opcao = args[0];
			tipoCargaXml = args.length > 1 ? args[1] : null;
			reiniciarOperacaoEmCasoErro = args.length > 2 ? args[2] : "S";
			continuarOperacaoCompletaEmCasoErro = args.length > 3 ? args[3] : "N";
		} else {
			System.out.println("Digite código da operação a ser executada:");
			System.out.println("1: BaixaListaDeNumerosDeProcessos");
			System.out.println("2: GeraXMLsIndividuais");
			System.out.println("3: DESATIVADO: UnificaArquivosXML");
			System.out.println("4: ValidaEnviaArquivosCNJ");
			System.out.println("5: ConfereProtocolosCNJ");
			System.out.println("6: BackupConfiguracoes");
			System.out.println("X: OperacaoCompleta");
			System.out.println("Y: Nova operação completa (protótipo Op_Y_OperacaoFluxoContinuo)");
			opcao = Auxiliar.readStdin().toUpperCase();
		}
		
		//Permite que o parâmetro tipo_carga_xml seja passado pela linha de comando, desconsiderando o que estiver no arquivo .properties 
		Auxiliar.carregarPropertiesDoArquivo(tipoCargaXml);

		switch (opcao) {
		case "1":
			Op_1_BaixaListaDeNumerosDeProcessos.main(null);
			break;
		case "2":
			Op_2_GeraEValidaXMLsIndividuais.main(null);
			break;
		case "4":
			Op_3_EnviaArquivosCNJ.main(new String [] {reiniciarOperacaoEmCasoErro});
			break;
		case "5":
			Op_4_ConfereProtocolosCNJ.main(new String [] {reiniciarOperacaoEmCasoErro});
			break;
		case "6":
			Op_5_BackupConfiguracoes.main(null);
			break;
		case "X":
			Op_X_OperacaoCompleta.main(new String [] {reiniciarOperacaoEmCasoErro, continuarOperacaoCompletaEmCasoErro});
			break;
		case "Y":
			Op_Y_OperacaoFluxoContinuo.main(null);
			break;
		}
	}
}
