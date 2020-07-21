package br.jus.trt4.justica_em_numeros_2016.auxiliar;

public class DataJudException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DataJudException(String mensagem) {
		super(mensagem);
	}
	
	public DataJudException(String mensagem, Throwable cause) {
		super(mensagem, cause);
	}
}
