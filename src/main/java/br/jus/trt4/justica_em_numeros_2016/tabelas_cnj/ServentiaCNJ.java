package br.jus.trt4.justica_em_numeros_2016.tabelas_cnj;

/**
 * Representa uma serventia judiciária no padrão CNJ, com código e nome
 * 
 * @author fgiotto
 */
public class ServentiaCNJ {

	private String codigo;
	private String nome;
	
	public ServentiaCNJ(String codigo, String nome) {
		super();
		this.codigo = codigo;
		this.nome = nome;
	}

	public String getCodigo() {
		return codigo;
	}
	
	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}
	
	public String getNome() {
		return nome;
	}
	
	public void setNome(String nome) {
		this.nome = nome;
	}
}
