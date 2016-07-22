package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.cnj.intercomunicacao.beans.Intercomunicacao;
import br.jus.cnj.intercomunicacao.beans.ProcessoJudicial;
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
	
	public static void main(String[] args) throws JAXBException, IOException {
		
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

	private static void unificarArquivosXML(int grau) throws JAXBException, IOException {
		Op_3_UnificaArquivosXML baixaDados = new Op_3_UnificaArquivosXML(grau);
		baixaDados.unificarArquivosXML();
	}
	
	public Op_3_UnificaArquivosXML(int grau) {
		this.grau = grau;
		this.arquivoSaida = new File("output/" + grau + "g/xmls_unificados.xml");
		this.arquivoSaida.getParentFile().mkdirs();
	}

	private void unificarArquivosXML() throws JAXBException, IOException {
		
		// Pesquisando todos os arquivos que serão unificados
		LOGGER.info("Pesquisando todos os arquivos que serão unificados...");
		List<File> arquivosParaProcessar = new ArrayList<>();
		File pastaRaiz = new File("output/" + grau + "g/xmls_individuais");
		localizaTodosArquivosXMLRecursivamente(pastaRaiz, arquivosParaProcessar);
		
		// Objetos responsáveis por ler os arquivos XML
		JAXBContext jaxbContext = JAXBContext.newInstance(Intercomunicacao.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		
		// Objeto que conterá todos os dados de todos os processos
		Intercomunicacao todosProcessos = Auxiliar.getObjectFactory().createIntercomunicacao();
		
		// Itera sobre todos os XMLs que existem na pasta "output/xmls_individuais/(grau)"
		int i=0;
		for (File arquivoXML: arquivosParaProcessar) {
			LOGGER.debug("Processando arquivo " + arquivoXML + " (" + (++i) + "/" + arquivosParaProcessar.size() + ")");
			
			// Objeto que conterá os dados de um único processo
			
			try (FileInputStream fis = new FileInputStream(arquivoXML)) {
			
				Source source = new StreamSource(fis);
				JAXBElement<Intercomunicacao> root = jaxbUnmarshaller.unmarshal(source, Intercomunicacao.class);
				Intercomunicacao processoIndividual = root.getValue();			
			
				// Adiciona os dados lidos do XML na lista "global"
				for (ProcessoJudicial processo: processoIndividual.getProcessojudicial()) {
					todosProcessos.getProcessojudicial().add(processo);
				}
			}
		}
		
		// Objetos auxiliares para gerar o XML a partir das classes Java
		LOGGER.info("Gerando arquivo " + arquivoSaida + "...");
		Auxiliar.salvarObjetoEmArquivoXML(arquivoSaida, todosProcessos);
		LOGGER.info("Arquivo gerado!");
	}
	
	private void localizaTodosArquivosXMLRecursivamente(File file, List<File> lista) {
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
