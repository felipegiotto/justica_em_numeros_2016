package br.jus.trt4.justica_em_numeros_2016.dto;

import java.sql.ResultSet;
import java.sql.SQLException;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;

/**
 * DTO que armazena informações de um (tipo de) evento processual
 *
 * @author felipegiotto@gmail.com
 */
public class EventoDto {

	private int id;
	private Integer idEventoSuperior;
	
	public EventoDto(ResultSet rsEventos) throws SQLException {
		this.id = rsEventos.getInt("id_evento");
		this.idEventoSuperior = Auxiliar.getCampoIntOrNull(rsEventos, "id_evento_superior");
	}
	
	public int getId() {
		return id;
	}
	
	public Integer getIdEventoSuperior() {
		return idEventoSuperior;
	}
}
