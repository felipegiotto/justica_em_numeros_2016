package br.jus.trt4.justica_em_numeros_2016.tabelas_cnj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Connection;

import org.junit.Test;

import br.jus.cnj.modeloDeTransferenciaDeDados.TipoAssuntoLocal;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoAssuntoProcessual;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.AbstractTestCase;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.enums.BaseEmAnaliseEnum;

public class AnalisaAssuntosCNJTest extends AbstractTestCase {

	@Test
	public void analisaAssuntoNacional() throws Exception {
		BaseEmAnaliseEnum baseEmAnalise = BaseEmAnaliseEnum.PJE;
		Connection conexaoBasePrincipal = Auxiliar.getConexao(1, baseEmAnalise);
		try (AnalisaAssuntosCNJ a = new AnalisaAssuntosCNJ(1, conexaoBasePrincipal, false, baseEmAnalise)) {
		
			// Assunto que existe em tabela nacional:
			// 2540 - Vale Transporte
			TipoAssuntoProcessual assunto = a.getAssunto(2540, baseEmAnalise);
			assertNotNull(assunto);
			
			// Testa os campos do assunto nacional
			assertEquals(2540, (int) assunto.getCodigoNacional());
			assertNull(assunto.getAssuntoLocal());
		}
	}

	/**
	 * Testa se o assunto está sendo exibido com seu nome completo (com toda a hierarquia)
	 *
	 * Se, eventualmente, esse assunto local for inserido nas tabelas nacionais, será preciso buscar outro assunto local e alterar esse teste.
	 */
	@Test
	public void analisaNomeCompletoAssuntoLocal() throws Exception {
		BaseEmAnaliseEnum baseEmAnalise = BaseEmAnaliseEnum.PJE;
		Connection conexaoBasePrincipal = Auxiliar.getConexao(1, baseEmAnalise);
		try (AnalisaAssuntosCNJ a = new AnalisaAssuntosCNJ(1, conexaoBasePrincipal, false, baseEmAnalise)) {
			TipoAssuntoProcessual assunto = a.getAssunto(10861, baseEmAnalise);
			
			// Testa os campos do assunto local
			TipoAssuntoLocal assuntoLocal = assunto.getAssuntoLocal();
			assertNotNull(assuntoLocal);
			assertEquals(10861, assuntoLocal.getCodigoAssunto());
			assertEquals("DIREITO PROCESSUAL CIVIL E DO TRABALHO (8826) / Liquidação / Cumprimento / Execução (9148) / Prisão Civil (10573) / Alienação Fiduciária", assunto.getAssuntoLocal().getDescricao());
		}
	}
	
	@Test
	public void analisaAssuntoLocal() throws Exception {
		BaseEmAnaliseEnum baseEmAnalise = BaseEmAnaliseEnum.PJE;
		Connection conexaoBasePrincipal = Auxiliar.getConexao(1, baseEmAnalise);
		try (AnalisaAssuntosCNJ a = new AnalisaAssuntosCNJ(1, conexaoBasePrincipal, false, baseEmAnalise)) {
		
			// Assunto que NÃO existe em tabela nacional:
			// 55492 - Honorários na Justiça do Trabalho
			TipoAssuntoProcessual assunto = a.getAssunto(55492, baseEmAnalise);
			assertNotNull(assunto);
			
			// Testa os campos do assunto nacional
			assertEquals(null, assunto.getCodigoNacional());
			assertNotNull(assunto.getAssuntoLocal());
			
			// Testa os campos do assunto local
			assertEquals(55492, assunto.getAssuntoLocal().getCodigoAssunto());
			assertEquals("DIREITO PROCESSUAL CIVIL E DO TRABALHO (8826) / Partes e Procuradores (8842) / Sucumbência (8874) / Honorários na Justiça do Trabalho", assunto.getAssuntoLocal().getDescricao());
			
			// O campo "código pai nacional" será preenchido localizando assuntos de forma recursiva, ex:
			// 55492 - "Honorários na Justiça do Trabalho" não existe na tabela nacional, mas seu "pai" sim:
			// 8874 - "Sucumbência".
			assertEquals(8874, assunto.getAssuntoLocal().getCodigoPaiNacional());
		}
	}
	
	@Test
	public void analisaAssuntoLocalMapeadoPorTabelaDePara() throws Exception {
		BaseEmAnaliseEnum baseEmAnalise = BaseEmAnaliseEnum.PJE;
		Connection conexaoBasePrincipal = Auxiliar.getConexao(1, baseEmAnalise);
		try (AnalisaAssuntosCNJ a = new AnalisaAssuntosCNJ(1, conexaoBasePrincipal, false, baseEmAnalise)) {
		
			// Mapeia manualmente um assunto local, para executar o teste
			// 55492 (Honorários da Justiça do Trabalho), mapeado para 10655 (Honorários Advocatícios)
			a.getAssuntosProcessuaisDePara().put(55492, 10655);
			
			// Assunto que NÃO existe em tabela nacional, mas que foi mapeado por tabela DE-PARA
			// 55492 (Honorários na Justiça do Trabalho)
			TipoAssuntoProcessual assunto = a.getAssunto(55492, baseEmAnalise);
			assertNotNull(assunto);
			
			// Testa os campos do assunto nacional
			assertEquals(10655, (int) assunto.getCodigoNacional());
			assertNull(assunto.getAssuntoLocal());
		}
	}
}
