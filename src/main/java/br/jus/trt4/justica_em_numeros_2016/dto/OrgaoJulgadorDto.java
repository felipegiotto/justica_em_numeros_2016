package br.jus.trt4.justica_em_numeros_2016.dto;

public class OrgaoJulgadorDto {

	public int idMunicipioIBGE;
	public int codigoServentia;
	
	//código da serventia no CNJ utilizada apenas para o sistema legado.
	public String descricaoServentiaJudiciariaLegado;

	public void setIdMunicipioIBGE(int idMunicipioIBGE, int codigoServentia) {
		this.idMunicipioIBGE = idMunicipioIBGE;
		this.codigoServentia = codigoServentia;
	}

	public int getCodigoServentia() {
		return codigoServentia;
	}

	public int getIdMunicipioIBGE() {
		return idMunicipioIBGE;
	}

	public String getDescricaoServentiaJudiciariaLegado() {
		return descricaoServentiaJudiciariaLegado;
	}

	public void setDescricaoServentiaJudiciariaLegado(String descricaoServentiaJudiciariaLegado) {
		this.descricaoServentiaJudiciariaLegado = descricaoServentiaJudiciariaLegado;
	}

}
