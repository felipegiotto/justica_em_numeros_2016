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
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ProgressoInterfaceGrafica;

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
	private static ProgressoInterfaceGrafica progresso;
	
	private int grau;
	private final File pastaSaida;
	private int numeroLoteAtual = 1;
	private int qtdArquivosXMLGerados = 0;
	private List<File> arquivosParaProcessar;
	
	public static void main(String[] args) throws JAXBException, DadosInvalidosException {
		if (!Auxiliar.deveMontarLotesDeProcessos()) {
			return;
		}
		
		progresso = new ProgressoInterfaceGrafica("(3/5) Unificação de Arquivos XML");
		try {
			Auxiliar.prepararPastaDeSaida();
	
			Op_3_UnificaArquivosXML baixaDados1g = Auxiliar.deveProcessarPrimeiroGrau() ? new Op_3_UnificaArquivosXML(1) : null;
			Op_3_UnificaArquivosXML baixaDados2g = Auxiliar.deveProcessarSegundoGrau()  ? new Op_3_UnificaArquivosXML(2) : null;
			
			// Primeiro verifica se já não há arquivos unificados que foram enviados.
			// Se eles forem gerados novamente, podem ser perdidas estatísticas de envio!
			if (baixaDados1g != null) {
				baixaDados1g.verificarSeArquivosUnificadosJaForamEnviados();
			}
			if (baixaDados2g != null) {
				baixaDados2g.verificarSeArquivosUnificadosJaForamEnviados();
			}
			
			// Analisa a quantidade total de processos que deve ser unificada,
			// para mostrar barra de progresso
			int totalArquivos = 0;
			if (baixaDados1g != null) {
				totalArquivos += baixaDados1g.carregarListaArquivosParaProcessar().size();
			}
			if (baixaDados2g != null) {
				totalArquivos += baixaDados2g.carregarListaArquivosParaProcessar().size();
			}
			progresso.setMax(totalArquivos);
			
			// Exclui arquivos antigos (se existirem) e unifica os XMLs individuais
			if (baixaDados1g != null) {
				baixaDados1g.excluirArquivosUnificadosAntigos();
				baixaDados1g.unificarArquivosXML();
			}
			if (baixaDados2g != null) {
				baixaDados2g.excluirArquivosUnificadosAntigos();
				baixaDados2g.unificarArquivosXML();
			}
			
	        DadosInvalidosException.mostrarWarningSeHouveAlgumErro();
			LOGGER.info("Fim!");
		} finally {
			progresso.close();
			progresso = null;
		}
	}
	
	public Op_3_UnificaArquivosXML(int grau) {
		this.grau = grau;
		this.pastaSaida = Auxiliar.getPastaXMLsUnificados(grau);
	}
	
	/**
	 * Verifica se algum arquivo unificado já foi enviado ao CNJ. 
	 * 
	 * Quando o usuário executa esta tarefa (de geração de XMLs unificados), se houver
	 * arquivos unificados antigos, esses arquivos serão excluídos para a geração de novos.
	 * Mas, pode ser que algum desses XMLs já tenham sido enviados ao CNJ! Nesse caso, a 
	 * operação será abortada, para que o usuário intervenha manualmente!
	 * 
	 * @throws DadosInvalidosException
	 */
	private void verificarSeArquivosUnificadosJaForamEnviados() throws DadosInvalidosException {
		File[] arquivosUnificados = this.pastaSaida.listFiles();
		
		LOGGER.info("Verificando se arquivos unificados já não foram enviados...");
		for (File filho: arquivosUnificados) {
			if (new File(filho.getAbsolutePath() + Auxiliar.SUFIXO_ARQUIVO_ENVIADO).exists()) {
				throw new DadosInvalidosException("Lote já foi enviado e não será gerado automaticamente! Se necessário, exclua os lotes manualmente e gere os XMLs unificados outra vez.", filho.toString());
			}
		}
	}

	/**
	 * Exclui XMLs unificados que já existam na pasta, para que novos sejam gerados.
	 */
	private void excluirArquivosUnificadosAntigos() {
		
		File[] arquivosUnificados = this.pastaSaida.listFiles();
		LOGGER.info("Excluindo arquivos unificados anteriormente...");
		for (File filho: arquivosUnificados) {
			LOGGER.info("* Excluindo " + filho + "...");
			if (!FileUtils.deleteQuietly(filho)) {
				LOGGER.error("  * Não foi possível excluir o arquivo " + filho);
			}
		}
	}

	/**
	 * Carrega, em memória, a lista de todos os XMLs individuais que serão unificados
	 * 
	 * @return
	 */
	private List<File> carregarListaArquivosParaProcessar() {
		arquivosParaProcessar = new ArrayList<>();
		File pastaRaiz = Auxiliar.getPastaXMLsIndividuais(grau);
		localizaTodosArquivosXMLRecursivamente(pastaRaiz);
		return arquivosParaProcessar;
	}
	
	/**
	 * Lê cada um dos XMLs individuais e gera os lotes (XMLs unificados) com base nas 
	 * configurações de tamanho de lote.
	 * 
	 * @throws JAXBException
	 */
	private void unificarArquivosXML() throws JAXBException {
		
		LOGGER.info("Iniciando unificação de arquivos XML no " + grau + "o Grau...");
		
		// Pesquisando todos os arquivos que serão unificados
		LOGGER.info("Pesquisando todos os arquivos que serão unificados no " + grau + "o Grau...");
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
			
			progresso.incrementProgress();
		}
		
		// Grava em arquivo XML os processos remanescentes.
		if (!todosProcessos.getProcesso().isEmpty()) {
			gravarProximoLoteXML(jaxbMarshaller, todosProcessos);
		}
		
		LOGGER.info("Total de " + qtdTotal + " processos gravados em " + qtdArquivosXMLGerados + " arquivos XML.");
	}
	
	
	/**
	 * Grava os dados dos processos lidos até o momento em um XML unificado (lote).
	 */
	private void gravarProximoLoteXML(Marshaller jaxbMarshaller, Processos listaProcessos) throws JAXBException {
		File arquivoSaida = new File(pastaSaida, "Lote_" + StringUtils.leftPad(Integer.toString(numeroLoteAtual), 6, '0') + ".xml");
		LOGGER.info("Gerando arquivo " + arquivoSaida + "...");
		jaxbMarshaller.marshal(listaProcessos, arquivoSaida);
		numeroLoteAtual++;
		qtdArquivosXMLGerados++;
	}

	private void localizaTodosArquivosXMLRecursivamente(File file) {
		
		if (file.getName().startsWith("_")) {
			LOGGER.warn("Este arquivo não será processado pois seu nome começa com '_': " + file);
			return;
		}
		
		if (file.isDirectory()) {
			for (File filho: file.listFiles()) {
				localizaTodosArquivosXMLRecursivamente(filho);
			}
		} else {
			if (file.getName().toUpperCase().endsWith(".XML")) {
				arquivosParaProcessar.add(file);
			}
		}
	}
}
