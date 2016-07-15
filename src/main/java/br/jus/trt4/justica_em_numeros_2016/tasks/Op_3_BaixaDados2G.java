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
	private int codigoMunicipioIBGETRT;

	public static void main(String[] args) throws SQLException, Exception {

		Op_3_BaixaDados2G baixaDados = new Op_3_BaixaDados2G();
		try {

			// Abre conexões com o PJe e prepara consultas a serem realizadas
			baixaDados.prepararConexao();

			// Executa consultas e grava arquivo XML
			baixaDados.gerarXML();
		} finally {
			baixaDados.close();
		}
	}

	private void gerarXML() throws IOException, SQLException, JAXBException {

		// TODO: revisar todos os campos que possam estar gravando NULL nos atributos. Verificar se será necessário utilizar o Auxiliar.getIntNotNull ou equivalente.

		// Objetos auxiliares para gerar o XML
		ObjectFactory factory = new ObjectFactory();
		JAXBContext context = JAXBContext.newInstance(Processos.class);
		Marshaller jaxbMarshaller = context.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		Processos processos = factory.createProcessos();

		// TODO: Verificar se CNJ irá disponibilizar ferramenta para extrair dados do PJe:
		// Para extrair os dados do PJE ou MNI, o CNJ disponibilizará um programa capaz de gerar as informações segundo o modelo MNI. Para tanto, o tribunal deverá prover listagens com os números dos processos, separados entre 2º grau, 1º grau, Juizados Especiais e Turmas Recursais.
		// Fonte: http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/2016-06-02-17-51-25

		// Executa a consulta no banco de dados do PJe
		// TODO: Restringir a consulta de processos, conforme regra do CNJ:
		// * Para a carga completa devem ser encaminhados a totalidade dos processos em tramitação em 
		//   31 de julho de 2016, bem como daqueles que foram baixados de 1° de janeiro de 2015 até 31 de julho de 2016. 
		// * Para a carga mensal devem ser transmitidos os processos que tiveram movimentação ou alguma atualização 
		// no mês de agosto de 2016, com todos os dados e movimentos dos respectivos processos, de forma a evitar 
		// perda de algum tipo de informação.
		// Fonte: http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/2016-06-02-17-51-25
		// 3. Quais processos devem ser enviados? 
		// R: Todos os processos em tramitação na data de 31/07/2016 e todos os baixados no período de 01/01/2016 a 31/07/2016 deverão ser encaminhados, com todas os movimentos desde o seu ajuizamento.
		// 4. E nas cargas mensais? 
		// R: Para a carga mensal devem ser transmitidos os processos que tiveram movimentação ou alguma atualização no mês de agosto de 2016, com todos os dados e movimentos destes processos, desde o seu início, de forma a evitar perda de alguma informação em relação ao processo.		
		// Fonte: http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/perguntas-frequentes
		LOGGER.info("Executando consulta no banco de dados...");
		try (ResultSet rsProcessos = nsConsultaProcessos.executeQuery()) {
			int i=0;
			while (rsProcessos.next()) {
				String numeroCompletoProcesso = rsProcessos.getString("numero_completo_processo");
				LOGGER.debug("Analisando processo " + numeroCompletoProcesso + " (" + (++i) + ")");

				TipoProcessoJudicial processoJudicial = new TipoProcessoJudicial();
				preencheDadosProcesso(processoJudicial, rsProcessos);
				processos.getProcesso().add(processoJudicial);
			}
		}

		// Arquivo onde o XML será gravado
		arquivoSaida.getParentFile().mkdirs();

		// Gera o arquivo XML
		LOGGER.info("Gerando arquivo XML: " + arquivoSaida + "...");
		jaxbMarshaller.marshal(processos, arquivoSaida);
		LOGGER.info("Arquivo XML gerado!");
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
	private void preencheDadosProcesso(TipoProcessoJudicial processoJudicial, ResultSet rsProcesso) throws SQLException, IOException {

		// Cabeçalho com dados básicos do processo:
		TipoCabecalhoProcesso cabecalhoProcesso = new TipoCabecalhoProcesso();
		processoJudicial.setDadosBasicos(cabecalhoProcesso);

		// Script TRT14:
		// raise notice '<dadosBasicos nivelSigilo="%" numero="%" classeProcessual="%" codigoLocalidade="%" dataAjuizamento="%">' 
		//  , proc.nivelSigilo, proc.nr_processo, proc.cd_classe_judicial, proc.id_municipio_ibge_origem, proc.dt_autuacao;
		cabecalhoProcesso.setNivelSigilo(Auxiliar.getCampoIntNotNull(rsProcesso, "nivelSigilo"));
		cabecalhoProcesso.setNumero(Auxiliar.getCampoStringNotNull(rsProcesso, "nr_processo"));
		cabecalhoProcesso.setClasseProcessual(Auxiliar.getCampoIntNotNull(rsProcesso, "cd_classe_judicial"));
		cabecalhoProcesso.setCodigoLocalidade(Integer.toString(codigoMunicipioIBGETRT));
		cabecalhoProcesso.setDataAjuizamento(Auxiliar.getCampoStringNotNull(rsProcesso, "dt_autuacao"));

		// Consulta todos os polos do processo
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

				// Consulta as partes de um determinado polo no processo
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

						// Consulta os documentos da parte
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

		// Consulta os assuntos desse processo
		nsAssuntos.setInt("id_processo", rsProcesso.getInt("id_processo_trf"));
		try (ResultSet rsAssuntos = nsAssuntos.executeQuery()) {
			boolean encontrouAssunto = false;
			while (rsAssuntos.next()) {

				// Script TRT14:
				// raise notice '<assunto>'; -- principal="%">', assunto.in_assunto_principal;
				// raise notice '<codigoNacional>%</codigoNacional>', assunto.cd_assunto_trf;
				// raise notice '</assunto>';
				TipoAssuntoProcessual assunto = new TipoAssuntoProcessual();
				assunto.setCodigoNacional(Auxiliar.getCampoIntNotNull(rsAssuntos, "cd_assunto_trf"));
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

		// TODO: Campo "Competência"
		/*
		11. Como preencher o campo “Competência”? 
			    R: Os tribunais poderão utilizar seus próprios descritivos internos.
			    Fonte: http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/perguntas-frequentes
		 */		

		// Órgão julgador:
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
		TipoOrgaoJulgador orgaoJulgador = new TipoOrgaoJulgador();
		//		orgaoJulgador.setCodigoOrgao(rsProcesso.getString("ds_sigla")); // TODO: Falta definir origem do campo "CodigoOrgao"!
		//		orgaoJulgador.setNomeOrgao(rsProcesso.getString("ds_orgao_julgador")); // TODO: Falta definir origem do campo "NomeOrgao"!
		orgaoJulgador.setInstancia(rsProcesso.getString("tp_instancia"));
		orgaoJulgador.setCodigoMunicipioIBGE(codigoMunicipioIBGETRT);
		cabecalhoProcesso.setOrgaoJulgador(orgaoJulgador);

		// Consulta os movimentos processuais desse processo
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

				// Consulta os complementos desse movimento processual
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
						} else {
							// TODO: Verificar complemento de movimento sem cd_complemento!
							// Fonte: http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/2016-06-02-17-51-25
							/*
								5. Como devo informar o complemento de um movimento, visto que ele é composto de duas informações e o modelo só tem um campo? 

								    R: O complemento é composto pelo seu código e descrição, do complemento e do complemento tabela deverá ser colocado no formato:

								    <código do complemento><”:”><descrição do complemento><”:”><código do complemento tabelado><descrição do complemento tabelado, ou de texto livre, conforme o caso>							 * 
							 */
							// Fonte: http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/perguntas-frequentes
							//LOGGER.warn("Verificar complemento de movimento sem cd_complemento!");
						}
						sb.append(":");
						sb.append(rsComplementos.getString("nm_complemento"));
						movimento.getComplemento().add(sb.toString());
					}
				}	
			}
		}
	}

	private void prepararConexao() throws SQLException, IOException {

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

		// O código IBGE do município onde fica o TRT vem do arquivo de configuração, já que será diferente para cada regional
		codigoMunicipioIBGETRT = Auxiliar.getParametroInteiroConfiguracao("codigo_municipio_ibge_trt");		
	}

	private void close() {

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
}
