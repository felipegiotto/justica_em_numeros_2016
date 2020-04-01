package br.jus.trt4.justica_em_numeros_2016.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO que armazena informações de um polo processual, a partir da consulta do arquivo "02_consulta_polos.sql".
 *
 * @author felipegiotto@gmail.com
 */
public class PoloDto {

	private String inParticipacao;
	
	List<ParteProcessualDto> partes = new ArrayList<>();

	public void setInParticipacao(String inParticipacao) {
		this.inParticipacao = inParticipacao;
	}
	
	public String getInParticipacao() {
		return inParticipacao;
	}
	
	public List<ParteProcessualDto> getPartes() {
		return partes;
	}
}
