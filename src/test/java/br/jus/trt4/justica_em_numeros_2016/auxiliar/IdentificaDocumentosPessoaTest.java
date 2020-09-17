package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import br.jus.cnj.modeloDeTransferenciaDeDados.ModalidadeDocumentoIdentificador;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoDocumentoIdentificacao;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoPessoa;
import br.jus.trt4.justica_em_numeros_2016.enums.BaseEmAnaliseEnum;

public class IdentificaDocumentosPessoaTest extends AbstractTestCase {

	@Test
	public void testDocumentoTituloDeEleitor() throws Exception {
		TipoPessoa pessoa = carregaDocumentosPessoa(1, "ROGERIO MELO DE CASTRO", 85007);
		
		// Localiza Título de Eleitor
		TipoDocumentoIdentificacao tituloEleitor = getDocumentoDoTipo(ModalidadeDocumentoIdentificador.TE, pessoa.getDocumento());
		assertEquals("0059509160434", tituloEleitor.getCodigoDocumento());
		assertEquals("Tribunal Superior Eleitoral", tituloEleitor.getEmissorDocumento());
		assertNull(tituloEleitor.getNome()); // Nome só deve ser preenchido quando for diferente do nome da pessoa
	}

	@Test
	public void testDocumentoCPF() throws Exception {
		TipoPessoa pessoa = carregaDocumentosPessoa(1, "ROGERIO MELO DE CASTRO", 85007);
		
		// Localiza CPF (CMF, no CNJ)
		TipoDocumentoIdentificacao cpf = getDocumentoDoTipo(ModalidadeDocumentoIdentificador.CMF, pessoa.getDocumento());
		assertEquals("57310009053", cpf.getCodigoDocumento());
		assertEquals("Secretaria da Receita Federal", cpf.getEmissorDocumento());
		assertNull(cpf.getNome()); // Nome só deve ser preenchido quando for diferente do nome da pessoa
		
		// Verifica se gravou documento principal
		assertEquals("57310009053", pessoa.getNumeroDocumentoPrincipal());
	}
	
	@Test
	public void testPessoaComDoisDocumentosPrincipais() throws Exception {
		TipoPessoa pessoa = carregaDocumentosPessoa(1, "EDISON SOLIVARGAS ALMEIDA", 511199);
		
		// Localiza CPF (CMF, no CNJ)
		TipoDocumentoIdentificacao cpf = getDocumentoDoTipo(ModalidadeDocumentoIdentificador.CMF, pessoa.getDocumento());
		assertEquals("82343071004", cpf.getCodigoDocumento());
		
		// Localiza Título de Eleitor
		TipoDocumentoIdentificacao tituloEleitor = getDocumentoDoTipo(ModalidadeDocumentoIdentificador.TE, pessoa.getDocumento());
		assertEquals("0075924700469", tituloEleitor.getCodigoDocumento());
		
		// Verifica se gravou documento principal
		assertEquals("82343071004", pessoa.getNumeroDocumentoPrincipal());
	}
	
	@Test
	public void testDocumentoCNPJ() throws Exception {
		TipoPessoa pessoa = carregaDocumentosPessoa(1, "FORTUNA CONSTRUCOES LTDA  - ME", 637282);
		
		// Localiza CPF (CMF, no CNJ)
		TipoDocumentoIdentificacao cnpj = getDocumentoDoTipo(ModalidadeDocumentoIdentificador.CMF, pessoa.getDocumento());
		assertEquals("16567898000114", cnpj.getCodigoDocumento());
		assertEquals("Secretaria da Receita Federal", cnpj.getEmissorDocumento());
		assertNull(cnpj.getNome()); // Nome só deve ser preenchido quando for diferente do nome da pessoa
		
		// Verifica se gravou documento principal
		assertEquals("16567898000114", pessoa.getNumeroDocumentoPrincipal());
	}
	
	/**
	 * Novo campo, conforme arquivo "modelo-de-transferencia-de-dados-1.0.xsd"
	 */
	@Test
	public void testDocumentoINSS() throws Exception {
		TipoPessoa pessoa = carregaDocumentosPessoa(1, "LUIZ SETIMO PALANDI", 50151);
		
		// Localiza CPF (CMF, no CNJ)
		TipoDocumentoIdentificacao cnpj = getDocumentoDoTipo(ModalidadeDocumentoIdentificador.NB, pessoa.getDocumento());
		assertEquals("500171813686", cnpj.getCodigoDocumento());
		assertNull(cnpj.getNome()); // Nome só deve ser preenchido quando for diferente do nome da pessoa
	}
	
	@Test
	public void testDocumentosInvalidos() throws Exception {
		TipoPessoa pessoa = carregaDocumentosPessoa(1, "ANDRE SOARES FARIAS", 6810);
		
		// Título de eleitor está cadastrado com o valor "____.____.____"
		naoDevePossuirDocumentoDoTipo(ModalidadeDocumentoIdentificador.TE, pessoa.getDocumento());
		
		// Documento do tipo "RGE" cadastrado, sem correspondência no CNJ
		// Basta executar a rotina e ver se os documentos válidos serão gravados corretamente
		assertEquals(1, pessoa.getDocumento().size());
		
		// Localiza CPF (CMF, no CNJ)
		TipoDocumentoIdentificacao cpf = getDocumentoDoTipo(ModalidadeDocumentoIdentificador.CMF, pessoa.getDocumento());
		assertEquals("61227676034", cpf.getCodigoDocumento());
		assertEquals("Secretaria da Receita Federal", cpf.getEmissorDocumento());
		assertEquals("Andre Soares Farias", cpf.getNome()); // Nome só deve ser preenchido quando for diferente do nome da pessoa
		
		// Verifica se gravou documento principal
		assertEquals("61227676034", pessoa.getNumeroDocumentoPrincipal());
	}
	
	@Test
	public void testDocumentoSemNumeros() throws Exception {
		TipoPessoa pessoa = carregaDocumentosPessoa(1, "FUNDACAO HOSPITALAR SANTA TEREZINHA DE ERECHIM", 9669);
		
		// Documento RJC dessa pessoa está cadastrado com o texto "órgão público"
		naoDevePossuirDocumentoDoTipo(ModalidadeDocumentoIdentificador.RJC, pessoa.getDocumento());
		
		// Verifica se gravou documento principal
		assertEquals("89421259000110", pessoa.getNumeroDocumentoPrincipal());
	}
	
	@Test
	public void testDocumentoMuitoLongo() throws Exception {
		TipoPessoa pessoa = carregaDocumentosPessoa(1, "Bárbara Silveira de Araujo Otharan", 518669);
		
		// Documento RIC possui valor inválido: 9901552014200020041000565636
		TipoDocumentoIdentificacao cpf = getDocumentoDoTipo(ModalidadeDocumentoIdentificador.RIC, pessoa.getDocumento());
		assertEquals("9901552014200020041000565636", cpf.getCodigoDocumento());
		assertEquals("Serviço de Registros Publicos,Capão da Canoa,RS", cpf.getEmissorDocumento());
		assertNull(cpf.getNome()); // Nome só deve ser preenchido quando for diferente do nome da pessoa
		
		// Verifica se gravou documento principal
		assertNull(pessoa.getNumeroDocumentoPrincipal());
	}
	
	private TipoDocumentoIdentificacao getDocumentoDoTipo(ModalidadeDocumentoIdentificador tipoDocumento, List<TipoDocumentoIdentificacao> documentos) {
		ArrayList<String> encontrados = new ArrayList<>();
		for (TipoDocumentoIdentificacao documento: documentos) {
			if (tipoDocumento.equals(documento.getTipoDocumento())) {
				return documento;
			}
			encontrados.add(documento.getTipoDocumento() + "=" + documento.getCodigoDocumento());
		}
		fail("Não há nenhum documento do tipo " + tipoDocumento + " na lista: " + encontrados);
		return null;
	}

	private void naoDevePossuirDocumentoDoTipo(ModalidadeDocumentoIdentificador tipoDocumento, List<TipoDocumentoIdentificacao> documentos) {
		for (TipoDocumentoIdentificacao documento: documentos) {
			if (tipoDocumento.equals(documento.getTipoDocumento())) {
				fail("Pessoa não deveria possuir documento do tipo " + tipoDocumento + ", mas tem!");
			}
		}
	}
	
	private TipoPessoa carregaDocumentosPessoa(int grau, String nomePessoa, int idPessoa) throws SQLException, IOException {
		BaseEmAnaliseEnum baseEmAnalise = BaseEmAnaliseEnum.PJE;
		try (Connection conexaoBasePrincipal = Auxiliar.getConexao(grau, baseEmAnalise)) {
			try (IdentificaDocumentosPessoa doc = new IdentificaDocumentosPessoa(conexaoBasePrincipal, baseEmAnalise, grau)) {
				TipoPessoa pessoa = new TipoPessoa();
				pessoa.setNome(nomePessoa);
				doc.preencherDocumentosPessoa(pessoa, idPessoa);
				return pessoa;
			}
		}
	}
}
