package br.jus.trt4.justica_em_numeros_2016.dto;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DTO que armazena informações de um endereço de parte processual, a partir da consulta do arquivo "04_consulta_enderecos_pessoa.sql".
 *
 * @author felipegiotto@gmail.com
 */
public class EnderecoDto {

	private String cep;
	private String logradouro;
	private String numero;
	private String complemento;
	private String bairro;
	private String municipio;
	private int idMunicipioIBGE;
	
	public EnderecoDto(ResultSet rsEnderecos) throws SQLException {
		this.cep = rsEnderecos.getString("nr_cep");
		this.logradouro = rsEnderecos.getString("nm_logradouro");
		this.numero = rsEnderecos.getString("nr_endereco");
		this.complemento = rsEnderecos.getString("ds_complemento");
		this.bairro = rsEnderecos.getString("nm_bairro");
		this.municipio = rsEnderecos.getString("ds_municipio");
		this.idMunicipioIBGE = rsEnderecos.getInt("id_municipio_ibge");
	}
	
	public String getCep() {
		return cep;
	}
	
	public String getLogradouro() {
		return logradouro;
	}
	
	public String getNumero() {
		return numero;
	}
	
	public String getComplemento() {
		return complemento;
	}
	
	public String getBairro() {
		return bairro;
	}
	
	public String getMunicipio() {
		return municipio;
	}
	
	public int getIdMunicipioIBGE() {
		return idMunicipioIBGE;
	}
}
