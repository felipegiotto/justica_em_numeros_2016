package br.jus.trt4.justica_em_numeros_2016.tabelas_cnj;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.cnj.modeloDeTransferenciaDeDados.TipoMovimentoLocal;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoMovimentoNacional;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoMovimentoProcessual;
import br.jus.trt3.depara.GerenteDeParaJTCNJ;
import br.jus.trt3.depara.exception.DeParaJTCNJException;
import br.jus.trt3.depara.leitorarquivo.TipoTipoComplemento;
import br.jus.trt3.depara.vo.ComplementoCNJ;
import br.jus.trt3.depara.vo.MovimentoCNJ;
import br.jus.trt3.depara.vo.MovimentoJT;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DataJudException;
import br.jus.trt4.justica_em_numeros_2016.dto.ComplementoDto;
import br.jus.trt4.justica_em_numeros_2016.dto.EventoDto;
import br.jus.trt4.justica_em_numeros_2016.dto.MovimentoDto;
import br.jus.trt4.justica_em_numeros_2016.dto.ProcessoDto;
import br.jus.trt4.justica_em_numeros_2016.enums.BaseEmAnaliseEnum;
import br.jus.trt4.justica_em_numeros_2016.enums.Parametro;
import br.jus.trt4.justica_em_numeros_2016.enums.TipoTipoComplementoCNJ;

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
public class AnalisaMovimentosCNJ {

	private static final Logger LOGGER = LogManager.getLogger(AnalisaMovimentosCNJ.class);
	private List<Integer> movimentosProcessuaisCNJ;
	private Map<Integer, EventoDto> eventosPorId;
	private GerenteDeParaJTCNJ gerenteDeParaJTCNJ;
	private static Map<Integer, TipoTipoComplementoCNJ> tiposTipoComplementoCNJ = new HashMap<Integer, TipoTipoComplementoCNJ>();
	
	//A conexaoPJe só será utilizada quando baseEmAnaliseEnum.isBasePJe()
	public AnalisaMovimentosCNJ(BaseEmAnaliseEnum baseEmAnaliseEnum, Connection conexaoPJe) throws IOException, SQLException {
		super();
		carregarTiposComplementoCNJ();

		File arquivoMovimentos = new File("src/main/resources/tabelas_cnj/movimentos_cnj.csv");
		LOGGER.info("Carregando lista de movimentos CNJ do arquivo " + arquivoMovimentos + "...");
		
		// Lista de movimentos processuais unificados, do CNJ.
		// Fonte: http://www.cnj.jus.br/sgt/versoes.php?tipo_tabela=M
		this.movimentosProcessuaisCNJ = new ArrayList<>();
		for (String movimentoString: FileUtils.readLines(arquivoMovimentos, "UTF-8")) {
			if (!StringUtils.isBlank(movimentoString) && !movimentoString.startsWith("#")) {
				movimentosProcessuaisCNJ.add(Integer.parseInt(movimentoString));
			}
		}
		
		// PreparedStatements que localizarão movimentos no banco de dados do PJe
		this.eventosPorId = new HashMap<>();
		try (PreparedStatement psEventos = conexaoPJe.prepareStatement("SELECT * FROM tb_evento")) {
			try (ResultSet rsEventos = psEventos.executeQuery()) {
				while (rsEventos.next()) {
					EventoDto evento = new EventoDto(rsEventos);
					this.eventosPorId.put(evento.getId(), evento);
				}
			}
		}
	    if (baseEmAnaliseEnum.isBasePJe()) {
			// Ferramenta DE-PARA de movimentos e complementos, criada pelo TRT3.
			try {
				gerenteDeParaJTCNJ = GerenteDeParaJTCNJ.getInstancia();
				gerenteDeParaJTCNJ.carregarDePara();
			} catch (DeParaJTCNJException e) {
				throw new IOException("Erro iniciando DE-PARA de movimentos", e);
			}
		}
	}
	
	
	/**
	 * Preeche um objeto TipoMovimentoProcessual, para ser inserido no XML do CNJ, a partir do código 
	 * informado.
	 * 
	 * Serão preenchidos os campos "TipoMovimentoNacional" ou "TipoMovimentoLocal", conforme a
	 * existência (ou não) do código do movimento nas tabelas nacionais do CNJ. 
	 * @param numeroInstancia 
	 * 
	 * @throws SQLException 
	 * @throws DataJudException 
	 * @throws DadosInvalidosException 
	 */
	public void preencheDadosMovimentoCNJ(ProcessoDto processo, TipoMovimentoProcessual movimento, MovimentoDto movimentoDto, BaseEmAnaliseEnum baseEmAnaliseEnum) throws SQLException, DataJudException {

		int codigoMovimento = movimentoDto.getCodMovimentoCNJ();
		
		String descricao = movimentoDto.getTextoMovimento();
		String descricaoEventoProcessual = movimentoDto.getTextoEvento();
		if (baseEmAnaliseEnum.isBasePJe()) {
			// Aplica o DE-PARA de movimentos e complementos. Essa variável será verdadeira quando for encontrado um mapeamento
			// válido para ela no mapeador DE-PARA
			boolean movimentoMapeadoNoDePara = false;

			Optional<MovimentoCNJ> recuperarMovimentoComplementosCNJ = aplicarMapeadorDePara(processo, movimentoDto);
			
			if(recuperarMovimentoComplementosCNJ.isPresent()) {
				movimentoMapeadoNoDePara = true;				
				// Substitui o código do movimento
				codigoMovimento = Integer.parseInt(recuperarMovimentoComplementosCNJ.get().getCodigoMovimento());				
			}			

			// TODO: Preencher este novo campo do XSD
			// Atributo que permite a atribuição da decisão como sendo monocrática (proferida por um magistrado), ou colegiada;
			// Valores possíveis são numéricos (0 ou 1):
			// 0 - decisão MONOCRATICA
			// 1 - decisão COLEGIADA
			// movimento.setTipoDecisao(...);

			if (Auxiliar.getParametroBooleanConfiguracaoComValorPadrao(Parametro.descartar_movimentos_pje_ausentes_de_para_cnj, false)) {
				if (movimentoMapeadoNoDePara) {
					//Apenas movimentos mapeados no DE-PARA do CNJ serão mantidos
					if (movimentoExisteNasTabelasNacionais(codigoMovimento)) {
						movimento.setMovimentoNacional(criarMovimentoNacional(codigoMovimento));
					} else {
						// O mapeador DE-PARA também permite que cada Regional faça seus próprios mapeamentos, incluindo movimentos
						// locais da JT, por meio do arquivo /src/main/resources/depara/de_para_jt_cnj_regional.csv
						movimento.setMovimentoLocal(criarMovimentoLocal(movimento, codigoMovimento, descricao));
					}
				}
				else {
					// Movimento não mapeado
					String complementos = "";
					for (ComplementoDto complemento : movimentoDto.getComplementos()) {
						complementos += complemento.getCodigoTipoComplemento() + ":" +
											complemento.getCodigoComplemento() + " | ";
					}

					LOGGER.trace(String.format("O movimento do %sº Grau do PJe (id=%s) de código %s (complementos: %s) foi descartado pelo mapeador DE-PARA.",
							processo.getNumeroInstancia(),
							movimentoDto.getIdProcessoEvento(),
							movimentoDto.getCodMovimentoCNJ(),
							complementos.length() > 0 ? complementos.substring(0, complementos.length() - 3) : "nenhum"));
				}
			} else {
				// Parâmetro descartar_movimentos_pje_ausentes_de_para_cnj está com valor NAO, ou seja, inclui o movimento 
				// mesmo que não exista mapeamento
				
				// Verifica se o movimento deve ser enviado como NACIONAL ou como LOCAL
				if (movimentoExisteNasTabelasNacionais(codigoMovimento)) {
					movimento.setMovimentoNacional(criarMovimentoNacional(codigoMovimento));
				} else {
					if (descricao == null) {
						LOGGER.trace("Movimento com código " + codigoMovimento + " não possui descrição. Será utilizada a descrição da tabela tb_evento_processual: " + descricaoEventoProcessual);
						descricao = descricaoEventoProcessual;
					}
					movimento.setMovimentoLocal(criarMovimentoLocal(movimento, codigoMovimento, descricao));
				}
			}		
		} else {
			//Base staging com movimentos do legado
			
			if (movimentoExisteNasTabelasNacionais(codigoMovimento)) {
				//Movimento Nacional
				movimento.setMovimentoNacional(criarMovimentoNacional(codigoMovimento));

				//Verifica se o complemento é do tipo tabelado ou não
				for (ComplementoDto complementoDto : movimentoDto.getComplementos()) {
					TipoTipoComplementoCNJ tipoTipoComplementoCNJ = getTipoTipoComplementoCNJPorCodigo(
							complementoDto.getCodigoTipoComplemento());
					complementoDto.setComplementoTipoTabelado(
							tipoTipoComplementoCNJ != null && tipoTipoComplementoCNJ.isComplementoTabelado());
				}
			} else {
				//Movimento Local

				if (descricao == null) {
					LOGGER.trace("Movimento com código " + codigoMovimento + " não possui descrição. Será utilizada a descrição da tabela tb_evento_processual: " + descricaoEventoProcessual);
					descricao = descricaoEventoProcessual;
				}

				movimento.setMovimentoLocal(criarMovimentoLocal(movimento, codigoMovimento, descricao));
			}
			
		}

	}


	/**
	 * Método responsável por aplicar o mapeador DE-PARA ao parâmetro movimentoDto, caso seja encontrado um mapeamento válido.
	 * 
	 * @param processo
	 * @param movimentoDto
	 * @return
	 * @throws DataJudException
	 */
	private Optional<MovimentoCNJ> aplicarMapeadorDePara(ProcessoDto processo, MovimentoDto movimentoDto)
			throws DataJudException {
		
		int codigoMovimento = movimentoDto.getCodMovimentoCNJ();
		
		Optional<MovimentoCNJ> recuperarMovimentoComplementosCNJ = Optional.empty();
		
		// Instancia o movimento com os complementos, no formato esperado pelo DE-PARA
		MovimentoJT movimentoJT = new MovimentoJT(processo.getNumeroInstancia(), Integer.toString(codigoMovimento), Integer.toString(processo.getClasseJudicial().getCodigo()));
		for (ComplementoDto complementoDto : movimentoDto.getComplementos()) {
			movimentoJT.adicionaComplementoJT(Integer.toString(complementoDto.getCodigoTipoComplemento()), complementoDto.getCodigoComplemento(), complementoDto.getValor());
		}
		try {
			// Verifica se o movimento está nas tabelas DE-PARA
			recuperarMovimentoComplementosCNJ = gerenteDeParaJTCNJ.recuperarMovimentoComplementosCNJ(movimentoJT);
			if (recuperarMovimentoComplementosCNJ.isPresent()) {

				// Se o movimento está nas tabelas DE-PARA, precisa substituir totalmente os dados dos movimentos e complementos
				// do banco de dados do PJe pelos retornados pela ferramenta.
				MovimentoCNJ movimentoCNJ = recuperarMovimentoComplementosCNJ.get();

				// Substitui os complementos
				movimentoDto.getComplementos().clear();
				for(ComplementoCNJ complementoCNJ : movimentoCNJ.getListaComplementos()) {
					ComplementoDto complementoDto = new ComplementoDto();
					complementoDto.setCodigoTipoComplemento(Integer.parseInt(complementoCNJ.getCodigoTipo()));
					complementoDto.setNome(complementoCNJ.getDescricaoTipo());
					complementoDto.setCodigoComplemento(complementoCNJ.getCodigoValor());
					//TODO: verificar se esse tratamento continuará sendo dado ao movimento 1061
					complementoDto.setValor(this.isComplementoDataMovimento1061(movimentoCNJ, complementoCNJ) 
							? DateTimeFormatter.ofPattern("dd/MM/yyyy").format(movimentoDto.getDataAtualizacao()) 
									: complementoCNJ.getDescricaoValor());
					complementoDto.setComplementoTipoTabelado(TipoTipoComplemento.TABELADO.equals(complementoCNJ.getTipoTipoComplemento()));
					movimentoDto.getComplementos().add(complementoDto);
					
					if((complementoDto.getCodigoComplemento() == null && TipoTipoComplemento.TABELADO.equals(complementoCNJ.getTipoTipoComplemento())))  {
						LOGGER.trace(String.format("O complemento tabelado %s (%s) do movimento %s foi mapeado com o seu "
								+ "CÓDIGO de complemento igual a nulo.", complementoDto.getCodigoTipoComplemento(), 
								complementoDto.getNome(), movimentoCNJ.getCodigoMovimento()));
					} else if (complementoDto.getValor() == null && TipoTipoComplemento.IDENTIFICADOR.equals(complementoCNJ.getTipoTipoComplemento())){
						LOGGER.trace(String.format("O complemento identificador %s (%s) do movimento %s foi mapeado com o seu "
								+ "VALOR de complemento igual a nulo.", complementoDto.getCodigoTipoComplemento(), 
								complementoDto.getNome(), movimentoCNJ.getCodigoMovimento()));
					}
				}
			}
		} catch (DeParaJTCNJException e) {
			throw new DataJudException("Erro ao aplicar DE-PARA de movimentos e complementos do TRT3", e);
		}
		
		return recuperarMovimentoComplementosCNJ;
	}


	private TipoMovimentoNacional criarMovimentoNacional(int codigoMovimento) {
		TipoMovimentoNacional movimentoNacional = new TipoMovimentoNacional();
		movimentoNacional.setCodigoNacional(codigoMovimento);
		return movimentoNacional;
	}
	
	private TipoMovimentoLocal criarMovimentoLocal(TipoMovimentoProcessual movimento, int codigoMovimento, String descricao) throws SQLException {
		
		TipoMovimentoLocal movimentoLocal = new TipoMovimentoLocal();
		movimentoLocal.setCodigoMovimento(codigoMovimento);
		movimentoLocal.setDescricao(descricao);
		
		int movimentoPai = procurarRecursivamenteMovimentoPaiNaTabelaNacional(codigoMovimento);
		movimentoLocal.setCodigoPaiNacional(movimentoPai);
		if (movimentoPai == 0) {
			LOGGER.warn("Não foi possível identificar um \"movimento pai nacional\" para o movimento " + codigoMovimento + " - " + descricao);
		}
		
		return movimentoLocal;
	}
	
	private boolean isComplementoDataMovimento1061 (MovimentoCNJ movimentoCNJ, ComplementoCNJ complementoCNJ) {
		return movimentoCNJ.getCodigoMovimento().equals("1061") && complementoCNJ.getDescricaoTipo().equals("data");
	}

	
	/**
	 * Pesquisa recursivamente os "pais" desse movimento, até encontrar um que exista nas tabelas nacionais do CNJ.
	 */
	private int procurarRecursivamenteMovimentoPaiNaTabelaNacional(int idEvento) throws SQLException {
		int tentativas = 0;
		while (tentativas < 50) {
			
			EventoDto evento = this.eventosPorId.get(idEvento);
			if (evento != null) {
				if (movimentoExisteNasTabelasNacionais(evento.getId())) {
					return evento.getId();
					
				} else if (evento.getIdEventoSuperior() != null) {
					idEvento = evento.getIdEventoSuperior();
					
				} else {
					LOGGER.warn("Evento com id_evento = " + idEvento + " não possui evento superior na hierarquia.");
					return 0;
				}
				
			} else {
				LOGGER.warn("Não localizei evento com id_evento = " + idEvento);
				return 0;
			}
			
			tentativas++;
		}
		LOGGER.warn("Excedido o limite de tentativas recursivas de identificar movimento pai!");
		return 0;
	}
	
	public boolean movimentoExisteNasTabelasNacionais(int codigoMovimento) {
		return movimentosProcessuaisCNJ.contains(codigoMovimento);
	}
	
	/**
	 * Carrega o conjunto de códigos de tipo de complemento e o tipo de tipo de complemento associado (tabelado, identificador ou livre)
	 * dos complementos do CNJ. A informação no PJe está errada (pje.tb_tipo_complemento) e não há garantia de que 
	 * toda a lista de complementos estará na ferramenta DE-PARA (classe EstruturasDeParaJTCNJ), 
	 * além de ser trabalhoso buscar apenas qual é o tipo de um tipo de complemento. 
	 */
	private static void carregarTiposComplementoCNJ() {
		if(tiposTipoComplementoCNJ.isEmpty()) {
			File arquivoComplementos = new File("src/main/resources/tabelas_cnj/tipos_de_complemento_cnj.csv");
			
			if(!arquivoComplementos.exists()) {
				LOGGER.error("Arquivo tipos_de_complemento_cnj.csv não existe! Não será possível definir se os complementos da base legada "
						+ "são tabelados ou não.");
				return;
			}
			
			LOGGER.info("Carregando lista de complementos CNJ do arquivo " + arquivoComplementos + "...");
			
			// Lista de complementos do CNJ. Essa lista definirá se o complemento é tabelado ou não, 
			// e também tem mais informações, como os outros tipos de complemento (Identificador e livre), descrição do
			// complemento e o seu nome.
			// Fonte: https://www.cnj.jus.br/sgt/gerenciar_complementos.php
			
			Scanner scanner = null;
			
			try {
				scanner = new Scanner(arquivoComplementos, "UTF-8");
				
				int linha = 0;
				while (scanner.hasNextLine()) {
					linha++;
					String line = scanner.nextLine();
					if (line.isEmpty() || line.startsWith("#")) {
						continue;
					}
					
					// Quebra cada linha em quatro partes: o código do complemento, nome do complemento, tipo do complemento, descrição do complemento
					String[] partes = line.split(";");
					if (partes.length < 3) {
						LOGGER.error("Inconsistência na linha " + linha + " do arquivo '" + arquivoComplementos + "': a linha deve conter pelo menos 3 campos, sendo o 4 opcional, separados por ponto e vírgula: o código do complemento, nome do complemento, tipo do complemento, descrição do complemento.");
					}
					
					int codigoComplemento;
					TipoTipoComplementoCNJ tipoTipoComplementoCNJ;
					
					//Se for necessário usar no futuro
					//String nomeComplemento = partes[1];					
					//String descricaoComplemento = partes[3];

					try {
						codigoComplemento = Integer.parseInt(partes[0]);
						tipoTipoComplementoCNJ = TipoTipoComplementoCNJ.getTipoTipoComplemento(partes[2]);
						tiposTipoComplementoCNJ.put(codigoComplemento, tipoTipoComplementoCNJ);
					} catch (NumberFormatException ex) {
						LOGGER.error("Inconsistência na linha " + linha + " do arquivo '" + arquivoComplementos + "': o código do complemento deve ser um valor numérico inteiro.");
					} catch (DataJudException e) {
						LOGGER.error("Inconsistência na linha " + linha + " do arquivo '" + arquivoComplementos + "': a descrição do tipo de complemento deve ser T, I ou L.");
					}
				}
			} catch (FileNotFoundException e) {
				LOGGER.error("Arquivo tipos_de_complemento_cnj.csv não existe! Não será possível definir se os complementos da base legada "
						+ "são tabelados ou não.");
			} finally {
				if (scanner != null) {
					scanner.close();
				}
			}
		}
	}
	
	/**
	 * Retorna o tipo do tipo de complemento (tabelado, identificador ou livre) dos complementos do CNJ.
	 * @param codigo Código do tipo do complemento do CNJ
	 * @return Enumeração {@link TipoTipoComplementoCNJ} referente ao código do tipo de complemento
	 */
	private TipoTipoComplementoCNJ getTipoTipoComplementoCNJPorCodigo(int codigo) {
		TipoTipoComplementoCNJ tipoTipoComplementoCNJ = tiposTipoComplementoCNJ.get(codigo);
		
		if (tipoTipoComplementoCNJ == null) {
			LOGGER.error("Não foi possível localizar o tipo do tipo de complemento do CNJ de código: " + codigo);
		}
		
		return tipoTipoComplementoCNJ;		
	}
}
