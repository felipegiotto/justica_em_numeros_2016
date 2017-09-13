package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
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
    private static final Map<String, Set<String>> errosPorTipo = new HashMap<>();
    private static int qtdErros = 0;
    
    public DadosInvalidosException(String tipoErro, String origem) {
        super(origem + ": " + tipoErro);
        
        // Armazena até 10 registros de cada tipo de erro, para mostrar um relatório no final da operação 
        synchronized (DadosInvalidosException.class) {
        	Set<String> listaOrigens;
			if (!errosPorTipo.containsKey(tipoErro)) {
				listaOrigens = new TreeSet<String>();
				errosPorTipo.put(tipoErro, listaOrigens);
				
				// Mostra warnings encontrados na interface gráfica
				ProgressoInterfaceGrafica.setWarnings("PROBLEMAS (detalhes nos arquivos de log):\n\n" + StringUtils.join(errosPorTipo.keySet(), "\n\n"));
				
			} else {
				listaOrigens = errosPorTipo.get(tipoErro);
			}
			
			if (listaOrigens.size() < 10) {
				listaOrigens.add(origem);
			}
		}
        qtdErros++;
    }

    public static int getQtdErros() {
		return qtdErros;
	}
    
    public static void mostrarWarningSeHouveAlgumErro() {
        if (qtdErros > 0) {
            LOGGER.warn((qtdErros == 1 ? "Ocorreu 1 erro" : "Ocorreram " + qtdErros + " erros") + " durante a execução dessa rotina! Verifique atentamente os arquivos de log! Estes são alguns:");
            for (String tipoErro: errosPorTipo.keySet()) {
            	LOGGER.warn("* " + tipoErro + ": ");
            	for (String origem: errosPorTipo.get(tipoErro)) {
                	LOGGER.warn("    => " + origem);
            	}
            }
        }
    }
    
    public static void zerarQtdErros() {
    	qtdErros = 0;
    	errosPorTipo.clear();
    }
}
