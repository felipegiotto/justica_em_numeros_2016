package br.jus.trt4.justica_em_numeros_2016.entidades;

import java.time.LocalDateTime;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import br.jus.trt4.justica_em_numeros_2016.entidades.conversores.OrigemProcessoEnumConverter;
import br.jus.trt4.justica_em_numeros_2016.entidades.conversores.SituacaoLoteProcessoEnumConverter;
import br.jus.trt4.justica_em_numeros_2016.enums.OrigemProcessoEnum;
import br.jus.trt4.justica_em_numeros_2016.enums.SituacaoLoteProcessoEnum;

/**
 * Classe que modela a tabela que relaciona um lote e um processo.
 */
@Entity
@Table(name = "tb_lote_processo")
public class LoteProcesso extends BaseEntidade {

	private static final long serialVersionUID = 1L;

	// TODO: avaliar desempenho allocationSize = 100)
	@Id
	@SequenceGenerator(name = "generator", sequenceName = "sq_tb_lote_processo", allocationSize = 1) 
	@GeneratedValue(generator = "generator")
	@Column(name = "id_lote_processo", unique = true, nullable = false)
	private Long id;

	@Column(name = "dh_envio_local", nullable = true)
	private LocalDateTime dataEnvioLocal;

	@Column(name = "dh_recebimento_cnj", nullable = true)
	private LocalDateTime dataRecebimentoCNJ;

	@Column(name = "nm_protocolo_cnj", nullable = true, length = 60)
	private String protocoloCNJ;

	@Column(name = "nm_hash_cnj", nullable = true, length = 64)
	private String hashCNJ;

	@Convert(converter = SituacaoLoteProcessoEnumConverter.class)
	@Column(name = "en_situacao", length = 2, nullable = true)
	private SituacaoLoteProcessoEnum situacao;

	@Convert(converter = OrigemProcessoEnumConverter.class)
	@Column(name = "en_origem_processo", length = 1, nullable = true)
	private OrigemProcessoEnum origem;

	@Column(name = "conteudo_xml", nullable = true)
	private byte[] conteudoXML;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_lote")
	private Lote lote;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_chave_processo_cnj")
	private ChaveProcessoCNJ chaveProcessoCNJ;

	public LoteProcesso() {
		// construtor padrao
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDateTime getDataEnvioLocal() {
		return dataEnvioLocal;
	}

	public void setDataEnvioLocal(LocalDateTime dataEnvioLocal) {
		this.dataEnvioLocal = dataEnvioLocal;
	}

	public LocalDateTime getDataRecebimentoCNJ() {
		return dataRecebimentoCNJ;
	}

	public void setDataRecebimentoCNJ(LocalDateTime dataRecebimentoCNJ) {
		this.dataRecebimentoCNJ = dataRecebimentoCNJ;
	}

	public String getProtocoloCNJ() {
		return protocoloCNJ;
	}

	public void setProtocoloCNJ(String protocoloCNJ) {
		this.protocoloCNJ = protocoloCNJ;
	}

	public String getHashCNJ() {
		return hashCNJ;
	}

	public void setHashCNJ(String hashCNJ) {
		this.hashCNJ = hashCNJ;
	}

	public SituacaoLoteProcessoEnum getSituacao() {
		return situacao;
	}

	public void setSituacao(SituacaoLoteProcessoEnum situacao) {
		this.situacao = situacao;
	}

	public OrigemProcessoEnum getOrigem() {
		return origem;
	}

	public void setOrigem(OrigemProcessoEnum origem) {
		this.origem = origem;
	}

	@Lob @Basic(fetch = FetchType.LAZY)
	public byte[] getConteudoXML() {
		return conteudoXML;
	}

	public void setConteudoXML(byte[] conteudoXML) {
		this.conteudoXML = conteudoXML;
	}

	public Lote getLote() {
		return lote;
	}

	public void setLote(Lote lote) {
		this.lote = lote;
	}

	public ChaveProcessoCNJ getChaveProcessoCNJ() {
		return chaveProcessoCNJ;
	}

	public void setChaveProcessoCNJ(ChaveProcessoCNJ chaveProcessoCNJ) {
		this.chaveProcessoCNJ = chaveProcessoCNJ;
	}

}
