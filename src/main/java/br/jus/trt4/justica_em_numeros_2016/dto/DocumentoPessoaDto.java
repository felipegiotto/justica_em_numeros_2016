package br.jus.trt4.justica_em_numeros_2016.dto;

import java.sql.ResultSet;
import java.sql.SQLException;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;

/**
 * DTO que armazena informações de um documento pessoal, a partir da consulta do arquivo "04_consulta_documentos_pessoa.sql".
 *
 * @author felipegiotto@gmail.com
 */
public class DocumentoPessoaDto {

	private int idPessoa;
	private String tipoDocumento;
	private String numero;
	private String emissor;
	private String nomePessoa;
	private boolean principal;
	
	public DocumentoPessoaDto(ResultSet rsDocumentos) throws SQLException {
		this.idPessoa = rsDocumentos.getInt("id_pessoa");
		this.tipoDocumento = Auxiliar.getCampoStringNotNull(rsDocumentos, "cd_tp_documento_identificacao").trim();
		this.numero = Auxiliar.getCampoStringNotNull(rsDocumentos, "nr_documento");
		this.emissor = rsDocumentos.getString("ds_emissor");
		this.nomePessoa = rsDocumentos.getString("ds_nome_pessoa").trim();
		this.principal = "S".equals(rsDocumentos.getString("in_principal"));
	}

	public int getIdPessoa() {
		return idPessoa;
	}
	
	public String getTipoDocumento() {
		return tipoDocumento;
	}
	
	public String getNumero() {
		return numero;
	}
	
	public String getEmissor() {
		return emissor;
	}
	
	public String getNomePessoa() {
		return nomePessoa;
	}
	
	public boolean isPrincipal() {
		return principal;
	}
}
