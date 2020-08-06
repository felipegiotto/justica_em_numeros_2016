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
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DataJudException;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Parametro;
import br.jus.trt4.justica_em_numeros_2016.enums.BaseEmAnaliseEnum;

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
	private BaseEmAnaliseEnum baseEmAnaliseEnum;
	
	//A conexaoPJe só será utilizada quando baseEmAnaliseEnum.isBasePJe()
	public AnalisaAssuntosCNJ(int grau, Connection conexaoPJe, boolean carregarArquivoDePara, BaseEmAnaliseEnum baseEmAnaliseEnum) throws IOException, SQLException, InterruptedException, DataJudException {
		super();
		
		this.baseEmAnaliseEnum = baseEmAnaliseEnum;
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
		
		if (this.baseEmAnaliseEnum.isBasePJe()) {
			// PreparedStatements que localizarão assuntos no banco de dados do PJe
			this.psConsultaAssuntoPorCodigo = conexaoPJe.prepareStatement("SELECT * FROM tb_assunto_trf WHERE cd_assunto_trf=?");
			this.psConsultaAssuntoPorID = conexaoPJe.prepareStatement("SELECT * FROM tb_assunto_trf WHERE id_assunto_trf=?");			
		}
		
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
			if (carregarArquivoDePara && fileAssuntoDePara != null) {
				
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
				
				// UPDATE: Não precisa mais fazer essa validação, já que há a opção de validar diretamente com o aplicativo do CNJ (parâmetro "url_validador_cnj").
//				if (!assuntosMapeadosIncorretamente.isEmpty()) {
//					LOGGER.warn("");
//					LOGGER.warn("Há assuntos que estão descritos na tabela 'de-para' como válidos no CNJ, mas não estão na lista de assuntos nacionais do CNJ: ");
//					for (int codigo: assuntosMapeadosIncorretamente) {
//						LOGGER.warn("* " + codigo);
//					}
//					Auxiliar.aguardaUsuarioApertarENTERComTimeout(2);
//				}
			}
		}
		
		if (codigoAssuntoPadraoString != null) {
			int codigo = Integer.parseInt(codigoAssuntoPadraoString);
			this.assuntoProcessualPadrao = getAssunto(codigo, baseEmAnaliseEnum);
			if (this.assuntoProcessualPadrao != null) {
				this.assuntoProcessualPadrao.setPrincipal(true);
			} else {
			    throw new DataJudException("Foi definido um assunto padrão com código '" + codigoAssuntoPadraoString + "', mas esse assunto não foi localizado no PJe!");
			}
		}
	}
	
	
	/**
	 * Gera um objeto TipoAssuntoProcessual, para ser inserido no XML do CNJ, a partir do código 
	 * informado.
	 */
	public TipoAssuntoProcessual getAssunto(int codigoAssunto, BaseEmAnaliseEnum baseEmAnaliseEnum) throws SQLException {
		TipoAssuntoProcessual assunto = new TipoAssuntoProcessual();
		if (baseEmAnaliseEnum.isBasePJe()) {
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
				
				// FIXME: Resolver possível problema de acesso concorrente a esse PreparedStatement na geração de XMLs em várias threads
				synchronized(psConsultaAssuntoPorCodigo) {
				
				// Pesquisa recursivamente os "pais" desse assunto, até encontrar um que exista nas
				// tabelas nacionais do CNJ.
				psConsultaAssuntoPorCodigo.setString(1, Integer.toString(codigoAssunto));
				try (ResultSet rs = psConsultaAssuntoPorCodigo.executeQuery()) { // TODO: Otimizar acessos repetidos
					if (rs.next()) {
						String descricaoAssuntoLocal = Auxiliar.getCampoStringNotNull(rs, "ds_assunto_trf");
						
						// Variável que receberá um código de assunto que faça parte das TPUs, preenchida
						// a partir de uma pesquisa recursiva na árvore de assuntos do processo, até
						// encontrar algum nó pai que esteja nas TPUs.
						Integer codigoPaiNacional = null;
						
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
							
							// FIXME: Resolver possível problema de acesso concorrente a esse PreparedStatement na geração de XMLs em várias threads
							synchronized(psConsultaAssuntoPorID) {
							
							psConsultaAssuntoPorID.setInt(1, idProximoAssunto);
							try (ResultSet rsAssunto = psConsultaAssuntoPorID.executeQuery()) { // TODO: Otimizar acessos repetidos
								
								// Verifica se chegou no fim da árvore
								if (!rsAssunto.next()) {
									break;
								}
								
								// Se ainda está pesquisando um codigoPaiNacional e encontrou um, grava
								// seu código.
								int codigo = rsAssunto.getInt("cd_assunto_trf");
								if (codigoPaiNacional == null && assuntoExisteNasTabelasNacionais(codigo)) {
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
							}
							}
							tentativasRecursivas++;
						}
						
						// UPDATE 10/07/2020: CNJ não está mais aceitando assuntos locais com "codigoPaiNacional=0", então caso não encontre um pai nacional o assunto não será informado.
						if (codigoPaiNacional == null) {
							LOGGER.warn("Não foi possível identificar um \"código pai nacional\" para o assunto " + assuntoLocal.getCodigoAssunto() + " - " + descricaoAssuntoLocal);
							return null;
						}
						assuntoLocal.setDescricao(descricaoAssuntoLocal);
						assuntoLocal.setCodigoPaiNacional(codigoPaiNacional);
						
					} else {
						throw new RuntimeException("Não foi encontrado assunto com código " + codigoAssunto + " na base do PJe!");
					}
				}
				}
			}
		} else {
			//FIXME: Apenas assuntos nacionais do sistema judicial legado serão enviados. Cada Regional poderá dar um tratamento diferente.
			assunto.setCodigoNacional(codigoAssunto);
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
		File file = new File("src/main/resources/assuntos_de-para/assuntos_de-para.properties");
		if (file.isFile() && file.canRead()) {
			return file;
		} else {
			throw new IOException("Parâmetro 'assuntos_de_para' solicitou leitura do arquivo '" + file + "', mas ele não pode ser lido. ");
		}
	}

	public Map<Integer, Integer> getAssuntosProcessuaisDePara() {
		return assuntosProcessuaisDePara;
	}

	@Override
	public void close() throws SQLException {
		if (this.baseEmAnaliseEnum.isBasePJe()) {
			psConsultaAssuntoPorCodigo.close();
			psConsultaAssuntoPorCodigo = null;
			psConsultaAssuntoPorID.close();
			psConsultaAssuntoPorID = null;
		}
	}
}
