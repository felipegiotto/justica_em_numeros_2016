package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.cnj.intercomunicacao_2_2.ModalidadePoloProcessual;
import br.jus.cnj.intercomunicacao_2_2.ModalidadeRepresentanteProcessual;
import br.jus.cnj.intercomunicacao_2_2.TipoAssuntoProcessual;
import br.jus.cnj.intercomunicacao_2_2.TipoCabecalhoProcesso;
import br.jus.cnj.intercomunicacao_2_2.TipoEndereco;
import br.jus.cnj.intercomunicacao_2_2.TipoMovimentoNacional;
import br.jus.cnj.intercomunicacao_2_2.TipoMovimentoProcessual;
import br.jus.cnj.intercomunicacao_2_2.TipoOrgaoJulgador;
import br.jus.cnj.intercomunicacao_2_2.TipoParte;
import br.jus.cnj.intercomunicacao_2_2.TipoPessoa;
import br.jus.cnj.intercomunicacao_2_2.TipoPoloProcessual;
import br.jus.cnj.intercomunicacao_2_2.TipoProcessoJudicial;
import br.jus.cnj.intercomunicacao_2_2.TipoQualificacaoPessoa;
import br.jus.cnj.intercomunicacao_2_2.TipoRepresentanteProcessual;
import br.jus.cnj.replicacao_nacional.ObjectFactory;
import br.jus.cnj.replicacao_nacional.Processos;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DadosInvalidosException;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.IdentificaDocumentosPessoa;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.IdentificaGeneroPessoa;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.NamedParameterStatement;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Parametro;
import br.jus.trt4.justica_em_numeros_2016.tabelas_cnj.AnalisaAssuntosCNJ;
import br.jus.trt4.justica_em_numeros_2016.tabelas_cnj.AnalisaClassesProcessuaisCNJ;
import br.jus.trt4.justica_em_numeros_2016.tabelas_cnj.AnalisaMovimentosCNJ;
import br.jus.trt4.justica_em_numeros_2016.tabelas_cnj.AnalisaServentiasCNJ;
import br.jus.trt4.justica_em_numeros_2016.tabelas_cnj.ServentiaCNJ;

/**
 * Carrega as listas de processos geradas pela classe {@link Op_1_BaixaListaDeNumerosDeProcessos} e,
 * para cada processo, gera seu arquivo XML na pasta "output/.../Xg/xmls_individuais".
 * 
 * Fonte: http://www.mkyong.com/java/jaxb-hello-world-example/
 * 
 * @author felipe.giotto@trt4.jus.br
 */
public class Op_2_GeraXMLsIndividuais {

	private static final Logger LOGGER = LogManager.getLogger(Op_2_GeraXMLsIndividuais.class);
	private int grau;
	private Connection conexaoBasePrincipal;
	private NamedParameterStatement nsConsultaProcessos;
	private NamedParameterStatement nsPolos;
	private NamedParameterStatement nsPartes;
	private NamedParameterStatement nsEnderecos;
	private NamedParameterStatement nsAssuntos;
	private NamedParameterStatement nsMovimentos;
	private NamedParameterStatement nsComplementos;
	private int codigoMunicipioIBGETRT;
	private static AnalisaServentiasCNJ processaServentiasCNJ;
	private AnalisaAssuntosCNJ analisaAssuntosCNJ;
	private AnalisaClassesProcessuaisCNJ analisaClassesProcessuaisCNJ;
	private AnalisaMovimentosCNJ analisaMovimentosCNJ;
	private IdentificaGeneroPessoa identificaGeneroPessoa;
	private IdentificaDocumentosPessoa identificaDocumentosPessoa;
	
	/**
	 * Gera todos os XMLs (1G e/ou 2G), conforme definido no arquivo "config.properties"
	 */
	public static void main(String[] args) throws SQLException, Exception {
		Auxiliar.prepararPastaDeSaida();

		// Verifica se deve gerar XML para 2o Grau
		if (Auxiliar.getParametroBooleanConfiguracao(Parametro.gerar_xml_2G)) {
			gerarXMLs(2);
		}
		
		// Verifica se deve gerar XML para 1o Grau
		if (Auxiliar.getParametroBooleanConfiguracao(Parametro.gerar_xml_1G)) {
			gerarXMLs(1);
		}
		
		AnalisaServentiasCNJ.mostrarWarningSeAlgumaServentiaNaoFoiEncontrada();
        DadosInvalidosException.mostrarWarningSeHouveAlgumErro();
        LOGGER.info("Fim!");
	}

	
	private static void gerarXMLs(int grau) throws Exception {
		Op_2_GeraXMLsIndividuais baixaDados = new Op_2_GeraXMLsIndividuais(grau);
		try {

			// Abre conexões com o PJe e prepara consultas a serem realizadas
			baixaDados.prepararConexao();

			// Executa consultas e grava arquivo XML
			baixaDados.gerarXML();
		} finally {
			baixaDados.close();
		}
	}

	
	public Op_2_GeraXMLsIndividuais(int grau) {
		this.grau = grau;
	}
	
	
	private void gerarXML() throws IOException, SQLException, JAXBException {

		LOGGER.info("Gerando XMLs do " + grau + "o Grau...");
		
		// Objetos auxiliares para gerar o XML a partir das classes Java
		ObjectFactory factory = new ObjectFactory();
		JAXBContext context = JAXBContext.newInstance(Processos.class);
		Marshaller jaxbMarshaller = context.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		
		// Variáveis auxiliares para calcular o tempo estimado
		int qtdXMLGerados = 0;
		long tempoGasto = 0;
		
		// Pasta onde serão gerados os arquivos XML
		File pastaRaiz = Auxiliar.getPastaXMLsIndividuais(grau);
		pastaRaiz = new File(pastaRaiz, "PJe");
		
		// Carrega a lista de processos que precisará ser analisada
		List<String> listaProcessos = Auxiliar.carregarListaProcessosDoArquivo(Auxiliar.getArquivoListaProcessos(grau));
		int i=0;
		for (String numeroProcesso: listaProcessos) {
			
			// Cálculo do tempo restante
			long antes = System.currentTimeMillis();
			i++;

			// Arquivo XML que conterá os dados do processo
			// Depois da geração do XML temporário, visando garantir a integridade do arquivo XML 
			// definitivo, o temporário só será excluído depois da gravação completa do definitivo.
			File arquivoXMLTemporario = new File(pastaRaiz, numeroProcesso + ".temp");
			File arquivoXML = new File(pastaRaiz, numeroProcesso + ".xml");
			arquivoXMLTemporario.delete();
			
			// Se o script for abortado bem na hora da cópia do arquivo temporário para o definitivo, o definitivo
			// pode ficar vazio. Se isso ocorrer, apaga o XML vazio, para que um novo seja gerado.
			if (arquivoXML.exists() && arquivoXML.length() == 0) {
				arquivoXML.delete();
			}
			
			// Verifica se o XML do processo já foi gerado
			if (arquivoXML.exists()) {
				LOGGER.debug("O arquivo XML do processo " + numeroProcesso + " já existe e não será gerado novamente.");
				continue;
			}
			
			// Calcula tempo restante
			if (LOGGER.isDebugEnabled()) {
				int xmlsRestantes = listaProcessos.size() - i;
				long tempoRestante = 0;
				long mediaPorProcesso = 0;
				if (qtdXMLGerados > 0) {
					mediaPorProcesso = tempoGasto / qtdXMLGerados;
					tempoRestante = xmlsRestantes * mediaPorProcesso;
				}
				
				LOGGER.debug("Processo " + numeroProcesso + " (" + i + "/" + listaProcessos.size() + " - " + i * 100 / listaProcessos.size() + "%" + (tempoRestante == 0 ? "" : " - ETA: " + DurationFormatUtils.formatDurationHMS(tempoRestante)) + (mediaPorProcesso == 0 ? "" : ", media de " + mediaPorProcesso + "ms/processo") + "). Arquivo de saída: " + arquivoXML);
			}

			// Executa a consulta desse processo no banco de dados do PJe
			TipoProcessoJudicial processoJudicial = null;
			try {
				processoJudicial = analisarProcessoJudicialCompleto(numeroProcesso);
			} catch (Exception ex) {
				LOGGER.warn("Erro gerando XML do processo " + numeroProcesso + " (" + grau + "): " + ex.getLocalizedMessage(), ex);
			}
			
			if (processoJudicial != null) {
			
				// Objeto que, de acordo com o padrão MNI, que contém uma lista de processos. 
				// Nesse caso, ele conterá somente UM processo. Posteriormente, os XMLs de cada
				// processo serão unificados, junto com os XMLs dos outros sistemas legados.
				Processos processos = factory.createProcessos();
				processos.getProcesso().add(processoJudicial);
				
				// Gera o arquivo XML temporário
				arquivoXML.getParentFile().mkdirs();
				jaxbMarshaller.marshal(processos, arquivoXMLTemporario);
				
				// Copia o XML temporário sobre o definitivo e exclui o temporário
				FileUtils.copyFile(arquivoXMLTemporario, arquivoXML);
				arquivoXMLTemporario.delete();
				
				// Cálculo do tempo restante
				tempoGasto += System.currentTimeMillis() - antes;
				qtdXMLGerados++;
				
			} else {
				LOGGER.warn("O XML do processo " + numeroProcesso + " não foi gerado na base " + grau + "G!");
			}
		}
		
		LOGGER.info("Arquivos XML do " + grau + "o Grau gerados!");
	}

	
	/**
	 * Consulta os dados do processo informado no banco de dados e gera um objeto da classe
	 * {@link TipoProcessoJudicial}
	 * 
	 * @param numeroProcesso
	 * @return
	 * @throws SQLException 
	 * @throws DadosInvalidosException 
	 * @throws IOException
	 */
	public TipoProcessoJudicial analisarProcessoJudicialCompleto(String numeroProcesso) throws SQLException, DadosInvalidosException {
		
		nsConsultaProcessos.setString("numero_processo", numeroProcesso);
		try (ResultSet rsProcessos = nsConsultaProcessos.executeQuery()) {
			if (rsProcessos.next()) {
				return analisarProcessoJudicialCompleto(rsProcessos);
			} else {
				return null;
			}
		}
	}

	
	/**
	 * Método criado com base no script recebido do TRT14
	 * para preencher os dados de um processo judicial dentro das classes que gerarão o XML.
	 * 
	 * @param processoJudicial
	 * @param rsProcesso
	 * @throws SQLException 
	 * @throws DadosInvalidosException 
	 * @throws IOException 
	 */
	public TipoProcessoJudicial analisarProcessoJudicialCompleto(ResultSet rsProcesso) throws SQLException, DadosInvalidosException {

		// Objeto que será retornado
		TipoProcessoJudicial processoJudicial = new TipoProcessoJudicial();
		
		// Cabeçalho com dados básicos do processo:
		processoJudicial.setDadosBasicos(analisarCabecalhoProcesso(rsProcesso));

		// Movimentos processuais e complementos
		processoJudicial.getMovimento().addAll(analisarMovimentosProcesso(rsProcesso.getInt("id_processo_trf")));
		
		return processoJudicial;
	}

	
	private TipoCabecalhoProcesso analisarCabecalhoProcesso(ResultSet rsProcesso) throws SQLException, DadosInvalidosException {
		
		String numeroCompletoProcesso = rsProcesso.getString("numero_completo_processo");
		
		// Script TRT14:
		// raise notice '<dadosBasicos nivelSigilo="%" numero="%" classeProcessual="%" codigoLocalidade="%" dataAjuizamento="%">' 
		//  , proc.nivelSigilo, proc.nr_processo, proc.cd_classe_judicial, proc.id_municipio_ibge_origem, proc.dt_autuacao;
		TipoCabecalhoProcesso cabecalhoProcesso = new TipoCabecalhoProcesso();
		cabecalhoProcesso.setNivelSigilo(Auxiliar.getCampoIntNotNull(rsProcesso, "nivelSigilo"));
		cabecalhoProcesso.setNumero(Auxiliar.getCampoStringNotNull(rsProcesso, "nr_processo"));
		
		// Grava a classe processual, conferindo se ela está na tabela nacional do CNJ
		analisaClassesProcessuaisCNJ.preencherClasseProcessualVerificandoTPU(cabecalhoProcesso, rsProcesso.getInt("cd_classe_judicial"), rsProcesso.getString("ds_classe_judicial"), numeroCompletoProcesso);
		
		cabecalhoProcesso.setDataAjuizamento(Auxiliar.getCampoStringNotNull(rsProcesso, "dt_autuacao"));
		cabecalhoProcesso.setValorCausa(Auxiliar.getCampoDoubleOrNull(rsProcesso, "vl_causa")); 
		if (grau == 1) {
			
			// Em 1G, pega como localidade o município do OJ do processo
			cabecalhoProcesso.setCodigoLocalidade(Auxiliar.getCampoStringNotNull(rsProcesso, "id_municipio_ibge"));
		} else {
			
			// Em 2G, pega como localidade o município do TRT, que está definido no arquivo de configurações
			cabecalhoProcesso.setCodigoLocalidade(Integer.toString(codigoMunicipioIBGETRT));
		}

		// Consulta todos os polos do processo
		cabecalhoProcesso.getPolo().addAll(analisarPolosProcesso(rsProcesso.getInt("id_processo_trf")));

		// Consulta todos os assuntos desse processo
		cabecalhoProcesso.getAssunto().addAll(analisarAssuntosProcesso(numeroCompletoProcesso));

		// Preenche dados do órgão julgador do processo
		cabecalhoProcesso.setOrgaoJulgador(analisarOrgaoJulgadorProcesso(rsProcesso));
		
		return cabecalhoProcesso;
	}

	
	private List<TipoPoloProcessual> analisarPolosProcesso(int idProcesso) throws SQLException, DadosInvalidosException {
		
		List<TipoPoloProcessual> polos = new ArrayList<TipoPoloProcessual>();
		
		// Consulta todos os polos do processo
		nsPolos.setInt("id_processo", idProcesso);
		try (ResultSet rsPolos = nsPolos.executeQuery()) {
			while (rsPolos.next()) {

				// Script TRT14:
				// raise notice '<polo polo="%">', polo.in_polo_participacao;
				TipoPoloProcessual polo = new TipoPoloProcessual();
				String tipoPoloPJe = rsPolos.getString("in_participacao");
				if ("A".equals(tipoPoloPJe)) {
					polo.setPolo(ModalidadePoloProcessual.AT); // AT: polo ativo
				} else if ("P".equals(tipoPoloPJe)) {
					polo.setPolo(ModalidadePoloProcessual.PA); // PA: polo passivo
				} else if ("T".equals(tipoPoloPJe)) {
					polo.setPolo(ModalidadePoloProcessual.TC); // TC: terceiro
				} else {
					throw new RuntimeException("Tipo de polo não reconhecido: " + tipoPoloPJe);
				}
				polos.add(polo);

				// Consulta as partes de um determinado polo no processo
				nsPartes.setInt("id_processo", idProcesso);
				nsPartes.setString("in_participacao", rsPolos.getString("in_participacao"));
				try (ResultSet rsPartes = nsPartes.executeQuery()) {
					
					// O PJe considera, como partes, tanto os autores e réus quanto seus advogados,
					// procuradores, tutores, curadores, assistentes, etc.
					// Por isso, a identificação das partes será feita em etapas:
					// 1. Todas as partes e advogados serão identificados e gravados no HashMap
					//    "partesPorIdParte"
					// 2. As partes representadas e seus representantes serão gravados no HashMap
					//    "partesERepresentantes"
					// 2. As partes identificadas que forem representantes de alguma outra parte 
					//    serão removidas de "partesPorIdParte" e registradas efetivamente como 
					//    representantes no atributo "advogado" da classe TipoParte
					// 3. As partes que "restarem" no HashMap, então, serão inseridas no XML.
					HashMap<Integer, TipoParte> partesPorIdParte = new HashMap<>(); // id_processo_parte -> TipoParte
					HashMap<Integer, List<Integer>> partesERepresentantes = new HashMap<>(); // id_processo_parte -> lista dos id_processo_parte dos seus representantes
					HashMap<Integer, ModalidadeRepresentanteProcessual> tiposRepresentantes = new HashMap<>(); // id_processo_parte -> Modalidade do representante (advogado, procurador, etc).
					while (rsPartes.next()) {

						String nomeParte = Auxiliar.getCampoStringNotNull(rsPartes, "ds_nome");
						
						// Script TRT14:
						// raise notice '<parte>';
						TipoParte parte = new TipoParte();
						int idProcessoParte = rsPartes.getInt("id_processo_parte");
						int idProcessoParteRepresentante = rsPartes.getInt("id_parte_representante");
						partesPorIdParte.put(idProcessoParte, parte);
						
						// Verifica se, no PJe, essa parte possui representante (advogado, procurador, tutor, curador, etc)
						if (idProcessoParteRepresentante > 0) {
							if (!partesERepresentantes.containsKey(idProcessoParte)) {
								partesERepresentantes.put(idProcessoParte, new ArrayList<Integer>());
							}
							partesERepresentantes.get(idProcessoParte).add(idProcessoParteRepresentante);
							
							String tipoParteRepresentante = rsPartes.getString("ds_tipo_parte_representante");
							if ("ADVOGADO".equals(tipoParteRepresentante)) {
								tiposRepresentantes.put(idProcessoParteRepresentante, ModalidadeRepresentanteProcessual.A);
								
							} else if ("PROCURADOR".equals(tipoParteRepresentante)) {
								tiposRepresentantes.put(idProcessoParteRepresentante, ModalidadeRepresentanteProcessual.P);
								
							} else if ("ADMINISTRADOR".equals(tipoParteRepresentante)
									|| "ASSISTENTE".equals(tipoParteRepresentante)
									|| "ASSISTENTE TÉCNICO".equals(tipoParteRepresentante)
									|| "CURADOR".equals(tipoParteRepresentante)
									|| "INVENTARIANTE".equals(tipoParteRepresentante)
									|| "REPRESENTANTE".equals(tipoParteRepresentante)
									|| "TERCEIRO INTERESSADO".equals(tipoParteRepresentante)
									|| "TUTOR".equals(tipoParteRepresentante)) {
								// Não fazer nada, pois esses tipos de parte (PJe), apesar de estarem descritos
								// no arquivo "intercomunicacao-2.2.2", não estão sendo enviados ao CNJ
								// pela ferramenta "replicacao-client".
								
							} else {
								LOGGER.warn("O representante da parte '" + nomeParte + "' (id_processo_parte=" + idProcessoParte + ") possui um tipo de parte que ainda não foi tratado: " + tipoParteRepresentante);
							}
						}

						// Script TRT4:
						// -- pessoa
						//   IF parte.nr_documento IS NOT NULL THEN
						//     raise notice '<pessoa nome="%" tipoPessoa="%" sexo="%" numeroDocumentoPrincipal="%">'
						//     , parte.ds_nome, parte.in_tipo_pessoa, parte.tp_sexo, parte.nr_documento;
						//   ELSE 
						//     raise notice '<pessoa nome="%" tipoPessoa="%" sexo="%">'
						//     , parte.ds_nome, parte.in_tipo_pessoa, parte.tp_sexo;
						//   END IF;
						TipoPessoa pessoa = new TipoPessoa();
						parte.setPessoa(pessoa);
						pessoa.setNome(nomeParte);
						
						// Tipo de pessoa (física / jurídica / outros)
						// Pergunta feita à ASSTECO: no e-mail com assunto "Dúvidas sobre envio de dados para Justiça em Números", em 03/07/2017 14:15:
						//   Estou gerando os dados para o Selo Justiça em Números, do CNJ. Ocorre que o MNI 2.2, utilizado como referência, permite somente o envio de dados de pessoas dos tipos "Física", "Jurídica", "Autoridade" e "Órgão de Representação".
						//   No PJe, os tipos são parecidos, mas ligeiramente diferentes: "Física", "Jurídica", "Autoridade", "MPT" e "Órgão Público". Quanto aos três primeiros, vejo que eles tem correspondência direta, mas fiquei na dúvida sobre como tratar os outros dois! Preciso, para cada pessoa do "lado" do PJe, encontrar um correspondente do "lado" do MNI.
						//   Vocês saberiam me informar qual o tipo correto, no padrão MNI, para enviar partes dos tipos "MPT" e "Órgão Público"?
						// Resposta da ASSTECO no e-mail em 03/07/2017 15:30:
						//   Não há um glossário do Selo Justiça em Números?
						//   O termo "Órgão de representação" não é usual, e não sabemos o que se enquadraria nele.
						//   O MPT deve se enquadrar como autoridade, e os órgãos públicos são pessoas jurídicas de direito público (pessoas jurídicas, portanto).
						//   Se não houver um glossário, e se vocês não tiverem algum contato para esclarecer essa dúvida, enquadra dessa forma.
						// Enquanto aguardo resposta do CNJ, mantenho a implementação sugerida.
						String tipoPessoaPJe = Auxiliar.getCampoStringNotNull(rsPartes, "in_tipo_pessoa");
						if ("F".equals(tipoPessoaPJe)) {
							pessoa.setTipoPessoa(TipoQualificacaoPessoa.FISICA);
						} else if ("J".equals(tipoPessoaPJe)) {
							pessoa.setTipoPessoa(TipoQualificacaoPessoa.JURIDICA);
						} else if ("A".equals(tipoPessoaPJe)) {
							pessoa.setTipoPessoa(TipoQualificacaoPessoa.AUTORIDADE);
						} else if ("M".equals(tipoPessoaPJe)) {
							pessoa.setTipoPessoa(TipoQualificacaoPessoa.AUTORIDADE);
						} else if ("O".equals(tipoPessoaPJe)) {
							pessoa.setTipoPessoa(TipoQualificacaoPessoa.JURIDICA);
						} else {
							throw new DadosInvalidosException("Tipo de pessoa desconhecido para '" + nomeParte + "': " + tipoPessoaPJe);
//							LOGGER.warn("Tipo de pessoa desconhecido para '" + nomeParte + "': " + tipoPessoaPJe);
//							pessoa.setTipoPessoa(TipoQualificacaoPessoa.FISICA);
						}
						
						// Consulta os documentos da parte
						identificaDocumentosPessoa.preencherDocumentosPessoa(pessoa, rsPartes.getInt("id_pessoa"));
						
						// Identifica o gênero (sexo) da pessoa (pode ser necessário consultar na outra instância)
						identificaGeneroPessoa.preencherSexoPessoa(pessoa, rsPartes.getString("in_sexo"), rsPartes.getString("ds_nome_consulta"));
						
						// Identifica os endereços da parte
						nsEnderecos.setInt("id_processo_parte", rsPartes.getInt("id_processo_parte"));
						try (ResultSet rsEnderecos = nsEnderecos.executeQuery()) {
							while (rsEnderecos.next()) {
								
								// intercomunicacao-2.2.2: Atributo indicador do código de endereçamento 
								// postal do endereço no diretório nacional de endereços da ECT. O 
								// valor deverá ser uma sequência de 8 dígitos, sem qualquer separador. 
								// O atributo é opcional para permitir a apresentação de endereços 
								// desprovidos de CEP e de endereços internacionais.
								// <restriction base="string"><pattern value="\d{8}"></pattern></restriction>
								String cep = rsEnderecos.getString("nr_cep");
								if (!StringUtils.isBlank(cep)) {
									cep = cep.replaceAll("[^0-9]", "");
								}
								
								if (!StringUtils.isBlank(cep)) {
									TipoEndereco endereco = new TipoEndereco();
									endereco.setCep(cep);
									
									// intercomunicacao-2.2.2: O logradouro pertinente a este endereço, 
									// tais como rua, praça, quadra etc. O elemento é opcional para permitir 
									// que as implementações acatem a indicação de endereço exclusivamente 
									// pelo CEP, quando o CEP já encerrar o dado respectivo.
									endereco.setLogradouro(rsEnderecos.getString("nm_logradouro"));
									
									// intercomunicacao-2.2.2: O número vinculado a este endereço. O elemento 
									// é opcional para permitir que as implementações acatem a indicação de 
									// endereço exclusivamente pelo CEP, quando o CEP já encerrar o dado respectivo.
									endereco.setNumero(rsEnderecos.getString("nr_endereco"));
									
									// intercomunicacao-2.2.2: O complemento vinculado a este endereço. 
									// O elemento é opcional em razão de sua própria natureza.
									endereco.setComplemento(rsEnderecos.getString("ds_complemento"));
									
									// intercomunicacao-2.2.2: O bairro vinculado a este endereço. O elemento 
									// é opcional para permitir que as implementações acatem a indicação 
									// de endereço exclusivamente pelo CEP, quando o CEP já encerrar o dado respectivo.
									endereco.setBairro(rsEnderecos.getString("nm_bairro"));
									
									// intercomunicacao-2.2.2: A cidade vinculada a este endereço. O elemento 
									// é opcional para permitir que as implementações acatem a indicação 
									// de endereço exclusivamente pelo CEP, quando o CEP já encerrar o dado respectivo.
									endereco.setCidade(rsEnderecos.getString("ds_municipio"));
									
									pessoa.getEndereco().add(endereco);
								}
							}
						}
						
						// Outras dados da pessoa
						if (rsPartes.getDate("dt_nascimento") != null) {
							pessoa.setDataNascimento(Auxiliar.formataDataAAAAMMDD(rsPartes.getDate("dt_nascimento")));
						}
						if (rsPartes.getDate("dt_obito") != null) {
							pessoa.setDataObito(Auxiliar.formataDataAAAAMMDD(rsPartes.getDate("dt_obito")));
						}
						pessoa.setNomeGenitor(rsPartes.getString("nm_genitor"));
						pessoa.setNomeGenitora(rsPartes.getString("nm_genitora"));
					}
					
					// Para cada parte identificada, localiza todos os seus representantes
					for (int idProcessoParte: partesERepresentantes.keySet()) {
						for (int idProcessoParteRepresentante : partesERepresentantes.get(idProcessoParte)) {
							
							// Isola a parte e seu representante
							TipoParte parte = partesPorIdParte.get(idProcessoParte);
							TipoParte representante = partesPorIdParte.get(idProcessoParteRepresentante);
							
							// Pode ocorrer de a parte estar ATIVA e possuir um advogado cujo registro 
							// em tb_processo_parte está INATIVO. Nesse caso, não insere o advogado
							// como representante.
							if (representante != null) {
								
								// Cria um objeto TipoRepresentanteProcessual a partir dos dados do representante
								// OBS: somente se o tipo do representante foi identificado, pois
								//      alguns tipos (como tutores e curadores) não são processados
								//      pelo "replicacao-client", do CNJ, e não serão incluídos no
								//      arquivo XML.
								if (tiposRepresentantes.containsKey(idProcessoParteRepresentante)) {
									TipoRepresentanteProcessual representanteProcessual = new TipoRepresentanteProcessual();
									parte.getAdvogado().add(representanteProcessual);
									representanteProcessual.setNome(representante.getPessoa().getNome());
									representanteProcessual.setIntimacao(true); // intercomunicacao-2.2.2: Indicativo verdadeiro (true) ou falso (false) relativo à escolha de o advogado, escritório ou órgão de representação ser o(s) preferencial(is) para a realização de intimações.
									representanteProcessual.setNumeroDocumentoPrincipal(representante.getPessoa().getNumeroDocumentoPrincipal());
									representanteProcessual.setTipoRepresentante(tiposRepresentantes.get(idProcessoParteRepresentante));
									representanteProcessual.getEndereco().addAll(representante.getPessoa().getEndereco());
								}
							}
						}
					}
					
					// Retira, da lista de partes do processo, as partes que são representantes
					// (e, por isso, já estão constando dentro das suas partes representadas)
					for (List<Integer> idProcessoParteRepresentantes: partesERepresentantes.values()) {
						for (int idProcessoParteRepresentante: idProcessoParteRepresentantes) {
							partesPorIdParte.remove(idProcessoParteRepresentante);
						}
					}
					
					// Insere as partes "restantes" no XML
					for (TipoParte parte: partesPorIdParte.values()) {
						polo.getParte().add(parte);
					}
				}
			}
		}
		return polos;
	}

	
	private List<TipoAssuntoProcessual> analisarAssuntosProcesso(String nrProcesso) throws SQLException, DadosInvalidosException {
		
		List<TipoAssuntoProcessual> assuntos = new ArrayList<TipoAssuntoProcessual>();
		
		// Consulta todos os assuntos do processo
		nsAssuntos.setString("nr_processo", nrProcesso);
		try (ResultSet rsAssuntos = nsAssuntos.executeQuery()) {
			boolean encontrouAlgumAssunto = false;
			boolean encontrouAssuntoPrincipal = false;
			while (rsAssuntos.next()) {

				// Script TRT14:
				// raise notice '<assunto>'; -- principal="%">', assunto.in_assunto_principal;
				// raise notice '<codigoNacional>%</codigoNacional>', assunto.cd_assunto_trf;
				// raise notice '</assunto>';
				
				// Analisa o assunto, que pode ou não estar nas tabelas processuais unificadas do CNJ.
				int codigo = Auxiliar.getCampoIntNotNull(rsAssuntos, "cd_assunto_trf");
				TipoAssuntoProcessual assunto = analisaAssuntosCNJ.getAssunto(codigo);
				assuntos.add(assunto);
				encontrouAlgumAssunto = true;

				// Trata o campo "assunto principal", verificando também se há mais de um assunto principal no processo.
				boolean assuntoPrincipal = "S".equals(rsAssuntos.getString("in_assunto_principal"));
				assunto.setPrincipal(assuntoPrincipal);
				if (assuntoPrincipal) {
					if (encontrouAssuntoPrincipal) {
						LOGGER.warn("Processo possui mais de um assunto principal: " + nrProcesso);
					} else {
						encontrouAssuntoPrincipal = true;
					}
				}
			}
			
			// Script TRT14:
			// -- se não tiver assunto? 
			// IF fl_assunto = 0 THEN 
			//   -- do something...
			//   raise notice '<assunto>'; 
			//   raise notice '<codigoNacional>2546</codigoNacional>'; -- Verbas Rescisórias
			//   raise notice '</assunto>';
			// END IF;
			if (!encontrouAlgumAssunto) {
				
				// Se não há nenhum assunto no processo, verifica se deve ser utilizando um assunto
				// padrão, conforme arquivo de configuração.
				TipoAssuntoProcessual assuntoPadrao = analisaAssuntosCNJ.getAssuntoProcessualPadrao();
				if (assuntoPadrao != null) {
					assuntos.add(assuntoPadrao);
					
				} else {
				    throw new DadosInvalidosException("Processo sem assunto cadastrado: " + nrProcesso);
					//LOGGER.warn("Processo sem assunto cadastrado: " + nrProcesso);
				}
				
			} else if (!encontrouAssuntoPrincipal) {
				LOGGER.info("Processo sem assunto principal: " + nrProcesso + ". O primeiro assunto será marcado como principal.");
				assuntos.get(0).setPrincipal(true);
			}
		}
		
		return assuntos;
	}

	
	private TipoOrgaoJulgador analisarOrgaoJulgadorProcesso(ResultSet rsProcesso) throws SQLException, DadosInvalidosException {
		/*
		 * Órgãos Julgadores
				Para envio do elemento <orgaoJulgador >, pede-se os atributos <codigoOrgao> e <nomeOrgao>, conforme definido em <tipoOrgaoJulgador>. 
				Em <codigoOrgao> deverão ser informados os mesmos códigos das serventias judiciárias cadastradas no Módulo de Produtividade Mensal (Resolução CNJ nº 76/2009).
				Em <nomeOrgao> deverão ser informados os mesmos descritivos das serventias judiciárias cadastradas no Módulo de Produtividade Mensal (Resolução CNJ nº 76/2009)
			Fonte: http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/2016-06-02-17-51-25
		 */
		/*
			10. Como preencher o campo “Órgão Julgador”? 
			    R: Os Tribunais deverão seguir os mesmos códigos e descrições utilizadas no módulo de produtividade.
			Fonte: http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/perguntas-frequentes
		 */
		// Script TRT14:
		// -- orgaoJulgador
		// raise notice '<orgaoJulgador codigoOrgao="%" nomeOrgao="%" instancia="%" codigoMunicipioIBGE="%"/>' -- codigoMunicipioIBGE="1100205" -- <=== 2º grau!!!
		//   , proc.ds_sigla, proc.ds_orgao_julgador, proc.tp_instancia, proc.id_municipio_ibge_atual;
		// Conversando com Clara, decidimos utilizar sempre a serventia do OJ do processo
		ServentiaCNJ serventiaCNJ = processaServentiasCNJ.getServentiaByOJ(rsProcesso.getString("ds_orgao_julgador"), true);
		TipoOrgaoJulgador orgaoJulgador = new TipoOrgaoJulgador();
		orgaoJulgador.setCodigoOrgao(serventiaCNJ.getCodigo());
		orgaoJulgador.setNomeOrgao(serventiaCNJ.getNome());
		if (grau == 1) {
			
			// Em 1G, pega como localidade do OJ o município do OJ do processo
			orgaoJulgador.setCodigoMunicipioIBGE(Auxiliar.getCampoIntNotNull(rsProcesso, "id_municipio_ibge"));
			
			// Em 1G, instância será sempre originária
			orgaoJulgador.setInstancia("ORIG");
		} else {
			
			// Em 2G, pega como localidade do OJ o município do TRT, que está definido no arquivo de configurações
			orgaoJulgador.setCodigoMunicipioIBGE(codigoMunicipioIBGETRT);
			
			// Em 2G, instância poderá ser originária ou não
			orgaoJulgador.setInstancia("2".equals(rsProcesso.getString("nr_instancia")) ? "ORIG" : "REV");
		}
		
		return orgaoJulgador;
	}

	
	private List<TipoMovimentoProcessual> analisarMovimentosProcesso(int idProcesso) throws SQLException {
		
		ArrayList<TipoMovimentoProcessual> movimentos = new ArrayList<>();
		
		// Consulta todos os movimentos do processo
		nsMovimentos.setInt("id_processo", idProcesso);
		try (ResultSet rsMovimentos = nsMovimentos.executeQuery()) {
			while (rsMovimentos.next()) {

				// Script TRT14:
				// raise notice '<movimento dataHora="%" nivelSigilo="%">', mov.dta_ocorrencia, mov.in_visibilidade_externa;
				// raise notice '<movimentoNacional codigoNacional="%">', mov.cd_movimento_cnj;
				TipoMovimentoProcessual movimento = new TipoMovimentoProcessual();
				movimento.setDataHora(Auxiliar.getCampoStringNotNull(rsMovimentos, "dta_ocorrencia"));
				movimento.setNivelSigilo(rsMovimentos.getInt("in_visibilidade_externa"));
				movimento.setIdentificadorMovimento(rsMovimentos.getString("id_processo_evento"));
				analisaMovimentosCNJ.preencheDadosMovimentoCNJ(movimento, Auxiliar.getCampoIntNotNull(rsMovimentos, "cd_movimento_cnj"), rsMovimentos.getString("ds_texto_final_interno"), rsMovimentos.getString("ds_movimento"));
				movimentos.add(movimento);

				// Consulta os complementos desse movimento processual.
				// OBS: os complementos só existem no MovimentoNacional
				TipoMovimentoNacional movimentoNacional = movimento.getMovimentoNacional();
				if (movimentoNacional != null) {
					int idMovimento = rsMovimentos.getInt("id_processo_evento");
					nsComplementos.setInt("id_processo_evento", idMovimento);
					try (ResultSet rsComplementos = nsComplementos.executeQuery()) {
						while (rsComplementos.next()) {
	
							// Script TRT14:
							//  IF '' = trim(compl.cd_complemento) THEN
							//    raise notice '<complemento>%:%:%</complemento>'
							//    , compl.cd_tipo_complemento, compl.ds_nome, compl.nm_complemento;
							//  ELSE
							//    raise notice '<complemento>%:%:%:%</complemento>'
							//    , compl.cd_tipo_complemento, compl.ds_nome, compl.cd_complemento, compl.nm_complemento;
							//  END IF;
							StringBuilder sb = new StringBuilder();
							sb.append(rsComplementos.getString("cd_tipo_complemento"));
							sb.append(":");
							sb.append(rsComplementos.getString("ds_nome"));
							String codigoComplemento = rsComplementos.getString("cd_complemento");
							if (!StringUtils.isBlank(codigoComplemento)) {
								sb.append(":");
								sb.append(codigoComplemento);
								/*
								O elemento <complemento> possui formato string e deverá ser preenchido da seguinte forma:
								<código do complemento><”:”><descrição do complemento><”:”><código do complemento tabelado><descrição do complemento tabelado, ou de texto livre, conforme o caso>
									
								Ex.: no movimento 123, seria
									18:motivo_da_remessa:38:em grau de recurso
									7:destino:1ª Vara Cível
								 */
								// Fonte: http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/2016-06-02-17-51-25
							}
							sb.append(":");
							sb.append(rsComplementos.getString("nm_complemento"));
							movimentoNacional.getComplemento().add(sb.toString());
						}
					}
				}
			}
		}
		
		return movimentos;
	}

	
	public void prepararConexao() throws SQLException, IOException, DadosInvalidosException {

		LOGGER.info("Preparando informações para gerar XMLs do " + grau + "o Grau...");
		
		// Objeto que fará o de/para dos OJ e OJC do PJe para os do CNJ
		if (processaServentiasCNJ == null) {
			processaServentiasCNJ = new AnalisaServentiasCNJ();
		}
		
		// Abre conexão com o banco de dados do PJe
		conexaoBasePrincipal = Auxiliar.getConexaoPJe(grau);
		conexaoBasePrincipal.setAutoCommit(false);

		// Objeto que auxiliará na identificação do sexo das pessoas na OUTRA INSTANCIA, quando 
		// essa informação estiver ausente na instância atual.
		int outraInstancia = grau == 1 ? 2 : 1;
		identificaGeneroPessoa = new IdentificaGeneroPessoa(outraInstancia);
		
		// Objeto que auxiliará na identificação dos documentos de identificação das pessoas
		identificaDocumentosPessoa = new IdentificaDocumentosPessoa(conexaoBasePrincipal);
		
		// SQL que fará a consulta de um processo
		String sqlConsultaProcessos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/01_consulta_processo.sql");
		nsConsultaProcessos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaProcessos, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);
		
		// SQL que fará a consulta de todos os polos
		String sqlConsultaPolos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/02_consulta_polos.sql");
		nsPolos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaPolos);		

		// SQL que fará a consulta das partes
		String sqlConsultaPartes = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/03_consulta_partes.sql");
		nsPartes = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaPartes);

		// SQL que fará a consulta dos endereços da parte
		String sqlConsultaEnderecos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/04_consulta_enderecos_pessoa.sql");
		nsEnderecos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaEnderecos);

		// SQL que fará a consulta dos assuntos do processo
		String sqlConsultaAssuntos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/05_consulta_assuntos.sql");
		nsAssuntos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaAssuntos);

		// SQL que fará a consulta dos movimentos processuais
		String sqlConsultaMovimentos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/06_consulta_movimentos.sql");
		nsMovimentos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaMovimentos);

		// Le o SQL que fará a consulta dos complementos dos movimentos processuais
		String sqlConsultaComplementos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/07_consulta_complementos.sql");
		nsComplementos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaComplementos);

		// O código IBGE do município onde fica o TRT vem do arquivo de configuração, já que será diferente para cada regional
		codigoMunicipioIBGETRT = Auxiliar.getParametroInteiroConfiguracao(Parametro.codigo_municipio_ibge_trt);
		
		// Objeto que identificará os assuntos e movimentos processuais das tabelas nacionais do CNJ
		analisaAssuntosCNJ = new AnalisaAssuntosCNJ(grau, conexaoBasePrincipal);
		analisaMovimentosCNJ = new AnalisaMovimentosCNJ(grau, conexaoBasePrincipal);
		analisaClassesProcessuaisCNJ = new AnalisaClassesProcessuaisCNJ(grau);
	}

	
	public void close() {

		// Fecha objeto que analisa os movimentos processuais do CNJ
		if (analisaMovimentosCNJ != null) {
			try {
				analisaMovimentosCNJ.close();
				analisaMovimentosCNJ = null;
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando 'analisaMovimentosCNJ': " + e.getLocalizedMessage(), e);
			}
		}
				
		// Fecha objeto que analisa os assuntos processuais do CNJ
		if (analisaAssuntosCNJ != null) {
			try {
				analisaAssuntosCNJ.close();
				analisaAssuntosCNJ = null;
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando 'analisaAssuntosCNJ': " + e.getLocalizedMessage(), e);
			}
		}
		
		// Fecha PreparedStatements
		if (nsConsultaProcessos != null) {
			try {
				nsConsultaProcessos.close();
				nsConsultaProcessos = null;
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando consulta 'nsConsultaProcessos': " + e.getLocalizedMessage(), e);
			}
		}
		if (nsPolos != null) {
			try {
				nsPolos.close();
				nsPolos = null;
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando consulta 'nsPolos': " + e.getLocalizedMessage(), e);
			}
		}
		if (nsPartes != null) {
			try {
				nsPartes.close();
				nsPartes = null;
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando consulta 'nsPartes': " + e.getLocalizedMessage(), e);
			}
		}
		if (nsEnderecos != null) {
			try {
				nsEnderecos.close();
				nsEnderecos = null;
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando consulta 'nsEnderecos': " + e.getLocalizedMessage(), e);
			}
		}
		if (nsAssuntos != null) {
			try {
				nsAssuntos.close();
				nsAssuntos = null;
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando consulta 'nsAssuntos': " + e.getLocalizedMessage(), e);
			}
		}
		if (nsMovimentos != null) {
			try {
				nsMovimentos.close();
				nsMovimentos = null;
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando consulta 'nsMovimentos': " + e.getLocalizedMessage(), e);
			}
		}
		if (nsComplementos != null) {
			try {
				nsComplementos.close();
				nsComplementos = null;
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando consulta 'nsComplementos': " + e.getLocalizedMessage(), e);
			}
		}

		if (identificaGeneroPessoa != null) {
			identificaGeneroPessoa.close();
			identificaGeneroPessoa = null;
		}
		if (identificaDocumentosPessoa != null) {
			identificaDocumentosPessoa.close();
			identificaDocumentosPessoa = null;
		}
		
		if (conexaoBasePrincipal != null) {
			try {
				conexaoBasePrincipal.close();
				conexaoBasePrincipal = null;
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando 'conexaoBasePrincipal': " + e.getLocalizedMessage(), e);
			}
		}
	}
}
