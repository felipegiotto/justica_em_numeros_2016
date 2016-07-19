package br.jus.trt4.justica_em_numeros.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;

import br.jus.cnj.intercomunicacao_2_2.TipoCabecalhoProcesso;
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
 * Os campos serão validados conforme os padrões exigidos em 
 * http://www.cnj.jus.br/images/dti/Comite_Gestao_TIC/Modelo_Nacional_Interoperabilidade/versao_07_07_2014/intercomunicacao-2.2.2.xsd
 * 
 * @author fgiotto
 */
public class Op_2_GeraXMLsTest {

	@Test
	public void testarCamposProcesso2G() throws Exception {
		
		TipoProcessoJudicial processoJudicial = retornaDadosProcesso(2, "0020821-54.2013.5.04.0221");

	    //TESTANDO: protected TipoCabecalhoProcesso dadosBasicos;
		TipoCabecalhoProcesso dadosBasicos = processoJudicial.getDadosBasicos();
	    //  TESTAR: TipoProcessoJudicial protected List<TipoPoloProcessual> polo;
	    //  TESTAR: TipoProcessoJudicial protected List<TipoAssuntoProcessual> assunto;
	    //  TESTAR: TipoProcessoJudicial protected List<String> magistradoAtuante;
	    //  TESTAR: TipoProcessoJudicial protected List<TipoVinculacaoProcessual> processoVinculado;
	    //  TESTAR: TipoProcessoJudicial protected List<String> prioridade;
	    //  TESTAR: TipoProcessoJudicial protected List<TipoParametro> outroParametro;
		
		// Valor da Causa
		/*
			<element minOccurs="0" name="valorCausa" type="double">
				<annotation>
					<documentation>Valor da causa.</documentation>
				</annotation>
			</element>
		 */
		assertEquals(50000.0, dadosBasicos.getValorCausa());
		
	    //  TESTAR: TipoProcessoJudicial protected TipoOrgaoJulgador orgaoJulgador;
		
		// Número do processo
		/*
			<simpleType name="tipoNumeroUnico">
				<annotation>
					<documentation>
						Tipo de elemento que limita a indicação de um número
						de
						processo ao padrão da numeração única.
					</documentation>
				</annotation>
				<restriction base="string">
					<pattern value="\d{20}"></pattern>
				</restriction>
			</simpleType>
		 */
		assertEquals("00208215420135040221", dadosBasicos.getNumero());
		
	    //  TESTAR: TipoProcessoJudicial protected Integer competencia; // Opcional
		
	    // Classe Processual
		/*
			<attribute name="classeProcessual" type="int" use="required">
				<annotation>
					<documentation>
						Código da classe processual conforme Resolução 46.
					</documentation>
				</annotation>
			</attribute>
		 */
		assertEquals(1009, dadosBasicos.getClasseProcessual()); // RECURSO ORDINÁRIO (1009)
		
		// Código da localidade
		/*
			<attribute name="codigoLocalidade" type="string"
				use="required">
				<annotation>
					<documentation>
						Código identificador da localidade a que pertence ou
						deve pertencer o processo. O atributo é obrigatório,
						especialmente para permitir a distribuição de
						processos iniciais por meio do uso desse serviço.
					</documentation>
				</annotation>
			</attribute>
		 */
		assertEquals("4314902", dadosBasicos.getCodigoLocalidade()); // 4314902: Porto Alegre
		
		// Nível de sigilo
		/*
			<attribute name="nivelSigilo" type="int" use="required">
				<annotation>
					<documentation>
						Nível de sigilo a ser aplicado ao processo.
						Dever-se-á utilizar os seguintes níveis: - 0:
						públicos, acessíveis a todos os servidores do
						Judiciário e dos demais órgãos públicos de
						colaboração na administração da Justiça, assim como
						aos advogados e a qualquer cidadão - 1: segredo de
						justiça, acessíveis aos servidores do Judiciário,
						aos servidores dos órgãos públicos de colaboração na
						administração da Justiça e às partes do processo. -
						2: sigilo mínimo, acessível aos servidores do
						Judiciário e aos demais órgãos públicos de
						colaboração na administração da Justiça - 3: sigilo
						médio, acessível aos servidores do órgão em que
						tramita o processo, à(s) parte(s) que provocou(ram)
						o incidente e àqueles que forem expressamente
						incluídos - 4: sigilo intenso, acessível a classes
						de servidores qualificados (magistrado, diretor de
						secretaria/escrivão, oficial de gabinete/assessor)
						do órgão em que tramita o processo, às partes que
						provocaram o incidente e àqueles que forem
						expressamente incluídos - 5: sigilo absoluto,
						acessível apenas ao magistrado do órgão em que
						tramita, aos servidores e demais usuários por ele
						indicado e às partes que provocaram o incidente.
					</documentation>
				</annotation>
			</attribute>
		 */
		assertEquals(0, dadosBasicos.getNivelSigilo());
		
	    //  TESTAR: TipoProcessoJudicial protected Boolean intervencaoMP; // Opcional
	    //  TESTAR: TipoProcessoJudicial protected Integer tamanhoProcesso; // Opcional
		
		// Data Ajuizamento
		/*
			<attribute name="dataAjuizamento" type="cnj:tipoDataHora">
				<annotation>
					<documentation>Indica a data em que o processo foi inicialmente recebido pelo Poder Judiciário no órgão consultado. Caso se trate de instância recursal, especial ou extraordinária, deve refletir a data de entrada do processo nessa instância.</documentation>
				</annotation>
			</attribute>
			<simpleType name="tipoDataHora">
				<annotation>
					<documentation>Tipo de elemento destinado a permitir a indicação de
						data e hora no formato
						AAAAMMDDHHMMSS
					</documentation>
				</annotation>
				<restriction base="string">
					<pattern value="\d{4}[0-1]\d[0-3]\d[0-2]\d[0-6]\d[0-6]\d"></pattern>
				</restriction>
			</simpleType>
		*/
		//            AAAAMMDDHHMMSS
		assertEquals("20150922083157", dadosBasicos.getDataAjuizamento());
		
	    //TESTAR: protected List<TipoMovimentoProcessual> movimento;
	    //TESTAR: protected List<TipoDocumento> documento;
		
	}
	
	public TipoProcessoJudicial retornaDadosProcesso(int grau, String numeroProcesso) throws SQLException, IOException {
		
		Op_2_GeraXMLs baixaDados = new Op_2_GeraXMLs(grau);
		try {

			// Abre conexões com o PJe e prepara consultas a serem realizadas
			baixaDados.prepararConexao();

			// SQL que fará a consulta de um processo específico
			String sqlConsultaProcessos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/01_consulta_processos.sql");
			sqlConsultaProcessos += " AND nr_processo = :nr_processo";
			try (NamedParameterStatement nsConsultaProcessos = new NamedParameterStatement(baixaDados.getConexaoBasePrincipal(), sqlConsultaProcessos)) {
				nsConsultaProcessos.setString("nr_processo", numeroProcesso);
				try (ResultSet rsProcessos = nsConsultaProcessos.executeQuery()) {
					if (!rsProcessos.next()) {
						fail("Não retornou dados do processo!");
					}
					
					// Chama o método que preenche um TipoProcessoJudicial a partir de um ResultSet
					TipoProcessoJudicial processoJudicial = new TipoProcessoJudicial();
					baixaDados.preencheDadosProcesso(processoJudicial, rsProcessos);
					return processoJudicial;					
				}
			}
		} finally {
			baixaDados.close();
		}
	}

}
