package br.jus.trt4.justica_em_numeros_2016.tabelas_cnj;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
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
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Parametro;
import br.jus.trt4.justica_em_numeros_2016.dto.ComplementoDto;
import br.jus.trt4.justica_em_numeros_2016.dto.EventoDto;
import br.jus.trt4.justica_em_numeros_2016.dto.MovimentoDto;
import br.jus.trt4.justica_em_numeros_2016.dto.ProcessoDto;
import br.jus.trt4.justica_em_numeros_2016.enums.BaseEmAnaliseEnum;

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
	
	//A conexaoPJe só será utilizada quando baseEmAnaliseEnum.isBasePJe()
	public AnalisaMovimentosCNJ(BaseEmAnaliseEnum baseEmAnaliseEnum, Connection conexaoPJe) throws IOException, SQLException {
		super();
		if (baseEmAnaliseEnum.isBasePJe()) {
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
			this.eventosPorId = new HashMap<>();
			try (PreparedStatement psEventos = conexaoPJe.prepareStatement("SELECT * FROM tb_evento")) {
				try (ResultSet rsEventos = psEventos.executeQuery()) {
					while (rsEventos.next()) {
						EventoDto evento = new EventoDto(rsEventos);
						this.eventosPorId.put(evento.getId(), evento);
					}
				}
			}
			
			// Ferramenta DE-PARA de movimentos e complementos, criada pelo TRT3.
			if (Auxiliar.getParametroBooleanConfiguracao(Parametro.movimentos_aplicar_de_para, false)) {
				try {
					gerenteDeParaJTCNJ = GerenteDeParaJTCNJ.getInstancia();
					gerenteDeParaJTCNJ.carregarDePara();
				} catch (DeParaJTCNJException e) {
					throw new IOException("Erro iniciando DE-PARA de movimentos", e);
				}
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
			// Aplica o DE-PARA de movimentos e complementos, se solicitado.
			boolean forcarMovimentoNacional = false;
			if (gerenteDeParaJTCNJ != null) {
				
				// Instancia o movimento com os complementos, no formato esperado pelo DE-PARA
				MovimentoJT movimentoJT = new MovimentoJT(processo.getNumeroInstancia(), Integer.toString(codigoMovimento), Integer.toString(processo.getClasseJudicial().getCodigo()));
				for (ComplementoDto complementoDto : movimentoDto.getComplementos()) {
					movimentoJT.adicionaComplementoJT(Integer.toString(complementoDto.getCodigoTipoComplemento()), complementoDto.getCodigoComplemento(), complementoDto.getValor());
				}
				try {
					
					// Verifica se o movimento está nas tabelas DE-PARA
					Optional<MovimentoCNJ> recuperarMovimentoComplementosCNJ = gerenteDeParaJTCNJ.recuperarMovimentoComplementosCNJ(movimentoJT);
					if (recuperarMovimentoComplementosCNJ.isPresent()) {
						
						// Se o movimento está nas tabelas DE-PARA, precisa substituir totalmente os dados dos movimentos e complementos
						// do banco de dados do PJe pelos retornados pela ferramenta.
						MovimentoCNJ movimentoCNJ = recuperarMovimentoComplementosCNJ.get();
						forcarMovimentoNacional = true;
						
						// Substitui os complementos
						codigoMovimento = Integer.parseInt(movimentoCNJ.getCodigoMovimento());
						movimentoDto.getComplementos().clear();
						for(ComplementoCNJ complementoCNJ : movimentoCNJ.getListaComplementos()) {
							ComplementoDto complementoDto = new ComplementoDto();
							complementoDto.setCodigoTipoComplemento(Integer.parseInt(complementoCNJ.getCodigoTipo()));
							complementoDto.setNome(complementoCNJ.getDescricaoTipo());
							complementoDto.setCodigoComplemento(complementoCNJ.getCodigoValor());
							complementoDto.setValor(complementoCNJ.getDescricaoValor());
							complementoDto.setComplementoTipoTabelado(TipoTipoComplemento.TABELADO.equals(complementoCNJ.getTipoTipoComplemento()));
							movimentoDto.getComplementos().add(complementoDto);
						}
					}
				} catch (DeParaJTCNJException e) {
					throw new DataJudException("Erro ao aplicar DE-PARA de movimentos e complementos do TRT3", e);
				}
			}
			
			// TODO: Preencher este novo campo do XSD
			// Atributo que permite a atribuição da decisão como sendo monocrática (proferida por um magistrado), ou colegiada;
			// Valores possíveis são numéricos (0 ou 1):
			// 0 - decisão MONOCRATICA
			// 1 - decisão COLEGIADA
			// movimento.setTipoDecisao(...);
			
			// Verifica se o movimento deve ser enviado como NACIONAL ou como LOCAL
			if (forcarMovimentoNacional || movimentoExisteNasTabelasNacionais(codigoMovimento)) {
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
		} else {
			//FIXME: Apenas movimentos nacionais do sistema judicial legado serão enviados pelo TRT6. Se algum Regional for enviar movimentos locais para o 
			//sistema legado, implementar aqui.
			TipoMovimentoNacional movimentoNacional = new TipoMovimentoNacional();
			movimentoNacional.setCodigoNacional(codigoMovimento);
			movimento.setMovimentoNacional(movimentoNacional);
		}
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

	
	public boolean movimentoExisteNasTabelasNacionais(int codigoAssunto) {
		return movimentosProcessuaisCNJ.contains(codigoAssunto);
	}
}
