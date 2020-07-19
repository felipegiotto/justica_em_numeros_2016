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
 * Essa exception tem um significado especial para as rotinas de envio (operação 4) e conferência de protocolos (operação 5),
 * pois se alguma exception desta for lançada durante a execução, a operação poderá ser posteriormente reiniciada.
 *
 * @author felipe.giotto@trt4.jus.br
 */
public class DadosInvalidosException extends Exception {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LogManager.getLogger(DadosInvalidosException.class);
    private static final Map<String, Set<String>> errosPorTipo = new HashMap<>();
    private static int qtdErros = 0;
    
    public DadosInvalidosException(String tipoErro, Object origem) {
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
				listaOrigens.add(origem != null ? origem.toString() : "null");
			}
		}
        qtdErros++;
    }

    public static int getQtdErros() {
		return qtdErros;
	}
    
    /**
     * Mostra warnings no console caso tenha ocorrido algum problema com a execução da rotina
     *
     * @return
     */
    public static boolean mostrarWarningSeHouveAlgumErro() {
        if (qtdErros > 0) {
            LOGGER.warn((qtdErros == 1 ? "Ocorreu 1 problema" : "Ocorreram " + qtdErros + " problemas") + " durante a execução dessa rotina! Verifique atentamente os arquivos de log! Estes são alguns:");
            for (String tipoErro: errosPorTipo.keySet()) {
            	LOGGER.warn("* " + tipoErro + ": ");
            	for (String origem: errosPorTipo.get(tipoErro)) {
                	LOGGER.warn("    => " + origem);
            	}
            }
            return false;
        }
        return true;
    }
    
    public static void zerarQtdErros() {
    	qtdErros = 0;
    	errosPorTipo.clear();
    	ProgressoInterfaceGrafica.setWarnings("");
    }
}
