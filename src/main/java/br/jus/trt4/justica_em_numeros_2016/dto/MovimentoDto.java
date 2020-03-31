package br.jus.trt4.justica_em_numeros_2016.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;

/**
 * DTO que armazena informações de um movimento processual, a partir da consulta do arquivo "06_consulta_movimentos.sql".
 *
 * @author felipegiotto@gmail.com
 */
public class MovimentoDto {

	private LocalDateTime dataAtualizacao;
	private boolean visibilidadeExterna;
	private int idProcessoEvento;
	private int codMovimentoCNJ;
	private String cpfUsuarioMovimento;
	private boolean usuarioMagistrado;
	private String textoMovimento;
	private String textoEvento;
	private boolean movimentoMagistradoJulgamento;
	
	public MovimentoDto(ResultSet rsMovimentos) throws SQLException {
		this.dataAtualizacao = rsMovimentos.getTimestamp("dt_atualizacao").toLocalDateTime();
		this.visibilidadeExterna = rsMovimentos.getBoolean("in_visibilidade_externa");
		this.idProcessoEvento = rsMovimentos.getInt("id_processo_evento");
		this.codMovimentoCNJ = Auxiliar.getCampoIntNotNull(rsMovimentos, "cd_movimento_cnj");
		this.cpfUsuarioMovimento = rsMovimentos.getString("ds_login");
		this.usuarioMagistrado = rsMovimentos.getString("id_magistrado") != null;
		this.textoMovimento = rsMovimentos.getString("ds_texto_final_interno");
		this.textoEvento = rsMovimentos.getString("ds_movimento");
		this.movimentoMagistradoJulgamento = rsMovimentos.getBoolean("is_magistrado_julgamento");
	}

	public LocalDateTime getDataAtualizacao() {
		return dataAtualizacao;
	}
	
	public boolean isVisibilidadeExterna() {
		return visibilidadeExterna;
	}
	
	public int getIdProcessoEvento() {
		return idProcessoEvento;
	}
	
	public int getCodMovimentoCNJ() {
		return codMovimentoCNJ;
	}
	
	public String getCPFUsuarioMovimento() {
		return cpfUsuarioMovimento;
	}
	
	public boolean isUsuarioMagistrado() {
		return usuarioMagistrado;
	}
	
	public String getTextoMovimento() {
		return textoMovimento;
	}
	
	public String getTextoEvento() {
		return textoEvento;
	}
	
	public boolean isMovimentoMagistradoJulgamento() {
		return movimentoMagistradoJulgamento;
	}
}
