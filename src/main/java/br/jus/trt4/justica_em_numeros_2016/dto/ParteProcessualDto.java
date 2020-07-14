package br.jus.trt4.justica_em_numeros_2016.dto;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;

/**
 * DTO que armazena informações de uma parte processual, a partir da consulta do arquivo "03_consulta_partes.sql".
 *
 * @author felipegiotto@gmail.com
 */
public class ParteProcessualDto {

	private String nomeParte;
	private String nomeConsultaParte;
	private int idProcessoParte;
	private int idPessoa;
	private Integer idProcessoParteRepresentante;
	private String tipoParteRepresentante;
	private String tipoPessoa;
	private String sexoPessoa;
	private LocalDate dataNascimento;
	private LocalDate dataObito;
	private String nomeGenitor;
	private String nomeGenitora;
	
	private List<EnderecoDto> enderecos = new ArrayList<>();
	private List<DocumentoPessoaDto> documentos = new ArrayList<>();
	
	public ParteProcessualDto(ResultSet rsPartes) throws SQLException {
		this.nomeParte = rsPartes.getString("ds_nome").trim();
		this.nomeConsultaParte = rsPartes.getString("ds_nome_consulta");
		this.idProcessoParte = rsPartes.getInt("id_processo_parte");
		this.idPessoa = rsPartes.getInt("id_pessoa");
		this.idProcessoParteRepresentante = Auxiliar.getCampoIntOrNull(rsPartes, "id_parte_representante");
		this.tipoParteRepresentante = rsPartes.getString("ds_tipo_parte_representante");
		this.tipoPessoa = Auxiliar.getCampoStringNotNull(rsPartes, "in_tipo_pessoa");
		this.sexoPessoa = rsPartes.getString("in_sexo");
		this.dataNascimento = dateToLocalDate(rsPartes.getDate("dt_nascimento"));
		this.dataObito = dateToLocalDate(rsPartes.getDate("dt_obito"));
		this.nomeGenitor = rsPartes.getString("nm_genitor");
		this.nomeGenitora = rsPartes.getString("nm_genitora");
	}

	private LocalDate dateToLocalDate(Date data) throws SQLException {
		if (data != null) {
			return data.toLocalDate();
		}
		return null;
	}

	public String getNomeParte() {
		return nomeParte;
	}
	
	public String getNomeConsultaParte() {
		return nomeConsultaParte;
	}
	
	public int getIdProcessoParte() {
		return idProcessoParte;
	}
	
	public int getIdPessoa() {
		return idPessoa;
	}
	
	public Integer getIdProcessoParteRepresentante() {
		return idProcessoParteRepresentante;
	}
	
	public String getTipoParteRepresentante() {
		return tipoParteRepresentante;
	}
	
	public String getTipoPessoa() {
		return tipoPessoa;
	}
	
	public String getSexoPessoa() {
		return sexoPessoa;
	}
	
	public LocalDate getDataNascimento() {
		return dataNascimento;
	}
	
	public LocalDate getDataObito() {
		return dataObito;
	}
	
	public String getNomeGenitor() {
		return nomeGenitor;
	}
	
	public String getNomeGenitora() {
		return nomeGenitora;
	}
	
	public List<EnderecoDto> getEnderecos() {
		return enderecos;
	}
	
	public List<DocumentoPessoaDto> getDocumentos() {
		return documentos;
	}
}
