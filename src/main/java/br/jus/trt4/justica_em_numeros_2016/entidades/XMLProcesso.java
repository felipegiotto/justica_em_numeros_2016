package br.jus.trt4.justica_em_numeros_2016.entidades;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Classe que modela a tabela que armazena os XMLs de um processo.
 * 
 * @author ivan.franca@trt6.jus.br
 */
@Entity
@Table(name = "tb_xml_processo")
public class XMLProcesso extends BaseEntidade {

	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name = "generator", sequenceName = "sq_tb_xml_processo", allocationSize = 1)
	@GeneratedValue(generator = "generator")
	@Column(name = "id_xml_processo", unique = true, nullable = false)
	private Long id;

	@Column(name = "conteudo_xml", nullable = false)
	private byte[] conteudoXML;

	@OneToOne(mappedBy = "xmlProcesso", fetch = FetchType.LAZY)
	private LoteProcesso loteProcesso;

	public XMLProcesso() {
		// construtor padrao
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Lob
	public byte[] getConteudoXML() {
		return conteudoXML;
	}

	public void setConteudoXML(byte[] conteudoXML) {
		this.conteudoXML = conteudoXML;
	}

	public LoteProcesso getLoteProcesso() {
		return loteProcesso;
	}

	public void setLoteProcesso(LoteProcesso loteProcesso) {
		this.loteProcesso = loteProcesso;
	}

}
