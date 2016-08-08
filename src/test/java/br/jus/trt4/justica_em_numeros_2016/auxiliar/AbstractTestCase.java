package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import org.junit.BeforeClass;

public class AbstractTestCase {

	@BeforeClass
	public static void beforeClass() {
		Auxiliar.prepararPastaDeSaida();
	}
}
