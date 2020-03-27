package br.jus.trt4.justica_em_numeros_2016.dto;

import java.time.LocalDateTime;

public class DocumentoDto {

	private LocalDateTime dataJuntada;
	private String cpfUsuarioAssinou;
	
	public LocalDateTime getDataJuntada() {
		return dataJuntada;
	}
	
	public String getCpfUsuarioAssinou() {
		return cpfUsuarioAssinou;
	}
	
	public void setDataJuntada(LocalDateTime dataJuntada) {
		this.dataJuntada = dataJuntada;
	}
	
	public void setCpfUsuarioAssinou(String cpfUsuarioAssinou) {
		this.cpfUsuarioAssinou = cpfUsuarioAssinou;
	}
}
