package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import java.io.File;

/**
 * Classe que representa um XML que deve ser enviado ao CNJ
 * 
 * @author felipe.giotto@trt4.jus.br
 */
public class XmlComInstancia {

	private File arquivoXML;
	private int grau;
	
	public XmlComInstancia(File arquivoXML, int grau) {
		super();
		this.arquivoXML = arquivoXML;
		this.grau = grau;
	}
	
	public File getArquivoXML() {
		return arquivoXML;
	}
	
	public int getGrau() {
		return grau;
	}
	
	@Override
	public String toString() {
		return arquivoXML.toString();
	}
}
