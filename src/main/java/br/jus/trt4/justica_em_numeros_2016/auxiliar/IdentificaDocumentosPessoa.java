package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.cnj.intercomunicacao_2_2.ModalidadeDocumentoIdentificador;
import br.jus.cnj.intercomunicacao_2_2.TipoDocumentoIdentificacao;
import br.jus.cnj.intercomunicacao_2_2.TipoPessoa;

/**
 * Classe responsável por localizar todos os documentos de uma pessoa e gravar, nos objetos do MNI,
 * de acordo com os padrões do CNJ.
 * 
 * @author fgiotto
 */
public class IdentificaDocumentosPessoa implements AutoCloseable {

	private static final Logger LOGGER = LogManager.getLogger(IdentificaDocumentosPessoa.class);
	private static Properties tiposDocumentosPJeCNJ;
	private NamedParameterStatement nsDocumentos;
	
	public IdentificaDocumentosPessoa(Connection conexaoBasePrincipal) throws IOException, SQLException {
		
		// Objeto que fará o de/para dos tipos de documentos do PJe para os do CNJ
		if (tiposDocumentosPJeCNJ == null) {
			tiposDocumentosPJeCNJ = Auxiliar.carregarPropertiesDoArquivo(new File("src/main/resources/tipos_de_documentos.properties"));
		}
		
		// SQL que fará a consulta dos documentos da pessoa
		String sqlConsultaDocumentos = Auxiliar.lerConteudoDeArquivo("src/main/resources/sql/op_2_gera_xmls/04_consulta_documentos_pessoa.sql");
		nsDocumentos = new NamedParameterStatement(conexaoBasePrincipal, sqlConsultaDocumentos);
	}
	
	
	/**
	 * Consulta todos os documentos dessa pessoa, no banco de dados, e preenche os campos conforme
	 * as regras estipuladas pelo CNJ.
	 * 
	 * @param pessoa
	 * @param idPessoa
	 * @throws SQLException
	 */
	public void preencherDocumentosPessoa(TipoPessoa pessoa, int idPessoa) throws SQLException {
		
		// Consulta todos os documentos da pessoa, no banco de dados do PJe
		nsDocumentos.setInt("id_pessoa", idPessoa);
		try (ResultSet rsDocumentos = nsDocumentos.executeQuery()) {
			
			while (rsDocumentos.next()) {

				// Considera CPF, CNPJ e RIC como documentos principais da pessoa, que ficam em um campo separado
				// (fora da lista de documentos)
				String tipoDocumentoPJe = Auxiliar.getCampoStringNotNull(rsDocumentos, "cd_tp_documento_identificacao").trim();
				String numeroDocumento = Auxiliar.getCampoStringNotNull(rsDocumentos, "nr_documento");
					
				// Script TRT14:
				// raise notice '<documento codigoDocumento="%" tipoDocumento="%" emissorDocumento="%" />'
				//  , documento.nr_documento, documento.tp_documento, documento.ds_emissor;
				if (tiposDocumentosPJeCNJ.containsKey(tipoDocumentoPJe)) {
					
					// Carrega o tipo de documento do CNJ a partir do tipo do PJe
					// OBS: Alguns documentos do PJe não possuem correspondente
					// no CNJ, ex: PFP. Esses tipos de documento estão cadastrados
					// no arquivo "properties" mas estarão em branco (string vazia).
					String tipoDocumentoCNJ = tiposDocumentosPJeCNJ.getProperty(tipoDocumentoPJe);
					if (!StringUtils.isBlank(tipoDocumentoCNJ)) {
						
						// Gera um WARNING se encontrar algum documento totalmente fora do padrão (sem números)
						// Ex: no TRT4, havia um documento da PJ "ESTADO DO RIO GRANDE DO SUL" do tipo "RJC" com número "Órgão Público com Procuradoria"
						if (StringUtils.isBlank(numeroDocumento.replaceAll("[^0-9]", ""))) {
							LOGGER.info("Documento do tipo '" + tipoDocumentoPJe + "' da pessoa '" + pessoa.getNome() + "' (id " + idPessoa + ") não possui números e será ignorado: '" + numeroDocumento + "'");
						} else {
							
							TipoDocumentoIdentificacao documento = new TipoDocumentoIdentificacao();
							numeroDocumento = numeroDocumento.replaceAll("[^0-9A-Za-z]", "");
							documento.setCodigoDocumento(numeroDocumento);
							documento.setEmissorDocumento(Auxiliar.getCampoStringNotNull(rsDocumentos, "ds_emissor"));
							documento.setTipoDocumento(ModalidadeDocumentoIdentificador.fromValue(tipoDocumentoCNJ));
							
							// Nome do documento, conforme documentação do XSD:
							// Nome existente no documento. Deve ser utilizado apenas se existente nome diverso daquele ordinariamente usado.
							String nomePessoaDocumento = rsDocumentos.getString("ds_nome_pessoa");
							if (!pessoa.getNome().equals(nomePessoaDocumento)) {
								documento.setNome(nomePessoaDocumento);
							}
							pessoa.getDocumento().add(documento);
							
							
							// De acordo com o XSD do CNJ, os documentos principais são CPF, CNPJ
							// ou RIC e devem possuir os tamanhos de 11, 14 e 14 caracteres, 
							// respectivamente (somente números).
							if ("S".equals(rsDocumentos.getString("in_principal"))) {
								int tamanhoMascara = 0;
								if ("CPF".equals(tipoDocumentoPJe)) {
									tamanhoMascara = 11;
								} else if ("CPJ".equals(tipoDocumentoPJe)) {
									tamanhoMascara = 14;
								} else if ("RIC".equals(tipoDocumentoPJe)) {
									tamanhoMascara = 14;
								} else {
									LOGGER.info("Documento do tipo '" + tipoDocumentoPJe + "' da pessoa '" + pessoa.getNome() + "' (id " + idPessoa + ") está marcado como principal mas não é dos tipos reconhecidos como principal pelo CNJ: CPF, CNPJ, RIC. Esse documento não será inserido como principal.");
								}
								
								if (tamanhoMascara > 0) {
									String numeroDocumentoPrincipal = numeroDocumento.replaceAll("[^0-9]", "");
									if (numeroDocumentoPrincipal.length() > tamanhoMascara) {
										LOGGER.info("Documento do tipo '" + tipoDocumentoPJe + "' da pessoa '" + pessoa.getNome() + "' (id " + idPessoa + ") possui tamanho maior (" + numeroDocumento.length() + ") do que o especificado pelo CNJ (" + tamanhoMascara + "): '" + numeroDocumentoPrincipal + "'. Esse documento não será inserido como principal.");
									} else {
										numeroDocumentoPrincipal = StringUtils.leftPad(numeroDocumentoPrincipal, tamanhoMascara, '0');
										pessoa.setNumeroDocumentoPrincipal(numeroDocumentoPrincipal);
									}
								}
							}
						}
					}
					
				} else {
					LOGGER.warn("Documento do tipo '" + tipoDocumentoPJe + "' da pessoa '" + pessoa.getNome() + "' (id " + idPessoa + ") não possui correspondente na tabela do CNJ. Esse documento não constará no XML.");
				}
			}
			
			// Verifica se existe documento principal. Orientação do CNJ, do arquivo
			// intercomunicacao-2.2.2.xsd:
			// Número do documento principal da pessoa individualizada, devendo ser 
			// utilizado o RIC ou o CPF para pessoas físicas, nessa ordem, ou o CNPJ 
			// para pessoas jurídicas. O atributo é opcional em razão da possibilidade 
			// de haver pessoas sem documentos ou cujos dados não estão disponíveis.
			if (StringUtils.isEmpty(pessoa.getNumeroDocumentoPrincipal())) {
				 LOGGER.debug("Pessoa '" + pessoa.getNome() + "' (id " + idPessoa + ") não possui documento principal!");
			}
		}
	}
	
	
	@Override
	public void close() {
		if (nsDocumentos != null) {
			try {
				nsDocumentos.close();
				nsDocumentos = null;
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando consulta 'nsDocumentos': " + e.getLocalizedMessage(), e);
			}
		}
	}
}
