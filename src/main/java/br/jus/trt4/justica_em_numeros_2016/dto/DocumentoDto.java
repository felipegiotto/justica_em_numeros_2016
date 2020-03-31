package br.jus.trt4.justica_em_numeros_2016.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class DocumentoDto {

	private LocalDateTime dataJuntada;
	private String cpfUsuarioAssinou;
	
	public DocumentoDto(ResultSet rs) throws SQLException {
		this.dataJuntada = rs.getTimestamp("dt_juntada").toLocalDateTime();
		this.cpfUsuarioAssinou = rs.getString("ds_login");
	}

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
