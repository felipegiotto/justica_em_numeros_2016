package br.jus.trt4.justica_em_numeros.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import br.jus.cnj.modeloDeTransferenciaDeDados.ModalidadeGeneroPessoa;
import br.jus.cnj.modeloDeTransferenciaDeDados.ModalidadePoloProcessual;
import br.jus.cnj.modeloDeTransferenciaDeDados.ModalidadeRepresentanteProcessual;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoAssuntoLocal;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoAssuntoProcessual;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoCabecalhoProcesso;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoComplementoNacional;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoEndereco;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoMovimentoProcessual;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoOrgaoJulgador;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoParte;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoPessoa;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoPoloProcessual;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoProcessoJudicial;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoRelacaoIncidental;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoRepresentanteProcessual;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.AbstractTestCase;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.enums.BaseEmAnaliseEnum;
import br.jus.trt4.justica_em_numeros_2016.enums.Parametro;
import br.jus.trt4.justica_em_numeros_2016.tasks.Op_2_GeraXMLsIndividuais;

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
public class Op_2_GeraXMLsIndividuaisTest extends AbstractTestCase {

	/**
	 * Testa a carga em lote de dados de processos.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGerarProcessosEmLote() throws Exception {
		Op_2_GeraXMLsIndividuais baixaDados = new Op_2_GeraXMLsIndividuais();
		int grau = 2;
		BaseEmAnaliseEnum base = BaseEmAnaliseEnum.PJE;
		try {
			baixaDados.prepararConexao(grau, base, true);
			List<String> numeros = new ArrayList<>();
			numeros.add("0020821-54.2013.5.04.0221");
			numeros.add("0020583-31.2014.5.04.0017");
			baixaDados.prepararCacheDadosProcessos(numeros, grau, base, true);
			
			TipoProcessoJudicial processo1 = baixaDados.analisarProcessoJudicialCompleto("0020821-54.2013.5.04.0221", grau, base);
			assertEquals("00208215420135040221", processo1.getDadosBasicos().getNumero());
			
			TipoProcessoJudicial processo2 = baixaDados.analisarProcessoJudicialCompleto("0020583-31.2014.5.04.0017", grau, base);
			assertEquals("00205833120145040017", processo2.getDadosBasicos().getNumero());
			
		} finally {
			baixaDados.close();
		}
	}
	
	@Test
	public void testCamposProcesso2G() throws Exception {
		
		TipoProcessoJudicial processoJudicial = retornaDadosProcesso(2, "0020821-54.2013.5.04.0221");

	    // TESTES DE protected TipoCabecalhoProcesso dadosBasicos;
		TipoCabecalhoProcesso dadosBasicos = processoJudicial.getDadosBasicos();
		
		// Valor da Causa
		/*
			<element minOccurs="0" name="valorCausa" type="double">
				<annotation>
					<documentation>Valor da causa.</documentation>
				</annotation>
			</element>
		 */
		assertEquals(50000.0, dadosBasicos.getValorCausa(), 0.001);
		
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
		
		// Sigla do Tribunal.Campo deve ser preenchido em formato maiúsculo, sem caractere especial, e sem espaço. Ex. TREBA; TJPA; STM
		assertEquals("TRT4", dadosBasicos.getSiglaTribunal());
		
		// Informar em qual grau de jurisdição. Valores possíveis:
		// 'G2': 2º Grau
		// 'G1': 1º grau
		// etc
		assertEquals("G2", dadosBasicos.getGrau());
		
		// Informar se o processo tramita em sistema eletrônico ou em papel. São valores possíveis
		// 1: Sistema Eletrônico
		// 2: Sistema Físico
		assertEquals(Integer.valueOf(1), dadosBasicos.getProcEl());
		
		// Informar em qual o sistema eletrônico o processo tramita. São valores possíveis:
		// PJe
		// ProJud
		// SAJ
		// EPROC
		// Outros
		assertEquals(1, (int) dadosBasicos.getDscSistema());
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
		assertEquals(47176, orgaoJulgador2G.getCodigoOrgao()); 
		assertEquals("Gabinete da Vice Presidência", orgaoJulgador2G.getNomeOrgao());
		
		// Processo 1G, responsabilidade da vara: 46904;VT Encantado
		TipoOrgaoJulgador orgaoJulgador1G = retornaDadosProcesso(1, "0020450-29.2013.5.04.0791").getDadosBasicos().getOrgaoJulgador();
		assertEquals(46904, orgaoJulgador1G.getCodigoOrgao());
		assertEquals("VT Encantado", orgaoJulgador1G.getNomeOrgao());
		
		// Processo 1G, nome de vara com caracteres acentuados
		TipoOrgaoJulgador orgaoJulgadorAcentuado = retornaDadosProcesso(1, "0020150-49.2015.5.04.0451").getDadosBasicos().getOrgaoJulgador();
		assertEquals(46994, orgaoJulgadorAcentuado.getCodigoOrgao());
		assertEquals("VT São Jerônimo", orgaoJulgadorAcentuado.getNomeOrgao());
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
		
		TipoParte parteAtivoRogerio = getParteComNome("ROGERIO MELO DE CASTRO", poloAtivo.getParte());
		TipoPessoa pessoaAtivoRogerio = parteAtivoRogerio.getPessoa();
		TipoParte partePassivoFAMF = getParteComNome("FAMF CONSTRUTORA LTDA", poloPassivo.getParte());
		TipoPessoa pessoaPassivoFAMF = partePassivoFAMF.getPessoa();
		
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
		assertEquals("57310009053", pessoaAtivoRogerio.getNumeroDocumentoPrincipal());
		assertEquals("17640725000147", pessoaPassivoFAMF.getNumeroDocumentoPrincipal());
		
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
		assertEquals(ModalidadeGeneroPessoa.M, pessoaAtivoRogerio.getSexo());
		
		// Polo ATIVO possui somente o ROGERIO MELO DE CASTRO com seus DOIS advogados
		// Os advogados não devem entrar como parte, mas como representantes dos autores e réus.
		assertEquals(1, poloAtivo.getParte().size());
		
		// Advogados de ROGERIO MELO DE CASTRO - CPF: 573.100.090-53:
		// * LIDIANE DA SILVA DANIEL - OAB: RS85590
		// * ANDERSON ALZENIR DE JESUS - OAB: RS69004
		assertEquals(2, parteAtivoRogerio.getAdvogado().size());
		
		// Confere os dados da advogada LIDIANE
		TipoRepresentanteProcessual repLidiane = getRepresentanteComNome("LIDIANE DA SILVA DANIEL", parteAtivoRogerio.getAdvogado());
		assertEquals(true, repLidiane.isIntimacao()); // Indicativo verdadeiro (true) ou falso (false) relativo à escolha de o advogado, escritório ou órgão de representação ser o(s) preferencial(is) para a realização de intimações.
		assertEquals("00374625042", repLidiane.getNumeroDocumentoPrincipal());
		assertEquals(ModalidadeRepresentanteProcessual.A, repLidiane.getTipoRepresentante());
	}
	
	
	@Test
	public void testProcessoAdvogadoRepresentandoMaisDeUmaParte() throws Exception {
		TipoProcessoJudicial processoJudicial = retornaDadosProcesso(2, "0021080-28.2016.5.04.0000");
		
		TipoPoloProcessual poloAtivo = getPolo(ModalidadePoloProcessual.AT, processoJudicial.getDadosBasicos().getPolo());
		
		TipoParte parteAtivoRui = getParteComNome("RUI MARTINS FLORES", poloAtivo.getParte());
		TipoParte parteAtivoAirton = getParteComNome("AIRTON PAULO DE ARAUJO", poloAtivo.getParte());
		
		// Advogada DANIELA RODRIGUES DALLA LANA - OAB: RS71777 representa duas partes do polo ativo:
		// * RUI MARTINS FLORES - CPF: 125.892.840-04
		// * AIRTON PAULO DE ARAUJO - CPF: 123.989.260-87
		getRepresentanteComNome("DANIELA RODRIGUES DALLA LANA", parteAtivoRui.getAdvogado());
		getRepresentanteComNome("DANIELA RODRIGUES DALLA LANA", parteAtivoAirton.getAdvogado());
	}
	
	@Test
	public void testProcessoAdvogadoInativoRepresentandoUmaParte() throws Exception {
		TipoProcessoJudicial processoJudicial = retornaDadosProcesso(2, "0020570-59.2015.5.04.0029");
		
		// Processo tem duas partes no polo passivo:
		// * ADRIANA BRAUCH WANOWSCHEK - CPF: 834.395.010-00
		// * BANCO BRADESCO SA - CNPJ: 60.746.948/0001-12
		TipoPoloProcessual poloPassivo = getPolo(ModalidadePoloProcessual.PA, processoJudicial.getDadosBasicos().getPolo());
		assertEquals(2, poloPassivo.getParte().size());
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
	
	private TipoRepresentanteProcessual getRepresentanteComNome(String nome, List<TipoRepresentanteProcessual> partes) {
		
		ArrayList<String> nomes = new ArrayList<>();
		for (TipoRepresentanteProcessual parte: partes) {
			String nomeParte = parte.getNome();
			if (nome.equals(nomeParte)) {
				return parte;
			}
			nomes.add(nomeParte);
		}
		fail("A pessoa '" + nome + "' não está na lista de representantes (" + nomes + ")");
		return null;
	}

	private TipoAssuntoLocal getAssuntoLocalComCodigo(int codigo, List<TipoAssuntoProcessual> assuntos) {
		
		for (TipoAssuntoProcessual assunto: assuntos) {
			if (assunto.getAssuntoLocal() != null && codigo == assunto.getAssuntoLocal().getCodigoAssunto()) {
				return assunto.getAssuntoLocal();
			}
		}
		fail("O assunto local com código '" + codigo + "' não está na lista de assuntos");
		return null;
	}
	
	private boolean existeAssuntoNacionalComCodigo(int codigo, List<TipoAssuntoProcessual> assuntos) {
		for (TipoAssuntoProcessual assunto: assuntos) {
			if (assunto.getCodigoNacional() != null && codigo == assunto.getCodigoNacional()) {
				return true;
			}
		}
		return false;
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
		
		/*
			<attribute name="identificadorMovimento" type="string" use="optional">
				<annotation>
					<documentation>Atributo incluído para permitir a atribuição de um
						identificador específico para a movimentação realizada em um
						determinado processo judicial.
					</documentation>
				</annotation>
			</attribute>
		 */
		assertEquals("514716", movimento.getIdentificadorMovimento());
		
		// Nível de sigilo
		assertEquals(0, (int) movimento.getNivelSigilo());
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
		List<String> complementosString = movimento.getMovimentoNacional().getComplemento();
		assertEquals(2, complementosString.size());
		assertEquals("5050:nome do magistrado:GRACIELA MAFFEI", complementosString.get(0)); // Complemento sem código
		assertEquals("3:tipo de conclusão:5:despacho", complementosString.get(1)); // Complemento com código
		
		// Novo campo "TipoMovimentoProcessual.complementoNacional", conforme novo arquivo "modelo-de-transferencia-de-dados-1.0.xsd"
		List<TipoComplementoNacional> complementos = movimento.getComplementoNacional();
		assertEquals(2, complementos.size());
		
		// Complemento "nome do magistrado"
		TipoComplementoNacional complementoNomeMagistrado = movimento.getComplementoNacional().get(0);
		assertEquals(5050, complementoNomeMagistrado.getCodComplemento());
		assertEquals("nome do magistrado", complementoNomeMagistrado.getDescricaoComplemento());
		
		// Complemento "tipo de conclusão"
		TipoComplementoNacional complementoTipoConclusao = movimento.getComplementoNacional().get(1);
		assertEquals(3, complementoTipoConclusao.getCodComplemento());
		assertEquals("tipo de conclusão", complementoTipoConclusao.getDescricaoComplemento());
		
		// responsavelMovimento: Informar o número do CPF do servidor ou magistrado responsável pelo movimento.
		assertEquals("80804152004", movimento.getResponsavelMovimento());
		
		// tipoResponsavelMovimento: Identidficação do responsável pelo movimento: Servidor=0; Magistrado=1;
		assertEquals(Integer.valueOf(0), movimento.getTipoResponsavelMovimento());
	}
	
	/**
	 * É preciso preencher o CPF do Magistrado prolator das sentenças e acórdãos. Ver arquivo "documento_XML_exemplo_RNv2_25032020.pdf".
	 * 
	 * Mas, no PJe, não existe uma ligação explícita do movimento processual (ex: o movimento 240 - "Conhecimento em Parte e Provimento"
	 * não está referenciando o magistrado).
	 * 
	 * Então será feita uma busca nos DOCUMENTOS (sentenças e acórdãos) do processo: a partir da data do movimento, 
	 * se ele referir-se a um movimento de JULGAMENTO do MAGISTRADO, será identificado o usuário que assinou o 
	 * documento (sentença ou acórdão) imediatamente anterior. Esse usuário será preenchido como "magistradoProlator".
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testPreencherMagistradoProlator() throws Exception {
		TipoProcessoJudicial processo = retornaDadosProcesso(2, "0020912-65.2014.5.04.0333");
		
		// Recebidos os autos para incluir em pauta
		TipoMovimentoProcessual movimentoServidor = getMovimentoComData("20150211113831", processo.getMovimento());
		assertEquals(0, movimentoServidor.getMagistradoProlator().size());
		
		// Julgado(s) procedente(s) em parte o(s) pedido(s) (RECURSO ORDINÁRIO (1009) / ) de ....
		TipoMovimentoProcessual movimentoMagistrado = getMovimentoComData("20150303112713", processo.getMovimento());
		assertEquals(1, movimentoMagistrado.getMagistradoProlator().size());
		assertEquals("30333369068", movimentoMagistrado.getMagistradoProlator().get(0));
	}
	
	/**
	 * Preenche o órgão julgador nos movimentos processuais.
	 * 
	 * A ideia é identificar o OJ no intante em que o movimento foi registrado, por isso será preciso utilizar 
	 * a tabela "tb_hist_desloca_oj", que armazena o histórico de órgãos julgadores do processo.
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testPreencherOrgaoJulgadorMovimentos() throws Exception {
		TipoProcessoJudicial processo = retornaDadosProcesso(1, "0021040-51.2015.5.04.0333");
		TipoOrgaoJulgador orgaoJulgador = processo.getDadosBasicos().getOrgaoJulgador();
		assertEquals("Posto de São Sebastião do Caí", orgaoJulgador.getNomeOrgao()); // Conforme arquivo "serventias_trt4.csv"
		assertEquals(4318705, orgaoJulgador.getCodigoMunicipioIBGE()); // SAO LEOPOLDO (município sede)
		
		// Distribuído por sorteio
		// Esse momento é antes de todos os deslocamentos, então o processo estava no "id_oj_origem" antes do primeiro deslocamento
		// id_hist_desloca_oj=3254
		// id_oj_origem=47 - 3ª Sao Leopoldo
		// dt_deslocamento=2015-09-10 17:02:27.4
		// 
		// id_oj_destino=53 - Posto de São Sebastião do Caí
		// dt_retorno=2016-10-03 16:51:36.523
		TipoMovimentoProcessual movimentoDistribuicao = getMovimentoComData("20150708152305", processo.getMovimento());
		assertNotNull(movimentoDistribuicao.getOrgaoJulgador());
		assertEquals("3ª Sao Leopoldo", movimentoDistribuicao.getOrgaoJulgador().getNomeOrgao()); // Conforme arquivo "serventias_trt4.csv"
		assertEquals(4318705, movimentoDistribuicao.getOrgaoJulgador().getCodigoMunicipioIBGE()); // SAO LEOPOLDO (município sede)
		
		// Iniciada a liquidação por cálculos
		// Esse momento é no meio de um deslocamento, então deve considerar o OJ de Destino (pois o processo está dentro da janela de deslocamento)
		// id_hist_desloca_oj=15120
		// id_oj_origem=47 - 3ª Sao Leopoldo
		// dt_deslocamento=2016-12-12 15:42:22.808
		// 
		// id_oj_destino=53 - Posto de São Sebastião do Caí
		// dt_retorno=2017-05-04 09:45:06.422
		TipoMovimentoProcessual iniciadaLiquidacao = getMovimentoComData("20170116162649", processo.getMovimento());
		assertNotNull(iniciadaLiquidacao.getOrgaoJulgador());
		assertEquals("Posto de São Sebastião do Caí", iniciadaLiquidacao.getOrgaoJulgador().getNomeOrgao());
		assertEquals(4318705, iniciadaLiquidacao.getOrgaoJulgador().getCodigoMunicipioIBGE()); // SAO LEOPOLDO (município sede)
		
		// Distribuído por sorteio
		// Esse momento é entre dois deslocamentos, então o processo estava no "id_oj_origem" antes do próximo deslocamento
		// id_hist_desloca_oj=15120
		// id_oj_origem=47 - 3ª Sao Leopoldo
		// dt_deslocamento=2016-12-12 15:42:22.808
		// 
		// id_oj_destino=53 - Posto de São Sebastião do Caí
		// dt_retorno=2017-05-04 09:45:06.422
		TipoMovimentoProcessual movimentoDecurso = getMovimentoComData("20161212143717", processo.getMovimento());
		assertNotNull(movimentoDecurso.getOrgaoJulgador());
		assertEquals("3ª Sao Leopoldo", movimentoDecurso.getOrgaoJulgador().getNomeOrgao()); // Conforme arquivo "serventias_trt4.csv"
		assertEquals(4318705, movimentoDecurso.getOrgaoJulgador().getCodigoMunicipioIBGE()); // SAO LEOPOLDO (município sede)
		
		// Arquivados os autos definitivamente
		// (esse momento é depois de todos os deslocamentos, então o processo estava no seu OJ conforme tabela de processos)
		TipoMovimentoProcessual movimentoArquivamento = getMovimentoComData("20180806131456", processo.getMovimento());
		assertNotNull(movimentoArquivamento.getOrgaoJulgador());
		assertEquals("Posto de São Sebastião do Caí", movimentoArquivamento.getOrgaoJulgador().getNomeOrgao());
		assertEquals(4318705, movimentoArquivamento.getOrgaoJulgador().getCodigoMunicipioIBGE()); // SAO LEOPOLDO (município sede)
	}
	
	public TipoProcessoJudicial retornaDadosProcesso(int grau, String numeroProcesso) throws Exception {
		
		Op_2_GeraXMLsIndividuais baixaDados = new Op_2_GeraXMLsIndividuais();
		BaseEmAnaliseEnum base = BaseEmAnaliseEnum.PJE;
		try {
			baixaDados.prepararConexao(grau, base, true);
			List<String> numeros = new ArrayList<>();
			numeros.add(numeroProcesso);
			baixaDados.prepararCacheDadosProcessos(numeros, grau, base, true);
			return baixaDados.analisarProcessoJudicialCompleto(numeroProcesso, grau, base);
		} finally {
			baixaDados.close();
		}
	}

	/**
	 * No processo 0021149-65.2015.5.04.0333 no 2o Grau, a pessoa "PETRONILO ROSA DA SILVA" não possui
	 * informação de gênero.
	 */
	@Test
	public void testCacheGeneroPessoa() throws Exception {
		
		// O parâmetro deve estar preenchido!!
		Auxiliar.getParametroBooleanConfiguracao(Parametro.contornar_falta_de_genero);
		
		// Ao consultar o processo no 2o Grau, essa informação deverá ser retornada
		TipoProcessoJudicial processoJudicial = retornaDadosProcesso(2, "0021149-65.2015.5.04.0333");
		TipoPoloProcessual poloPassivo = getPolo(ModalidadePoloProcessual.PA, processoJudicial.getDadosBasicos().getPolo());
		TipoParte parte = getParteComNome("PETRONILO ROSA DA SILVA", poloPassivo.getParte());
		TipoPessoa pessoa = parte.getPessoa();
		assertEquals(ModalidadeGeneroPessoa.M, pessoa.getSexo());
	}
	
	@Test
	public void testAssunto() throws Exception {
		
		// Processo que possui um assunto que PERTENCE às tabelas nacionais (deve ser lançado
		// somente com codigoNacional
		TipoProcessoJudicial processoJudicial = retornaDadosProcesso(2, "0000006-97.2012.5.04.0406");
		assertTrue(existeAssuntoNacionalComCodigo(8809, processoJudicial.getDadosBasicos().getAssunto()));
		
		// Processo que possui um assunto que NÃO PERTENCE às tabelas nacionais (deve ser lançado
		// como assuntoLocal
		// UPDATE: removi daqui, pois a rotina de análise de assuntos tem um teste próprio que trata isso (ver AnalisaAssuntosCNJTest.java)
		// TipoProcessoJudicial processoJudicial2 = retornaDadosProcesso(2, "0000002-60.2012.5.04.0406");
		// TipoAssuntoLocal assuntoLocal2 = getAssuntoLocalComCodigo(55207, processoJudicial2.getDadosBasicos().getAssunto());
		// assertEquals(55207, assuntoLocal2.getCodigoAssunto());
		// assertEquals(2656, assuntoLocal2.getCodigoPaiNacional());
		// assertEquals("DIREITO DO TRABALHO (864) / Rescisão do Contrato de Trabalho (2620) / Reintegração / Readmissão ou Indenização (2656) / Outras Hipóteses de Estabilidade", assuntoLocal2.getDescricao());
	}
	
	/**
	 * Processo sem assunto deve receber um assunto principal baseado nas configurações (campos "assunto_padrao_1G" e "assunto_padrao_2G")
	 *
	 * Se o assunto padrão for alterado OU SE o assunto padrão for mapeado para outro, ajustar esse teste.
	 * @throws Exception  
	 */
	@Test
	public void testAssuntoPadrao() throws Exception {
		 
		TipoProcessoJudicial processoJudicial3 = retornaDadosProcesso(1, "0021172-82.2013.5.04.0332");
		assertEquals(1, processoJudicial3.getDadosBasicos().getAssunto().size());
		assertTrue(existeAssuntoNacionalComCodigo(10652, processoJudicial3.getDadosBasicos().getAssunto()));
	}
	
	@Test
	public void testEnderecos() throws Exception {
		
		TipoProcessoJudicial processoJudicial = retornaDadosProcesso(1, "0020591-86.2014.5.04.0282");
		TipoPoloProcessual poloAtivo = getPolo(ModalidadePoloProcessual.AT, processoJudicial.getDadosBasicos().getPolo());
		TipoParte parteAtivoRogerio = getParteComNome("ROGERIO MELO DE CASTRO", poloAtivo.getParte());
		
		// Endereço de uma parte
		assertEquals(1, parteAtivoRogerio.getPessoa().getEndereco().size());
		TipoEndereco endereco = parteAtivoRogerio.getPessoa().getEndereco().get(0);
		assertEquals("RUA SAPIRANGA", endereco.getLogradouro());
		assertEquals("221", endereco.getNumero());
		//assertNull(endereco.getComplemento());
		assertEquals("CAPAO DA CRUZ", endereco.getBairro());
		assertEquals("SAPUCAIA DO SUL", endereco.getCidade());
		assertEquals("93226476", endereco.getCep());
		assertEquals(Integer.valueOf(4320008), endereco.getCodCidade());
		assertNull(endereco.getEstado());
		assertNull(endereco.getPais());
		
		// Endereço de um advogado
		TipoRepresentanteProcessual advogado = getRepresentanteComNome("ANDERSON ALZENIR DE JESUS", parteAtivoRogerio.getAdvogado());
		assertEquals(1, advogado.getEndereco().size());
		assertEquals("Rua Padre Claret", advogado.getEndereco().get(0).getLogradouro());
		
		// Processo que possui um endereço com CEP em branco
		TipoProcessoJudicial processoJudicial2 = retornaDadosProcesso(2, "0020242-05.2014.5.04.0017");
		TipoPoloProcessual poloAtivo2 = getPolo(ModalidadePoloProcessual.AT, processoJudicial2.getDadosBasicos().getPolo());
		TipoParte parteSindicato = getParteComNome("SINDICATO DOS BANCARIOS DE PORTO ALEGRE E REGIAO", poloAtivo2.getParte());
		TipoRepresentanteProcessual advogadoSindicato = getRepresentanteComNome("ANDRE HEINECK KRUSE", parteSindicato.getAdvogado());
		assertEquals(5, advogadoSindicato.getEndereco().size()); // Advogado possui 5 endedeços com CEP válido e 1 com CEP em branco.
	}
	
	/**
	 * Curadores e Tutores, apesar de estarem previstos no arquivo intercomunicacao-2.2.2, não estão
	 * sendo tratados pelo arquivo JAR do CNJ (replicacao-client). Seus dados estão sendo simplesmente
	 * ignorados. Por isso, também removerei essas pessoas do arquivo XML, já que, de acordo
	 * com Jeferson Andrade (TRT4), essas pessoas não são consideradas PARTES nos processos.
	 * @throws Exception
	 */
	@Test
	public void testCurador() throws Exception {
		
		TipoProcessoJudicial processoJudicial = retornaDadosProcesso(2, "0021300-74.2013.5.04.0406");
		TipoPoloProcessual poloPassivo = getPolo(ModalidadePoloProcessual.PA, processoJudicial.getDadosBasicos().getPolo());
		TipoParte partePassivoGuiomar = getParteComNome("GUIOMAR ANTUNES MACIEL", poloPassivo.getParte());
		
		// GUIOMAR possui, no PJe, um ADVOGADO (EDSON) e um CURADOR (DALVA). 
		// Mas, nesse arquivo XML, somente o ADVOGADO deve estar preenchido.
		assertEquals("EDSON DE CARLI", partePassivoGuiomar.getAdvogado().get(0).getNome());
		assertEquals(1, partePassivoGuiomar.getAdvogado().size());
		
		// O curador não deve estar constando como parte no processo 
		assertEquals(1, poloPassivo.getParte().size());
	}
	
	@Test
	public void testPolosDuplicados() throws Exception {
		
		TipoProcessoJudicial processoJudicial = retornaDadosProcesso(2, "0000007-82.2012.5.04.0406");
		
		// Processo deve ter somente três polos: Ativo, Passivo e Terceiros. 
		// Teste inserido para contornar problema que fazia com que os polos pudessem aparecer várias vezes.
		assertEquals(3, processoJudicial.getDadosBasicos().getPolo().size());
	}
	
	/**
	 * Testa processos relacionados (principal e incidental), conforme novos campos contidos no
	 * arquivo "modelo-de-transferencia-de-dados-1.0.xsd".
	 * 
	 * Informação recebida de "Rosfran Lins Borges" (CNJ) no assunto com e-mail "Re: Enc: Justiça em Números: Serviço Fora do Ar."
	 * O tipoRelacaoIncidental serve para qualquer caso, seja para informar a existência de incidentais num processo principal, 
	 * seja pra informar um processo principal, num incidental. Desse modo o atributo tipoRelacao informa o que o processo 
	 * informado é em relação ao processo atual;
	 * 
	 * O processo "0010070-26.2012.5.04.0000" é um incidental em cima do processo "0010053-87.2012.5.04.0000", que é o principal.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTipoRelacaoIncidental() throws Exception {
		
		// Dentro do processo incidental, deve referenciar o principal
		TipoProcessoJudicial processoIncidental = retornaDadosProcesso(2, "0010070-26.2012.5.04.0000");

		List<TipoRelacaoIncidental> relacaoIncidental = processoIncidental.getDadosBasicos().getRelacaoIncidental();
		assertNotNull(relacaoIncidental);
		assertEquals(1, relacaoIncidental.size());
		
		// De acordo com o validador do CNJ, deve gravar o número do processo de forma plana, sem sinais.
		// Erro reportado:
		// Caused by: org.xml.sax.SAXParseException; ... cvc-pattern-valid: Value '0020474-77.2016.5.04.0233' is not facet-valid with respect to pattern '\d{20}' for type 'tipoNumeroUnico'.
		// Linha do XML:
		// <relacaoIncidental numeroProcesso="0020474-77.2016.5.04.0233" tipoRelacao="PI" classeProcessual="1125"/>
		TipoRelacaoIncidental relacaoIncidentalParaPrincipal = relacaoIncidental.get(0);
		assertEquals("00100538720125040000", relacaoIncidentalParaPrincipal.getNumeroProcesso());
		assertEquals("PP", relacaoIncidentalParaPrincipal.getTipoRelacao());
		assertEquals(Integer.valueOf(120), relacaoIncidentalParaPrincipal.getClasseProcessual()); // Mandado de Segurança Cível
		
		
		// Dentro do processo principal, deve referenciar o incidental
		TipoProcessoJudicial processoPrincipal = retornaDadosProcesso(2, "0010053-87.2012.5.04.0000");

		List<TipoRelacaoIncidental> relacaoPrincipal = processoPrincipal.getDadosBasicos().getRelacaoIncidental();
		assertNotNull(relacaoPrincipal);
		assertEquals(2, relacaoPrincipal.size());
		
		TipoRelacaoIncidental relacaoPrincipalParaIncidental = relacaoPrincipal.get(0);
		assertEquals("00100702620125040000", relacaoPrincipalParaIncidental.getNumeroProcesso());
		assertEquals("PI", relacaoPrincipalParaIncidental.getTipoRelacao());
		assertEquals(Integer.valueOf(1005), relacaoPrincipalParaIncidental.getClasseProcessual()); // Agravo Regimental Trabalhista
	}
}
