package br.jus.trt4.justica_em_numeros_2016.dto;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DTO que armazena informações de um complemento de movimento processual, a partir da consulta do arquivo "07_consulta_complementos.sql".
 *
 * @author felipegiotto@gmail.com
 */
public class ComplementoDto {

	private int codigoTipoComplemento;
	private String codigoComplemento;
	private String nome;
	private String valor;
	
	public ComplementoDto(ResultSet rsComplementos) throws SQLException {
		this.codigoTipoComplemento = rsComplementos.getInt("cd_tipo_complemento");
		this.nome = rsComplementos.getString("ds_nome");
		this.codigoComplemento = rsComplementos.getString("cd_complemento");
		this.valor = rsComplementos.getString("ds_valor_complemento");
	}
	
	public int getCodigoTipoComplemento() {
		return codigoTipoComplemento;
	}
	
	public String getCodigoComplemento() {
		return codigoComplemento;
	}
	
	public String getNome() {
		return nome;
	}
	
	public String getValor() {
		return valor;
	}
}
