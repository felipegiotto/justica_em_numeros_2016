import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import br.jus.cnj.replicacao_nacional.Processos;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DadosInvalidosException;

public class TestarIntegridadeXML {

	public static void main(String[] args) throws Exception {
		File arquivoXML = new File("output/MENSAL 2017-06/G2/xmls_individuais/NovaJus4/TRT4_G2_24072017_1968-2017-10028.xml");
		
		// Prepara objetos para LER os arquivos XML e analisar a quantidade de processos dentro de cada um
		JAXBContext jaxbContext = JAXBContext.newInstance(Processos.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

		Processos processosXML = (Processos) jaxbUnmarshaller.unmarshal(arquivoXML);
		
		System.out.println(processosXML.getProcesso().size());
	}
}
