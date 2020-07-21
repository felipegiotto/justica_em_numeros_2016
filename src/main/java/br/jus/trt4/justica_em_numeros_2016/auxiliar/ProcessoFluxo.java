package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import java.io.File;

import br.jus.trt4.justica_em_numeros_2016.enums.BaseEmAnaliseEnum;
import br.jus.trt4.justica_em_numeros_2016.tasks.Op_Y_OperacaoFluxoContinuo;

/**
 * Controla o fluxo de geração de XML, envio e validação no CNJ de um único processo, para ser utilizado na classe {@link Op_Y_OperacaoFluxoContinuo}
 *
 * @author felipe.giotto@trt4.jus.br
 */
public class ProcessoFluxo {

	private int grau;
	private File arquivoXML;
	private File arquivoXMLErro;
	private File arquivoXMLProtocolo;
	private File arquivoXMLAceito;
	private File arquivoXMLNegado;
	private ProcessoSituacaoEnum situacao;
	
	public ProcessoFluxo(int grau, String numeroProcesso) {
		this.grau = grau;
		// TODO: Implementar também para sistemas legados
		this.arquivoXML = Auxiliar.gerarNomeArquivoIndividualParaProcesso(BaseEmAnaliseEnum.PJE, grau, numeroProcesso);
		this.arquivoXMLProtocolo = Auxiliar.gerarNomeArquivoProtocoloProcessoEnviado(arquivoXML);
		this.arquivoXMLAceito = Auxiliar.gerarNomeArquivoProcessoSucesso(arquivoXMLProtocolo);
		this.arquivoXMLNegado = Auxiliar.gerarNomeArquivoProcessoNegado(arquivoXMLProtocolo);
		this.arquivoXMLErro = Auxiliar.gerarNomeArquivoProcessoErro(arquivoXML);
		identificarSituacao();
	}

	public void identificarSituacao() {
		
		// Se o processo já foi concluído, não mudará mais de status
		if (ProcessoSituacaoEnum.CONCLUIDO.equals(situacao)) {
			return;
		}
		
		// Verifica se o XML já foi ACEITO ou NEGADO
		if (this.arquivoXMLAceito.exists()) {
			this.situacao = ProcessoSituacaoEnum.CONCLUIDO;
			return;
		}
		if (this.arquivoXMLNegado.exists() || this.arquivoXMLErro.exists()) {
			this.situacao = ProcessoSituacaoEnum.ERRO;
			return;
		}
		
		// Verifica se o XML já foi enviado (protocolo foi recebido)
		if (this.arquivoXMLProtocolo.exists()) {
			this.situacao = ProcessoSituacaoEnum.ENVIADO;
			return;
		}
		
		// Verifica se o XML já foi gerado
		if (arquivoXML.exists()) {
			this.situacao = ProcessoSituacaoEnum.XML_GERADO;
			return;
		}
		
		this.situacao = ProcessoSituacaoEnum.INICIO;
	}
	
	public int getGrau() {
		return grau;
	}

	public ProcessoSituacaoEnum getSituacao() {
		return situacao;
	}
	
	public File getArquivoXML() {
		return arquivoXML;
	}
}
