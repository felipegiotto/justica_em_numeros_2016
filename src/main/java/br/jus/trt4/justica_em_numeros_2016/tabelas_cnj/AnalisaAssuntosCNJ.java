package br.jus.trt4.justica_em_numeros_2016.tabelas_cnj;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.cnj.modeloDeTransferenciaDeDados.TipoAssuntoLocal;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoAssuntoProcessual;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.BenchmarkVariasOperacoes;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DadosInvalidosException;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Parametro;

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
	private List<Integer> assuntosProcessuaisCNJ = new ArrayList<>();
	private Map<Integer, Integer> assuntosProcessuaisDePara;
	private PreparedStatement psConsultaAssuntoPorCodigo;
	private PreparedStatement psConsultaAssuntoPorID;
	private TipoAssuntoProcessual assuntoProcessualPadrao;
	
	public AnalisaAssuntosCNJ(int grau, Connection conexaoPJe) throws IOException, SQLException, DadosInvalidosException, InterruptedException {
		super();
		
		File arquivoAssuntos = new File("src/main/resources/tabelas_cnj/assuntos_cnj.csv");
		LOGGER.info("Carregando lista de assuntos CNJ do arquivo " + arquivoAssuntos + "...");
		
		// Lista de assuntos processuais unificados, do CNJ. Essa lista definirá se o assunto do processo
		// deverá ser registrado com as tags "<assunto><codigoNacional>" ou "<assunto><assuntoLocal>"
		// Fonte: https://www.cnj.jus.br/sgt/versoes.php?tipo_tabela=A
		for (String assuntoString: FileUtils.readLines(arquivoAssuntos, "UTF-8")) {
			if (!StringUtils.isBlank(assuntoString) && !assuntoString.startsWith("#")) {
				assuntosProcessuaisCNJ.add(Integer.parseInt(assuntoString));
			}
		}
		
		// PreparedStatements que localizarão assuntos no banco de dados do PJe
		this.psConsultaAssuntoPorCodigo = conexaoPJe.prepareStatement("SELECT * FROM tb_assunto_trf WHERE cd_assunto_trf=?");
		this.psConsultaAssuntoPorID = conexaoPJe.prepareStatement("SELECT * FROM tb_assunto_trf WHERE id_assunto_trf=?");
		
		// Se o arquivo de configuração especificar um assunto padrão, tenta carregá-lo.
		// Posteriormente, se necessário, o assunto padrão será carregado do banco de dados.
		String codigoAssuntoPadraoString;
		if (grau == 1) {
			codigoAssuntoPadraoString = Auxiliar.getParametroConfiguracao(Parametro.assunto_padrao_1G, false);			
		} else if (grau == 2) {
			codigoAssuntoPadraoString = Auxiliar.getParametroConfiguracao(Parametro.assunto_padrao_2G, false);
		} else {
			throw new SQLException("Grau inválido: " + grau);
		}
		
		// Tabela "de-para" de assuntos
		if (assuntosProcessuaisDePara == null) {
			assuntosProcessuaisDePara = new HashMap<>();
			
			File fileAssuntoDePara = getArquivoAssuntosDePara();
			if (fileAssuntoDePara != null) {
				
				// Lê os assuntos do arquivo "de-para"
				Properties propertiesDePara = new Properties();
				try (InputStream is = Files.newInputStream(fileAssuntoDePara.toPath())) {
					propertiesDePara.load(is);
				}
				
				// Lista de assuntos do lado "PARA" que não existem na lista nacional (e, por isso, deverão ser negados pelo CNJ):
				Set<Integer> assuntosMapeadosIncorretamente = new TreeSet<>();
				
				// Grava os assuntos em um Map
				for (String assuntoDe : propertiesDePara.stringPropertyNames()) {
					String assuntoPara = propertiesDePara.getProperty(assuntoDe);
					int assuntoParaInt = Integer.parseInt(assuntoPara);
					assuntosProcessuaisDePara.put(Integer.parseInt(assuntoDe), assuntoParaInt);
					
					if (!assuntoExisteNasTabelasNacionais(assuntoParaInt)) {
						assuntosMapeadosIncorretamente.add(assuntoParaInt);
					}
				}
				
				if (!assuntosMapeadosIncorretamente.isEmpty()) {
					LOGGER.warn("");
					LOGGER.warn("Há assuntos que estão descritos na tabela 'de-para' como válidos no CNJ, mas não estão na lista de assuntos nacionais do CNJ: ");
					for (int codigo: assuntosMapeadosIncorretamente) {
						LOGGER.warn("* " + codigo);
					}
					Auxiliar.aguardaUsuarioApertarENTERComTimeout(2);
				}
			}
		}
		
		if (codigoAssuntoPadraoString != null) {
			int codigo = Integer.parseInt(codigoAssuntoPadraoString);
			this.assuntoProcessualPadrao = getAssunto(codigo);
			if (this.assuntoProcessualPadrao != null) {
				this.assuntoProcessualPadrao.setPrincipal(true);
			} else {
			    throw new DadosInvalidosException("Foi definido um assunto padrão com código '" + codigoAssuntoPadraoString + "', mas esse assunto não foi localizado no PJe!", Auxiliar.getArquivoconfiguracoes().toString());
			}
		}
	}
	
	
	/**
	 * Gera um objeto TipoAssuntoProcessual, para ser inserido no XML do CNJ, a partir do código 
	 * informado.
	 */
	public TipoAssuntoProcessual getAssunto(int codigoAssunto) throws SQLException {
		TipoAssuntoProcessual assunto = new TipoAssuntoProcessual();
		
		// Verifica se assunto está na tabela "de-para". Se estiver, utiliza diretamente o "para" como código nacional.
		if (assuntosProcessuaisDePara.containsKey(codigoAssunto)) {
			Integer novoCodigo = assuntosProcessuaisDePara.get(codigoAssunto);
			LOGGER.trace("Processo possui assunto (código " + codigoAssunto + ") que será mapeado para outro (código " + novoCodigo + ") de acordo com tabela DE-PARA");
			assunto.setCodigoNacional(novoCodigo);
			
		} else if (assuntoExisteNasTabelasNacionais(codigoAssunto)) {
			
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
			BenchmarkVariasOperacoes.globalInstance().inicioOperacao("Consulta de assunto por codigo");
			try (ResultSet rs = psConsultaAssuntoPorCodigo.executeQuery()) { // TODO: Otimizar acessos repetidos
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
						BenchmarkVariasOperacoes.globalInstance().inicioOperacao("Consulta de assunto por ID");
						try (ResultSet rsAssunto = psConsultaAssuntoPorID.executeQuery()) { // TODO: Otimizar acessos repetidos
							
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
								// TODO: Verificar se isso pode ser substituído pelo campo "ds_assunto_completo" de "tb_assunto_trf"
								descricaoAssuntoLocal = rsAssunto.getString("ds_assunto_trf") + " (" + codigo + ") / " + descricaoAssuntoLocal;
							}
						} finally {
							BenchmarkVariasOperacoes.globalInstance().fimOperacao();
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
			} finally {
				BenchmarkVariasOperacoes.globalInstance().fimOperacao();
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
	
	public static File getArquivoAssuntosDePara() throws IOException {
		String param = Auxiliar.getParametroConfiguracao(Parametro.assuntos_de_para, false);
		if (param != null) {
			File file = new File("src/main/resources/assuntos_de-para/" + param);
			if (file.isFile() && file.canRead()) {
				return file;
			} else {
				throw new IOException("Parâmetro 'assuntos_de_para' solicitou leitura do arquivo '" + file + "', mas ele não pode ser lido. ");
			}
		} else {
			return null;
		}
	}

	@Override
	public void close() throws SQLException {
		psConsultaAssuntoPorCodigo.close();
		psConsultaAssuntoPorCodigo = null;
		psConsultaAssuntoPorID.close();
		psConsultaAssuntoPorID = null;
	}
}
