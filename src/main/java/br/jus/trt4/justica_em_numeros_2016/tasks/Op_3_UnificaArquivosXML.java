package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.cnj.intercomunicacao_2_2.TipoProcessoJudicial;
import br.jus.cnj.replicacao_nacional.ObjectFactory;
import br.jus.cnj.replicacao_nacional.Processos;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;

/**
 * Lê todos os arquivos XML na pasta "output/Xg/xmls_individuais", tanto do PJe quanto dos
 * sistemas legados, e gera arquivos XML unificados na pasta "output/xmls_unificados", no formato
 * definido pelo CNJ: <SIGLA_TRIBUNAL>_<GRAU_JURISDICAO>_<DIAMESANO>-<SEQ>.
 * 
 * Será gerado um arquivo XML para cada 5000 processos (numero arbitrário, mas escolhido pois o
 * importador do CNJ processa lotes com essa quantidade), inserindo um sufixo no final do arquivo,
 * conforme orientação do CNJ no arquivo README.txt
 * 
 * @author fgiotto
 */
public class Op_3_UnificaArquivosXML {

	private static final Logger LOGGER = LogManager.getLogger(Op_3_UnificaArquivosXML.class);
	private static final int PROCESSOS_POR_LOTE = 5000;
	
	private int grau;
	private final File pastaSaida;
	private int numeroLoteAtual = 0;
	private String diaMesAno = new SimpleDateFormat("ddMMyyyy").format(new Date());
	
	public static void main(String[] args) throws JAXBException {
		
		// Verifica se deve gerar XML para 2o Grau
		if (Auxiliar.getParametroBooleanConfiguracao("gerar_xml_2G")) {
			unificarArquivosXML(2);
		}
		
		// Verifica se deve gerar XML para 1o Grau
		if (Auxiliar.getParametroBooleanConfiguracao("gerar_xml_1G")) {
			unificarArquivosXML(1);
		}
		
		LOGGER.info("Fim!");
	}

	private static void unificarArquivosXML(int grau) throws JAXBException {
		Op_3_UnificaArquivosXML baixaDados = new Op_3_UnificaArquivosXML(grau);
		baixaDados.unificarArquivosXML();
	}
	
	public Op_3_UnificaArquivosXML(int grau) {
		this.grau = grau;
		this.pastaSaida = Auxiliar.getPastaXMLsUnificados();
	}

	private void unificarArquivosXML() throws JAXBException {
		
		// Pesquisando todos os arquivos que serão unificados
		LOGGER.info("Pesquisando todos os arquivos que serão unificados no " + grau + "o Grau...");
		List<File> arquivosParaProcessar = new ArrayList<>();
		File pastaRaiz = Auxiliar.getPastaXMLsIndividuais(grau);
		localizaTodosArquivosXMLRecursivamente(pastaRaiz, arquivosParaProcessar);
		
		// Objetos responsáveis por ler e gravar os arquivos XML
		ObjectFactory factory = new ObjectFactory();
		JAXBContext jaxbContext = JAXBContext.newInstance(Processos.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		Processos todosProcessos = factory.createProcessos();
		
		// Itera sobre todos os XMLs que existem na pasta "output/xmls_individuais/(grau)"
		int i=0;
		int qtdLote = 0;
		for (File arquivoXML: arquivosParaProcessar) {
			
			i++;
			if (i % 1000 == 0) {
				LOGGER.debug("Processando arquivo " + arquivoXML + " (" + i + "/" + arquivosParaProcessar.size() + " - " + (i * 100 / arquivosParaProcessar.size()) + "%)");
			}
			
			// Objeto com os dados lidos deste arquivo XML
			Processos processosXML = (Processos) jaxbUnmarshaller.unmarshal(arquivoXML);
			
			// Adiciona os dados lidos do XML na lista "global"
			for (TipoProcessoJudicial processo: processosXML.getProcesso()) {
				todosProcessos.getProcesso().add(processo);
				
				// Se atingiu o tamanho do lote, grava em arquivo XML e recomeça a contar
				qtdLote++;
				if (qtdLote == PROCESSOS_POR_LOTE) {
					gravarProximoLoteXML(jaxbMarshaller, todosProcessos);
					todosProcessos.getProcesso().clear();
					qtdLote = 0;
				}
			}
		}
		
		// Grava em arquivo XML os processos remanescentes.
		if (!todosProcessos.getProcesso().isEmpty()) {
			gravarProximoLoteXML(jaxbMarshaller, todosProcessos);
		}
		
		LOGGER.info("Total de " + i + " processos gravados em " + numeroLoteAtual + " arquivos XML.");
	}
	
	
	/**
	 * Grava os dados de uma lista de processos em um arquivo XML
	 */
	private void gravarProximoLoteXML(Marshaller jaxbMarshaller, Processos listaProcessos) throws JAXBException {
		numeroLoteAtual++;
		File arquivoSaida = new File(pastaSaida, Auxiliar.getParametroConfiguracao("sigla_tribunal", true) + "_" + grau + "_" + diaMesAno + "-" + numeroLoteAtual + ".xml");
		LOGGER.info("Gerando arquivo " + arquivoSaida + "...");
		jaxbMarshaller.marshal(listaProcessos, arquivoSaida);
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
