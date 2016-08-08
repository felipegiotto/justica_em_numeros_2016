package br.jus.trt4.justica_em_numeros_2016.tabelas_cnj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Connection;

import org.junit.Test;

import br.jus.cnj.intercomunicacao_2_2.TipoMovimentoLocal;
import br.jus.cnj.intercomunicacao_2_2.TipoMovimentoProcessual;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.AbstractTestCase;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;

public class AnalisaMovimentosCNJTest extends AbstractTestCase {

	@Test
	public void analisaMovimentoNacional() throws Exception {
		Connection conexaoBasePrincipal = Auxiliar.getConexaoPJe(1);
		try (AnalisaMovimentosCNJ a = new AnalisaMovimentosCNJ(1, conexaoBasePrincipal)) {
		
			// Movimento que existe em tabela nacional:
			// 26 - Distribuído por sorteio
			TipoMovimentoProcessual movimento = new TipoMovimentoProcessual();
			a.preencheDadosMovimentoCNJ(movimento, 26, "Distribuído por sorteio", "Distribuído por sorteio");
			assertNotNull(movimento.getMovimentoNacional());
			assertEquals(26, movimento.getMovimentoNacional().getCodigoNacional());
			assertNull(movimento.getMovimentoLocal());
		}
	}

	@Test
	public void analisaMovimentoLocal() throws Exception {
		Connection conexaoBasePrincipal = Auxiliar.getConexaoPJe(1);
		try (AnalisaMovimentosCNJ a = new AnalisaMovimentosCNJ(1, conexaoBasePrincipal)) {
		
			// Movimento que NÃO existe em tabela nacional:
			// 50086 - Encerrada a conclusão
			TipoMovimentoProcessual movimento = new TipoMovimentoProcessual();
			a.preencheDadosMovimentoCNJ(movimento, 50086, "Encerrada a conclusão", "Encerrada a conclusão");
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
		
			TipoMovimentoProcessual movimento = new TipoMovimentoProcessual();
			a.preencheDadosMovimentoCNJ(movimento, -5, null, "Desmembrado o feito");
			assertNull(movimento.getMovimentoNacional());
			assertNotNull(movimento.getMovimentoLocal());

			TipoMovimentoLocal movimentoLocal = movimento.getMovimentoLocal();
			assertEquals(-5, movimentoLocal.getCodigoMovimento());
			assertEquals("Desmembrado o feito", movimentoLocal.getDescricao());
		}
	}
}
