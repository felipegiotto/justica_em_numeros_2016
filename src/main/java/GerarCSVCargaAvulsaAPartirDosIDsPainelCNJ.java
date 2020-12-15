import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.util.ProcessoUtil;

/**
 * 
 * @author ivan.franca@trt6.jus.br
 *
 */
public class GerarCSVCargaAvulsaAPartirDosIDsPainelCNJ {
	
	private static final Logger LOGGER = LogManager.getLogger(GerarCSVCargaAvulsaAPartirDosIDsPainelCNJ.class);
	
	public static void main(String[] args) throws Exception {
		Auxiliar.prepararPastaDeSaida();
		File arquivoIDsPainelQualificacaoCNJ = new File(
				"src/main/resources/carga_avulsa/lista_ids_painel_qualificacao.csv");
		Map<String, List<String>> processosPorGrau = new HashMap<String, List<String>>();
		Scanner scanner = new Scanner(arquivoIDsPainelQualificacaoCNJ, "UTF-8");
		try {
			int linha = 0;
			while (scanner.hasNextLine()) {
				linha++;
				String line = scanner.nextLine();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				String[] partes = line.split("_");
				if (partes.length != 5) {
					throw new IOException("Inconsistência na linha " + linha + " do arquivo '"
							+ arquivoIDsPainelQualificacaoCNJ
							+ "': a linha deve conter 5 valores, separados por underline ('_'), seguindo o padrão de IDs fornecidos pelo painel de qualificação do CNJ.");
				}
				if (partes[2] != null && partes[4] != null) {
					String grau = partes[2].replaceAll("[^0-9]", "");
					String numProcesso = partes[4];

					if (!processosPorGrau.containsKey(grau)) {
						processosPorGrau.put(grau, new ArrayList<String>());
					}

					processosPorGrau.get(grau).add(numProcesso);
				}

			}
			File arquivoSaida = new File("src/main/resources/carga_avulsa/carga_avulsa.csv");
			FileWriter fw = new FileWriter(arquivoSaida);
			try {
				for (String grau : processosPorGrau.keySet()) {
					for (String numeroProcesso : processosPorGrau.get(grau)) {
						fw.append(grau + ";" + ProcessoUtil.formatarNumeroProcesso(numeroProcesso));
						fw.append("\r\n");
					}
				}
			} finally {
				fw.close();
			}

		} finally {
			scanner.close();
		}
		
		LOGGER.info("Geração do arquivo carga_avulsa.csv finalizado!");
	}
}
