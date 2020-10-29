package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import br.jus.trt4.justica_em_numeros_2016.enums.SituacaoProtocoloCNJ;

public class MetaInformacaoEnvio {

	private String codHash;
	private String datDataEnvioProtocolo;
	private String flgExcluido;
	private String grau;
	private String numProtocolo;
	private String seqProtocolo;
	private String siglaOrgao;
	private String tamanhoArquivo;
	private String tipStatusProtocolo;
	private String urlArquivo;
	private String numProcesso;

	public MetaInformacaoEnvio(String codHash, String datDataEnvioProtocolo, String flgExcluido,
			String grau, String numProtocolo, String seqProtocolo, String siglaOrgao, String tamanhoArquivo,
			String tipStatusProtocolo, String urlArquivo) {
		this.codHash = codHash;
		this.datDataEnvioProtocolo = datDataEnvioProtocolo;
		this.flgExcluido = flgExcluido;
		this.grau = grau;
		this.numProtocolo = numProtocolo;
		this.seqProtocolo = seqProtocolo;
		this.siglaOrgao = siglaOrgao;
		this.tamanhoArquivo = tamanhoArquivo;
		this.tipStatusProtocolo = tipStatusProtocolo;
		this.urlArquivo = urlArquivo;
		this.numProcesso = null;
	}

	public String getCodHash() {
		return codHash;
	}

	public void setCodHash(String codHash) {
		this.codHash = codHash;
	}

	public String getDatDataEnvioProtocolo() {
		return datDataEnvioProtocolo;
	}

	public void setDatDataEnvioProtocolo(String datDataEnvioProtocolo) {
		this.datDataEnvioProtocolo = datDataEnvioProtocolo;
	}

	public String getFlgExcluido() {
		return flgExcluido;
	}

	public void setFlgExcluido(String flgExcluido) {
		this.flgExcluido = flgExcluido;
	}

	public String getGrau() {
		return grau;
	}

	public void setGrau(String grau) {
		this.grau = grau;
	}

	public String getNumProtocolo() {
		return numProtocolo;
	}

	public void setNumProtocolo(String numProtocolo) {
		this.numProtocolo = numProtocolo;
	}

	public String getSeqProtocolo() {
		return seqProtocolo;
	}

	public void setSeqProtocolo(String seqProtocolo) {
		this.seqProtocolo = seqProtocolo;
	}

	public String getSiglaOrgao() {
		return siglaOrgao;
	}

	public void setSiglaOrgao(String siglaOrgao) {
		this.siglaOrgao = siglaOrgao;
	}

	public String getTamanhoArquivo() {
		return tamanhoArquivo;
	}

	public void setTamanhoArquivo(String tamanhoArquivo) {
		this.tamanhoArquivo = tamanhoArquivo;
	}

	public String getTipStatusProtocolo() {
		return tipStatusProtocolo;
	}

	public void setTipStatusProtocolo(String tipStatusProtocolo) {
		this.tipStatusProtocolo = tipStatusProtocolo;
	}

	public String getUrlArquivo() {
		return urlArquivo;
	}

	public void setUrlArquivo(String urlArquivo) {
		this.urlArquivo = urlArquivo;
	}

	public String getNumProcesso() {
		return numProcesso;
	}

	public void setNumProcesso(String numProcesso) {
		this.numProcesso = numProcesso;
	}
	
	public String getDataEnvioProtocoloFormatada() {
		if (!this.datDataEnvioProtocolo.equals("")) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
			Timestamp ts=new Timestamp(new Long(this.datDataEnvioProtocolo));
			return dateFormat.format(new Date(ts.getTime()));
		}
		return "";
	}
	
	public String getInformacaoFormatada() {
		return this.numProcesso + ";" + this.grau + ";" + this.numProtocolo + ";" + SituacaoProtocoloCNJ.getDescricaoPeloID(new Integer(this.tipStatusProtocolo)) 
		+ ";" + this.codHash + ";" + this.getDataEnvioProtocoloFormatada();
	}
	
	public boolean hasStatusErro() {
		return SituacaoProtocoloCNJ.hasStatusErro(new Integer(this.tipStatusProtocolo));
	}

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((codHash == null) ? 0 : codHash.hashCode());
		result = prime * result + ((datDataEnvioProtocolo == null) ? 0 : datDataEnvioProtocolo.hashCode());
		result = prime * result + ((flgExcluido == null) ? 0 : flgExcluido.hashCode());
		result = prime * result + ((grau == null) ? 0 : grau.hashCode());
		result = prime * result + ((numProcesso == null) ? 0 : numProcesso.hashCode());
		result = prime * result + ((numProtocolo == null) ? 0 : numProtocolo.hashCode());
		result = prime * result + ((seqProtocolo == null) ? 0 : seqProtocolo.hashCode());
		result = prime * result + ((siglaOrgao == null) ? 0 : siglaOrgao.hashCode());
		result = prime * result + ((tamanhoArquivo == null) ? 0 : tamanhoArquivo.hashCode());
		result = prime * result + ((tipStatusProtocolo == null) ? 0 : tipStatusProtocolo.hashCode());
		result = prime * result + ((urlArquivo == null) ? 0 : urlArquivo.hashCode());
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
		MetaInformacaoEnvio other = (MetaInformacaoEnvio) obj;
		if (codHash == null) {
			if (other.codHash != null)
				return false;
		} else if (!codHash.equals(other.codHash))
			return false;
		if (datDataEnvioProtocolo == null) {
			if (other.datDataEnvioProtocolo != null)
				return false;
		} else if (!datDataEnvioProtocolo.equals(other.datDataEnvioProtocolo))
			return false;
		if (flgExcluido == null) {
			if (other.flgExcluido != null)
				return false;
		} else if (!flgExcluido.equals(other.flgExcluido))
			return false;
		if (grau == null) {
			if (other.grau != null)
				return false;
		} else if (!grau.equals(other.grau))
			return false;
		if (numProcesso == null) {
			if (other.numProcesso != null)
				return false;
		} else if (!numProcesso.equals(other.numProcesso))
			return false;
		if (numProtocolo == null) {
			if (other.numProtocolo != null)
				return false;
		} else if (!numProtocolo.equals(other.numProtocolo))
			return false;
		if (seqProtocolo == null) {
			if (other.seqProtocolo != null)
				return false;
		} else if (!seqProtocolo.equals(other.seqProtocolo))
			return false;
		if (siglaOrgao == null) {
			if (other.siglaOrgao != null)
				return false;
		} else if (!siglaOrgao.equals(other.siglaOrgao))
			return false;
		if (tamanhoArquivo == null) {
			if (other.tamanhoArquivo != null)
				return false;
		} else if (!tamanhoArquivo.equals(other.tamanhoArquivo))
			return false;
		if (tipStatusProtocolo == null) {
			if (other.tipStatusProtocolo != null)
				return false;
		} else if (!tipStatusProtocolo.equals(other.tipStatusProtocolo))
			return false;
		if (urlArquivo == null) {
			if (other.urlArquivo != null)
				return false;
		} else if (!urlArquivo.equals(other.urlArquivo))
			return false;
		return true;
	}

}
