package br.jus.trt4.justica_em_numeros.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import br.jus.cnj.intercomunicacao_2_2.ModalidadeDocumentoIdentificador;
import br.jus.cnj.intercomunicacao_2_2.ModalidadeGeneroPessoa;
import br.jus.cnj.intercomunicacao_2_2.ModalidadePoloProcessual;
import br.jus.cnj.intercomunicacao_2_2.TipoCabecalhoProcesso;
import br.jus.cnj.intercomunicacao_2_2.TipoDocumentoIdentificacao;
import br.jus.cnj.intercomunicacao_2_2.TipoMovimentoProcessual;
import br.jus.cnj.intercomunicacao_2_2.TipoOrgaoJulgador;
import br.jus.cnj.intercomunicacao_2_2.TipoParte;
import br.jus.cnj.intercomunicacao_2_2.TipoPessoa;
import br.jus.cnj.intercomunicacao_2_2.TipoPoloProcessual;
import br.jus.cnj.intercomunicacao_2_2.TipoProcessoJudicial;
import br.jus.trt4.justica_em_numeros_2016.tasks.Op_3_GeraXMLsIndividuais;

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
public class Op_3_GeraXMLsIndividuaisTest {

	private static final Logger LOGGER = LogManager.getLogger(Op_3_GeraXMLsIndividuaisTest.class);
	
	@Test
	public void testarCamposProcesso2G() throws Exception {
		
		TipoProcessoJudicial processoJudicial = retornaDadosProcesso(2, "0020821-54.2013.5.04.0221");

	    // TESTES DE protected TipoCabecalhoProcesso dadosBasicos;
		TipoCabecalhoProcesso dadosBasicos = processoJudicial.getDadosBasicos();
		
		//  TODO: TESTAR: TipoProcessoJudicial protected List<TipoPoloProcessual> polo;
	    //  TODO: TESTAR: TipoProcessoJudicial protected List<TipoAssuntoProcessual> assunto;
	    //  TODO: TESTAR: TipoProcessoJudicial protected List<String> magistradoAtuante;
	    //  TODO: TESTAR: TipoProcessoJudicial protected List<TipoVinculacaoProcessual> processoVinculado;
	    //  TODO: TESTAR: TipoProcessoJudicial protected List<String> prioridade;
	    //  TODO: TESTAR: TipoProcessoJudicial protected List<TipoParametro> outroParametro;
		
		// Valor da Causa
		/*
			<element minOccurs="0" name="valorCausa" type="double">
				<annotation>
					<documentation>Valor da causa.</documentation>
				</annotation>
			</element>
		 */
		assertEquals(50000.0, dadosBasicos.getValorCausa());
		
	    //  TODO: TESTAR: TipoProcessoJudicial protected TipoOrgaoJulgador orgaoJulgador;
		
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
		
	    // CAMPO NAO PREENCHIDO: TipoProcessoJudicial protected Integer competencia; // Opcional
		
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
		
	    // CAMPO NAO PREENCHIDO: TipoProcessoJudicial protected Boolean intervencaoMP; // Opcional
	    // CAMPO NAO PREENCHIDO: TipoProcessoJudicial protected Integer tamanhoProcesso; // Opcional
		
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
	}
	
	@Test
	public void testCampoNivelSigilo() throws Exception {
		
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
		
		TipoProcessoJudicial processoNaoSigiloso = retornaDadosProcesso(2, "0020821-54.2013.5.04.0221");
		assertEquals(0, processoNaoSigiloso.getDadosBasicos().getNivelSigilo());
		
		TipoProcessoJudicial processoSigiloso = retornaDadosProcesso(2, "0020583-31.2014.5.04.0017");
		assertEquals(5, processoSigiloso.getDadosBasicos().getNivelSigilo());
	}
	
	@Test
	public void testCampoCodigoLocalidade() throws Exception {
		
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
		
		TipoProcessoJudicial processo2G = retornaDadosProcesso(2, "0020821-54.2013.5.04.0221");
		assertEquals("4314902", processo2G.getDadosBasicos().getCodigoLocalidade()); // 4314902: Porto Alegre
		
		TipoProcessoJudicial processo1G = retornaDadosProcesso(1, "0020450-29.2013.5.04.0791");
		assertEquals("4306809", processo1G.getDadosBasicos().getCodigoLocalidade()); // 4306809: ENCANTADO
	}
	
	@Test
	public void testCampoOrgaoJulgadorServentiaCNJ() throws Exception {
		
		/*
		 * Órgãos Julgadores
Para envio do elemento <orgaoJulgador >, pede-se os atributos <codigoOrgao> e <nomeOrgao>, conforme definido em <tipoOrgaoJulgador>. 
Em <codigoOrgao> deverão ser informados os mesmos códigos das serventias judiciárias cadastradas no Módulo de Produtividade Mensal (Resolução CNJ nº 76/2009).
Em <nomeOrgao> deverão ser informados os mesmos descritivos das serventias judiciárias cadastradas no Módulo de Produtividade Mensal (Resolução CNJ nº 76/2009)
			Fonte: http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/2016-06-02-17-51-25
		 */
		/*
			10. Como preencher o campo “Órgão Julgador”? 
			    R: Os Tribunais deverão seguir os mesmos códigos e descrições utilizadas no módulo de produtividade.
			Fonte: http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/perguntas-frequentes
		 */
		
		// Processo 2G, responsabilidade do gabinete: GABINETE VANIA MATTOS;47074;GABINETE VANIA MARIA CUNHA MATTOS
		TipoOrgaoJulgador orgaoJulgador2G = retornaDadosProcesso(2, "0020821-54.2013.5.04.0221").getDadosBasicos().getOrgaoJulgador(); 
		assertEquals("47074", orgaoJulgador2G.getCodigoOrgao()); 
		assertEquals("GABINETE VANIA MARIA CUNHA MATTOS", orgaoJulgador2G.getNomeOrgao());
		
		// Processo 1G, responsabilidade da vara: 46904;VT Encantado
		TipoOrgaoJulgador orgaoJulgador1G = retornaDadosProcesso(1, "0020450-29.2013.5.04.0791").getDadosBasicos().getOrgaoJulgador();
		assertEquals("46904", orgaoJulgador1G.getCodigoOrgao());
		assertEquals("VT Encantado", orgaoJulgador1G.getNomeOrgao());
	}
	
	@Test
	public void testCampoOrgaoJulgadorTipoInstancia() throws Exception {
		
		/*
		<attribute name="instancia" use="required">
            <annotation>
            	<documentation>Os tipos de instância podem ser:
- ORIG: instância originária em que o processo teve início;
- REV: instância de revisão direta de um processo originariamente proposto em outra instância;
- ESP: instância de revisão especial de processo submetido ou não à revisão direta;
- EXT: instância de revisão extraordinária
- ADM: instância administrativa de análise.
</documentation>
            </annotation>
            <simpleType>
				<restriction base="string">
					<enumeration value="ORIG"></enumeration>
					<enumeration value="REV"></enumeration>
					<enumeration value="ESP"></enumeration>
					<enumeration value="EXT"></enumeration>
					<enumeration value="ADM"></enumeration>
				</restriction>
			</simpleType>
		</attribute>
		 */
		
		// Processo 1G
		TipoOrgaoJulgador orgaoJulgador1G = retornaDadosProcesso(1, "0020450-29.2013.5.04.0791").getDadosBasicos().getOrgaoJulgador();
		assertEquals("ORIG", orgaoJulgador1G.getInstancia());
		
		// Processo 2G, Recurso ordinário
		TipoOrgaoJulgador orgaoJulgador2G = retornaDadosProcesso(2, "0020821-54.2013.5.04.0221").getDadosBasicos().getOrgaoJulgador(); 
		assertEquals("REV", orgaoJulgador2G.getInstancia());
		
		// Processo 2G, originário
		TipoOrgaoJulgador orgaoJulgador2GOrig = retornaDadosProcesso(2, "0020970-29.2016.5.04.0000").getDadosBasicos().getOrgaoJulgador(); 
		assertEquals("ORIG", orgaoJulgador2GOrig.getInstancia());
	}
	
	@Test
	public void testCamposPartes() throws Exception {
		
		TipoProcessoJudicial processoJudicial = retornaDadosProcesso(1, "0020591-86.2014.5.04.0282");
		TipoPoloProcessual poloAtivo = getPolo(ModalidadePoloProcessual.AT, processoJudicial.getDadosBasicos().getPolo());
		TipoPoloProcessual poloPassivo = getPolo(ModalidadePoloProcessual.PA, processoJudicial.getDadosBasicos().getPolo());
	    // TODO: Testar campo: protected TipoPessoa pessoa;
		// TODO: Testar campo: protected String interessePublico;
		// TODO: Testar campo: protected List<TipoRepresentanteProcessual> advogado;
		// TODO: Testar campo: protected List<TipoParte> pessoaProcessualRelacionada;
		// TODO: Testar campo: protected Boolean assistenciaJudiciaria;
		// TODO: Testar campo: protected Integer intimacaoPendente;
		// TODO: Testar campo: protected ModalidadeRelacionamentoProcessual relacionamentoProcessual;
		
		
		TipoParte parteAtivo = getParteComNome("ROGERIO MELO DE CASTRO", poloAtivo.getParte());
		TipoPessoa pessoaAtivo = parteAtivo.getPessoa();
		TipoParte partePassivo = getParteComNome("FAMF CONSTRUTORA LTDA", poloPassivo.getParte());
		TipoPessoa pessoaPassivo = partePassivo.getPessoa();
		// TODO: Testar campo: protected List<TipoDocumentoIdentificacao> documento;
		// TODO: Testar campo: protected List<TipoEndereco> endereco;
		// TODO: Testar campo: protected List<TipoRelacionamentoPessoal> pessoaRelacionada;
		// TODO: Testar campo: protected TipoPessoa pessoaVinculada;
		// TODO: Testar campo: protected String nome;
		// TODO: Testar campo: protected String nomeGenitor;
		// TODO: Testar campo: protected String nomeGenitora;
		// TODO: Testar campo: protected String dataNascimento;
		// TODO: Testar campo: protected String dataObito;
		// TODO: Testar campo: protected String numeroDocumentoPrincipal;
		// TODO: Testar campo: protected TipoQualificacaoPessoa tipoPessoa;
		// TODO: Testar campo: protected String cidadeNatural;
		// TODO: Testar campo: protected String estadoNatural;
		// TODO: Testar campo: protected String nacionalidade;
		
		// Documento principal
		/*
			<attribute name="numeroDocumentoPrincipal" type="cnj:tipoCadastroIdentificador"
				use="optional">
				<annotation>
					<documentation>
						Número do documento principal da pessoa
						individualizada, devendo ser utilizado o RIC ou o
						CPF para pessoas
						físicas, nessa ordem, ou o CNPJ
						para pessoas jurídicas. O atributo é
						opcional em
						razão da possibilidade de haver pessoas sem
						documentos ou
						cujos dados não estão disponíveis.
					</documentation>
				</annotation>
			</attribute>
	
			<simpleType name="tipoCadastroIdentificador">
				<annotation>
					<documentation>
						Tipo de dados destinado a limitar a entrada de dados relativos a
						cadastros no Ministério da Fazenda Brasileiro (CPF e CNPJ) e/ou ao
						registro individual do cidadão (riC). A restrição imposta é que o
						dado qualificado por este tipo seja integralmente numérico, com 11
						(CPF e riC) ou 14 (CNPJ) dígitos.
					</documentation>
				</annotation>
				<restriction base="string">
					<pattern value="(\d{11})|(\d{14})"></pattern>
				</restriction>
			</simpleType>
		 */
		assertEquals("57310009053", pessoaAtivo.getNumeroDocumentoPrincipal());
		assertEquals("17640725000147", pessoaPassivo.getNumeroDocumentoPrincipal());
		
		// Outros documentos
		TipoDocumentoIdentificacao documento = pessoaAtivo.getDocumento().get(0);
		assertEquals(ModalidadeDocumentoIdentificador.TE, documento.getTipoDocumento()); // Título de Eleitor
		assertEquals("0059509160434", documento.getCodigoDocumento());
		assertEquals("Tribunal Superior Eleitoral", documento.getEmissorDocumento());
		
		// Gênero:
		/*
			<simpleType name="modalidadeGeneroPessoa">
		        <annotation>
		        	<documentation>Tipo destinado a permitir a identificação do gẽnero de uma dada pessoa, podendo ser:
		- M: masculino
		- F: feminido
		- D: desconhecido</documentation>
		        </annotation>
		        <restriction base="string">
					<enumeration value="M" />
					<enumeration value="F" />
					<enumeration value="D" />
				</restriction>
			</simpleType>
		 */
		assertEquals(ModalidadeGeneroPessoa.M, pessoaAtivo.getSexo());
	}
	
	
	private TipoPoloProcessual getPolo(ModalidadePoloProcessual siglaPolo, List<TipoPoloProcessual> polos) {
		for (TipoPoloProcessual polo: polos) {
			if (siglaPolo.equals(polo.getPolo())) {
				return polo;
			}
		}
		return null;
	}

	
	private TipoMovimentoProcessual getMovimentoComData(String data, List<TipoMovimentoProcessual> movimentos) {
		for (TipoMovimentoProcessual movimento: movimentos) {
			if (data.equals(movimento.getDataHora())) {
				return movimento;
			}
		}
		fail("O movimento com data '" + data + "' não está na lista de movimentos.");
		return null;
	}

	
	private TipoParte getParteComNome(String nome, List<TipoParte> partes) {
		
		ArrayList<String> nomes = new ArrayList<>();
		for (TipoParte parte: partes) {
			String nomeParte = parte.getPessoa().getNome();
			if (nome.equals(nomeParte)) {
				return parte;
			}
			nomes.add(nomeParte);
		}
		fail("A pessoa '" + nome + "' não está na lista de partes (" + nomes + ")");
		return null;
	}

	@Test
	public void testCampoMovimentoProcessual() throws Exception {
		
		// Código nacional do movimento
		/*
			<complexType name="tipoMovimentoProcessual">
				<annotation>
					<documentation>
						Tipo de elemento destinado a permitir apresentar
						informações relativas à movimentação processual.
					</documentation>
				</annotation>
		 */
		TipoMovimentoProcessual movimento = retornaDadosProcesso(2, "0020821-54.2013.5.04.0221").getMovimento().get(0);
		assertEquals(26, movimento.getMovimentoNacional().getCodigoNacional());
		
		// Data/Hora
		/*
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
		assertEquals("20150922083157", movimento.getDataHora());
		
		// Nível de sigilo
		assertEquals(0, movimento.getNivelSigilo());
	}
	
	@Test
	public void testCampoMovimentoProcessualComplemento() throws Exception {
		
		TipoMovimentoProcessual movimento = getMovimentoComData("20160204145720", retornaDadosProcesso(1, "0020063-73.2016.5.04.0123").getMovimento());
		
		/*
			<element name="complemento" type="string" maxOccurs="unbounded"
				minOccurs="0">
				<annotation>
					<documentation>
						Elemento destinado a permitir a inclusão dos
						complementos de movimentação.
					</documentation>
				</annotation>
			</element>
			O elemento <complemento> possui formato string e deverá ser preenchido da seguinte forma:
			<código do complemento><”:”><descrição do complemento><”:”><código do complemento tabelado><descrição do complemento tabelado, ou de texto livre, conforme o caso>
				
			Ex.: no movimento 123, seria
				18:motivo_da_remessa:38:em grau de recurso
				7:destino:1ª Vara Cível
			Fonte: http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/2016-06-02-17-51-25
		 */
		assertEquals("5050:nome do magistrado:GRACIELA MAFFEI", movimento.getComplemento().get(0)); // Complemento sem código
		assertEquals("3:tipo de conclusão:5:despacho", movimento.getComplemento().get(1)); // Complemento com código
	}
	
	public TipoProcessoJudicial retornaDadosProcesso(int grau, String numeroProcesso) throws SQLException, IOException {
		
		Op_3_GeraXMLsIndividuais baixaDados = new Op_3_GeraXMLsIndividuais(grau);
		try {
			baixaDados.prepararConexao();
			return baixaDados.analisarProcessoJudicialCompleto(numeroProcesso);
		} finally {
			baixaDados.close();
		}
	}

}
