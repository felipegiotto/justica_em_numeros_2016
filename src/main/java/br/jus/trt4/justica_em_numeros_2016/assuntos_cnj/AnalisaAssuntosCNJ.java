package br.jus.trt4.justica_em_numeros_2016.assuntos_cnj;

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

/**
 * Classe que montará um objeto do tipo {@link TipoAssuntoProcessual}, conforme o dado no PJe:
 * 
 * Se o assunto estiver na lista das tabelas nacionais do CNJ (que foi extraída do site
 * http://www.cnj.jus.br/sgt/versoes.php?tipo_tabela=A e gravada nos arquivos da pasta
 * "src/main/resources/assuntos_processuais_cnj"), gerará um assunto que possui somente
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
	
	public AnalisaAssuntosCNJ(int grau, Connection conexaoPJe) throws IOException, SQLException {
		super();
		
		// Lista de assuntos processuais unificados, do CNJ. Essa lista definirá se o assunto do processo
		// deverá ser registrado com as tags "<assunto><codigoNacional>" ou "<assunto><assuntoLocal>"
		// Fonte: http://www.cnj.jus.br/sgt/versoes.php?tipo_tabela=A
		this.assuntosProcessuaisCNJ = new ArrayList<>();
		for (String assuntoString: FileUtils.readLines(new File("src/main/resources/assuntos_processuais_cnj/assuntos_" + grau + "g.csv"), "UTF-8")) {
			assuntosProcessuaisCNJ.add(Integer.parseInt(assuntoString));
		}
		
		// PreparedStatements que localizarão assuntos no banco de dados do PJe
		this.psConsultaAssuntoPorCodigo = conexaoPJe.prepareStatement("SELECT * FROM tb_assunto_trf WHERE cd_assunto_trf=?");
		this.psConsultaAssuntoPorID = conexaoPJe.prepareStatement("SELECT * FROM tb_assunto_trf WHERE id_assunto_trf=?");
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
					assuntoLocal.setDescricao(rs.getString("ds_assunto_trf"));
					
					int assuntoPai = procurarRecursivamenteAssuntoPaiNaTabelaNacional(rs.getInt("id_assunto_trf"));
					assuntoLocal.setCodigoPaiNacional(assuntoPai);
					if (assuntoPai == 0) {
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
	 * Pesquisa recursivamente os "pais" desse assunto, até encontrar um que exista nas tabelas nacionais do CNJ.
	 */
	private int procurarRecursivamenteAssuntoPaiNaTabelaNacional(int idAssunto) throws SQLException {
		int tentativas = 0;
		while (tentativas < 50) {
			
			psConsultaAssuntoPorID.setInt(1, idAssunto);
			try (ResultSet rsAssunto = psConsultaAssuntoPorID.executeQuery()) {
				if (rsAssunto.next()) {
					int codigo = rsAssunto.getInt("cd_assunto_trf");
					if (assuntoExisteNasTabelasNacionais(codigo)) {
						return codigo;
					} else {
						idAssunto = rsAssunto.getInt("id_assunto_trf_superior");
					}
				} else {
					LOGGER.warn("Não localizei assunto com id_assunto_trf = " + idAssunto);
					return 0;
				}
			}
			
			tentativas++;
		}
		LOGGER.warn("Excedido o limite de tentativas recursivas de identificar assunto pai!");
		return 0;
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
