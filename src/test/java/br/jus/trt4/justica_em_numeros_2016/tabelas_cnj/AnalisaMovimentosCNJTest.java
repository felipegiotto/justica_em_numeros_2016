package br.jus.trt4.justica_em_numeros_2016.tabelas_cnj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Connection;
import java.util.List;

import org.junit.Test;

import br.jus.cnj.modeloDeTransferenciaDeDados.TipoComplementoNacional;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoMovimentoLocal;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoMovimentoNacional;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoMovimentoProcessual;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.AbstractTestCase;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.dto.ComplementoDto;
import br.jus.trt4.justica_em_numeros_2016.dto.MovimentoDto;
import br.jus.trt4.justica_em_numeros_2016.dto.ProcessoDto;

public class AnalisaMovimentosCNJTest extends AbstractTestCase {

	@Test
	public void analisaMovimentoNacional() throws Exception {
		Connection conexaoBasePrincipal = Auxiliar.getConexaoPJe(1);
		AnalisaMovimentosCNJ a = new AnalisaMovimentosCNJ(1, conexaoBasePrincipal);
		ProcessoDto processo = new ProcessoDto();
		
		// Movimento que existe em tabela nacional:
		// 26 - Distribuído por sorteio
		MovimentoDto movimentoDto = new MovimentoDto();
		movimentoDto.setCodMovimentoCNJ(26);
		movimentoDto.setTextoMovimento("Distribuído por sorteio");
		movimentoDto.setTextoEvento("Distribuído por sorteio");
		
		TipoMovimentoProcessual movimento = new TipoMovimentoProcessual();
		a.preencheDadosMovimentoCNJ(processo, movimento, movimentoDto);
		assertNotNull(movimento.getMovimentoNacional());
		assertEquals(26, movimento.getMovimentoNacional().getCodigoNacional());
		assertNull(movimento.getMovimentoLocal());
	}

	@Test
	public void analisaMovimentoLocal() throws Exception {
		Connection conexaoBasePrincipal = Auxiliar.getConexaoPJe(1);
		AnalisaMovimentosCNJ a = new AnalisaMovimentosCNJ(1, conexaoBasePrincipal);
		ProcessoDto processo = new ProcessoDto();
	
		// Movimento que NÃO existe em tabela nacional:
		// 50086 - Encerrada a conclusão
		MovimentoDto movimentoDto = new MovimentoDto();
		movimentoDto.setCodMovimentoCNJ(50086);
		movimentoDto.setTextoMovimento("Encerrada a conclusão");
		movimentoDto.setTextoEvento("Encerrada a conclusão");
		
		TipoMovimentoProcessual movimento = new TipoMovimentoProcessual();
		a.preencheDadosMovimentoCNJ(processo, movimento, movimentoDto);
		assertNull(movimento.getMovimentoNacional());
		assertNotNull(movimento.getMovimentoLocal());

		TipoMovimentoLocal movimentoLocal = movimento.getMovimentoLocal();
		assertEquals(50086, movimentoLocal.getCodigoMovimento());
		assertEquals("Encerrada a conclusão", movimentoLocal.getDescricao());
		
		// "Subindo" na árvore, o movimento 50086 possui como "pai" o movimento 48: "Escrivão/Diretor de Secretaria/Secretário Jurídico"
		assertEquals(48, movimentoLocal.getCodigoPaiNacional());
	}
	
	/**
	 * Contorna um caso, como ocorreu no TRT4 com o processo 0020280-66.2013.5.04.0012, onde
	 * há um movimento que não possui descrição. Constatei que, no PJe, quando isso ocorre, é
	 * utilizada a descrição da tabela "tb_evento_processual".
	 */
	@Test
	public void analisaMovimentoLocalSemDescricao() throws Exception {
		Connection conexaoBasePrincipal = Auxiliar.getConexaoPJe(1);
		AnalisaMovimentosCNJ a = new AnalisaMovimentosCNJ(1, conexaoBasePrincipal);
		ProcessoDto processo = new ProcessoDto();
	
		MovimentoDto movimentoDto = new MovimentoDto();
		movimentoDto.setCodMovimentoCNJ(-5);
		movimentoDto.setTextoMovimento(null);
		movimentoDto.setTextoEvento("Desmembrado o feito");
		
		TipoMovimentoProcessual movimento = new TipoMovimentoProcessual();
		a.preencheDadosMovimentoCNJ(processo, movimento, movimentoDto);
		assertNull(movimento.getMovimentoNacional());
		assertNotNull(movimento.getMovimentoLocal());

		TipoMovimentoLocal movimentoLocal = movimento.getMovimentoLocal();
		assertEquals(-5, movimentoLocal.getCodigoMovimento());
		assertEquals("Desmembrado o feito", movimentoLocal.getDescricao());
	}
	
	/**
	 * Analisa um movimento que é manipulado pela rotina "depara-jt-cnj" e possui complemento do tipo TABELADO,
	 * que precisa preencher em um campo especial (codTipoComplementoTabelado)
	 *
	 * @throws Exception
	 */
	@Test
	public void analisaMovimentoRemessaComDeParaETipoTabelado() throws Exception {
		Connection conexaoBasePrincipal = Auxiliar.getConexaoPJe(1);
		AnalisaMovimentosCNJ a = new AnalisaMovimentosCNJ(1, conexaoBasePrincipal);
		ProcessoDto processo = new ProcessoDto();
		processo.setNumeroInstancia(1);
	
		MovimentoDto movimentoDto = new MovimentoDto();
		movimentoDto.setCodMovimentoCNJ(123);
		movimentoDto.setTextoMovimento("Remetidos os autos para Órgão jurisdicional competente  para processar recurso");
		movimentoDto.setTextoEvento("Remetidos os autos para #{destino} #{motivo da remessa}");
		
		ComplementoDto complemento1 = new ComplementoDto();
		complemento1.setCodigoComplemento("38");
		complemento1.setCodigoTipoComplemento(18);
		complemento1.setNome("motivo da remessa");
		complemento1.setValor("para processar recurso");
		movimentoDto.getComplementos().add(complemento1);
		
		ComplementoDto complemento2 = new ComplementoDto();
		complemento2.setCodigoComplemento("7051");
		complemento2.setCodigoTipoComplemento(7);
		complemento2.setNome("destino");
		complemento2.setValor("Órgão jurisdicional competente ");
		movimentoDto.getComplementos().add(complemento2);
		
		TipoMovimentoProcessual movimento = new TipoMovimentoProcessual();
		a.preencheDadosMovimentoCNJ(processo, movimento, movimentoDto);
		
		// Confere se corrigiu o complemento conforme regras do de-para
		List<ComplementoDto> complementos = movimentoDto.getComplementos();
		assertEquals(2, complementos.size());
		
		complemento1 = complementos.get(0);
		assertEquals("38", complemento1.getCodigoComplemento());
		assertEquals("motivo_da_remessa", complemento1.getNome());
		assertEquals("em grau de recurso", complemento1.getValor());
		assertEquals(true, complemento1.isComplementoTipoTabelado());
		
		complemento2 = complementos.get(1);
		assertEquals("7051", complemento2.getCodigoComplemento());
		assertEquals("destino", complemento2.getNome());
		assertEquals("Órgão jurisdicional competente", complemento2.getValor());
		assertEquals(false, complemento2.isComplementoTipoTabelado());
	}
}
