package br.jus.trt4.justica_em_numeros_2016.dto;

import java.sql.ResultSet;
import java.sql.SQLException;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;

/**
 * DTO que armazena informações de um assunto processual, a partir da consulta do arquivo "05_consulta_assuntos.sql".
 *
 * @author felipegiotto@gmail.com
 */
public class AssuntoDto {

	private int codigo;
	private boolean principal;
	
	public AssuntoDto(ResultSet rsAssuntos) throws SQLException {
		this.codigo = Auxiliar.getCampoIntNotNull(rsAssuntos, "cd_assunto_trf");
		this.principal = "S".equals(rsAssuntos.getString("in_assunto_principal"));
	}
	
	public int getCodigo() {
		return codigo;
	}
	
	public boolean isPrincipal() {
		return principal;
	}
}
