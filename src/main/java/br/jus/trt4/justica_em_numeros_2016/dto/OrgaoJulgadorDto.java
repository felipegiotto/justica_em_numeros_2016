package br.jus.trt4.justica_em_numeros_2016.dto;

public class OrgaoJulgadorDto {

	public int idMunicipioIBGE;
	public String nomeNormalizado;
	//Informação utilizada apenas pelo sistema judicial legado
	public int codigo;
	
	public void setIdMunicipioIBGE(int idMunicipioIBGE, String nomeNormalizado, int codigo) {
		this.idMunicipioIBGE = idMunicipioIBGE;
		this.nomeNormalizado = nomeNormalizado;
		this.codigo = codigo;
	}
	
	public String getNomeNormalizado() {
		return nomeNormalizado;
	}
	
	public int getIdMunicipioIBGE() {
		return idMunicipioIBGE;
	}
	
	public int getCodigo() {
		return codigo;
	}
}
