package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Classe que representa um arquivo para envio ou validação no CNJ
 * 
 * @author felipe.giotto@trt4.jus.br
 */
public class ArquivoComInstancia {

	private static final Logger LOGGER = LogManager.getLogger(ArquivoComInstancia.class);
	private static final String SUFIXO_ARQUIVO_TENTOU_ENVIAR = ".tentativa_envio";
	private File arquivo;
	private int grau;
	
	public ArquivoComInstancia(File arquivo, int grau) {
		super();
		this.arquivo = arquivo;
		this.grau = grau;
	}
	
	public File getArquivo() {
		return arquivo;
	}
	
	/**
	 * Retorna um arquivo que pode ser utilizado como identificador de que o arquivo principal está sendo enviado.
	 * @return
	 */
	public File getArquivoControleTentativaEnvio() {
		return new File(arquivo.getAbsolutePath() + SUFIXO_ARQUIVO_TENTOU_ENVIAR);
	}
	
	public int getGrau() {
		return grau;
	}
	
	public static List<ArquivoComInstancia> localizarArquivosInstanciasHabilitadas(String sufixo) {
		
		sufixo = sufixo.toUpperCase();
		
		List<ArquivoComInstancia> arquivos = new ArrayList<>();
		if (Auxiliar.deveProcessarSegundoGrau()) {
			localizarArquivos(2, sufixo, arquivos);
		}
		if (Auxiliar.deveProcessarPrimeiroGrau()) {
			localizarArquivos(1, sufixo, arquivos);
		}
		
		// Coloca os arquivos que já tentou-se enviar (e, provavelmente, deu erro) no final da lista
		Collections.sort(arquivos, new Comparator<ArquivoComInstancia>() {
			
			@Override
			public int compare(ArquivoComInstancia o1, ArquivoComInstancia o2) {
				
				String o1Path = o1.getArquivo().getAbsolutePath();
				String o2Path = o2.getArquivo().getAbsolutePath();
				boolean o1TentouEnviar = new File(o1Path + SUFIXO_ARQUIVO_TENTOU_ENVIAR).exists();
				boolean o2TentouEnviar = new File(o2Path + SUFIXO_ARQUIVO_TENTOU_ENVIAR).exists();
				if (o1TentouEnviar && !o2TentouEnviar) {
					return 1;
				} else if (!o1TentouEnviar && o2TentouEnviar) {
					return -1;
				} else {
					return o1Path.compareTo(o2Path);
				}
			}
		});
		
		
		return arquivos;
	}
	
	/**
	 * Carrega os arquivos (individuais ou unificados) da instância selecionada (1G ou 2G)
	 * 
	 * @param grau
	 * @throws DadosInvalidosException 
	 */
	private static void localizarArquivos(int grau, String sufixo, List<ArquivoComInstancia> arquivosParaEnviar) {
		
		// Lê arquivos da lista de XMLs individuais
		File pastaXMLsParaEnvio = Auxiliar.getPastaXMLsIndividuais(grau);
		localizarArquivosRecursivamente(pastaXMLsParaEnvio, grau, sufixo, arquivosParaEnviar);
	}

	private static void localizarArquivosRecursivamente(File pasta, int grau, String sufixo, List<ArquivoComInstancia> arquivosParaEnviar) {
		
		LOGGER.trace("Localizando arquivos na pasta '" + pasta.getAbsolutePath() + "'...");
		if (!pasta.isDirectory()) {
			return;
		}
		
		// Filtro para localizar arquivos XML a serem enviados, bem como pastas para fazer busca recursiva
		FileFilter aceitarPastasOuXMLs = new FileFilter() {
			
			@Override
			public boolean accept(File file) {
				return file.isDirectory() || file.getName().toUpperCase().endsWith(sufixo);
			}
		};
		
		// Localiza todos os arquivos XML da pasta
		File[] arquivos = pasta.listFiles(aceitarPastasOuXMLs);
		for (File filho: arquivos) {
			
			if (filho.isDirectory()) {
				localizarArquivosRecursivamente(filho, grau, sufixo, arquivosParaEnviar);
			} else {
				arquivosParaEnviar.add(new ArquivoComInstancia(filho, grau));
			}
		}
	}

	public static void mostrarTotalDeArquivosPorPasta(List<ArquivoComInstancia> arquivosParaEnviar, String msg) {
		
		LOGGER.info(msg);
		
		// Calcula quantos arquivos existem por pasta
		Map<File, AtomicInteger> qtdPorPasta = new TreeMap<>();
		for (ArquivoComInstancia xml: arquivosParaEnviar) {
			File pasta = xml.getArquivo().getParentFile();
			if (qtdPorPasta.containsKey(pasta)) {
				qtdPorPasta.get(pasta).incrementAndGet();
			} else {
				qtdPorPasta.put(pasta, new AtomicInteger(1));
			}
		}
		
		// Mostra os totais por pasta
		int total = 0;
		for (File pasta: qtdPorPasta.keySet()) {
			int totalPasta = qtdPorPasta.get(pasta).get();
			LOGGER.info("* Pasta '" + pasta + "': " + totalPasta);
			total += totalPasta;
		}
		LOGGER.info("* TOTAL: " + total);
	}

	@Override
	public String toString() {
		return arquivo.toString();
	}
}
