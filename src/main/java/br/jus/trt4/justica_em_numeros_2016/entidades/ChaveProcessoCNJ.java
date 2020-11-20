package br.jus.trt4.justica_em_numeros_2016.entidades;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Classe que modela a tabela da chave de um processo no CNJ.
 * 
 * @author ivan.franca@trt6.jus.br
 */
@Entity
@Table(name = "tb_chave_processo_cnj", uniqueConstraints = @UniqueConstraint(columnNames = { "nr_processo",
		"cd_classe_judicial", "cd_orgao_julgador", "nm_grau" }))
public class ChaveProcessoCNJ extends BaseEntidade {

	private static final long serialVersionUID = 1L;
	
	@Id
	@SequenceGenerator(name = "generator", sequenceName = "sq_tb_chave_processo_cnj", allocationSize = 1) 
	@GeneratedValue(generator = "generator")
	@Column(name = "id_chave_processo_cnj", unique = true, nullable = false)
	private Long id;

	@Column(name = "nr_processo", nullable = false, length = 30)
	private String numeroProcesso;

	@Column(name = "cd_classe_judicial", nullable = false, length = 15)
	private String codigoClasseJudicial;

	@Column(name = "cd_orgao_julgador", nullable = false)
	private Long codigoOrgaoJulgador;

	@Column(name = "nm_grau", nullable = false, length = 1)
	private String grau;

	public ChaveProcessoCNJ() {
		// construtor padrao
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNumeroProcesso() {
		return numeroProcesso;
	}

	public void setNumeroProcesso(String numeroProcesso) {
		this.numeroProcesso = numeroProcesso;
	}

	public String getCodigoClasseJudicial() {
		return codigoClasseJudicial;
	}

	public void setCodigoClasseJudicial(String codigoClasseJudicial) {
		this.codigoClasseJudicial = codigoClasseJudicial;
	}

	public Long getCodigoOrgaoJulgador() {
		return codigoOrgaoJulgador;
	}

	public void setCodigoOrgaoJulgador(Long codigoOrgaoJulgador) {
		this.codigoOrgaoJulgador = codigoOrgaoJulgador;
	}

	public String getGrau() {
		return grau;
	}

	public void setGrau(String grau) {
		this.grau = grau;
	}

}
