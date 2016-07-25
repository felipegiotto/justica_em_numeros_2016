package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.cnj.intercomunicacao_2_2.ModalidadeDocumentoIdentificador;
import br.jus.cnj.intercomunicacao_2_2.ModalidadeGeneroPessoa;
import br.jus.cnj.intercomunicacao_2_2.ModalidadePoloProcessual;
import br.jus.cnj.intercomunicacao_2_2.TipoAssuntoProcessual;
import br.jus.cnj.intercomunicacao_2_2.TipoCabecalhoProcesso;
import br.jus.cnj.intercomunicacao_2_2.TipoDocumentoIdentificacao;
import br.jus.cnj.intercomunicacao_2_2.TipoMovimentoNacional;
import br.jus.cnj.intercomunicacao_2_2.TipoMovimentoProcessual;
import br.jus.cnj.intercomunicacao_2_2.TipoOrgaoJulgador;
import br.jus.cnj.intercomunicacao_2_2.TipoParte;
import br.jus.cnj.intercomunicacao_2_2.TipoPessoa;
import br.jus.cnj.intercomunicacao_2_2.TipoPoloProcessual;
import br.jus.cnj.intercomunicacao_2_2.TipoProcessoJudicial;
import br.jus.cnj.intercomunicacao_2_2.TipoQualificacaoPessoa;
import br.jus.cnj.replicacao_nacional.ObjectFactory;
import br.jus.cnj.replicacao_nacional.Processos;
import br.jus.trt4.justica_em_numeros_2016.assuntos_cnj.AnalisaAssuntosCNJ;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.NamedParameterStatement;
import br.jus.trt4.justica_em_numeros_2016.serventias_cnj.ProcessaServentiasCNJ;
import br.jus.trt4.justica_em_numeros_2016.serventias_cnj.ServentiaCNJ;

/**
 * Carrega as listas de processos geradas pela classe {@link Op_1_BaixaListaDeNumerosDeProcessos} e,
 * para cada processo, gera seu arquivo XML na pasta "output/Xg/xmls_individuais/PJe".
 * 
 * Fonte: http://www.mkyong.com/java/jaxb-hello-world-example/
 * 
 * @author fgiotto
 */
public class Op_2_GeraXMLsIndividuais {

	private static final Logger LOGGER = LogManager.getLogger(Op_2_GeraXMLsIndividuais.class);
	private int grau;
	private Connection conexaoBasePrincipal;
	private NamedParameterStatement nsConsultaProcessos;
	private NamedParameterStatement nsPolos;
	private NamedParameterStatement nsPartes;
	private NamedParameterStatement nsDocumentos;
	private NamedParameterStatement nsAssuntos;
	private NamedParameterStatement nsMovimentos;
	private NamedParameterStatement nsComplementos;
	private int codigoMunicipioIBGETRT;
	private static ProcessaServentiasCNJ processaServentiasCNJ;
	private static Properties tiposDocumentosPJeCNJ;
	private AnalisaAssuntosCNJ analisaAssuntosCNJ;

	
	/**
	 * Gera todos os XMLs (1G e/ou 2G), conforme definido no arquivo "config.properties"
	 */
	public static void main(String[] args) throws SQLException, Exception {

		// Verifica se deve gerar XML para 1o Grau
		if (Auxiliar.getParametroBooleanConfiguracao("gerar_xml_1G")) {
			gerarXMLs(1);
		}
		
		// Verifica se deve gerar XML para 2o Grau
		if (Auxiliar.getParametroBooleanConfiguracao("gerar_xml_2G")) {
			gerarXMLs(2);
		}
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
		
		// Variável auxiliar para tentar calcular o tempo total da carga
		long inicio = System.currentTimeMillis();
		
		// Carrega a lista de processos que precisará ser analisada
		List<String> listaProcessos = carregarListaProcessosDoArquivo(Auxiliar.getArquivoListaProcessos(grau));
		int i=0;
		for (String numeroProcesso: listaProcessos) {

			// Arquivo XML que conterá os dados do processo
			File arquivoXML = new File("output/" + grau + "g/xmls_individuais/PJe/" + numeroProcesso + ".xml");
			
			// Calcula estatísticas do tempo restante
			long agora = System.currentTimeMillis();
			long tempoAteAgora = agora - inicio;
			long tempoPrevisto = 0;
			if (i > 0) {
				tempoPrevisto = (tempoAteAgora / i) * (listaProcessos.size() - i) / 1000;
			}
			LOGGER.debug("Baixando dados do processo " + numeroProcesso + " no arquivo " + arquivoXML + " (" + (++i) + "/" + listaProcessos.size() + (tempoPrevisto == 0 ? "" : ", restante: " + tempoPrevisto + "s") + ")");

			// Executa a consulta desse processo no banco de dados do PJe
			TipoProcessoJudicial processoJudicial = analisarProcessoJudicialCompleto(numeroProcesso);
			
			// Objeto que, de acordo com o padrão MNI, que contém uma lista de processos. 
			// Nesse caso, ele conterá somente UM processo. Posteriormente, os XMLs de cada
			// processo serão unificados, junto com os XMLs dos outros sistemas legados.
			Processos processos = factory.createProcessos();
			processos.getProcesso().add(processoJudicial);
			
			// Gera o arquivo XML
			arquivoXML.getParentFile().mkdirs();
			jaxbMarshaller.marshal(processos, arquivoXML);
		}
		
		LOGGER.info("Arquivos XML do " + grau + "o Grau gerado!");
	}

	
	/**
	 * Consulta os dados do processo informado no banco de dados e gera um objeto da classe
	 * {@link TipoProcessoJudicial}
	 * 
	 * @param numeroProcesso
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	public TipoProcessoJudicial analisarProcessoJudicialCompleto(String numeroProcesso) throws SQLException {
		
		nsConsultaProcessos.setString("numero_processo", numeroProcesso);
		try (ResultSet rsProcessos = nsConsultaProcessos.executeQuery()) {
			if (!rsProcessos.next()) {
				throw new RuntimeException("O processo " + numeroProcesso + " não foi encontrado na base " + grau + "G!");
			}
			
			return analisarProcessoJudicialCompleto(rsProcessos);
		}
	}

	
	/**
	 * Método criado com base no script recebido do TRT14
	 * para preencher os dados de um processo judicial dentro das classes que gerarão o XML.
	 * 
	 * @param processoJudicial
	 * @param rsProcesso
	 * @throws SQLException 
	 * @throws IOException 
	 */
	public TipoProcessoJudicial analisarProcessoJudicialCompleto(ResultSet rsProcesso) throws SQLException {

		// Objeto que será retornado
		TipoProcessoJudicial processoJudicial = new TipoProcessoJudicial();
		
		// Cabeçalho com dados básicos do processo:
		processoJudicial.setDadosBasicos(analisarCabecalhoProcesso(rsProcesso));

		// Movimentos processuais
		processoJudicial.getMovimento().addAll(analisarMovimentosProcesso(rsProcesso.getInt("id_processo_trf")));
		
		return processoJudicial;
	}

	
	private TipoCabecalhoProcesso analisarCabecalhoProcesso(ResultSet rsProcesso) throws SQLException {
		
		// Script TRT14:
		// raise notice '<dadosBasicos nivelSigilo="%" numero="%" classeProcessual="%" codigoLocalidade="%" dataAjuizamento="%">' 
		//  , proc.nivelSigilo, proc.nr_processo, proc.cd_classe_judicial, proc.id_municipio_ibge_origem, proc.dt_autuacao;
		TipoCabecalhoProcesso cabecalhoProcesso = new TipoCabecalhoProcesso();
		cabecalhoProcesso.setNivelSigilo(Auxiliar.getCampoIntNotNull(rsProcesso, "nivelSigilo"));
		cabecalhoProcesso.setNumero(Auxiliar.getCampoStringNotNull(rsProcesso, "nr_processo"));
		cabecalhoProcesso.setClasseProcessual(Auxiliar.getCampoIntNotNull(rsProcesso, "cd_classe_judicial"));
		cabecalhoProcesso.setDataAjuizamento(Auxiliar.getCampoStringNotNull(rsProcesso, "dt_autuacao"));
		cabecalhoProcesso.setValorCausa(Auxiliar.getCampoDoubleOrNull(rsProcesso, "vl_causa")); 
		if (grau == 1) {
			
			// Em 1G, pega como localidade o município do OJ do processo
			cabecalhoProcesso.setCodigoLocalidade(Auxiliar.getCampoStringNotNull(rsProcesso, "id_municipio_ibge_origem"));
		} else {
			
			// Em 2G, pega como localidade o município do TRT, que está definido no arquivo de configurações
			cabecalhoProcesso.setCodigoLocalidade(Integer.toString(codigoMunicipioIBGETRT));
		}

		// Consulta todos os polos do processo
		cabecalhoProcesso.getPolo().addAll(analisarPolosProcesso(rsProcesso.getInt("id_processo_trf")));

		// Consulta todos os assuntos desse processo
		cabecalhoProcesso.getAssunto().addAll(analisarAssuntosProcesso(rsProcesso.getInt("id_processo_trf")));

		// Preenche dados do órgão julgador do processo
		cabecalhoProcesso.setOrgaoJulgador(analisarOrgaoJulgadorProcesso(rsProcesso));
		
		return cabecalhoProcesso;
	}

	
	private List<TipoPoloProcessual> analisarPolosProcesso(int idProcesso) throws SQLException {
		
		List<TipoPoloProcessual> polos = new ArrayList<TipoPoloProcessual>();
		
		// Consulta todos os polos do processo
		nsPolos.setInt("id_processo", idProcesso);
		try (ResultSet rsPolos = nsPolos.executeQuery()) {
			while (rsPolos.next()) {

				// Script TRT14:
				// raise notice '<polo polo="%">', polo.in_polo_participacao;
				TipoPoloProcessual polo = new TipoPoloProcessual();
				polo.setPolo(ModalidadePoloProcessual.valueOf(rsPolos.getString("in_polo_participacao")));
				polos.add(polo);

				// Consulta as partes de um determinado polo no processo
				nsPartes.setInt("id_processo", idProcesso);
				nsPartes.setString("in_participacao", rsPolos.getString("in_participacao"));
				try (ResultSet rsPartes = nsPartes.executeQuery()) {
					while (rsPartes.next()) {

						// Script TRT14:
						// raise notice '<parte>';
						TipoParte parte = new TipoParte();
						polo.getParte().add(parte);

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
						String nomePessoa = Auxiliar.getCampoStringNotNull(rsPartes, "ds_nome");
						pessoa.setNome(nomePessoa);
						pessoa.setSexo(ModalidadeGeneroPessoa.valueOf(Auxiliar.getCampoStringNotNull(rsPartes, "tp_sexo")));
						
						// Tipo de pessoa (física / jurídica / outros)
						String tipoPessoaPJe = Auxiliar.getCampoStringNotNull(rsPartes, "in_tipo_pessoa");
						if ("F".equals(tipoPessoaPJe)) {
							pessoa.setTipoPessoa(TipoQualificacaoPessoa.FISICA);
						} else if ("J".equals(tipoPessoaPJe)) {
							pessoa.setTipoPessoa(TipoQualificacaoPessoa.JURIDICA);
						} else if ("A".equals(tipoPessoaPJe)) {
							pessoa.setTipoPessoa(TipoQualificacaoPessoa.AUTORIDADE);
						} else {
							LOGGER.warn("Tipo de pessoa desconhecido para '" + nomePessoa + "': " + tipoPessoaPJe);
							pessoa.setTipoPessoa(TipoQualificacaoPessoa.FISICA);
						}
						
						// Consulta os documentos da parte
						nsDocumentos.setInt("id_pessoa", rsPartes.getInt("id_pessoa"));
						try (ResultSet rsDocumentos = nsDocumentos.executeQuery()) {
							
							while (rsDocumentos.next()) {

								// Considera CPF, CNPJ e RIC como documentos principais da pessoa, que ficam em um campo separado
								// (fora da lista de documentos)
								String tipoDocumentoPJe = Auxiliar.getCampoStringNotNull(rsDocumentos, "cd_tp_documento_identificacao").trim();
								String numeroDocumento = Auxiliar.getCampoStringNotNull(rsDocumentos, "nr_documento");
								if (tipoDocumentoPJe.equals("CPF") || tipoDocumentoPJe.equals("CPJ") || tipoDocumentoPJe.equals("RIC")) {
									
									if (tipoDocumentoPJe.equals("CPF")) {
										numeroDocumento = StringUtils.leftPad(numeroDocumento, 11, '0');
									} else {
										numeroDocumento = StringUtils.leftPad(numeroDocumento, 14, '0');
									}
									pessoa.setNumeroDocumentoPrincipal(numeroDocumento);
								} else {
									
									// Script TRT14:
									// raise notice '<documento codigoDocumento="%" tipoDocumento="%" emissorDocumento="%" />'
									//  , documento.nr_documento, documento.tp_documento, documento.ds_emissor;
									if (tiposDocumentosPJeCNJ.containsKey(tipoDocumentoPJe)) {
										TipoDocumentoIdentificacao documento = new TipoDocumentoIdentificacao();
										documento.setCodigoDocumento(numeroDocumento);
										documento.setEmissorDocumento(Auxiliar.getCampoStringNotNull(rsDocumentos, "ds_emissor"));
										
										// Carrega o tipo de documento do CNJ a partir do tipo do PJe
										documento.setTipoDocumento(ModalidadeDocumentoIdentificador.fromValue(tiposDocumentosPJeCNJ.getProperty(tipoDocumentoPJe)));
										
										// Nome do documento, conforme documentação do XSD:
										// Nome existente no documento. Deve ser utilizado apenas se existente nome diverso daquele ordinariamente usado.
										String nomePessoaDocumento = rsDocumentos.getString("ds_nome_pessoa");
										if (!pessoa.getNome().equals(nomePessoaDocumento)) {
											documento.setNome(nomePessoaDocumento);
										}
										
										pessoa.getDocumento().add(documento);
										
									} else {
										LOGGER.warn("Documento do tipo '" + tipoDocumentoPJe + "' não possui correspondente na tabela do CNJ!");
									}
								}
							}
						}
					}
				}
			}
		}
		return polos;
	}

	
	private List<TipoAssuntoProcessual> analisarAssuntosProcesso(int idProcesso) throws SQLException {
		
		List<TipoAssuntoProcessual> assuntos = new ArrayList<TipoAssuntoProcessual>();
		
		// Consulta todos os assuntos do processo
		nsAssuntos.setInt("id_processo", idProcesso);
		try (ResultSet rsAssuntos = nsAssuntos.executeQuery()) {
			boolean jaEncontrouAssunto = false;
			boolean jaEncontrouAssuntoPrincipal = false;
			while (rsAssuntos.next()) {

				// Script TRT14:
				// raise notice '<assunto>'; -- principal="%">', assunto.in_assunto_principal;
				// raise notice '<codigoNacional>%</codigoNacional>', assunto.cd_assunto_trf;
				// raise notice '</assunto>';
				
				// Analisa o assunto, que pode ou não estar nas tabelas processuais unificadas do CNJ.
				int codigo = Auxiliar.getCampoIntNotNull(rsAssuntos, "cd_assunto_trf");
				TipoAssuntoProcessual assunto = analisaAssuntosCNJ.getAssunto(codigo);
				assuntos.add(assunto);

				// Trata o campo "assunto principal", verificando também se há mais de um assunto principal no processo.
				boolean assuntoPrincipal = "S".equals(rsAssuntos.getString("in_assunto_principal"));
				assunto.setPrincipal(assuntoPrincipal);
				if (assuntoPrincipal) {
					if (jaEncontrouAssuntoPrincipal) {
						LOGGER.warn("Este processo possui mais de um assunto principal!");
					} else {
						jaEncontrouAssuntoPrincipal = true;
					}
				}
				
				jaEncontrouAssunto = true;
			}
			
			// Script TRT14:
			// -- se não tiver assunto? 
			// IF fl_assunto = 0 THEN 
			//   -- do something...
			//   raise notice '<assunto>'; 
			//   raise notice '<codigoNacional>2546</codigoNacional>'; -- Verbas Rescisórias
			//   raise notice '</assunto>';
			// END IF;
			if (!jaEncontrouAssunto) {
				LOGGER.warn("Processo sem assunto cadastrado! Decidir o que fazer!");
			} else if (!jaEncontrouAssuntoPrincipal) {
				LOGGER.warn("Processo sem assunto principal!");
			}
		}
		
		return assuntos;
	}

	
	private TipoOrgaoJulgador analisarOrgaoJulgadorProcesso(ResultSet rsProcesso) throws SQLException {
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
		ServentiaCNJ serventiaCNJ = processaServentiasCNJ.getServentiaByOJ(rsProcesso.getString("ds_orgao_julgador"));
		TipoOrgaoJulgador orgaoJulgador = new TipoOrgaoJulgador();
		orgaoJulgador.setCodigoOrgao(serventiaCNJ.getCodigo());
		orgaoJulgador.setNomeOrgao(serventiaCNJ.getNome());
		if (grau == 1) {
			
			// Em 1G, pega como localidade do OJ o município do OJ do processo
			orgaoJulgador.setCodigoMunicipioIBGE(Auxiliar.getCampoIntNotNull(rsProcesso, "id_municipio_ibge_atual"));
			
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
				TipoMovimentoProcessual movimento = new TipoMovimentoProcessual();
				movimento.setDataHora(rsMovimentos.getString("dta_ocorrencia"));
				movimento.setNivelSigilo(rsMovimentos.getInt("in_visibilidade_externa"));
				movimentos.add(movimento);

				// Script TRT14:
				// raise notice '<movimentoNacional codigoNacional="%">', mov.cd_movimento_cnj;
				TipoMovimentoNacional movimentoNacional = new TipoMovimentoNacional();
				movimentoNacional.setCodigoNacional(Auxiliar.getCampoIntNotNull(rsMovimentos, "cd_movimento_cnj"));
				movimento.setMovimentoNacional(movimentoNacional);

				// Consulta os complementos desse movimento processual
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
						boolean existeCodigoComplemento = !"".equals(rsComplementos.getString("cd_complemento").trim());
						if (existeCodigoComplemento) {
							sb.append(":");
							sb.append(rsComplementos.getString("cd_complemento"));
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
						movimento.getComplemento().add(sb.toString());
					}
				}	
			}
		}
		
		return movimentos;
	}

	
	public void prepararConexao() throws SQLException, IOException {

		LOGGER.info("Preparando informações para gerar XMLs do " + grau + "o Grau...");
		
		// Objeto que fará o de/para dos OJ e OJC do PJe para os do CNJ
		if (processaServentiasCNJ == null) {
			processaServentiasCNJ = new ProcessaServentiasCNJ();
		}
		
		// Objeto que fará o de/para dos tipos de documentos do PJe para os do CNJ
		if (tiposDocumentosPJeCNJ == null) {
			tiposDocumentosPJeCNJ = Auxiliar.carregarPropertiesDoArquivo(new File("src/main/resources/tipos_de_documentos.properties"));
		}
		
		// Abre conexão com o banco de dados do PJe
		conexaoBasePrincipal = Auxiliar.getConexaoPJe(grau);
		conexaoBasePrincipal.setAutoCommit(false);

		// SQL que fará a consulta de um processo
		String sqlConsultaProcessos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/01_consulta_processo.sql");
		nsConsultaProcessos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaProcessos, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD);
		
		// SQL que fará a consulta de todos os polos
		String sqlConsultaPolos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/02_consulta_polos.sql");
		nsPolos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaPolos);		

		// SQL que fará a consulta das partes
		String sqlConsultaPartes = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/03_consulta_partes.sql");
		nsPartes = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaPartes);

		// SQL que fará a consulta dos documentos da pessoa
		String sqlConsultaDocumentos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/04_consulta_documentos_pessoa.sql");
		nsDocumentos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaDocumentos);

		// SQL que fará a consulta dos assuntos do processo
		String sqlConsultaAssuntos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/05_consulta_assuntos.sql");
		nsAssuntos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaAssuntos);

		// SQL que fará a consulta dos movimentos processuais
		String sqlConsultaMovimentos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/06_consulta_movimentos.sql");
		nsMovimentos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaMovimentos);

		// Le o SQL que fará a consulta dos complementos dos movimentos processuais
		String sqlConsultaComplementos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/07_consulta_complementos.sql");
		nsComplementos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaComplementos);

		// O código IBGE do município onde fica o TRT vem do arquivo de configuração, já que será diferente para cada regional
		codigoMunicipioIBGETRT = Auxiliar.getParametroInteiroConfiguracao("codigo_municipio_ibge_trt");
		
		// Objeto que identificará os assuntos processuais das tabelas nacionais do CNJ
		analisaAssuntosCNJ = new AnalisaAssuntosCNJ(grau, conexaoBasePrincipal);
	}

	
	public void close() {

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
		if (nsDocumentos != null) {
			try {
				nsDocumentos.close();
				nsDocumentos = null;
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando consulta 'nsDocumentos': " + e.getLocalizedMessage(), e);
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

		// Fecha conexão com o PJe
		if (conexaoBasePrincipal != null) {
			try {
				conexaoBasePrincipal.close();
				conexaoBasePrincipal = null;
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando conexão com o PJe: " + e.getLocalizedMessage(), e);
			}
		}
	}
	
	
	public static List<String> carregarListaProcessosDoArquivo(File arquivoEntrada) throws IOException {
		List<String> listaProcessos = FileUtils.readLines(arquivoEntrada, "UTF-8");
		LOGGER.info("Arquivo '" + arquivoEntrada + "' carregado com " + listaProcessos.size() + " processo(s).");
		return listaProcessos;
	}
}
