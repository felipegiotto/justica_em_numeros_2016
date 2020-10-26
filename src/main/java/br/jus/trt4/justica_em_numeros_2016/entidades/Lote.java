package br.jus.trt4.justica_em_numeros_2016.entidades;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Classe que modela a tabela de lote.
 */
@Entity
@Table(name = "tb_lote")
public class Lote extends BaseEntidade {

	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name = "generator", sequenceName = "sq_tb_lote", allocationSize = 1)
	@GeneratedValue(generator = "generator")
	@Column(name = "id_lote", unique = true, nullable = false)
	private Long id;

	@Column(name = "nm_numero", nullable = false, length = 10)
	private String numero;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_remessa")
	private Remessa remessa;
	//TODO: Testar o @LazyCollection(LazyCollectionOption.EXTRA) se o desempenho estiver ruim
	@OneToMany(mappedBy = "lote", fetch = FetchType.LAZY)
	private List<LoteProcesso> lotesProcessos = new ArrayList<>(0);

	public Lote() {
		// construtor padrao
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public Remessa getRemessa() {
		return remessa;
	}

	public void setRemessa(Remessa remessa) {
		this.remessa = remessa;
	}

	public List<LoteProcesso> getLotesProcessos() {
		return lotesProcessos;
	}

	public void setLotesProcessos(List<LoteProcesso> lotesProcessos) {
		this.lotesProcessos = lotesProcessos;
	}

}
