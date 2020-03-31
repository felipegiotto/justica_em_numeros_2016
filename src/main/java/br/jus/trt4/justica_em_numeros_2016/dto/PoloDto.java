package br.jus.trt4.justica_em_numeros_2016.dto;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DTO que armazena informações de um polo processual, a partir da consulta do arquivo "02_consulta_polos.sql".
 *
 * @author felipegiotto@gmail.com
 */
public class PoloDto {

	private String inParticipacao;
	
	public PoloDto(ResultSet rsPolos) throws SQLException {
		this.inParticipacao = rsPolos.getString("in_participacao");
	}

	public String getInParticipacao() {
		return inParticipacao;
	}
}
