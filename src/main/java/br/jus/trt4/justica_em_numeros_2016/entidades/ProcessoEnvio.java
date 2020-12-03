package br.jus.trt4.justica_em_numeros_2016.entidades;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import br.jus.trt4.justica_em_numeros_2016.entidades.conversores.OrigemProcessoEnumConverter;
import br.jus.trt4.justica_em_numeros_2016.enums.OrigemProcessoEnum;

/**
 * Classe que modela a tabela que guarda as informações dos processos que serão enviados.
 * 
 * @author ivan.franca@trt6.jus.br
 */
@Entity
@Table(name = "tb_processo_envio", uniqueConstraints = @UniqueConstraint(columnNames = { "nr_processo",
		"nm_grau", "id_remessa" }))
public class ProcessoEnvio extends BaseEntidade {

	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name = "generator", sequenceName = "sq_tb_processo_envio", allocationSize = 1)
	@GeneratedValue(generator = "generator")
	@Column(name = "id_processo_envio", unique = true, nullable = false)
	private Long id;

	@Convert(converter = OrigemProcessoEnumConverter.class)
	@Column(name = "cd_origem_processo", length = 1, nullable = false)
	private OrigemProcessoEnum origem;

	@Column(name = "nr_processo", nullable = false, length = 30)
	private String numeroProcesso;

	@Column(name = "nm_grau", nullable = false, length = 1)
	private String grau;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_remessa")
	private Remessa remessa;

	public ProcessoEnvio() {
		// construtor padrao
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public OrigemProcessoEnum getOrigem() {
		return origem;
	}

	public void setOrigem(OrigemProcessoEnum origem) {
		this.origem = origem;
	}

	public String getNumeroProcesso() {
		return numeroProcesso;
	}

	public void setNumeroProcesso(String numeroProcesso) {
		this.numeroProcesso = numeroProcesso;
	}

	public String getGrau() {
		return grau;
	}

	public void setGrau(String grau) {
		this.grau = grau;
	}

	public Remessa getRemessa() {
		return remessa;
	}

	public void setRemessa(Remessa remessa) {
		this.remessa = remessa;
	}

}
