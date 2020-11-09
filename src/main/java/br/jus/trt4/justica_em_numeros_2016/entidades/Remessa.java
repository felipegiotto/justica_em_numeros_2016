package br.jus.trt4.justica_em_numeros_2016.entidades;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import br.jus.trt4.justica_em_numeros_2016.entidades.conversores.TipoRemessaEnumConverter;
import br.jus.trt4.justica_em_numeros_2016.enums.TipoRemessaEnum;

/**
 * Classe que modela a tabela de remessa.
 * 
 * @author ivan.franca@trt6.jus.br
 */
@Entity
@Table(name = "tb_remessa", uniqueConstraints = @UniqueConstraint(columnNames = {
		"en_tipo", "dt_corte" }))
public class Remessa extends BaseEntidade {

	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name = "generator", sequenceName = "sq_tb_remessa", allocationSize = 1)
	@GeneratedValue(generator = "generator")
	@Column(name = "id_remessa", unique = true, nullable = false)
	private Long id;

	@Convert(converter = TipoRemessaEnumConverter.class)
	@Column(name = "en_tipo", length = 1, nullable = false)
	private TipoRemessaEnum tipoRemessa;

	@Column(name = "dt_corte", nullable = false)
	private LocalDate dataCorte;

	@OneToMany(mappedBy = "remessa", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Lote> lotes = new ArrayList<>(0);
	
	@OneToMany(mappedBy = "remessa", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProcessoEnvio> processosEnvio = new ArrayList<>(0);

	public Remessa() {
		// construtor padrao
	}

	public TipoRemessaEnum getTipoRemessa() {
		return tipoRemessa;
	}

	public void setTipoRemessa(TipoRemessaEnum tipoRemessa) {
		this.tipoRemessa = tipoRemessa;
	}

	public LocalDate getDataCorte() {
		return dataCorte;
	}

	public void setDataCorte(LocalDate dataCorte) {
		this.dataCorte = dataCorte;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public List<Lote> getLotes() {
		return lotes;
	}

	public void setLotes(List<Lote> lotes) {
		this.lotes = lotes;
	}

	public List<ProcessoEnvio> getProcessosEnvio() {
		return processosEnvio;
	}

	public void setProcessosEnvio(List<ProcessoEnvio> processosEnvio) {
		this.processosEnvio = processosEnvio;
	}
	
	

}
