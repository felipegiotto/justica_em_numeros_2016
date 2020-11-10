package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.AcumuladorExceptions;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ArquivoComInstancia;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DataJudException;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.HttpUtil;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.MetaInformacaoEnvio;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.ProgressoInterfaceGrafica;
import br.jus.trt4.justica_em_numeros_2016.enums.Parametro;
import br.jus.trt4.justica_em_numeros_2016.enums.SituacaoProtocoloCNJ;

/**
 * Chama os webservices do CNJ, validando cada um dos protocolos recebidos
 * previamente do CNJ na fase 4: {@link Op_4_ValidaEnviaArquivosCNJ}.
 * 
 * TODO: Implementar também o reenvio automático, se for erro interno no CNJ,
 * dos processos dos protocolos com erro. Como sugestão criar um arquivo do tipo
 * .protocolo.erro que terá todos os protocolos com status de erro de um
 * processo.
 *
 * @author felipe.giotto@trt4.jus.br
 */
public class Op_5_ConfereProtocolosCNJ {

	private static final Logger LOGGER = LogManager.getLogger(Op_5_ConfereProtocolosCNJ.class);
	private CloseableHttpClient httpClient;
	private static ProgressoInterfaceGrafica progresso;

	// Codigo de status de processo com status "Processado com Erro".
	private static final String STATUS_CNJ_PROCESSADO_COM_ERRO = "6";
	// Codigo de status de processo com status "Erro no Arquivo"
	private static final String STATUS_CNJ_ERRO_ARQUIVO = "7";

	private static final String NOME_PARAMETRO_PROTOCOLO = "protocolo";
	private static final String NOME_PARAMETRO_DATA_INICIO = "dataInicio";
	private static final String NOME_PARAMETRO_DATA_FIM = "dataFim";
	private static final String NOME_PARAMETRO_STATUS = "status";
	private static final String NOME_PARAMETRO_PAGINA = "page";

	// Padrão de como a última página consultada no CNJ de um status é armazenada no
	// arquivo.
	// Composto pelo número do status (que é preenchido dinamicamente), espaço e o
	// número da página (dígito com 1 a 10 números).
	private static final String PADRAO_ER_ARQUIVO_ULTIMA_PAGINA_CONSULTADA = "^(%s )(\\d{1,10})$";

	public static void main(String[] args) throws Exception {

		String resposta;
		
		if(args != null && args.length > 0) {
			resposta = args[0];
		}
		else {
			System.out.println(
					"Se algum arquivo ainda não foi processado no CNJ, ou se ocorrer algum erro na resposta do CNJ, você quer que a operação seja reiniciada?");
			System.out.println(
					"Responda 'S' para que as validações no CNJ rodem diversas vezes, até que o webservice não recuse nenhum arquivo e até que todos os XMLs sejam processados.");
			System.out.println("Responda 'N' para que o envio ao CNJ rode somente uma vez.");
			resposta = Auxiliar.readStdin().toUpperCase();
		}		

		executarOperacaoConfereProtocolosCNJ("S".equals(resposta));
	}

	public static void executarOperacaoConfereProtocolosCNJ(boolean continuarEmCasoDeErro) throws Exception {

		Auxiliar.prepararPastaDeSaida();

		if (continuarEmCasoDeErro) {
			LOGGER.info("Se ocorrer algum erro no envio, a operação será reiniciada quantas vezes for necessário!");
		}

		progresso = new ProgressoInterfaceGrafica("(5/6) Conferência dos protocolos no CNJ");
		try {
			boolean executar = true;
			do {
				progresso.setProgress(0);

				Op_5_ConfereProtocolosCNJ operacao = new Op_5_ConfereProtocolosCNJ();

				String tipo_validacao_protocolo_cnj = Auxiliar
						.getParametroConfiguracao(Parametro.tipo_validacao_protocolo_cnj, false);

				if (tipo_validacao_protocolo_cnj == null
						|| Auxiliar.VALIDACAO_CNJ_TODOS_COM_ERRO.equals(tipo_validacao_protocolo_cnj)
						|| Auxiliar.VALIDACAO_CNJ_APENAS_COM_ERRO_PROCESSADO_COM_ERRO
								.equals(tipo_validacao_protocolo_cnj)
						|| Auxiliar.VALIDACAO_CNJ_APENAS_COM_ERRO_NO_ARQUIVO.equals(tipo_validacao_protocolo_cnj)
						|| Auxiliar.VALIDACAO_CNJ_TODOS.equals(tipo_validacao_protocolo_cnj)) {
					List<ArquivoComInstancia> arquivosProtocolos = ArquivoComInstancia
							.localizarArquivosInstanciasHabilitadas(Auxiliar.SUFIXO_PROTOCOLO, false);

					ArquivoComInstancia.mostrarTotalDeArquivosPorPasta(arquivosProtocolos,
							"Total de arquivos protocolos encontrados");
					int totalArquivos = arquivosProtocolos.size();

					LOGGER.info("Filtrando os protocolos que ainda não foram processados no CNJ.");

					// Filtra somente os arquivos XML que possuem protocolos e que ainda NÃO foram
					// processados no CNJ
					// TODO Avaliar se não haveria outra forma mais eficiente de executar a lógica
					// do método abaixo, como essa implementação está muito demorada
					List<ArquivoComInstancia> arquivosParaConsultar = filtrarSomenteArquivosPendentesDeConsulta(arquivosProtocolos);

					// Mostra os arquivos que serão consultados
					ArquivoComInstancia.mostrarTotalDeArquivosPorPasta(arquivosParaConsultar,
							"Protocolos que ainda precisam ser conferidos");
					
					operacao.consultarProtocolosCNJ(tipo_validacao_protocolo_cnj, arquivosParaConsultar);
				} else {
					throw new RuntimeException("Valor desconhecido para o parâmetro 'tipo_validacao_protocolo_cnj': "
							+ tipo_validacao_protocolo_cnj);
				}

				// TODO Fazer reenvio dos protocolos com erro

				AcumuladorExceptions.instance().mostrarExceptionsAcumuladas();

				// Verifica se deve executar novamente em caso de erros
				if (continuarEmCasoDeErro) {
					if (AcumuladorExceptions.instance().isExisteExceptionRegistrada()) {
						progresso.setInformacoes("Aguardando para reiniciar...");
						LOGGER.warn(
								"A operação foi concluída com erros! O envio será reiniciado em 10min... Se desejar, aborte este script.");
						Thread.sleep(10 * 60_000);
						progresso.setInformacoes("");
					} else {
						executar = false;
					}
				} else {
					executar = false;
				}
			} while (executar);
		} finally {
			progresso.setInformacoes("");
			progresso.close();
			progresso = null;
		}

		// Apaga o arquivo com a última página consultada no CNJ. Como a operação foi
		// finalizada com sucesso,
		// não tem sentido mantê-lo, pois ele terá consultado todas as páginas.
		File arquivo = Auxiliar.getArquivoUltimaPaginaConsultaCNJ();

		if (arquivo.exists()) {
			arquivo.delete();
		}

		LOGGER.info("Fim!");
	}

	/**
	 * Prepara o componente HttpClient para conectar aos serviços REST do CNJ
	 * 
	 * @throws Exception
	 */
	public Op_5_ConfereProtocolosCNJ() {
		httpClient = HttpUtil.criarNovoHTTPClientComAutenticacaoCNJ();
	}

	/**
	 * Carrega os arquivos XML das instâncias selecionadas (1G e/ou 2G) e envia ao
	 * CNJ.
	 * 
	 */
	public void localizarProtocolosConsultarNoCNJ() {

		// Lista com todos os arquivos pendentes
		Auxiliar.prepararPastaDeSaida();

		LOGGER.info("Carregando os arquivos de protocolos.");

		// TODO Avaliar a necessidade de ordenação dessa lista, uma vez que será
		// necessário iterar por toda ela para filtrar os
		// protocolos que foram processados com sucesso e não necessitam de reenvio
		List<ArquivoComInstancia> arquivosProtocolos = ArquivoComInstancia
				.localizarArquivosInstanciasHabilitadas(Auxiliar.SUFIXO_PROTOCOLO, false);

		ArquivoComInstancia.mostrarTotalDeArquivosPorPasta(arquivosProtocolos,
				"Total de arquivos protocolos encontrados");
		int totalArquivos = arquivosProtocolos.size();

		LOGGER.info("Filtrando os protocolos que ainda não foram processados no CNJ.");

		// Filtra somente os arquivos XML que possuem protocolos e que ainda NÃO foram
		// processados no CNJ
		// TODO Avaliar se não haveria outra forma mais eficiente de executar a lógica
		// do método abaixo, como essa implementação está muito demorada
		List<ArquivoComInstancia> arquivosParaConsultar = filtrarSomenteArquivosPendentesDeConsulta(arquivosProtocolos);

		// Mostra os arquivos que serão consultados
		ArquivoComInstancia.mostrarTotalDeArquivosPorPasta(arquivosParaConsultar,
				"Protocolos que ainda precisam ser conferidos");

		// Atualiza o progresso na interface
		if (progresso != null) {
			progresso.setMax(totalArquivos);
			progresso.setProgress(totalArquivos - arquivosParaConsultar.size());
		}

		// Inicia o pesquisa
		consultarProtocolosCNJ(null, arquivosParaConsultar);

		// Envio finalizado
		List<ArquivoComInstancia> arquivosXMLPendentes = filtrarSomenteArquivosPendentesDeConsulta(
				arquivosParaConsultar);
		ArquivoComInstancia.mostrarTotalDeArquivosPorPasta(arquivosXMLPendentes,
				"Arquivos XML ainda pendentes de envio");
	}

	public static List<ArquivoComInstancia> filtrarSomenteArquivosPendentesDeConsulta(
			List<ArquivoComInstancia> arquivosProtocolos) {
		List<ArquivoComInstancia> arquivosPendentes = new ArrayList<>();
		for (ArquivoComInstancia arquivo : arquivosProtocolos) {
			if (deveConsultarArquivo(arquivo.getArquivo())) {
				arquivosPendentes.add(arquivo);
			}
		}
		return arquivosPendentes;
	}

	/**
	 * Consulta os protocolos no CNJ de acordo com o tipo de validação preenchido no
	 * arquivo de configuração: TODOS, TODOS_COM_ERRO (status 6 e 7),
	 * APENAS_COM_ERRO_PROCESSADO_COM_ERRO (status 6) e APENAS_COM_ERRO_NO_ARQUIVO
	 * (status 7). Salva a lista de processos em arquivo e também a página atual da
	 * consulta para permitir o reinício a partir dessa página.
	 * 
	 * TODO: Reenviar automaticamente os protocolos com erro.
	 * 
	 */
	private void consultarProtocolosCNJ(String tipo_validacao_protocolo_cnj, List<ArquivoComInstancia> arquivosProtocolos) {

		String parametroProcolo = "";
		String parametroDataInicio = "";
		String parametroDataFim = "";

		int totalProcessosAnalisados = 0;

		if (tipo_validacao_protocolo_cnj != null) {
			LOGGER.info(String.format("Tipo de validação escolhido: %s.", tipo_validacao_protocolo_cnj));
		} else {
			// Validação padrão quando nenhum valor é preenchido
			LOGGER.info("Tipo de validação escolhido: VALIDACAO_CNJ_TODOS.");
		}

		if (arquivosProtocolos.size() == 0) {
			LOGGER.error(String.format("Todos os protocolos já foram analisados ou "
					+ "não foi encontrado nenhum arquivo .PROTOCOLO nas pastas %s e/ou %s.",
					Auxiliar.getPastaXMLsIndividuais(1).getAbsolutePath(),
					Auxiliar.getPastaXMLsIndividuais(2).getAbsolutePath()));
			return;
		}

		LOGGER.info("Carregando a data de início e data de fim da remessa.");

		List<String> parametrosData = carregarParametrosData(arquivosProtocolos);

		parametroDataInicio = parametrosData.get(0);
		parametroDataFim = parametrosData.get(1);

		LOGGER.info(String.format("Data de início: %s. Data fim: %s.", parametroDataInicio, parametroDataFim));

		LOGGER.info("Carregando os protocolos dos arquivos.");

		// Carregando a tupla protocolo/arquivo do protocolo num Map pois a consulta
		// média é bem mais rápida do que em uma lista.
		Map<String, File> mapProtocolosComArquivos = carregarListaFilesPorProtocolo(arquivosProtocolos);

		Set<MetaInformacaoEnvio> setMetaInformacoesProcessosG1 = new HashSet<MetaInformacaoEnvio>();
		Set<MetaInformacaoEnvio> setMetaInformacoesProcessosG2 = new HashSet<MetaInformacaoEnvio>();

		URIBuilder builder;

		if (tipo_validacao_protocolo_cnj == null || Auxiliar.VALIDACAO_CNJ_TODOS.equals(tipo_validacao_protocolo_cnj)) {
			// Constrói a URL com o status "Processado com erro"
			builder = construirBuilderWebServiceCNJ(parametroProcolo, parametroDataInicio, parametroDataFim, "");

			executarConsultasProtocolosCNJ(builder, mapProtocolosComArquivos, setMetaInformacoesProcessosG1,
					setMetaInformacoesProcessosG2, Auxiliar.VALIDACAO_CNJ_TODOS);
		} else {
			if (Auxiliar.VALIDACAO_CNJ_TODOS_COM_ERRO.equals(tipo_validacao_protocolo_cnj)
					|| Auxiliar.VALIDACAO_CNJ_APENAS_COM_ERRO_PROCESSADO_COM_ERRO
							.equals(tipo_validacao_protocolo_cnj)) {
				// Constrói a URL com o status "Processado com erro"
				builder = construirBuilderWebServiceCNJ(parametroProcolo, parametroDataInicio, parametroDataFim,
						STATUS_CNJ_PROCESSADO_COM_ERRO);

				executarConsultasProtocolosCNJ(builder, mapProtocolosComArquivos, setMetaInformacoesProcessosG1,
						setMetaInformacoesProcessosG2, Auxiliar.VALIDACAO_CNJ_APENAS_COM_ERRO_PROCESSADO_COM_ERRO);
			}

			if (Auxiliar.VALIDACAO_CNJ_TODOS_COM_ERRO.equals(tipo_validacao_protocolo_cnj)
					|| Auxiliar.VALIDACAO_CNJ_APENAS_COM_ERRO_NO_ARQUIVO.equals(tipo_validacao_protocolo_cnj)) {
				// Constrói a URL com o status "Erro no arquivo"
				builder = construirBuilderWebServiceCNJ(parametroProcolo, parametroDataInicio, parametroDataFim,
						STATUS_CNJ_ERRO_ARQUIVO);

				executarConsultasProtocolosCNJ(builder, mapProtocolosComArquivos, setMetaInformacoesProcessosG1,
						setMetaInformacoesProcessosG2, Auxiliar.VALIDACAO_CNJ_APENAS_COM_ERRO_NO_ARQUIVO);
			}
		}

		if (setMetaInformacoesProcessosG1.size() > 0 || setMetaInformacoesProcessosG2.size() > 0) {
			if (Auxiliar.deveProcessarPrimeiroGrau()) {

				totalProcessosAnalisados += setMetaInformacoesProcessosG1.size();

				if (Auxiliar.deveProcessarSegundoGrau()) {
					totalProcessosAnalisados += setMetaInformacoesProcessosG2.size();
					LOGGER.warn("Um total de " + totalProcessosAnalisados
							+ " processos foram ANALISADOS no CNJ. A lista completa foi gravada " + "nos arquivos "
							+ Auxiliar.getArquivoListaProcessosProtocolo(1).getAbsoluteFile() + " e "
							+ Auxiliar.getArquivoListaProcessosProtocolo(2).getAbsoluteFile() + ".");
				} else {
					LOGGER.warn("Um total de " + totalProcessosAnalisados
							+ " processos foram ANALISADOS no CNJ. A lista completa foi gravada " + "no arquivo "
							+ Auxiliar.getArquivoListaProcessosProtocolo(1).getAbsoluteFile() + ".");
				}
			} else if (Auxiliar.deveProcessarSegundoGrau()) {
				totalProcessosAnalisados += setMetaInformacoesProcessosG2.size();
				LOGGER.warn("Um total de " + totalProcessosAnalisados
						+ " processos foram ANALISADOS no CNJ. A lista completa foi gravada " + "no arquivo "
						+ Auxiliar.getArquivoListaProcessosProtocolo(2).getAbsoluteFile() + ".");
			}
		} else {
			LOGGER.warn("Nenhum protocolo foi encontrado para o tipo de validação informada.");
		}
	}

	/**
	 * Itera sobre todos os arquivos carregados para buscar a menor e maior data dos
	 * arquivos de protocolos.
	 * 
	 * @param arquivosProtocolos Arquivos de protocolos
	 * @return Lista com duas String: a primeira com a menor data e a segunda com a
	 *         maior data
	 */
	private List<String> carregarParametrosData(List<ArquivoComInstancia> arquivosProtocolos) {

		List<String> datas = new ArrayList<>();

		Date dataInicio;
		Date dataFim;

		BasicFileAttributes atributos;

		try {
			atributos = Files.readAttributes(arquivosProtocolos.get(0).getArquivo().toPath(),
					BasicFileAttributes.class);
			dataInicio = new Date(atributos.lastModifiedTime().toMillis());
			dataFim = dataInicio;
		} catch (IOException e1) {
			LOGGER.warn(
					"Não foi possível ler os atributos do arquivo " + arquivosProtocolos.get(0).getArquivo().toPath());
			dataInicio = new GregorianCalendar(2100, Calendar.DECEMBER, 31).getTime();
			dataFim = new GregorianCalendar(1900, Calendar.JANUARY, 01).getTime();
		}

		for (ArquivoComInstancia arquivoComInstancia : arquivosProtocolos) {
			try {
				atributos = Files.readAttributes(arquivoComInstancia.getArquivo().toPath(), BasicFileAttributes.class);
				Date dataModificacaoArquivo = new Date(atributos.lastModifiedTime().toMillis());

				if (dataInicio.after(dataModificacaoArquivo)) {
					dataInicio = dataModificacaoArquivo;
				} else if (dataFim.before(dataModificacaoArquivo)) {
					dataFim = dataModificacaoArquivo;
				}

			} catch (IOException e) {
				LOGGER.warn(
						"Não foi possível ler os atributos do arquivo " + arquivoComInstancia.getArquivo().toPath());
			}
		}

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

		
		Calendar calendar = Calendar.getInstance();
        calendar.setTime(dataInicio);
        calendar.add(Calendar.DATE, - 1);
        dataInicio = calendar.getTime();
        
        calendar = Calendar.getInstance();
        calendar.setTime(dataFim);
        calendar.add(Calendar.DATE, + 1);
        dataFim = calendar.getTime();
		
		datas.add(formatter.format(dataInicio));
		datas.add(formatter.format(dataFim));

		return datas;
	}

	/**
	 * Realiza a consulta ao webservice do CNJ e gera um arquivo com a lista de
	 * processos e suas meta-informações para cada instância.
	 * 
	 * @param builder                       URIBuilder com a URI a ser consultada
	 * @param mapProtocolosComArquivos      Map com a tupla Protocolo/Arquivo do
	 *                                      Protocolo para conseguir o número do
	 *                                      processo a partir do protocolo retornado
	 *                                      pelo serviço do CNJ.
	 * @param setMetaInformacoesProcessosG1 Lista com as meta informações,
	 *                                      retornadas pelo CNJ, dos processos
	 *                                      analisados do 1° Grau
	 * @param setMetaInformacoesProcessosG2 Lista com as meta informações,
	 *                                      retornadas pelo CNJ, dos processos
	 *                                      analisados do 2° Grau
	 */
	private void executarConsultasProtocolosCNJ(URIBuilder builder, Map<String, File> mapProtocolosComArquivos,
			Set<MetaInformacaoEnvio> setMetaInformacoesProcessosG1,
			Set<MetaInformacaoEnvio> setMetaInformacoesProcessosG2, String tipoValidacaoProtocolo) {

		String agrupadorErrosConsultaProtocolosCNJ = "Consulta de protocolos no CNJ";

		int parametroPaginaAtual = buscarUltimaPaginaConsultadaPorTipoDeValidacao(tipoValidacaoProtocolo) + 1;

		File arquivoProcessosProtocolosG1 = Auxiliar.getArquivoListaProcessosProtocolo(1);
		File arquivoProcessosProtocolosG2 = Auxiliar.getArquivoListaProcessosProtocolo(2);

		if (!Auxiliar.getArquivoUltimaPaginaConsultaCNJ().exists()) {
			// Consulta está começando pela primeira vez, então se existirem os arquivos com
			// a lista de processos,
			// eles serão removidos, pois serão lixo. Não faz a comparação com o número da
			// página, pois a consulta de protocolos
			// pode ter sido executada com outro tipo de validação. Nesse caso, o arquivo
			// com o resultado do processamento
			// pode estar preenchido com informações analisadas em outro tipo de validação e
			// esse arquivo deve ser apagado nesse caso.

			if (Auxiliar.deveProcessarPrimeiroGrau() && arquivoProcessosProtocolosG1.exists()) {
				arquivoProcessosProtocolosG1.delete();
			} else if (Auxiliar.deveProcessarSegundoGrau() && arquivoProcessosProtocolosG2.exists()) {
				arquivoProcessosProtocolosG2.delete();
			}

		}

		if (parametroPaginaAtual > 0) {
			LOGGER.info("Reiniciando o processo da página " + parametroPaginaAtual + "...");
		}

		// TODO Avaliar se vale à pena verificar e alertar se ainda existe algum
		// protocolo com o status em processamento

		// Como o serviço do CNJ não está implementado corretamente, decidiu-se por
		// verificar se a consulta a uma página
		// retornou algum resultado para parar de consultar em vez de pegar o total de
		// registros e dividir pelo tamanho da página.
		// Sempre será efetuada uma consulta a mais, porém esse método fica mais livre
		// de alterações e falhas como a alteração do tamanho
		// da página ou a correção do serviço.
		boolean consultaCNJRetornouResultado = false;

		do {
			URI uri = atualizarParametroURIBuilder(builder, NOME_PARAMETRO_PAGINA,
					Integer.toString(parametroPaginaAtual));

			LOGGER.info(String.format(
					"Consultando a página %s com tipo de validação %s da lista de protocolos no CNJ. URL: %s.",
					parametroPaginaAtual, tipoValidacaoProtocolo, uri.toString()));

			String body = "";
			try {
				body = executarRequisicaoWebServiceCNJ(uri);
			} catch (ParseException | IOException ex) {
				AcumuladorExceptions.instance().adicionarException(agrupadorErrosConsultaProtocolosCNJ,
						ex.getLocalizedMessage(), ex, true);

				return;
			}

			LOGGER.info("* Analisando a resposta JSON...");

			List<List<MetaInformacaoEnvio>> protocolosRespostaJson = carregaProtocolosRespostaJson(body);

			List<MetaInformacaoEnvio> listaMetaInformacoesAnalisadasG1 = protocolosRespostaJson.get(0);
			List<MetaInformacaoEnvio> listaMetaInformacoesAnalisadasG2 = protocolosRespostaJson.get(1);

			// Verifica se a consulta ao CNJ retornou algum resultado.
			consultaCNJRetornouResultado = !listaMetaInformacoesAnalisadasG1.isEmpty()
					|| !listaMetaInformacoesAnalisadasG2.isEmpty();

			List<Set<MetaInformacaoEnvio>> listaProcessosRespostaJson = carregaListaProcessos(mapProtocolosComArquivos,
					listaMetaInformacoesAnalisadasG1, listaMetaInformacoesAnalisadasG2);

			Set<MetaInformacaoEnvio> setProcessosAnalisadosRespostaG1 = listaProcessosRespostaJson.get(0);
			Set<MetaInformacaoEnvio> setProcessosAnalisadosRespostaG2 = listaProcessosRespostaJson.get(1);

			// Remove os duplicados. É usado o iterator no lugar do foreach pois a exceção
			// ConcurrentModificationException
			// será lançada ao tentar iterar sobre a lista após apagar um de seus elementos.
			for (Iterator<MetaInformacaoEnvio> i = setProcessosAnalisadosRespostaG1.iterator(); i.hasNext();) {
				MetaInformacaoEnvio metaInformacao = i.next();
				if (setMetaInformacoesProcessosG1.contains(metaInformacao)) {
					i.remove();
				}
			}

			for (Iterator<MetaInformacaoEnvio> i = setProcessosAnalisadosRespostaG2.iterator(); i.hasNext();) {
				MetaInformacaoEnvio metaInformacao = i.next();
				if (setMetaInformacoesProcessosG2.contains(metaInformacao)) {
					i.remove();
				}
			}

			// Salva o número de processos nos arquivos
			if (Auxiliar.deveProcessarPrimeiroGrau()) {
				try {
					gravarListaProcessosEmArquivoEMarcarComoProcessado(mapProtocolosComArquivos,
							setProcessosAnalisadosRespostaG1, Auxiliar.getArquivoListaProcessosProtocolo(1), true);

				} catch (IOException e) {
					LOGGER.warn("Não foi possível escrever a lista de processos analisados no arquivo "
							+ Auxiliar.getArquivoListaProcessosProtocolo(1).getPath());
				}
			}

			if (Auxiliar.deveProcessarSegundoGrau()) {
				try {
					gravarListaProcessosEmArquivoEMarcarComoProcessado(mapProtocolosComArquivos,
							setProcessosAnalisadosRespostaG2, Auxiliar.getArquivoListaProcessosProtocolo(2), true);
				} catch (IOException e) {
					LOGGER.warn("Não foi possível escrever a lista de processos analisados no arquivo "
							+ Auxiliar.getArquivoListaProcessosProtocolo(2).getPath());
				}
			}

			// Adiciona os processos da resposta atual ao conjunto total de processos
			setMetaInformacoesProcessosG1.addAll(setProcessosAnalisadosRespostaG1);
			setMetaInformacoesProcessosG2.addAll(setProcessosAnalisadosRespostaG2);

			gravarUltimaPaginaConsultada(tipoValidacaoProtocolo, Integer.toString(parametroPaginaAtual));

			parametroPaginaAtual++;

		} while (consultaCNJRetornouResultado);
	}

	/**
	 * Retorna o valor de um parâmetro do URIBuilder
	 * 
	 * @param builder       URIBuilder a ser consultado
	 * @param nomeParametro nome do parâmetro do URIBuilder
	 * @return Valor do parâmetro
	 */
	private String consultarValorParametroBuilder(URIBuilder builder, String nomeParametro) {

		List<NameValuePair> params = builder.getQueryParams();

		for (NameValuePair nameValuePair : params) {
			if (nameValuePair.getName().equals(nomeParametro)) {
				return nameValuePair.getValue();
			}
		}

		throw new RuntimeException(
				"Não foi possível encontrar o parâmetro " + nomeParametro + " no URIBuilder " + builder.getPath());
	}

	/**
	 * Carrega a lista de meta-informações retornada no objeto JSON.
	 * 
	 * @param body Corpo da resposta da requisição feita ao serviço do CNJ
	 * 
	 * @return Lista com 2 objetos: 1) Lista de protocolos analisados no 1° Grau; 2)
	 *         Lista de protocolos analisados no 2° Grau;
	 */
	private List<List<MetaInformacaoEnvio>> carregaProtocolosRespostaJson(String body) {

		List<List<MetaInformacaoEnvio>> resposta = new ArrayList<List<MetaInformacaoEnvio>>();

		List<MetaInformacaoEnvio> listaProtocolosAnalisadosG1 = new ArrayList<MetaInformacaoEnvio>();
		List<MetaInformacaoEnvio> listaProtocolosAnalisadosG2 = new ArrayList<MetaInformacaoEnvio>();
		try {
			JsonObject rootObject = JsonParser.parseString(body).getAsJsonObject();
	
			JsonArray processos = rootObject.get("resultado").getAsJsonArray();
	
			for (JsonElement processoElement : processos) {
	
				JsonObject processoObject = processoElement.getAsJsonObject();
				String codHash = !processoObject.get("codHash").toString().equals("null")
						? processoObject.get("codHash").getAsString()
						: "";
				String datDataEnvioProtocolo = !processoObject.get("datDataEnvioProtocolo").toString().equals("null")
						? processoObject.get("datDataEnvioProtocolo").getAsString()
						: "";
				String flgExcluido = !processoObject.get("flgExcluido").toString().equals("null")
						? processoObject.get("flgExcluido").getAsString()
						: "";
				String grau = !processoObject.get("grau").toString().equals("null")
						? processoObject.get("grau").getAsString()
						: "";
				String numProtocolo = !processoObject.get("numProtocolo").toString().equals("null")
						? processoObject.get("numProtocolo").getAsString()
						: "";
				String seqProtocolo = !processoObject.get("seqProtocolo").toString().equals("null")
						? processoObject.get("seqProtocolo").getAsString()
						: "";
				String siglaOrgao = !processoObject.get("siglaOrgao").toString().equals("null")
						? processoObject.get("siglaOrgao").getAsString()
						: "";
				String tamanhoArquivo = !processoObject.get("tamanhoArquivo").toString().equals("null")
						? processoObject.get("tamanhoArquivo").getAsString()
						: "";
				String tipStatusProtocolo = !processoObject.get("tipStatusProtocolo").toString().equals("null")
						? processoObject.get("tipStatusProtocolo").getAsString()
						: "";
				String urlArquivo = !processoObject.get("urlArquivo").toString().equals("null")
						? processoObject.get("urlArquivo").getAsString()
						: "";
	
				if (!numProtocolo.equals("") && !grau.equals("")) {
					if ("G1".equals(grau)) {
						listaProtocolosAnalisadosG1.add(
								new MetaInformacaoEnvio(codHash, datDataEnvioProtocolo, flgExcluido, grau, numProtocolo,
										seqProtocolo, siglaOrgao, tamanhoArquivo, tipStatusProtocolo, urlArquivo));
					} else if ("G2".equals(grau)) {
						listaProtocolosAnalisadosG2.add(
								new MetaInformacaoEnvio(codHash, datDataEnvioProtocolo, flgExcluido, grau, numProtocolo,
										seqProtocolo, siglaOrgao, tamanhoArquivo, tipStatusProtocolo, urlArquivo));
					}
				} else {
					LOGGER.warn(String.format("Erro ao carregar o protocolo %s do Grau %s.", numProtocolo, grau));
				}
			}
		} catch (RuntimeException ex) {
			LOGGER.warn("Não foi possível ler o número do protocolo JSON do CNJ: " + body);
		}
		resposta.add(listaProtocolosAnalisadosG1);
		resposta.add(listaProtocolosAnalisadosG2);

		return resposta;
	}

	/**
	 * Carrega a lista de processos a partir dos protocolos.
	 * 
	 * @param mapProtocolosComProcessos        Tupla Protocolo/Processo
	 * @param listaMetaInformacoesAnalisadasG1 Lista de meta-informações analisadas
	 *                                         no 1° grau
	 * @param listaMetaInformacoesAnalisadasG2 Lista de meta-informações analisadas
	 *                                         no 2° grau
	 * 
	 * @return Lista com 2 posições: 1) Set com lista de processos analisados no 1°
	 *         grau; 2) Set com lista de processos analisados no 2° grau.
	 */
	private List<Set<MetaInformacaoEnvio>> carregaListaProcessos(Map<String, File> mapProtocolosComProcessos,
			List<MetaInformacaoEnvio> listaMetaInformacoesAnalisadasG1,
			List<MetaInformacaoEnvio> listaMetaInformacoesAnalisadasG2) {

		List<Set<MetaInformacaoEnvio>> resposta = new ArrayList<Set<MetaInformacaoEnvio>>();

		Set<MetaInformacaoEnvio> setProcessosAnalisadosG1 = new HashSet<MetaInformacaoEnvio>();
		Set<MetaInformacaoEnvio> setProcessosAnalisadosG2 = new HashSet<MetaInformacaoEnvio>();

		for (MetaInformacaoEnvio metaInformacaoEnvioG1 : listaMetaInformacoesAnalisadasG1) {

			// Pesquisa o número do processo a partir dos protocolo lidos nos arquivos
			File arquivoProtocolo = mapProtocolosComProcessos.get(metaInformacaoEnvioG1.getNumProtocolo());
			String numProcesso = arquivoProtocolo == null ? null : arquivoProtocolo.getName().replace(".xml.protocolo", "");

			if (numProcesso != null) {
				metaInformacaoEnvioG1.setNumProcesso(numProcesso);
				setProcessosAnalisadosG1.add(metaInformacaoEnvioG1);
			} else {
				LOGGER.warn("Não foi encontrado nos arquivos de protocolo o processo referente ao protocolo "
						+ metaInformacaoEnvioG1.getNumProtocolo());
			}
		}

		for (MetaInformacaoEnvio metaInformacaoEnvioG2 : listaMetaInformacoesAnalisadasG2) {

			// Pesquisa o número do processo a partir dos protocolo lidos nos arquivos
			File arquivoProtocolo = mapProtocolosComProcessos.get(metaInformacaoEnvioG2.getNumProtocolo());
			String numProcesso = arquivoProtocolo == null ? null :arquivoProtocolo.getName().replace(".xml.protocolo", "");

			if (numProcesso != null) {
				metaInformacaoEnvioG2.setNumProcesso(numProcesso);
				setProcessosAnalisadosG2.add(metaInformacaoEnvioG2);
			} else {
				LOGGER.warn("Não foi encontrado nos arquivos de protocolo o processo referente ao protocolo "
						+ metaInformacaoEnvioG2.getNumProtocolo());			
			}
		}

		resposta.add(setProcessosAnalisadosG1);
		resposta.add(setProcessosAnalisadosG2);

		return resposta;
	}

	/**
	 * Atualiza o valor de um parâmetro do URIBuilder retornando a URI alterada
	 * 
	 * @param builder        URIBuilder
	 * @param nomeParametro  Nome do parâmetro contido no URIBuilder
	 * @param valorParametro Valor do parâmetro a ser atualizado.
	 * 
	 * @return Nova URI com o parâmetro atualizado
	 */
	private URI atualizarParametroURIBuilder(URIBuilder builder, String nomeParametro, String valorParametro) {

		builder.setParameter(nomeParametro, valorParametro);

		try {
			return builder.build();
		} catch (URISyntaxException e) {
			throw new RuntimeException("Erro na criação da URL para consultar protocolos.", e);
		}
	}

	/**
	 * Constrói o URIBuilder a partir do parâmetro Parametro.url_webservice_cnj
	 * concatenada com a string "/protocolos" e os parâmetros da URL passados para
	 * esse método.
	 * 
	 * @param parametroProcolo    Número do protocolo
	 * @param parametroDataInicio Data de início do envio
	 * @param parametroDataFim    Data de Fim do Envio
	 * @param parametroStatus     Status do protocolo: 1 (Aguardando processamento),
	 *                            3 (Processado com sucesso), 4 (Enviado), 5
	 *                            (Duplicado), 6 (Processado com Erro), 7 (Erro no
	 *                            Arquivo)
	 * 
	 * @return URIBuilder composto por Parametro.url_webservice_cnj mais
	 *         "/protocolos" e os parâmetros passados para o método.
	 */
	private URIBuilder construirBuilderWebServiceCNJ(String parametroProcolo, String parametroDataInicio,
			String parametroDataFim, String parametroStatus) {

		// Campo "status" da URL de consulta de protocolos:
		// 1 = Aguardando processamento
		// 3 = Processado com sucesso
		// 4 = Enviado
		// 5 = Duplicado
		// 6 = Processado com Erro
		// 7 = Erro no Arquivo

		// Monta a URL para consultar protocolos no CNJ.
		// Exemplo de URL:
		// https://www.cnj.jus.br/modelo-de-transferencia-de-dados/v1/processos/protocolos?protocolo=&dataInicio=2020-07-21&dataFim=2020-07-22&status=6&page=0
		final String url = Auxiliar.getParametroConfiguracao(Parametro.url_webservice_cnj, true) + "/protocolos";

		URIBuilder builder = null;

		try {
			builder = new URIBuilder(url);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Erro na criação da URL para consultar protocolos.", e);
		}

		builder.setParameter(NOME_PARAMETRO_PROTOCOLO, parametroProcolo)
				.setParameter(NOME_PARAMETRO_DATA_INICIO, parametroDataInicio)
				.setParameter(NOME_PARAMETRO_DATA_FIM, parametroDataFim)
				.setParameter(NOME_PARAMETRO_STATUS, parametroStatus);

		return builder;
	}

	/**
	 * Realiza a requisição HTTP ao serviço do CNJ. Mesmo quando o serviço está
	 * ativo ele retorna o erro 504 algumas vezes, dessa forma são feitas 10
	 * tentativas com 5 minutos entre cada uma antes de lançar a exceção
	 * IOException.
	 * 
	 * @param uri URI do serviço
	 * 
	 * @return O corpo (body) da resposta do serviço
	 * 
	 * @throws IOException
	 */
	private String executarRequisicaoWebServiceCNJ(URI uri) throws IOException {

		HttpGet get = new HttpGet(uri);
		HttpUtil.adicionarCabecalhoAutenticacao(get);

		// TODO: Validar esse timeout considerando os muitos dados de produção
		// // Timeout
		// int CONNECTION_TIMEOUT_MS = 300_000; // Timeout in millis (5min)
		// RequestConfig requestConfig = RequestConfig.custom()
		// .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
		// .setConnectTimeout(CONNECTION_TIMEOUT_MS)
		// .setSocketTimeout(CONNECTION_TIMEOUT_MS)
		// .build();
		// post.setConfig(requestConfig);

		long tempo;
		HttpResponse response = null;
		int statusCode = 0;

		// Como o erro 504 acontece com alguma frequência mesmo quando o
		// serviço está funcionando, faz a tentativa até 10 vezes
		int tentativasTimeOut = 0;

		String body = "";

		try {
			do {
				// Executa o GET
				tempo = System.currentTimeMillis();
				response = httpClient.execute(get);
				HttpEntity result = response.getEntity();
				body = EntityUtils.toString(result, Charset.forName("UTF-8"));
				statusCode = response.getStatusLine().getStatusCode();
				LOGGER.info("* Resposta em " + tempo + "ms (" + statusCode + ")");
				if (statusCode == 504 && tentativasTimeOut < 10) {
					LOGGER.info("* Aguardando 5 minutos...");
					try {
						Thread.sleep(5 * 60_000);
					} catch (InterruptedException e) {
						LOGGER.warn("Erro ao tentar pausar por 5 minutos...");
					}
					LOGGER.info("* Tentando novamente...");
				}
			} while (statusCode == 504 && tentativasTimeOut++ < 10); // Timeout
		} catch (ClientProtocolException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			if (response != null) {
				EntityUtils.consumeQuietly(response.getEntity());				
			}
		}

		if (statusCode != 200 && statusCode != 201) {
			throw new IOException(
					"Falha ao conectar no Webservice do CNJ (codigo " + statusCode + ", esperado 200 ou 201)");
		}

		return body;
	}

	/**
	 * Retorna a última página consultada de acordo com o status do processamento do
	 * protocolo. A página inicial é a 0.
	 * 
	 * @param tipoValidacaoProtocolo Tipo de validação de protocolos utilizada no
	 *                               processamento de protocolos no CNJ
	 * @return -1 ou a última página consultada
	 */
	private int buscarUltimaPaginaConsultadaPorTipoDeValidacao(String tipoValidacaoProtocolo) {

		File arquivoPaginaAtual = Auxiliar.getArquivoUltimaPaginaConsultaCNJ();

		int numUltimaPaginaConsultada = -1;

		if (arquivoPaginaAtual.exists() && arquivoPaginaAtual.length() != 0) {
			try {
				Pattern padraoER = Pattern
						.compile(String.format(PADRAO_ER_ARQUIVO_ULTIMA_PAGINA_CONSULTADA, tipoValidacaoProtocolo));

				List<String> conteudoArquivo = new ArrayList<>(
						Files.readAllLines(arquivoPaginaAtual.toPath(), StandardCharsets.UTF_8));

				for (int i = 0; i < conteudoArquivo.size(); i++) {
					Matcher matcher = padraoER.matcher(conteudoArquivo.get(i));

					if (matcher.find()) {
						return Integer.valueOf(matcher.group(2));
					}
				}
			} catch (IOException e) {
				LOGGER.warn("* Não foi possível ler o arquivo " + arquivoPaginaAtual.toString());
			} catch (NumberFormatException e) {
				LOGGER.warn("* Não foi possível converter para inteiro o conteúdo do arquivo "
						+ arquivoPaginaAtual.toString());
			}
		}

		return numUltimaPaginaConsultada;
	}

	private void gravarUltimaPaginaConsultada(String tipoValidacaoProtocolo, String pagina) {

		File arquivoPaginaAtual = Auxiliar.getArquivoUltimaPaginaConsultaCNJ();

		if (arquivoPaginaAtual.exists() && arquivoPaginaAtual.length() != 0) {
			try {
				Pattern padraoER = Pattern
						.compile(String.format(PADRAO_ER_ARQUIVO_ULTIMA_PAGINA_CONSULTADA, tipoValidacaoProtocolo));

				List<String> conteudoArquivo = new ArrayList<>(
						Files.readAllLines(arquivoPaginaAtual.toPath(), StandardCharsets.UTF_8));

				for (int i = 0; i < conteudoArquivo.size(); i++) {
					Matcher matcher = padraoER.matcher(conteudoArquivo.get(i));

					if (matcher.find()) {
						// Encontrou uma linha com o formato, então apenas atualiza.
						conteudoArquivo.set(i, String.format("%s %s", tipoValidacaoProtocolo, pagina));
						Files.write(arquivoPaginaAtual.toPath(), conteudoArquivo, StandardCharsets.UTF_8);
						return;
					}
				}
				// Não encontrou uma linha com o formato
				FileUtils.write(arquivoPaginaAtual, String.format("%s %s", tipoValidacaoProtocolo, pagina),
						StandardCharsets.UTF_8, true);

			} catch (IOException e) {
				LOGGER.warn("* Não foi possível ler o arquivo " + arquivoPaginaAtual.toString());
			} catch (NumberFormatException e) {
				LOGGER.warn("* Não foi possível converter para inteiro o conteúdo do arquivo "
						+ arquivoPaginaAtual.toString());
			}
		} else {
			// Arquivo não existente
			try {
				FileUtils.write(arquivoPaginaAtual, String.format("%s %s", tipoValidacaoProtocolo, pagina),
						StandardCharsets.UTF_8);
			} catch (IOException e) {
				LOGGER.warn("Não foi possível escrever o número da página atual consultada no arquivo "
						+ arquivoPaginaAtual.getPath());
			}
		}
	}

	/**
	 * Itera sobre todos os arquivos com o número do protocolo para associá-los aos
	 * respectivos arquivos.
	 * 
	 * @param arquivosComProtocolo
	 * @return HashMap com a dupla Protocolo/File de todos os processos com
	 *         protocolo a serem pesquisados.
	 */
	private Map<String, File> carregarListaFilesPorProtocolo(List<ArquivoComInstancia> arquivosComProtocolo) {
		Map<String, File> protocolosComProcessos = new HashMap<String, File>();

		for (ArquivoComInstancia arquivoComInstancia : arquivosComProtocolo) {
			protocolosComProcessos.put(arquivoComInstancia.getProtocolo(), arquivoComInstancia.getArquivo());																								// ""));
		}
		return protocolosComProcessos;
	}

	/**
	 * Verifica se um determinado protocolo ainda está pendente de conferência no
	 * CNJ
	 * 
	 * @param arquivo
	 * @return
	 */
	private static boolean deveConsultarArquivo(File arquivo) {
		return !Auxiliar.gerarNomeArquivoProcessoSucesso(arquivo).exists()
				&& !Auxiliar.gerarNomeArquivoProcessoNegado(arquivo).exists();
	}

	private void marcarArquivoComoProcessado(File arquivo, String json, boolean sucesso) {
		File arquivoConfirmacao = sucesso ? Auxiliar.gerarNomeArquivoProcessoSucesso(arquivo)
				: Auxiliar.gerarNomeArquivoProcessoNegado(arquivo);
		try {
			FileUtils.write(arquivoConfirmacao, json, StandardCharsets.UTF_8);
		} catch (IOException ex) {
			LOGGER.warn("Não foi possível marcar arquivo como processado: " + arquivo, ex);
		}
	}

	public void gravarTotalProtocolosRecusados() throws IOException {
		List<ArquivoComInstancia> arquivosProtocolos = ArquivoComInstancia
				.localizarArquivosInstanciasHabilitadas(Auxiliar.SUFIXO_PROTOCOLO_ERRO, true);
		if (!arquivosProtocolos.isEmpty()) {
			File listaRecusados = new File(Auxiliar.prepararPastaDeSaida(), "lista_protocolos_recusados_cnj.txt");
			try (FileWriter fw = new FileWriter(listaRecusados)) {
				for (ArquivoComInstancia arquivo : arquivosProtocolos) {
					fw.append(arquivo.getArquivo().toString());
					fw.append("\n");
				}
			}
			LOGGER.warn("Um total de " + arquivosProtocolos.size()
					+ " processos foram RECUSADOS no CNJ. A lista completa foi gravada neste arquivo: "
					+ listaRecusados);
		}
	}

	/**
	 * Grava o conjunto de processos e suas meta-informações no arquivo de saída e
	 * marca o arquvido como processado com sucesso ou com erro (Informação
	 * utilizada no controle da Operação Y).
	 * 
	 * @param setMetaInformacoes      Conjunto de meta-informações dos processos
	 *                                analisados
	 * @param arquivoSaida            Arquivo a ser gravado
	 * @param acrescentarFinalArquivo Indica se os processos devem ser acrescentados
	 *                                ao final do arquivo, ou se um novo arquivo
	 *                                será gerado
	 * 
	 * @throws IOException
	 */
	public void gravarListaProcessosEmArquivoEMarcarComoProcessado(Map<String, File> mapProtocolosComArquivos,
			Set<MetaInformacaoEnvio> setMetaInformacoes, File arquivoSaida, boolean acrescentarFinalArquivo)
			throws IOException {
		arquivoSaida.getParentFile().mkdirs();
		FileWriter fw = new FileWriter(arquivoSaida, acrescentarFinalArquivo);
		try {
			for (MetaInformacaoEnvio metaInformacaoEnvio : setMetaInformacoes) {
				// Salva as meta-informacoes em um arquivo único
				fw.append(metaInformacaoEnvio.getInformacaoFormatada());
				fw.append("\r\n");

				Integer status = 0;
				try {
					status = Integer.parseInt(metaInformacaoEnvio.getTipStatusProtocolo());
				} catch (NumberFormatException e) {
					LOGGER.error(String.format("Não foi possível converter o status do protocolo em um número: %s.", 
							metaInformacaoEnvio.getTipStatusProtocolo()));
				}

				// Marca o arquvido como processado com sucesso ou com erro.
				boolean sucesso = SituacaoProtocoloCNJ.SUCESSO.getId().equals(status);

				if (sucesso) {
					this.marcarArquivoComoProcessado(mapProtocolosComArquivos.get(metaInformacaoEnvio.getNumProtocolo()),
							metaInformacaoEnvio.getInformacaoFormatada(), sucesso);
					LOGGER.debug(
							"Protocolo baixado com SUCESSO: " + metaInformacaoEnvio.getNumProtocolo() + ", arquivo '"
									+ mapProtocolosComArquivos.get(metaInformacaoEnvio.getNumProtocolo()) + "'");
				} else if(metaInformacaoEnvio.hasStatusErro()) {
					this.marcarArquivoComoProcessado(mapProtocolosComArquivos.get(metaInformacaoEnvio.getNumProtocolo()),
							metaInformacaoEnvio.getInformacaoFormatada(), sucesso);
					LOGGER.warn("Protocolo baixado com ERRO: " + metaInformacaoEnvio.getNumProtocolo() + ", arquivo '"
							+ mapProtocolosComArquivos.get(metaInformacaoEnvio.getNumProtocolo()) + "'");
				} else {
					LOGGER.debug(
							"Protocolo NÃO baixado ainda: " + metaInformacaoEnvio.getNumProtocolo() + ", arquivo '"
									+ mapProtocolosComArquivos.get(metaInformacaoEnvio.getNumProtocolo()) + "'");
				}
			}
		} finally {
			fw.close();
		}

	}

	/**
	 * Grava em um arquivo as seguintes informações separadas por ponto e vírgula:
	 * instância, número do processo, número do protocolo e data de geração do
	 * arquivo .PROTOCOLO.
	 * 
	 * @param arquivosComProtocolo    Lista de arquivos com instância e protocolo
	 * @param arquivoSaida            Arquivo que será gravado. Como sugestão usar o
	 *                                método new File(Auxiliar.getPastaOutputRaiz(),
	 *                                "/nomeDoArquivo.txt"), caso tenha realizado
	 *                                envios de vários tipos como COMPLETA, TESTES,
	 *                                PROCESSO, ETC. e queria que o arquivo seja
	 *                                gravado no mesmo local independentemente do
	 *                                método de geração usado.
	 * @param acrescentarFinalArquivo Indica se deverá sobrescrever o conteúdo do
	 *                                arquivo existente, ou então acrescentar o
	 *                                conteúdo ao seu final, mantendo o conteúdo do
	 *                                arquivo.
	 * 
	 * @throws IOException
	 */
	private void GravarListaProcessoProtocoloInstanciaEmArquivo(File arquivoSaida, boolean acrescentarFinalArquivo)
			throws IOException {
		List<ArquivoComInstancia> arquivosComProtocolo = ArquivoComInstancia
				.localizarArquivosInstanciasHabilitadas(Auxiliar.SUFIXO_PROTOCOLO, false);
		FileWriter fw = new FileWriter(arquivoSaida, acrescentarFinalArquivo);

		for (ArquivoComInstancia arquivoComInstancia : arquivosComProtocolo) {

			BasicFileAttributes atributos = Files.readAttributes(arquivoComInstancia.getArquivo().toPath(),
					BasicFileAttributes.class);
			Date dataInicio = new Date(atributos.lastModifiedTime().toMillis());
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");

			fw.append(String.format("%s;%s;%s;%s", arquivoComInstancia.getGrau(),
					arquivoComInstancia.getArquivo().getName().replace(".xml.protocolo", ""),
					arquivoComInstancia.getProtocolo(), formatter.format(dataInicio)));
			fw.append("\r\n");
		}

		fw.close();
	}
}
