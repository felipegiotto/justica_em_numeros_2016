package br.jus.trt4.justica_em_numeros_2016.tabelas_cnj;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.cnj.intercomunicacao_2_2.TipoAssuntoLocal;
import br.jus.cnj.intercomunicacao_2_2.TipoAssuntoProcessual;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DadosInvalidosException;

/**
 * Classe que montará um objeto do tipo {@link TipoAssuntoProcessual}, conforme o dado no PJe:
 * 
 * Se o assunto estiver na lista das tabelas nacionais do CNJ (que foi extraída do site
 * http://www.cnj.jus.br/sgt/versoes.php?tipo_tabela=A e gravada nos arquivos 
 * "src/main/resources/tabelas_cnj/assuntos_*"), gerará um assunto que possui somente
 * o campo "<codigoNacional>".
 * 
 * Se o assunto NÃO estiver na lista das tabelas nacionais do CNJ, gera um objeto "<assuntoLocal>"
 * com os dados existentes no PJe, procurando ainda, de forma recursiva, algum assunto "pai" que
 * esteja nas tabelas nacionais, preenchendo o atributo "codigoPaiNacional".
 * 
 * @author fgiotto
 */
public class AnalisaAssuntosCNJ implements AutoCloseable {

	private static final Logger LOGGER = LogManager.getLogger(AnalisaAssuntosCNJ.class);
	private List<Integer> assuntosProcessuaisCNJ;
	private PreparedStatement psConsultaAssuntoPorCodigo;
	private PreparedStatement psConsultaAssuntoPorID;
	private TipoAssuntoProcessual assuntoProcessualPadrao;
	
	public AnalisaAssuntosCNJ(int grau, Connection conexaoPJe) throws IOException, SQLException, DadosInvalidosException {
		super();
		
		File arquivoAssuntos = new File("src/main/resources/tabelas_cnj/" + getNomeArquivoAssuntos(grau));
		LOGGER.info("Carregando lista de assuntos CNJ do arquivo " + arquivoAssuntos + "...");
		
		// Lista de assuntos processuais unificados, do CNJ. Essa lista definirá se o assunto do processo
		// deverá ser registrado com as tags "<assunto><codigoNacional>" ou "<assunto><assuntoLocal>"
		// Fonte: http://www.cnj.jus.br/sgt/versoes.php?tipo_tabela=A
		this.assuntosProcessuaisCNJ = new ArrayList<>();
		for (String assuntoString: FileUtils.readLines(arquivoAssuntos, "UTF-8")) {
			assuntosProcessuaisCNJ.add(Integer.parseInt(assuntoString));
		}
		
		// PreparedStatements que localizarão assuntos no banco de dados do PJe
		this.psConsultaAssuntoPorCodigo = conexaoPJe.prepareStatement("SELECT * FROM tb_assunto_trf WHERE cd_assunto_trf=?");
		this.psConsultaAssuntoPorID = conexaoPJe.prepareStatement("SELECT * FROM tb_assunto_trf WHERE id_assunto_trf=?");
		
		// Se o arquivo de configuração especificar um assunto padrão, tenta carregá-lo.
		// Posteriormente, se necessário, o assunto padrão será carregado do banco de dados.
		String codigoAssuntoPadraoString = Auxiliar.getParametroConfiguracao("assunto_padrao_" + grau + "G", false);
		if (codigoAssuntoPadraoString != null) {
			int codigo = Integer.parseInt(codigoAssuntoPadraoString);
			this.assuntoProcessualPadrao = getAssunto(codigo);
			if (this.assuntoProcessualPadrao != null) {
				this.assuntoProcessualPadrao.setPrincipal(true);
			} else {
			    throw new DadosInvalidosException("Foi definido um assunto padrão com código '" + codigoAssuntoPadraoString + "', mas esse assunto não foi localizado no PJe!");
			}
		}
	}
	
	
	/**
	 * Identifica se o usuário quer utilizar a lista de assuntos da JT ou a lista completa do CNJ
	 */
	private String getNomeArquivoAssuntos(int grau) {
		String tabelaAssuntosNacionais = Auxiliar.getParametroConfiguracao("tabela_de_assuntos_nacionais", "CNJ-JT");
		
		if ("CNJ-JT".equals(tabelaAssuntosNacionais)) {
			return "assuntos_jt_" + grau + "g.csv";
		} else if ("CNJ-GLOBAL".equals(tabelaAssuntosNacionais)) {
			return "assuntos_global.csv";
		} else {
			throw new RuntimeException("Valor inválido para o parâmetro tabela_de_assuntos_nacionais: '" + tabelaAssuntosNacionais + "'. Verifique o arquivo de configurações.");
		}
	}


	/**
	 * Gera um objeto TipoAssuntoProcessual, para ser inserido no XML do CNJ, a partir do código 
	 * informado.
	 */
	public TipoAssuntoProcessual getAssunto(int codigoAssunto) throws SQLException {
		TipoAssuntoProcessual assunto = new TipoAssuntoProcessual();
		
		if (assuntoExisteNasTabelasNacionais(codigoAssunto)) {
			
			// Se o assunto estiver nas tabelas nacionais, o código é a única informação solicitada
			// no XSD do CNJ
			assunto.setCodigoNacional(codigoAssunto);
		} else {
			
			// Se o assunto NÃO estiver nas tabelas nacionais, será preciso detalhar o assunto local.
			TipoAssuntoLocal assuntoLocal = new TipoAssuntoLocal();
			assunto.setAssuntoLocal(assuntoLocal);
			assuntoLocal.setCodigoAssunto(codigoAssunto);
			
			// Pesquisa recursivamente os "pais" desse assunto, até encontrar um que exista nas
			// tabelas nacionais do CNJ.
			psConsultaAssuntoPorCodigo.setString(1, Integer.toString(codigoAssunto));
			try (ResultSet rs = psConsultaAssuntoPorCodigo.executeQuery()) {
				if (rs.next()) {
					String descricaoAssuntoLocal = Auxiliar.getCampoStringNotNull(rs, "ds_assunto_trf");
					
					// Variável que receberá um código de assunto que faça parte das TPUs, preenchida
					// a partir de uma pesquisa recursiva na árvore de assuntos do processo, até
					// encontrar algum nó pai que esteja nas TPUs.
					int codigoPaiNacional = 0;
					
					// Limita a quantidade de nós recursivos, para evitar um possível loop infinito
					int tentativasRecursivas = 0;
					
					// Próximo assunto que será pesquisado recursivamente
					int idProximoAssunto = rs.getInt("id_assunto_trf");
					
					// Variável auxiliar para montar a descrição do assunto no padrão do PJe, mostrando
					// os códigos somente nos assuntos "pai", sem mostrar o código no assunto "folha", ex:
					// DIREITO PROCESSUAL CIVIL E DO TRABALHO (8826) / Partes e Procuradores (8842) / Sucumbência (8874) / Honorários na Justiça do Trabalho
					boolean assuntoFolha = true;
					
					// Itera, recursivamente, localizando assuntos "pai" na tabela
					while (idProximoAssunto > 0 && tentativasRecursivas < 50) {
						psConsultaAssuntoPorID.setInt(1, idProximoAssunto);
						try (ResultSet rsAssunto = psConsultaAssuntoPorID.executeQuery()) {
							
							// Verifica se chegou no fim da árvore
							if (!rsAssunto.next()) {
								break;
							}
							
							// Se ainda está pesquisando um codigoPaiNacional e encontrou um, grava
							// seu código.
							int codigo = rsAssunto.getInt("cd_assunto_trf");
							if (codigoPaiNacional == 0 && assuntoExisteNasTabelasNacionais(codigo)) {
								codigoPaiNacional = codigo;
							}
							
							// Localiza o próximo assunto que deverá ser consultado recursivamente
							idProximoAssunto = rsAssunto.getInt("id_assunto_trf_superior");
							
							// Coloca o nó atual na descrição do assunto
							if (assuntoFolha) {
								assuntoFolha = false;
							} else {
								descricaoAssuntoLocal = rsAssunto.getString("ds_assunto_trf") + " (" + codigo + ") / " + descricaoAssuntoLocal;
							}
						}
						tentativasRecursivas++;
					}
					
					assuntoLocal.setDescricao(descricaoAssuntoLocal);
					assuntoLocal.setCodigoPaiNacional(codigoPaiNacional);
					if (codigoPaiNacional == 0) {
						LOGGER.warn("Não foi possível identificar um \"código pai nacional\" para o assunto " + assuntoLocal.getCodigoAssunto() + " - " + assuntoLocal.getDescricao());
					}
					
				} else {
					throw new RuntimeException("Não foi encontrado assunto com código " + codigoAssunto + " na base do PJe!");
				}
			}
		}
		return assunto;
	}

	
	/**
	 * Se os parâmetros "assunto_padrao_1G" e "assunto_padrao_2G" estiverem habilitados,
	 * permite a utilização de assuntos "padrão" nos processos, quando os processos não tiverem
	 * assuntos no PJe.
	 */
	public TipoAssuntoProcessual getAssuntoProcessualPadrao() {
		return assuntoProcessualPadrao;
	}
	
	
	public boolean assuntoExisteNasTabelasNacionais(int codigoAssunto) {
		return assuntosProcessuaisCNJ.contains(codigoAssunto);
	}
	
	
	@Override
	public void close() throws SQLException {
		psConsultaAssuntoPorCodigo.close();
		psConsultaAssuntoPorCodigo = null;
		psConsultaAssuntoPorID.close();
		psConsultaAssuntoPorID = null;
	}
}
