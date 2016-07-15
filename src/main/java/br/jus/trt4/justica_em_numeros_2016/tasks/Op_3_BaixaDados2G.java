package br.jus.trt4.justica_em_numeros_2016.tasks;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

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
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.NamedParameterStatement;

/**
 * Consulta os processos no banco de dados de 2o Grau e gera o arquivo XML em "output/dados_2g.xml".
 * 
 * Fonte: http://www.mkyong.com/java/jaxb-hello-world-example/
 * 
 * @author fgiotto
 */
public class Op_3_BaixaDados2G {

	private static final File arquivoSaida = new File("output/dados_2g.xml");

	private static final Logger LOGGER = LogManager.getLogger(Op_3_BaixaDados2G.class);
	private Connection conexaoBasePrincipal;
	private NamedParameterStatement nsConsultaProcessos;
	private NamedParameterStatement nsPolos;
	private NamedParameterStatement nsPartes;
	private NamedParameterStatement nsDocumentos;
	private NamedParameterStatement nsAssuntos;
	private NamedParameterStatement nsMovimentos;
	private NamedParameterStatement nsComplementos;

	public static void main(String[] args) throws SQLException, Exception {

		Op_3_BaixaDados2G baixaDados = new Op_3_BaixaDados2G();
		try {
			baixaDados.init();
			baixaDados.gerarXML();
		} finally {
			baixaDados.close();
		}
	}

	private void gerarXML() throws IOException, SQLException, JAXBException {

		// Objetos auxiliares para gerar o XML
		ObjectFactory factory = new ObjectFactory();
		Processos processos = factory.createProcessos();

		nsConsultaProcessos.setString("cd_municipio_ibge_trt", Auxiliar.getParametroConfiguracao("codigo_municipio_ibge_trt", true));

		// Executa a consulta no banco de dados do PJe
		LOGGER.info("Executando consulta no banco de dados...");
		try (ResultSet rsProcessos = nsConsultaProcessos.executeQuery()) {
			int i=0;
			while (rsProcessos.next()) {
				String numeroCompletoProcesso = rsProcessos.getString("numero_completo_processo");
				LOGGER.debug("Analisando processo " + numeroCompletoProcesso + " (" + (++i) + ")");

				TipoProcessoJudicial processoJudicial = new TipoProcessoJudicial();
				preencheProcessoJudicialComBaseEmScriptDoTRT14(processoJudicial, rsProcessos);
				processos.getProcesso().add(processoJudicial);
			}
		}

		// Arquivo onde o XML será gravado
		arquivoSaida.getParentFile().mkdirs();

		// Objetos auxiliares para gerar o XML
		JAXBContext context = JAXBContext.newInstance(Processos.class);
		Marshaller jaxbMarshaller = context.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		LOGGER.info("Gerando arquivo XML: " + arquivoSaida + "...");
		jaxbMarshaller.marshal(processos, arquivoSaida);
		LOGGER.info("Arquivo XML gerado!");
	}

	/**
	 * Método criado com base no script recebido do TRT14 (Felypp De Assis Oliveira)
	 * para preencher os dados de um processo judicial dentro das classes que gerarão o XML.
	 * 
	 * @param processoJudicial
	 * @param rsProcesso
	 * @throws SQLException 
	 * @throws IOException 
	 */
	private void preencheProcessoJudicialComBaseEmScriptDoTRT14(TipoProcessoJudicial processoJudicial, ResultSet rsProcesso) throws SQLException, IOException {

		// Cabeçalho com dados básicos do processo:
		TipoCabecalhoProcesso cabecalhoProcesso = new TipoCabecalhoProcesso();
		processoJudicial.setDadosBasicos(cabecalhoProcesso);

		// Script TRT14:
		// raise notice '<dadosBasicos nivelSigilo="%" numero="%" classeProcessual="%" codigoLocalidade="%" dataAjuizamento="%">' 
		//  , proc.nivelSigilo, proc.nr_processo, proc.cd_classe_judicial, proc.id_municipio_ibge_origem, proc.dt_autuacao;
		cabecalhoProcesso.setNivelSigilo(Auxiliar.getCampoIntNotNull(rsProcesso, "nivelSigilo"));
		cabecalhoProcesso.setNumero(Auxiliar.getCampoStringNotNull(rsProcesso, "nr_processo"));
		cabecalhoProcesso.setClasseProcessual(Auxiliar.getCampoIntNotNull(rsProcesso, "cd_classe_judicial"));
		cabecalhoProcesso.setCodigoLocalidade(Auxiliar.getCampoStringNotNull(rsProcesso, "id_municipio_ibge_origem"));
		cabecalhoProcesso.setDataAjuizamento(Auxiliar.getCampoStringNotNull(rsProcesso, "dt_autuacao"));

		nsPolos.setInt("id_processo", rsProcesso.getInt("id_processo_trf"));

		try (ResultSet rsPolos = nsPolos.executeQuery()) {
			while (rsPolos.next()) {

				// Script TRT14:
				// raise notice '<polo polo="%">', polo.in_polo_participacao;
				// TODO: IMPORTANTE: não estão sendo tratados os casos em que a parte é considerada um interesse público abstrato cuja defesa está a cargo do Ministério Público ou da Defensoria Pública
				// TODO: IMPORTANTE: não estão sendo tratados os casos de representação e assistência dos pais, representação ou substituição processual em ações coletivas, tutela e curatela
				// TODO: IMPORTANTE: não está sendo preenchido o elemento opcional 'advogado'
				TipoPoloProcessual polo = new TipoPoloProcessual();
				polo.setPolo(ModalidadePoloProcessual.valueOf(rsPolos.getString("in_polo_participacao")));
				cabecalhoProcesso.getPolo().add(polo);

				nsPartes.setInt("id_processo", rsProcesso.getInt("id_processo_trf"));
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
						pessoa.setNome(rsPartes.getString("ds_nome"));
						pessoa.setTipoPessoa(TipoQualificacaoPessoa.fromValue(rsPartes.getString("in_tipo_pessoa")));
						pessoa.setSexo(ModalidadeGeneroPessoa.valueOf(rsPartes.getString("tp_sexo")));
						pessoa.setNumeroDocumentoPrincipal(rsPartes.getString("nr_documento"));

						nsDocumentos.setInt("id_pessoa", rsPartes.getInt("id_pessoa"));
						try (ResultSet rsDocumentos = nsDocumentos.executeQuery()) {
							while (rsDocumentos.next()) {

								// Script TRT14:
								// raise notice '<documento codigoDocumento="%" tipoDocumento="%" emissorDocumento="%" />'
								//  , documento.nr_documento, documento.tp_documento, documento.ds_emissor;
								TipoDocumentoIdentificacao documento = new TipoDocumentoIdentificacao();
								documento.setCodigoDocumento(rsDocumentos.getString("nr_documento"));
								documento.setTipoDocumento(ModalidadeDocumentoIdentificador.fromValue(rsDocumentos.getString("tp_documento")));
								documento.setEmissorDocumento(rsDocumentos.getString("ds_emissor"));
								pessoa.getDocumento().add(documento);
							}
						}
					}
				}
			}
		}

		nsAssuntos.setInt("id_processo", rsProcesso.getInt("id_processo_trf"));
		try (ResultSet rsAssuntos = nsAssuntos.executeQuery()) {
			boolean encontrouAssunto = false;
			while (rsAssuntos.next()) {

				// Script TRT14:
				// raise notice '<assunto>'; -- principal="%">', assunto.in_assunto_principal;
				// raise notice '<codigoNacional>%</codigoNacional>', assunto.cd_assunto_trf;
				// raise notice '</assunto>';
				TipoAssuntoProcessual assunto = new TipoAssuntoProcessual();
				assunto.setCodigoNacional(rsAssuntos.getInt("cd_assunto_trf"));
				cabecalhoProcesso.getAssunto().add(assunto);

				encontrouAssunto = true;
			}

			// Script TRT14:
			// -- se não tiver assunto? 
			// IF fl_assunto = 0 THEN 
			//   -- do something...
			//   raise notice '<assunto>'; 
			//   raise notice '<codigoNacional>2546</codigoNacional>'; -- Verbas Rescisórias
			//   raise notice '</assunto>';
			// END IF;				
			if (!encontrouAssunto) {
				// TODO: Decidir o que fazer quando não tiver assunto!
				LOGGER.warn("Processo sem assunto cadastrado! Decidir o que fazer!");
			}
		}


		// Script TRT14:
		// -- orgaoJulgador
		// raise notice '<orgaoJulgador codigoOrgao="%" nomeOrgao="%" instancia="%" codigoMunicipioIBGE="%"/>' -- codigoMunicipioIBGE="1100205" -- <=== 2º grau!!!
		//   , proc.ds_sigla, proc.ds_orgao_julgador, proc.tp_instancia, proc.id_municipio_ibge_atual;
		TipoOrgaoJulgador orgaoJulgador = new TipoOrgaoJulgador();
		//		orgaoJulgador.setCodigoOrgao(rsProcesso.getString("ds_sigla")); // TODO: Falta definir origem do campo "CodigoOrgao"!
		//		orgaoJulgador.setNomeOrgao(rsProcesso.getString("ds_orgao_julgador")); // TODO: Falta definir origem do campo "NomeOrgao"!
		orgaoJulgador.setInstancia(rsProcesso.getString("tp_instancia"));
		orgaoJulgador.setCodigoMunicipioIBGE(rsProcesso.getInt("id_municipio_ibge_atual"));
		cabecalhoProcesso.setOrgaoJulgador(orgaoJulgador);


		nsMovimentos.setInt("id_processo", rsProcesso.getInt("id_processo_trf"));
		try (ResultSet rsMovimentos = nsMovimentos.executeQuery()) {
			while (rsMovimentos.next()) {

				// Script TRT14:
				// raise notice '<movimento dataHora="%" nivelSigilo="%">', mov.dta_ocorrencia, mov.in_visibilidade_externa;
				TipoMovimentoProcessual movimento = new TipoMovimentoProcessual();
				movimento.setDataHora(rsMovimentos.getString("dta_ocorrencia"));
				movimento.setNivelSigilo(rsMovimentos.getInt("in_visibilidade_externa"));
				processoJudicial.getMovimento().add(movimento);

				// Script TRT14:
				// raise notice '<movimentoNacional codigoNacional="%">', mov.cd_movimento_cnj;
				TipoMovimentoNacional movimentoNacional = new TipoMovimentoNacional();
				movimentoNacional.setCodigoNacional(rsMovimentos.getInt("cd_movimento_cnj"));
				movimento.setMovimentoNacional(movimentoNacional);


				nsComplementos.setInt("id_processo_evento", rsMovimentos.getInt("id_processo_evento"));
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
						if (!"".equals(rsComplementos.getString("cd_complemento").trim())) { // TODO: Conferir se o cd_complemento realmente vem antes do nm_complemento
							sb.append(":");
							sb.append(rsComplementos.getString("cd_complemento"));
						}
						sb.append(":");
						sb.append(rsComplementos.getString("nm_complemento"));
						movimento.getComplemento().add(sb.toString());
					}
				}	
			}
		}
	}

	private void init() throws SQLException, IOException {

		// Abre conexão com o banco de dados do PJe
		conexaoBasePrincipal = Auxiliar.getConexaoPJe2G();

		// SQL que fará a consulta de todos os processos
		String sqlConsultaProcessos = Auxiliar.lerConteudoDeArquivo("src/main/resources/baixa_dados_2g/01_consulta_processos.sql");

		// Em ambiente de testes, processa somente um lote menor, para ficar mais rápido
		if (Auxiliar.getParametroBooleanConfiguracao("testar_com_lote_pequeno")) {
			LOGGER.warn(">>>>>>>>>> CUIDADO! Somente uma fração dos dados estão sendo carregados, para testes! <<<<<<<<<<");
			sqlConsultaProcessos += " LIMIT 30";
		}

		// PreparedStatement que fará a consulta de todos os processos
		nsConsultaProcessos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaProcessos);

		// SQL que fará a consulta de todos os polos
		String sqlConsultaPolos = Auxiliar.lerConteudoDeArquivo("src/main/resources/baixa_dados_2g/02_consulta_polos.sql");
		nsPolos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaPolos);		

		// SQL que fará a consulta das partes
		String sqlConsultaPartes = Auxiliar.lerConteudoDeArquivo("src/main/resources/baixa_dados_2g/03_consulta_partes.sql");
		nsPartes = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaPartes);

		// SQL que fará a consulta dos documentos da pessoa
		String sqlConsultaDocumentos = Auxiliar.lerConteudoDeArquivo("src/main/resources/baixa_dados_2g/04_consulta_documentos_pessoa.sql");
		nsDocumentos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaDocumentos);

		// SQL que fará a consulta dos assuntos do processo
		String sqlConsultaAssuntos = Auxiliar.lerConteudoDeArquivo("src/main/resources/baixa_dados_2g/05_consulta_assuntos.sql");
		nsAssuntos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaAssuntos);

		// SQL que fará a consulta dos movimentos processuais
		String sqlConsultaMovimentos = Auxiliar.lerConteudoDeArquivo("src/main/resources/baixa_dados_2g/06_consulta_movimentos.sql");
		nsMovimentos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaMovimentos);

		// Le o SQL que fará a consulta dos complementos dos movimentos processuais
		String sqlConsultaComplementos = Auxiliar.lerConteudoDeArquivo("src/main/resources/baixa_dados_2g/07_consulta_complementos.sql");
		nsComplementos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaComplementos);
	}

	private void close() {

		// Fecha PreparedStatements
		if (nsConsultaProcessos != null) {
			try {
				nsConsultaProcessos.close();
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando consulta 'nsConsultaProcessos': " + e.getLocalizedMessage(), e);
			}
		}
		if (nsPolos != null) {
			try {
				nsPolos.close();
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando consulta 'nsPolos': " + e.getLocalizedMessage(), e);
			}
		}
		if (nsPartes != null) {
			try {
				nsPartes.close();
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando consulta 'nsPartes': " + e.getLocalizedMessage(), e);
			}
		}
		if (nsDocumentos != null) {
			try {
				nsDocumentos.close();
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando consulta 'nsDocumentos': " + e.getLocalizedMessage(), e);
			}
		}
		if (nsAssuntos != null) {
			try {
				nsAssuntos.close();
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando consulta 'nsAssuntos': " + e.getLocalizedMessage(), e);
			}
		}
		if (nsMovimentos != null) {
			try {
				nsMovimentos.close();
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando consulta 'nsMovimentos': " + e.getLocalizedMessage(), e);
			}
		}
		if (nsComplementos != null) {
			try {
				nsComplementos.close();
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando consulta 'nsComplementos': " + e.getLocalizedMessage(), e);
			}
		}

		// Fecha conexão com o PJe
		if (conexaoBasePrincipal != null) {
			try {
				conexaoBasePrincipal.close();
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando conexão com o PJe: " + e.getLocalizedMessage(), e);
			}
		}
	}
}
