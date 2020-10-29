package br.jus.trt4.justica_em_numeros_2016.dto;

import br.jus.trt4.justica_em_numeros_2016.enums.OrigemProcessoEnum;

/**
 * DTO que armazena informações de uma entity ChaveProcessoCNJ, a partir das consultas realizadas na operação 1.
 *
 * @author ivan.franca@trt6.jus.br
 */
public class ChaveProcessoCNJDto {

	private String numeroProcesso;

	private String codigoClasseJudicial;

	private Long codigoOrgaoJulgador;

	private String grau;

	private OrigemProcessoEnum origemProcessoEnum;

	public ChaveProcessoCNJDto(String numeroProcesso, String codigoClasseJudicial, Long codigoOrgaoJulgador,
			String grau, OrigemProcessoEnum origemProcessoEnum) {
		this.numeroProcesso = numeroProcesso;
		this.codigoClasseJudicial = codigoClasseJudicial;
		this.codigoOrgaoJulgador = codigoOrgaoJulgador;
		this.grau = grau;
		this.origemProcessoEnum = origemProcessoEnum;
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

	public OrigemProcessoEnum getOrigemProcessoEnum() {
		return origemProcessoEnum;
	}

	public void setOrigemProcessoEnum(OrigemProcessoEnum origemProcessoEnum) {
		this.origemProcessoEnum = origemProcessoEnum;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((codigoClasseJudicial == null) ? 0 : codigoClasseJudicial.hashCode());
		result = prime * result + ((codigoOrgaoJulgador == null) ? 0 : codigoOrgaoJulgador.hashCode());
		result = prime * result + ((grau == null) ? 0 : grau.hashCode());
		result = prime * result + ((numeroProcesso == null) ? 0 : numeroProcesso.hashCode());
		result = prime * result + ((origemProcessoEnum == null) ? 0 : origemProcessoEnum.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChaveProcessoCNJDto other = (ChaveProcessoCNJDto) obj;
		if (codigoClasseJudicial == null) {
			if (other.codigoClasseJudicial != null)
				return false;
		} else if (!codigoClasseJudicial.equals(other.codigoClasseJudicial))
			return false;
		if (codigoOrgaoJulgador == null) {
			if (other.codigoOrgaoJulgador != null)
				return false;
		} else if (!codigoOrgaoJulgador.equals(other.codigoOrgaoJulgador))
			return false;
		if (grau == null) {
			if (other.grau != null)
				return false;
		} else if (!grau.equals(other.grau))
			return false;
		if (numeroProcesso == null) {
			if (other.numeroProcesso != null)
				return false;
		} else if (!numeroProcesso.equals(other.numeroProcesso))
			return false;
		if (origemProcessoEnum != other.origemProcessoEnum)
			return false;
		return true;
	}

}
