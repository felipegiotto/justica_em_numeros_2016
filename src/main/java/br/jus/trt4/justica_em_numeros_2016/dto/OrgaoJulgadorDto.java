package br.jus.trt4.justica_em_numeros_2016.dto;

public class OrgaoJulgadorDto {

	public int idMunicipioIBGE;
	public String nomeNormalizado;
	
	public void setIdMunicipioIBGE(int idMunicipioIBGE, String nomeNormalizado) {
		this.idMunicipioIBGE = idMunicipioIBGE;
		this.nomeNormalizado = nomeNormalizado;
	}
	
	public String getNomeNormalizado() {
		return nomeNormalizado;
	}
	
	public int getIdMunicipioIBGE() {
		return idMunicipioIBGE;
	}
}
