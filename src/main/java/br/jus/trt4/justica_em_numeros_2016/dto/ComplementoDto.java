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
	private boolean complementoTipoTabelado;
	
	public ComplementoDto(ResultSet rsComplementos) throws SQLException {
		this.codigoTipoComplemento = rsComplementos.getInt("cd_tipo_complemento");
		this.nome = rsComplementos.getString("ds_nome") != null ? rsComplementos.getString("ds_nome").trim() : null;
		this.codigoComplemento = rsComplementos.getString("cd_complemento") != null ? rsComplementos.getString("cd_complemento").trim() : null;
		this.valor = rsComplementos.getString("ds_valor_complemento") != null ? rsComplementos.getString("ds_valor_complemento").trim() : null;

	}
	
	public ComplementoDto() {
	}
	
	public int getCodigoTipoComplemento() {
		return codigoTipoComplemento;
	}
	
	public void setCodigoTipoComplemento(int codigoTipoComplemento) {
		this.codigoTipoComplemento = codigoTipoComplemento;
	}
	
	public String getCodigoComplemento() {
		return codigoComplemento;
	}
	
	public void setCodigoComplemento(String codigoComplemento) {
		this.codigoComplemento = codigoComplemento;
	}
	
	public String getNome() {
		return nome;
	}
	
	public void setNome(String nome) {
		this.nome = nome;
	}
	
	public String getValor() {
		return valor;
	}
	
	public void setValor(String valor) {
		this.valor = valor;
	}
	
	public boolean isComplementoTipoTabelado() {
		return complementoTipoTabelado;
	}
	
	public void setComplementoTipoTabelado(boolean complementoTipoTabelado) {
		this.complementoTipoTabelado = complementoTipoTabelado;
	}
}
