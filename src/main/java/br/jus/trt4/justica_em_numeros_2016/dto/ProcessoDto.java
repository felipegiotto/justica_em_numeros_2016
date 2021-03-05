package br.jus.trt4.justica_em_numeros_2016.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;

/**
 * DTO que armazena informações de um processo, a partir da consulta do arquivo "01_consulta_processo.sql".
 * 
 * @author felipegiotto@gmail.com
 */
public class ProcessoDto {

	private String numeroProcesso;
	private int numeroInstancia;
	private boolean segredoJustica;
	private LocalDateTime dataAutuacao;
	private Double valorCausa;
	
	private ClasseJudicialDto classeJudicial;
	private OrgaoJulgadorDto orgaoJulgador;
	private ProcessoDto processoReferencia;
	
	private Map<String, PoloDto> polosPorTipoParticipacao = new LinkedHashMap<>();
	private List<MovimentoDto> movimentos = new ArrayList<>();
	private List<AssuntoDto> assuntos = new ArrayList<>();
	private List<ProcessoDto> incidentes = new ArrayList<>();
	private List<DocumentoDto> sentencasAcordaos = new ArrayList<>();
	private List<HistoricoDeslocamentoOJDto> historicosDeslocamentoOJ = new ArrayList<>();
	
	public ProcessoDto() {
	}
	
	public ProcessoDto(ResultSet rsProcesso, boolean somenteProcessoEClasse) throws SQLException {
		this.numeroProcesso = rsProcesso.getString("nr_processo");
		
		this.classeJudicial = new ClasseJudicialDto();
		this.classeJudicial.setCodigo(rsProcesso.getInt("cd_classe_judicial"));
		this.classeJudicial.setDescricao(rsProcesso.getString("ds_classe_judicial"));
		this.classeJudicial.setRecursal("S".equals(rsProcesso.getString("in_recursal")));
		
		if (somenteProcessoEClasse) {
			return;
		}
		
		// UPDATE: NÃO PEGA a instância do banco de dados, pois a grande maioria dos registros no segundo grau estão com nr_instancia NULAS
		// this.numeroInstancia = rsProcesso.getInt("nr_instancia");
		this.segredoJustica = "S".equals(rsProcesso.getString("in_segredo_justica"));
		this.dataAutuacao = Auxiliar.getCampoLocalDateTimeOrNull(rsProcesso, "dt_autuacao");
		this.valorCausa = Auxiliar.getCampoDoubleOrNull(rsProcesso, "vl_causa");
		
		this.orgaoJulgador = new OrgaoJulgadorDto();
		this.orgaoJulgador.setIdMunicipioIBGE(rsProcesso.getInt("id_municipio_ibge"), rsProcesso.getInt("cd_orgao_julgador"));
		this.orgaoJulgador.setDescricaoServentiaJudiciariaLegado(rsProcesso.getString("ds_orgao_julgador"));
		
		if (rsProcesso.getString("nr_processo_ref") != null) {
			this.processoReferencia = new ProcessoDto();
			this.processoReferencia.numeroProcesso = rsProcesso.getString("nr_processo_ref");
			this.processoReferencia.classeJudicial = new ClasseJudicialDto();
			this.processoReferencia.classeJudicial.setCodigo(rsProcesso.getInt("cd_classe_judicial_ref"));
			this.processoReferencia.classeJudicial.setDescricao(rsProcesso.getString("ds_classe_judicial_ref"));
		}
	}
	
	public String getNumeroProcesso() {
		return numeroProcesso;
	}
	
	public String getNumeroProcessoSemSinais() {
		return numeroProcesso.replaceAll("[^0-9]", "");
	}
	
	public int getNumeroInstancia() {
		return numeroInstancia;
	}
	
	public void setNumeroInstancia(int numeroInstancia) {
		this.numeroInstancia = numeroInstancia;
	}
	
	public boolean isSegredoJustica() {
		return segredoJustica;
	}
	
	public LocalDateTime getDataAutuacao() {
		return dataAutuacao;
	}
	
	public Double getValorCausa() {
		return valorCausa;
	}
	
	public ClasseJudicialDto getClasseJudicial() {
		return classeJudicial;
	}
	
	public void setClasseJudicial(ClasseJudicialDto classeJudicial) {
		this.classeJudicial = classeJudicial;
	}
	
	public OrgaoJulgadorDto getOrgaoJulgador() {
		return orgaoJulgador;
	}
	
	public ProcessoDto getProcessoReferencia() {
		return processoReferencia;
	}
	
	public Map<String, PoloDto> getPolosPorTipoParticipacao() {
		return polosPorTipoParticipacao;
	}
	
	public List<MovimentoDto> getMovimentos() {
		return movimentos;
	}
	
	public List<AssuntoDto> getAssuntos() {
		return assuntos;
	}
	
	public List<ProcessoDto> getIncidentes() {
		return incidentes;
	}
	
	public List<DocumentoDto> getSentencasAcordaos() {
		return sentencasAcordaos;
	}
	
	public List<HistoricoDeslocamentoOJDto> getHistoricosDeslocamentoOJ() {
		return historicosDeslocamentoOJ;
	}
}
