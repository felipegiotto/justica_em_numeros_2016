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

import br.jus.cnj.modeloDeTransferenciaDeDados.TipoMovimentoLocal;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoMovimentoNacional;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoMovimentoProcessual;

/**
 * Classe que preencherá um objeto do tipo {@link TipoMovimentoProcessual}, conforme o dado no PJe:
 * 
 * Se o movimento estiver na lista das tabelas nacionais do CNJ (que foi extraída do site
 * http://www.cnj.jus.br/sgt/versoes.php?tipo_tabela=M e gravada nos arquivos
 * "src/main/resources/tabelas_cnj/movimentos_*"), gerará um movimento que possui somente
 * o campo "<movimentoNacional>".
 * 
 * Se o movimento NÃO estiver na lista das tabelas nacionais do CNJ, gera um objeto "<movimentoLocal>"
 * com os dados existentes no PJe, procurando ainda, de forma recursiva, algum movimento "pai" que
 * esteja nas tabelas nacionais, preenchendo o atributo "codigoPaiNacional".
 * 
 * @author fgiotto
 */
public class AnalisaMovimentosCNJ implements AutoCloseable {

	private static final Logger LOGGER = LogManager.getLogger(AnalisaMovimentosCNJ.class);
	private List<Integer> movimentosProcessuaisCNJ;
	private PreparedStatement psConsultaEventoPorID;
	
	public AnalisaMovimentosCNJ(int grau, Connection conexaoPJe) throws IOException, SQLException {
		super();
		
		File arquivoMovimentos = new File("src/main/resources/tabelas_cnj/movimentos_cnj.csv");
		LOGGER.info("Carregando lista de movimentos CNJ do arquivo " + arquivoMovimentos + "...");
		
		// Lista de assuntos processuais unificados, do CNJ. Essa lista definirá se o assunto do processo
		// deverá ser registrado com as tags "<assunto><codigoNacional>" ou "<assunto><assuntoLocal>"
		// Fonte: http://www.cnj.jus.br/sgt/versoes.php?tipo_tabela=A
		this.movimentosProcessuaisCNJ = new ArrayList<>();
		for (String movimentoString: FileUtils.readLines(arquivoMovimentos, "UTF-8")) {
			movimentosProcessuaisCNJ.add(Integer.parseInt(movimentoString));
		}
		
		// PreparedStatements que localizarão movimentos no banco de dados do PJe
		this.psConsultaEventoPorID = conexaoPJe.prepareStatement("SELECT * FROM tb_evento WHERE id_evento = ?");
	}
	
	
	/**
	 * Preeche um objeto TipoMovimentoProcessual, para ser inserido no XML do CNJ, a partir do código 
	 * informado.
	 * 
	 * Serão preenchidos os campos "TipoMovimentoNacional" ou "TipoMovimentoLocal", conforme a
	 * existência (ou não) do código do movimento nas tabelas nacionais do CNJ. 
	 * 
	 * @throws SQLException 
	 */
	public void preencheDadosMovimentoCNJ(TipoMovimentoProcessual movimento, int codigoMovimento, String descricao, String descricaoEventoProcessual) throws SQLException {
		if (movimentoExisteNasTabelasNacionais(codigoMovimento)) {
			TipoMovimentoNacional movimentoNacional = new TipoMovimentoNacional();
			movimentoNacional.setCodigoNacional(codigoMovimento);
			movimento.setMovimentoNacional(movimentoNacional);
		} else {
			if (descricao == null) {
				LOGGER.info("Movimento com código " + codigoMovimento + " não possui descrição. Será utilizada a descrição da tabela tb_evento_processual: " + descricaoEventoProcessual);
				descricao = descricaoEventoProcessual;
			}
			TipoMovimentoLocal movimentoLocal = new TipoMovimentoLocal();
			movimentoLocal.setCodigoMovimento(codigoMovimento);
			movimentoLocal.setDescricao(descricao);
			movimento.setMovimentoLocal(movimentoLocal);
			
			int movimentoPai = procurarRecursivamenteMovimentoPaiNaTabelaNacional(codigoMovimento);
			movimentoLocal.setCodigoPaiNacional(movimentoPai);
			if (movimentoPai == 0) {
				LOGGER.warn("Não foi possível identificar um \"movimento pai nacional\" para o movimento " + codigoMovimento + " - " + descricao);
			}
		}
	}

	
	/**
	 * Pesquisa recursivamente os "pais" desse movimento, até encontrar um que exista nas tabelas nacionais do CNJ.
	 */
	private int procurarRecursivamenteMovimentoPaiNaTabelaNacional(int idEvento) throws SQLException {
		int tentativas = 0;
		while (tentativas < 50) {
			
			psConsultaEventoPorID.setInt(1, idEvento);
			try (ResultSet rsMovimento = psConsultaEventoPorID.executeQuery()) { // TODO: Otimizar acessos repetidos
				if (rsMovimento.next()) {
					int codigo = rsMovimento.getInt("id_evento");
					if (movimentoExisteNasTabelasNacionais(codigo)) {
						return codigo;
					} else {
						idEvento = rsMovimento.getInt("id_evento_superior");
					}
				} else {
					LOGGER.warn("Não localizei assunto com id_evento = " + idEvento);
					return 0;
				}
			}
			
			tentativas++;
		}
		LOGGER.warn("Excedido o limite de tentativas recursivas de identificar movimento pai!");
		return 0;
	}

	
	public boolean movimentoExisteNasTabelasNacionais(int codigoAssunto) {
		return movimentosProcessuaisCNJ.contains(codigoAssunto);
	}
	
	
	@Override
	public void close() throws SQLException {
		psConsultaEventoPorID.close();
		psConsultaEventoPorID = null;
	}
}
