package br.jus.trt4.justica_em_numeros_2016.dto;

public class OrgaoJulgadorDto {

	public int idMunicipioIBGE;
	public String nomeNormalizado;
	//Informação utilizada apenas pelo sistema judicial legado
	public int codigoServentiaJudiciariaLegado;
	
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
	
	public int getCodigoServentiaJudiciariaLegado() {
		return codigoServentiaJudiciariaLegado;
	}
	
	public void setCodigoServentiaJudiciariaLegado(int codigo) {
		this.codigoServentiaJudiciariaLegado = codigo;
	}
}
