package br.jus.trt4.justica_em_numeros_2016.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class HistoricoDeslocamentoOJDto {

	private LocalDateTime dataDeslocamento;
	private LocalDateTime dataRetorno;
	private String nomeOrgaoJulgadorOrigem;
	private String nomeOrgaoJulgadorDestino;
	private int codigoOrgaoJulgadorOrigem;
	private int codigoOrgaoJulgadorDestino;
	private int idMunicipioOrigem;
	private int idMunicipioDestino;

	public HistoricoDeslocamentoOJDto(ResultSet rs) throws SQLException {
		this.dataDeslocamento = rs.getTimestamp("dt_deslocamento").toLocalDateTime();
		this.dataRetorno = rs.getTimestamp("dt_retorno").toLocalDateTime();
		this.nomeOrgaoJulgadorOrigem = rs.getString("ds_oj_origem");
		this.nomeOrgaoJulgadorDestino = rs.getString("ds_oj_destino");
		this.codigoOrgaoJulgadorOrigem = rs.getInt("cd_oj_origem");
		this.codigoOrgaoJulgadorDestino = rs.getInt("cd_oj_destino");
		this.idMunicipioOrigem = rs.getInt("id_municipio_origem");
		this.idMunicipioDestino = rs.getInt("id_municipio_destino");
	}

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

	public int getCodigoOrgaoJulgadorOrigem() {
		return codigoOrgaoJulgadorOrigem;
	}

	public void setCodigoOrgaoJulgadorOrigem(int codigoOrgaoJulgadorOrigem) {
		this.codigoOrgaoJulgadorOrigem = codigoOrgaoJulgadorOrigem;
	}

	public int getCodigoOrgaoJulgadorDestino() {
		return codigoOrgaoJulgadorDestino;
	}

	public void setCodigoOrgaoJulgadorDestino(int codigoOrgaoJulgadorDestino) {
		this.codigoOrgaoJulgadorDestino = codigoOrgaoJulgadorDestino;
	}

}
