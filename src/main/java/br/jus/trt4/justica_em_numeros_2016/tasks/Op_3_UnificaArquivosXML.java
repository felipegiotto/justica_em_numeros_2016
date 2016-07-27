package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.util.ArrayList;
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
 * sistemas legados, e gera arquivos XML unificados na pasta "output/Xg", no arquivo 
 * "xmls_unificados.xml".
 * 
 * @author fgiotto
 */
public class Op_3_UnificaArquivosXML {

	private static final Logger LOGGER = LogManager.getLogger(Op_3_UnificaArquivosXML.class);
	private int grau;
	private final File arquivoSaida;
	
	public static void main(String[] args) throws JAXBException {
		
		// Verifica se deve gerar XML para 1o Grau
		if (Auxiliar.getParametroBooleanConfiguracao("gerar_xml_1G")) {
			unificarArquivosXML(1);
		}
		
		// Verifica se deve gerar XML para 2o Grau
		if (Auxiliar.getParametroBooleanConfiguracao("gerar_xml_2G")) {
			unificarArquivosXML(2);
		}
		
		LOGGER.info("Fim!");
	}

	private static void unificarArquivosXML(int grau) throws JAXBException {
		Op_3_UnificaArquivosXML baixaDados = new Op_3_UnificaArquivosXML(grau);
		baixaDados.unificarArquivosXML();
	}
	
	public Op_3_UnificaArquivosXML(int grau) {
		this.grau = grau;
		this.arquivoSaida = new File("output/" + grau + "g/xmls_unificados.xml");
		this.arquivoSaida.getParentFile().mkdirs();
	}

	private void unificarArquivosXML() throws JAXBException {
		
		// Pesquisando todos os arquivos que serão unificados
		LOGGER.info("Pesquisando todos os arquivos que serão unificados...");
		List<File> arquivosParaProcessar = new ArrayList<>();
		File pastaRaiz = Auxiliar.getPastaXMLsIndividuais(grau);
		localizaTodosArquivosXMLRecursivamente(pastaRaiz, arquivosParaProcessar);
		
		// Objetos responsáveis por ler os arquivos XML
		ObjectFactory factory = new ObjectFactory();
		JAXBContext jaxbContext = JAXBContext.newInstance(Processos.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		Processos todosProcessos = factory.createProcessos();
		
		// Itera sobre todos os XMLs que existem na pasta "output/xmls_individuais/(grau)"
		int i=0;
		for (File arquivoXML: arquivosParaProcessar) {
			LOGGER.debug("Processando arquivo " + arquivoXML + " (" + (++i) + "/" + arquivosParaProcessar.size() + ")");
			
			// Objeto que conterá os dados de um único processo
			Processos processoIndividual = (Processos) jaxbUnmarshaller.unmarshal(arquivoXML);
			
			// Adiciona os dados lidos do XML na lista "global"
			for (TipoProcessoJudicial processo: processoIndividual.getProcesso()) {
				todosProcessos.getProcesso().add(processo);
			}
		}
		
		// Objetos auxiliares para gerar o XML a partir das classes Java
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		jaxbMarshaller.marshal(todosProcessos, arquivoSaida);
		LOGGER.info("Arquivo gerado: " + arquivoSaida);
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
