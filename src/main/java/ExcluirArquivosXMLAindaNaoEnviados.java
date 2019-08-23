import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.XmlComInstancia;
import br.jus.trt4.justica_em_numeros_2016.tasks.Op_4_ValidaEnviaArquivosCNJ;

/**
 * Classe auxiliar que pode ser utilizada se muitos arquivos forem negados no CNJ e for preciso gerar novamente os XMLs.
 * 
 * Esta classe localiza os XMLs gerados que ainda não foram marcados como enviados e os exclui, permitindo uma nova geração.
 *  
 * @author felipe.giotto@trt4.jus.br
 *
 */
public class ExcluirArquivosXMLAindaNaoEnviados {

	private static final Logger LOGGER = LogManager.getLogger(ExcluirArquivosXMLAindaNaoEnviados.class);
	
	public static void main(String[] args) throws Exception {
		List<XmlComInstancia> arquivosXML = new ArrayList<>();
		
		Op_4_ValidaEnviaArquivosCNJ op = new Op_4_ValidaEnviaArquivosCNJ();
		op.localizarXMLsInstanciasHabilitadas(arquivosXML);
		arquivosXML = op.filtrarSomenteArquivosPendentesDeEnvio(arquivosXML);
		
		if (arquivosXML.isEmpty()) {
			LOGGER.info("Nenhum arquivo pendente de envio!");
			return;
		}
		
		LOGGER.info("Arquivos para exclusão:" + arquivosXML.size());
		for (XmlComInstancia xml: arquivosXML) {
			LOGGER.info("* " + xml.getArquivoXML());
		}
		
		LOGGER.warn("Confirma a exclusão dos " + arquivosXML.size() + " arquivos acima? (S/N)");
		if ("S".equals(Auxiliar.readStdin().toUpperCase())) {
			for (XmlComInstancia xml: arquivosXML) {
				LOGGER.info("* Excluindo " + xml.getArquivoXML());
				xml.getArquivoXML().delete();
			}
		}
		
		LOGGER.info("Operação concluída!");
	}
}
