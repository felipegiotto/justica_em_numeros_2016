package br.jus.trt4.justica_em_numeros_2016.dto;

public class ClasseJudicialDto {
	
	private int codigo;
	private String descricao;
	private boolean recursal;
	
	public int getCodigo() {
		return codigo;
	}
	
	public void setCodigo(int codigo) {
		this.codigo = codigo;
	}
	
	public String getDescricao() {
		return descricao;
	}
	
	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}
	
	public boolean isRecursal() {
		return recursal;
	}
	
	public void setRecursal(boolean recursal) {
		this.recursal = recursal;
	}
}
