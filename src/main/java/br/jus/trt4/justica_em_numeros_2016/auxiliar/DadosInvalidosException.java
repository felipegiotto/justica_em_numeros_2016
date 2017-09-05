package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Exceção lançada quando algum dado inválido é encontrado no processo, dado esse que faria com que o arquivo XML fosse
 * negado no CNJ.
 * 
 * @author fgiotto
 *
 */
public class DadosInvalidosException extends Exception {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LogManager.getLogger(DadosInvalidosException.class);
    private static int qtdErros = 0;
    
    public DadosInvalidosException(String cause) {
        super(cause);
        qtdErros++;
    }

    public static int getQtdErros() {
		return qtdErros;
	}
    
    public static void mostrarWarningSeHouveAlgumErro() {
        if (qtdErros > 0) {
            LOGGER.warn((qtdErros == 1 ? "Ocorreu 1 erro" : "Ocorreram " + qtdErros + " erros") + " durante a execução dessa rotina! Verifique atentamente os arquivos de log!");
        }
    }
    
    public static void zerarQtdErros() {
    	qtdErros = 0;
    }
}
