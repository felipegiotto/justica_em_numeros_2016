import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.AcumuladorExceptions;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DataJudException;
import br.jus.trt4.justica_em_numeros_2016.dao.LoteDao;
import br.jus.trt4.justica_em_numeros_2016.dao.LoteProcessoDao;
import br.jus.trt4.justica_em_numeros_2016.entidades.ChaveProcessoCNJ;
import br.jus.trt4.justica_em_numeros_2016.entidades.Lote;
import br.jus.trt4.justica_em_numeros_2016.entidades.LoteProcesso;
import br.jus.trt4.justica_em_numeros_2016.enums.SituacaoLoteProcessoEnum;
import br.jus.trt4.justica_em_numeros_2016.enums.TipoRemessaEnum;

/**
 * 
 * Essa Classe pode ser utilizada para copiar os XMLs que foram salvos no banco para o sistema de Arquivos. 
 * Basta informar no main: 
 * - A data de corte da Remessa; 
 * - O tipo da Remessa; 
 * - O número do Lote (o número, não o id!); 
 * - O grau dos processos que devem ter o XML copiado. Se o array for vazio, todos os graus serão considerados; 
 * - Assituações dos loteProcessos que devem ter o XML copiado. Se o array for vazio, todas as situações serão consideradas.
 * 
 * @author ivan.franca@trt6.jus.br
 *
 */
public class CopiarXMLsDoBancoDeDadosParaODisco {

	private static final Logger LOGGER = LogManager.getLogger(CopiarXMLsDoBancoDeDadosParaODisco.class);

	private static final LoteDao loteDAO = new LoteDao();
	private static final LoteProcessoDao loteProcessoDAO = new LoteProcessoDao();

	public static void main(String[] args) throws Exception {
		Auxiliar.prepararPastaDeSaida();

		LocalDate dataCorteRemessa = LocalDate.of(2021, Month.JANUARY, 31);
		TipoRemessaEnum tipoRemessa = TipoRemessaEnum.MENSAL;
		String numeroDoLote = "1";
		int[] grausAnalisados = { 1, 2 };
		SituacaoLoteProcessoEnum[] situacoesLoteProcesso = { SituacaoLoteProcessoEnum.XML_GERADO_COM_ERRO };

		try {
			CopiarXMLsDoBancoDeDadosParaODisco.copiarXMLs(dataCorteRemessa, tipoRemessa, numeroDoLote, grausAnalisados,
					situacoesLoteProcesso);
		} catch (Exception e) {
			String origemOperacao = "Erro ao copiar os XMLs.";
			AcumuladorExceptions.instance().adicionarException(origemOperacao,
					"Erro ao copiar os XMLs: " + e.getLocalizedMessage(), e, true);
		}

		LOGGER.info("Copia dos XMLs finalizada!");
	}

	public static void copiarXMLs(LocalDate dataCorteRemessa, TipoRemessaEnum tipoRemessa, String numeroDoLote,
			int[] grausAnalisados, SituacaoLoteProcessoEnum[] situacoesLoteProcesso) throws Exception {
		List<SituacaoLoteProcessoEnum> situacoesParaAnalisadar = new ArrayList<SituacaoLoteProcessoEnum>();
		for (SituacaoLoteProcessoEnum situacao : situacoesLoteProcesso) {
			situacoesParaAnalisadar.add(situacao);
		}

		List<String> grausAProcessar = new ArrayList<String>();
		for (int grau : grausAnalisados) {
			grausAProcessar.add(Integer.toString(grau));
		}

		Lote lote = loteDAO.getLoteDeUmaRemessaPeloNumero(dataCorteRemessa, tipoRemessa, numeroDoLote);

		if (lote == null) {
			throw new DataJudException("Não foi possível encontrar o lote informado.");
		} else {

			List<Long> idProcessosComXMLParaCopiar = loteProcessoDAO.getIDProcessosPorLoteESituacao(lote,
					situacoesParaAnalisadar, grausAProcessar);

			final int tamanhoLote = 100;
			final int qtdThreads = 10;
			final AtomicInteger counter = new AtomicInteger();

			final Collection<List<Long>> idsLoteLoteProcessos = idProcessosComXMLParaCopiar.stream()
					.collect(Collectors.groupingBy(it -> counter.getAndIncrement() / tamanhoLote)).values();

			// Para evitar a exceção "Unable to invoke factory method in class
			// org.apache.logging.log4j.core.appender.RollingFileAppender
			// for element RollingFile" ao tentar criar um appender RollingFile para uma thread de um arquivo
			// inexistente
			int numeroThreads = qtdThreads > idsLoteLoteProcessos.size() ? idsLoteLoteProcessos.size() : qtdThreads;

			LOGGER.info("Iniciando a copia de " + idProcessosComXMLParaCopiar.size() + " XMLs, utilizando "
					+ numeroThreads + " thread(s)");
			for (List<Long> idsLoteProcessos : idsLoteLoteProcessos) {
				try {
					List<LoteProcesso> loteProcessosComXML = loteProcessoDAO.getProcessosComXMLPorIDs(idsLoteProcessos);
					ExecutorService threadPool = Executors.newFixedThreadPool(numeroThreads);
					for (LoteProcesso loteProcesso : loteProcessosComXML) {
						threadPool.execute(() -> {
							Auxiliar.prepararThreadLog();
							ChaveProcessoCNJ processo = loteProcesso.getChaveProcessoCNJ();

							File arquivoXML = Auxiliar.gerarNomeArquivoIndividualParaProcesso(
									Integer.parseInt(processo.getGrau()), processo.getNumeroProcesso(),
									loteProcesso.getOrigem());

							if (arquivoXML.exists() && arquivoXML.length() == 0) {
								arquivoXML.delete();
							}

							try {
								FileUtils.writeByteArrayToFile(arquivoXML,
										loteProcesso.getXmlProcesso().getConteudoXML());
							} catch (IOException e) {
								String origemOperacao = "Erro ao escrever o XML do processo: "
										+ processo.getNumeroProcesso() + "; Grau: " + processo.getGrau();
								AcumuladorExceptions.instance().adicionarException(origemOperacao,
										"Erro ao escrever o XML do processo: " + processo.getNumeroProcesso()
												+ "; Grau: " + processo.getGrau() + ": " + e.getLocalizedMessage(),
										e, true);
							}

						});
					}

					threadPool.shutdown();
					threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
				} catch (Exception e) {
					String origemOperacao = "Erro ao copiar os XMLs dos processos para o sistema de arquivos.";
					AcumuladorExceptions.instance().adicionarException(origemOperacao,
							"Erro ao copiar os XMLs dos processos para o sistema de arquivos: "
									+ e.getLocalizedMessage(),
							e, true);
				}
			}
		}

	}

}
