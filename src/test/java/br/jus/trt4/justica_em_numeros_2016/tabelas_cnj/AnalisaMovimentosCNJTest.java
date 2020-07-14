package br.jus.trt4.justica_em_numeros_2016.tabelas_cnj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Connection;

import org.junit.Test;

import br.jus.cnj.modeloDeTransferenciaDeDados.TipoMovimentoLocal;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoMovimentoProcessual;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.AbstractTestCase;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.dto.MovimentoDto;
import br.jus.trt4.justica_em_numeros_2016.dto.ProcessoDto;

public class AnalisaMovimentosCNJTest extends AbstractTestCase {

	@Test
	public void analisaMovimentoNacional() throws Exception {
		Connection conexaoBasePrincipal = Auxiliar.getConexaoPJe(1);
		try (AnalisaMovimentosCNJ a = new AnalisaMovimentosCNJ(1, conexaoBasePrincipal)) {
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
	}

	@Test
	public void analisaMovimentoLocal() throws Exception {
		Connection conexaoBasePrincipal = Auxiliar.getConexaoPJe(1);
		try (AnalisaMovimentosCNJ a = new AnalisaMovimentosCNJ(1, conexaoBasePrincipal)) {
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
	}
	
	/**
	 * Contorna um caso, como ocorreu no TRT4 com o processo 0020280-66.2013.5.04.0012, onde
	 * há um movimento que não possui descrição. Constatei que, no PJe, quando isso ocorre, é
	 * utilizada a descrição da tabela "tb_evento_processual".
	 */
	@Test
	public void analisaMovimentoLocalSemDescricao() throws Exception {
		Connection conexaoBasePrincipal = Auxiliar.getConexaoPJe(1);
		try (AnalisaMovimentosCNJ a = new AnalisaMovimentosCNJ(1, conexaoBasePrincipal)) {
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
	}
}
