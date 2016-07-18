package br.jus.trt4.justica_em_numeros.tasks;

import static org.junit.Assert.*;

import java.sql.ResultSet;

import org.junit.Test;

import br.jus.cnj.intercomunicacao_2_2.TipoProcessoJudicial;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.NamedParameterStatement;
import br.jus.trt4.justica_em_numeros_2016.tasks.Op_2_GeraXMLs;

/**
 * Testa a geração dos campos nos formatos exigidos pelo CNJ.
 * 
 * Como os testes envolvem consultas no banco de dados (não foram utilizados mocks), eles funcionarão
 * somente na base do TRT4!
 *  
 * @author fgiotto
 */
public class Op_2_GeraXMLsTest {

	@Test
	public void testarCamposProcesso2G() throws Exception {
		Op_2_GeraXMLs baixaDados = new Op_2_GeraXMLs(2);
		try {

			// Abre conexões com o PJe e prepara consultas a serem realizadas
			baixaDados.prepararConexao();

			// SQL que fará a consulta de um processo específico
			String sqlConsultaProcessos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/01_consulta_processos.sql");
			sqlConsultaProcessos += " AND nr_processo = :nr_processo";
			try (NamedParameterStatement nsConsultaProcessos = new NamedParameterStatement(baixaDados.getConexaoBasePrincipal(), sqlConsultaProcessos)) {
				nsConsultaProcessos.setString("nr_processo", "0020821-54.2013.5.04.0221");
				try (ResultSet rsProcessos = nsConsultaProcessos.executeQuery()) {
					if (!rsProcessos.next()) {
						fail("Não retornou dados do processo!");
					}
					
					TipoProcessoJudicial processoJudicial = new TipoProcessoJudicial();
					baixaDados.preencheDadosProcesso(processoJudicial, rsProcessos);
					
					// Confere se o número do processo está de acordo com o padrão definido no arquivo XSD:
					// http://www.cnj.jus.br/images/dti/Comite_Gestao_TIC/Modelo_Nacional_Interoperabilidade/versao_07_07_2014/intercomunicacao-2.2.2.xsd
					// <simpleType name="tipoNumeroUnico">
					// <pattern value="\d{20}"></pattern>
					assertEquals("00208215420135040221", processoJudicial.getDadosBasicos().getNumero());
					
				}
			}
		} finally {
			baixaDados.close();
		}

	}

}
