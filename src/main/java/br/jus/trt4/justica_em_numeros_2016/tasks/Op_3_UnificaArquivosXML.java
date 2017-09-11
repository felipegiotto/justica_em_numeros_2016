package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.cnj.intercomunicacao_2_2.TipoProcessoJudicial;
import br.jus.cnj.replicacao_nacional.ObjectFactory;
import br.jus.cnj.replicacao_nacional.Processos;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DadosInvalidosException;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Parametro;

/**
 * Lê todos os arquivos XML na pasta "output/.../Xg/xmls_individuais", tanto do PJe quanto dos
 * sistemas legados, e gera arquivos XML unificados na pasta "output/.../xmls_unificados", no formato
 * definido pelo CNJ: <SIGLA_TRIBUNAL>_<GRAU_JURISDICAO>_<DIAMESANO>-<SEQ>.
 * 
 * Será gerado um arquivo XML para cada 5000 processos (numero arbitrário, mas escolhido pois o
 * importador do CNJ processa lotes com essa quantidade), inserindo um sufixo no final do arquivo,
 * conforme orientação do CNJ no arquivo README.txt
 * 
 * @author felipe.giotto@trt4.jus.br
 */
public class Op_3_UnificaArquivosXML {

	private static final Logger LOGGER = LogManager.getLogger(Op_3_UnificaArquivosXML.class);
	
	private int grau;
	private final File pastaSaida;
	private int numeroLoteAtual = 1;
	private int qtdArquivosXMLGerados = 0;
	
	public static void main(String[] args) throws JAXBException {
		Auxiliar.prepararPastaDeSaida();

		if (!Auxiliar.deveMontarLotesDeProcessos()) {
			return;
		}
		
		if (Auxiliar.deveProcessarSegundoGrau()) {
			unificarArquivosXML(2);
		}
		
		if (Auxiliar.deveProcessarPrimeiroGrau()) {
			unificarArquivosXML(1);
		}
		
        DadosInvalidosException.mostrarWarningSeHouveAlgumErro();
		LOGGER.info("Fim!");
	}

	private static void unificarArquivosXML(int grau) throws JAXBException {
		
		LOGGER.info("");
		LOGGER.info("Iniciando unificação de arquivos XML no " + grau + "o Grau...");
		Op_3_UnificaArquivosXML baixaDados = new Op_3_UnificaArquivosXML(grau);
		baixaDados.excluirArquivosUnificadosAntigos();
		baixaDados.unificarArquivosXML();
	}
	
	public Op_3_UnificaArquivosXML(int grau) {
		this.grau = grau;
		this.pastaSaida = Auxiliar.getPastaXMLsUnificados(grau);
	}

	private void excluirArquivosUnificadosAntigos() {
		
		LOGGER.info("Excluindo arquivos unificados antigos...");
		for (File filho: this.pastaSaida.listFiles()) {
			LOGGER.info("* Excluindo " + filho + "...");
			if (!FileUtils.deleteQuietly(filho)) {
				LOGGER.error("  * Não foi possível excluir o arquivo " + filho);
			}
		}
	}

	private void unificarArquivosXML() throws JAXBException {
		
		// Pesquisando todos os arquivos que serão unificados
		LOGGER.info("Pesquisando todos os arquivos que serão unificados no " + grau + "o Grau...");
		List<File> arquivosParaProcessar = new ArrayList<>();
		File pastaRaiz = Auxiliar.getPastaXMLsIndividuais(grau);
		localizaTodosArquivosXMLRecursivamente(pastaRaiz, arquivosParaProcessar);
		int tamanhoLote = Auxiliar.getParametroInteiroConfiguracao(Parametro.tamanho_lote_processos);
		LOGGER.info("Serão gerados lotes de até " + tamanhoLote + " Bytes");
		
		// Objetos responsáveis por ler e gravar os arquivos XML
		ObjectFactory factory = new ObjectFactory();
		JAXBContext jaxbContext = JAXBContext.newInstance(Processos.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		Processos todosProcessos = factory.createProcessos();
		
		// Itera sobre todos os XMLs que existem na pasta "output/.../xmls_individuais/(grau)"
		int qtdTotal = 0;
		int bytesLote = 0;
		for (File arquivoXML: arquivosParaProcessar) {
			
			qtdTotal++;
			if (qtdTotal % 1000 == 0) {
				LOGGER.debug("Processando arquivo " + arquivoXML + " (" + qtdTotal + "/" + arquivosParaProcessar.size() + " - " + (qtdTotal * 100 / arquivosParaProcessar.size()) + "%)");
			}
			
			// Objeto com os dados lidos deste arquivo XML
			Processos processosXML = (Processos) jaxbUnmarshaller.unmarshal(arquivoXML);
			
			// Adiciona os dados lidos do XML na lista "global"
			for (TipoProcessoJudicial processo: processosXML.getProcesso()) {
				
				// Se atingiu o tamanho do lote, grava em arquivo XML e recomeça a contar
				// O tamanho do lote foi fixado em 1MB no "API REST.pdf" (não sei se será 1.048.576 
				// ou 1.000.000, escolherei o mais prudente)
				if (bytesLote + arquivoXML.length() > tamanhoLote && !todosProcessos.getProcesso().isEmpty()) {
					gravarProximoLoteXML(jaxbMarshaller, todosProcessos);
					todosProcessos.getProcesso().clear();
					bytesLote = 0;
				}
				
				// Adiciona este processo no lote
				todosProcessos.getProcesso().add(processo);
				bytesLote += arquivoXML.length();
			}
		}
		
		// Grava em arquivo XML os processos remanescentes.
		if (!todosProcessos.getProcesso().isEmpty()) {
			gravarProximoLoteXML(jaxbMarshaller, todosProcessos);
		}
		
		LOGGER.info("Total de " + qtdTotal + " processos gravados em " + qtdArquivosXMLGerados + " arquivos XML.");
	}
	
	
	/**
	 * Grava os dados de uma lista de processos em um arquivo XML
	 */
	private void gravarProximoLoteXML(Marshaller jaxbMarshaller, Processos listaProcessos) throws JAXBException {
		File arquivoSaida = new File(pastaSaida, "Lote_" + StringUtils.leftPad(Integer.toString(numeroLoteAtual), 6, '0') + ".xml");
		LOGGER.info("Gerando arquivo " + arquivoSaida + "...");
		jaxbMarshaller.marshal(listaProcessos, arquivoSaida);
		numeroLoteAtual++;
		qtdArquivosXMLGerados++;
	}

	
	private void localizaTodosArquivosXMLRecursivamente(File file, List<File> lista) {
		
		if (file.getName().startsWith("_")) {
			LOGGER.warn("Este arquivo não será processado pois seu nome começa com '_': " + file);
			return;
		}
		
		if (file.isDirectory()) {
			for (File filho: file.listFiles()) {
				localizaTodosArquivosXMLRecursivamente(filho, lista);
			}
		} else {
			if (file.getName().toUpperCase().endsWith(".XML")) {
				lista.add(file);
			}
		}
	}
}
