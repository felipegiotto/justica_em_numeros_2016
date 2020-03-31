package br.jus.trt4.justica_em_numeros_2016.dto;

import java.time.LocalDateTime;

public class HistoricoDeslocamentoOJDto {

	private LocalDateTime dataDeslocamento;
	private LocalDateTime dataRetorno;
	private String nomeOrgaoJulgadorOrigem;
	private String nomeOrgaoJulgadorDestino;
	private int idMunicipioOrigem;
	private int idMunicipioDestino;
	
	public LocalDateTime getDataDeslocamento() {
		return dataDeslocamento;
	}
	
	public void setDataDeslocamento(LocalDateTime dataDeslocamento) {
		this.dataDeslocamento = dataDeslocamento;
	}
	
	public LocalDateTime getDataRetorno() {
		return dataRetorno;
	}
	
	public void setDataRetorno(LocalDateTime dataRetorno) {
		this.dataRetorno = dataRetorno;
	}
	
	public String getNomeOrgaoJulgadorOrigem() {
		return nomeOrgaoJulgadorOrigem;
	}
	
	public void setNomeOrgaoJulgadorOrigem(String nomeOrgaoJulgadorOrigem) {
		this.nomeOrgaoJulgadorOrigem = nomeOrgaoJulgadorOrigem;
	}
	
	public String getNomeOrgaoJulgadorDestino() {
		return nomeOrgaoJulgadorDestino;
	}
	
	public void setNomeOrgaoJulgadorDestino(String nomeOrgaoJulgadorDestino) {
		this.nomeOrgaoJulgadorDestino = nomeOrgaoJulgadorDestino;
	}
	
	public int getIdMunicipioOrigem() {
		return idMunicipioOrigem;
	}
	
	public void setIdMunicipioOrigem(int idMunicipioOrigem) {
		this.idMunicipioOrigem = idMunicipioOrigem;
	}
	
	public int getIdMunicipioDestino() {
		return idMunicipioDestino;
	}
	
	public void setIdMunicipioDestino(int idMunicipioDestino) {
		this.idMunicipioDestino = idMunicipioDestino;
	}
}
