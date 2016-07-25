package br.jus.trt4.justica_em_numeros_2016.tabelas_cnj;

import static org.junit.Assert.*;

import java.sql.Connection;

import org.junit.Test;

import br.jus.cnj.intercomunicacao_2_2.TipoAssuntoProcessual;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;

public class AnalisaAssuntosCNJTest {

	@Test
	public void analisaAssuntoNacional() throws Exception {
		Connection conexaoBasePrincipal = Auxiliar.getConexaoPJe(1);
		try (AnalisaAssuntosCNJ a = new AnalisaAssuntosCNJ(1, conexaoBasePrincipal)) {
		
			// Assunto que existe em tabela nacional:
			// 2540 - Vale Transporte
			TipoAssuntoProcessual assunto = a.getAssunto(2540);
			assertNotNull(assunto);
			
			// Testa os campos do assunto nacional
			assertEquals(2540, assunto.getCodigoNacional());
			assertNull(assunto.getAssuntoLocal());
		}
	}

	@Test
	public void analisaAssuntoLocal() throws Exception {
		Connection conexaoBasePrincipal = Auxiliar.getConexaoPJe(1);
		try (AnalisaAssuntosCNJ a = new AnalisaAssuntosCNJ(1, conexaoBasePrincipal)) {
		
			// Assunto que NÃO existe em tabela nacional:
			// 55492 - Honorários na Justiça do Trabalho
			TipoAssuntoProcessual assunto = a.getAssunto(55492);
			assertNotNull(assunto);
			
			// Testa os campos do assunto nacional
			assertEquals(null, assunto.getCodigoNacional());
			assertNotNull(assunto.getAssuntoLocal());
			
			// Testa os campos do assunto local
			assertEquals(55492, assunto.getAssuntoLocal().getCodigoAssunto());
			assertEquals("Honorários na Justiça do Trabalho", assunto.getAssuntoLocal().getDescricao());
			
			// O campo "código pai nacional" será preenchido localizando assuntos de forma recursiva, ex:
			// 55492 - "Honorários na Justiça do Trabalho" não existe na tabela nacional, mas seu "pai" sim:
			// 8874 - "Sucumbência".
			assertEquals(8874, assunto.getAssuntoLocal().getCodigoPaiNacional());
		}
	}
}
