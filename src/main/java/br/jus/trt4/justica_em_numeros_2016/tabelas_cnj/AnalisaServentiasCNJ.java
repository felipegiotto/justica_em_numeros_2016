package br.jus.trt4.justica_em_numeros_2016.tabelas_cnj;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.AcumuladorExceptions;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.enums.BaseEmAnaliseEnum;
import br.jus.trt4.justica_em_numeros_2016.enums.OrigemProcessoEnum;
import br.jus.trt4.justica_em_numeros_2016.enums.Parametro;
import br.jus.trt4.justica_em_numeros_2016.enums.TipoRemessaEnum;

/**
 * Classe responsável por ler os arquivos de serventia do CNJ (conforme parâmetro arquivo_serventias_cnj)
 * e auxiliar no preenchimento correto dos XMLs.
 * 
 * Objeto que fará o "de/para" dos OJ e OJC do PJe para as serventias do CNJ
 * 
 * @author fgiotto
 */
public class AnalisaServentiasCNJ {

	private static final Logger LOGGER = LogManager.getLogger(AnalisaServentiasCNJ.class);
	private Map<String, ServentiaCNJ> serventiasCNJ = new HashMap<>();
	private File arquivoServentias;
	private int grau;
	private Set<String> orgaosJulgadoresSemServentiasCadastradas = new TreeSet<>();
	
	public AnalisaServentiasCNJ(BaseEmAnaliseEnum baseEmAnaliseEnum, int grau) throws IOException {
		this.grau = grau;
		//Como para o sistema judicial legado as informações já estarão dentro do padrão esperado,
		//pois serão corrigidas no pentaho, o procedimento abaixo só será aplicado ao pje.
		if (baseEmAnaliseEnum.isBasePJe()) {
			// Arquivo de onde os dados das serventias serão lidos, conforme configuração.
			arquivoServentias = getArquivoServentias();
			if (!arquivoServentias.exists()) {
				throw new IOException("O arquivo '" + arquivoServentias + "' não existe! Verifique o arquivo de configuração.");
			}
			
			// Abre o arquivo e lê, linha por linha
			Scanner scanner = new Scanner(arquivoServentias, "UTF-8");
			try {
				int linha = 0;
				while (scanner.hasNextLine()) {
					linha++;
					String line = scanner.nextLine();
					if (line.isEmpty() || line.startsWith("#")) {
						continue;
					}
					
					// Quebra cada linha em quatro partes: o id do OJ/OJC no PJe, o grau, o código da serventia no CNJ e o nome da serventia no CNJ
					String[] partes = line.split(";");
					if (partes.length != 4) {
						throw new IOException("Inconsistência na linha " + linha + " do arquivo '" + arquivoServentias + "': a linha deve conter 4 campos, separados por ponto e vírgula: o id do OJ/OJC no PJe, o grau, o código da serventia no CNJ e o nome da serventia no CNJ.");
					}
					String idOrgaoJulgadorPJe = partes[0];
					
					int grauOjcodigoServentiaCNJ;
					int codigoServentiaCNJ;

					try {
						grauOjcodigoServentiaCNJ = Integer.parseInt(partes[1]);
						codigoServentiaCNJ = Integer.parseInt(partes[2]);
					} catch (NumberFormatException ex) {
						throw new IOException("Inconsistência na linha " + linha + " do arquivo '" + arquivoServentias + "': o grau e o id da serventia devem ser um valor numérico inteiro.");
					}
					String nomeServentiaCNJ = partes[3];
					
					if (!StringUtils.isBlank(idOrgaoJulgadorPJe) && grauOjcodigoServentiaCNJ == grau) {
						
						// Verifica se não há OJs/OJCs declarados em duplicidade
						if (serventiasCNJ.containsKey(idOrgaoJulgadorPJe)) {
							LOGGER.warn("Inconsistência na linha " + linha + " do arquivo '" + arquivoServentias + "': o órgão julgador de id '" + idOrgaoJulgadorPJe + "' está definido mais de uma vez.");
						}
						
						// Adiciona o OJ/OJC na lista de serventias conhecidas
						ServentiaCNJ serventia = new ServentiaCNJ(codigoServentiaCNJ, nomeServentiaCNJ);
						serventiasCNJ.put(idOrgaoJulgadorPJe, serventia);
					}
				}
			} finally {
				scanner.close();
			}
		}
	}

	public static File getArquivoServentias() {
		return new File("src/main/resources/serventias_cnj/" + Auxiliar.getParametroConfiguracao(Parametro.arquivo_serventias_cnj, true));
	}

	/**
	 * Busca os dados de uma serventia judicial.
	 * 
	 * Ao ler dados do PJe, deve obrigatoriamente existir um mapeamento no arquivo de serventias.
	 * Ao ler dados do sistema legado, o mapeamento não será realizado: o código e a descrição recebidos por parâmetro deverão estar preenchidos já com os valores corretos.
	 * 
	 * @param descricaoOrgaoJudicialLegado : nome da serventia do CNJ (sistemas legados), não utilizado no PJe
	 * @param listaNumerosProcesso : Lista com números dos processos daquele OJ/serventia separados por ponto e vírgula e espaço 
	 * @param codigoOrgaoJudicial : id do órgão julgador (PJe) ou código da serventia do CNJ (sistemas legados)
	 * @param baseEmAnaliseEnum : sistema (PJe ou Legado) que está sendo analisado
	 * @return
	 * @throws DadosInvalidosException
	 */
	public ServentiaCNJ getServentiaByOJ(String descricaoOrgaoJudicialLegado, String listaNumerosProcesso, int codigoOrgaoJudicial, BaseEmAnaliseEnum baseEmAnaliseEnum) {
		if (baseEmAnaliseEnum.isBasePJe()) {
			if (serventiasCNJ.containsKey(Integer.toString(codigoOrgaoJudicial))) {
				return serventiasCNJ.get(Integer.toString(codigoOrgaoJudicial));
				
			} else {
				if (orgaosJulgadoresSemServentiasCadastradas.add(Integer.toString(codigoOrgaoJudicial))) {
					LOGGER.warn("Falta mapear serventia no arquivo " + AnalisaServentiasCNJ.getArquivoServentias() + ": " + Integer.toString(codigoOrgaoJudicial) + ";" + this.grau + ";<CODIGO SERVENTIA CNJ>;<NOME SERVENTIA CNJ>");
					if(listaNumerosProcesso != null) {
						LOGGER.warn(String.format("    Processos com histórico deslocamento ou na serventia de id %s: %s.", Integer.toString(codigoOrgaoJudicial), listaNumerosProcesso));
					}
				}
				return null;
			}
		} else {
			//No sistema judicial legado, as informações de código e descrição já estarão com os
			//valores corretos na base intermediária
			if (descricaoOrgaoJudicialLegado != null) {
				return new ServentiaCNJ(codigoOrgaoJudicial, descricaoOrgaoJudicialLegado);
			}
			return null;
		}
	}
	
	public boolean mostrarWarningSeAlgumaServentiaNaoFoiEncontrada() {
		
		String agrupadorErro = "Órgãos julgadores sem serventia cadastrada no arquivo " + getArquivoServentias();
		AcumuladorExceptions.instance().removerExceptionsDoAgrupador(agrupadorErro);
		if (!orgaosJulgadoresSemServentiasCadastradas.isEmpty()) {
			
			LOGGER.warn("");
			LOGGER.warn("Há pelo menos um órgão julgador que não possui serventia cadastrada no arquivo " + getArquivoServentias().getName() + " para o grau " + this.grau + " (veja instruções na chave 'arquivo_serventias_cnj' do arquivo de configurações e cadastre as linhas abaixo):");
			for (String oj: orgaosJulgadoresSemServentiasCadastradas) {
				LOGGER.warn("* " + oj + ";" + this.grau + ";<CODIGO SERVENTIA CNJ>;<NOME SERVENTIA CNJ>");
				AcumuladorExceptions.instance().adicionarException(oj, agrupadorErro);
			}
			return true;
		}
		
		return false;
	}

	public boolean diagnosticarServentiasPjeInexistentes() throws SQLException {
		LOGGER.info("Iniciando diagnóstico de serventias inexistentes no grau " + this.grau + " ...");

		List<String> listaProcessos = Auxiliar.carregarListaProcessosPJe(Integer.toString(grau));

		if (!listaProcessos.isEmpty()) {
			
			// Monta um SQL plano com todos os números de processo
			// OBS: PreparedStatement não funcionou, por causa do número de parâmetros muito grande!
			StringBuilder sqlNumerosProcessos = new StringBuilder();
			for (int i=0; i<listaProcessos.size(); i++) {
				if (i > 0) {
					sqlNumerosProcessos.append(", ");
				}
				sqlNumerosProcessos.append("'" + listaProcessos.get(i) + "'");
			}
			
			try (Connection conexao = Auxiliar.getConexao(this.grau, BaseEmAnaliseEnum.PJE)) {
				//TODO: ajustar consultas
				// Monta SQL para consultar os ids dos OJs de todos os processos da lista, nessa instância
				String sql = "SELECT DISTINCT oj.id_orgao_julgador as cd_orgao_julgador, " +
						" 				STRING_AGG(proc.nr_processo, '; ') as nr_processos " +
						"FROM tb_processo proc " +
						"INNER JOIN tb_processo_trf ptrf ON (proc.id_processo = ptrf.id_processo_trf) " +
						"INNER JOIN tb_orgao_julgador oj USING (id_orgao_julgador) " +
						"WHERE proc.nr_processo IN (" + sqlNumerosProcessos + ") " +
						"GROUP BY oj.id_orgao_julgador";
				try (ResultSet rs = conexao.createStatement().executeQuery(sql.toString())) {
					analisarExistenciaServentiasPje(rs);
				}
				
				// Monta SQL para consultar os nomes de todos os outros OJs que o processo já passou, com base na tabela "tb_hist_desloca_oj".
				// Esses dados serão utilizados para identificar o OJ que emitiu cada movimento processual.
				String sqlHistorico = "SELECT DISTINCT oj.id_orgao_julgador as cd_orgao_julgador, " + 
						" 					STRING_AGG(proc.nr_processo, '; ') as nr_processos " +
						"FROM tb_hist_desloca_oj hdo " + 
						"INNER JOIN tb_processo proc ON (proc.id_processo = hdo.id_processo_trf) " + 
						"INNER JOIN tb_orgao_julgador oj ON (oj.id_orgao_julgador = hdo.id_oj_origem) " + 
						"WHERE proc.nr_processo IN (" + sqlNumerosProcessos + ") " +
						"GROUP BY oj.id_orgao_julgador " +
						"UNION " + 
						"SELECT DISTINCT oj.id_orgao_julgador as cd_orgao_julgador, " +
						" 					STRING_AGG(proc.nr_processo, '; ') as nr_processos " +
						"FROM tb_hist_desloca_oj hdo " + 
						"INNER JOIN tb_processo proc ON (proc.id_processo = hdo.id_processo_trf) " + 
						"INNER JOIN tb_orgao_julgador oj ON (oj.id_orgao_julgador = hdo.id_oj_destino) " + 
						"WHERE proc.nr_processo IN (" + sqlNumerosProcessos + ") " + 
						"GROUP BY oj.id_orgao_julgador";
				LOGGER.info("Consultando historicos de deslocamento...");
				try (ResultSet rs = conexao.createStatement().executeQuery(sqlHistorico.toString())) {
					analisarExistenciaServentiasPje(rs);
				}
			}
		}

		LOGGER.info("Finalizado diagnóstico de serventias inexistentes no grau " + this.grau + ".");
		
		return this.mostrarWarningSeAlgumaServentiaNaoFoiEncontrada();
	}

	
	private void analisarExistenciaServentiasPje(ResultSet rs) throws SQLException {
		while (rs.next()) {
			getServentiaByOJ("", rs.getString("nr_processos"), rs.getInt("cd_orgao_julgador"), BaseEmAnaliseEnum.PJE);
		}
	}

	public static void main(String[] args) throws Exception {
		AnalisaServentiasCNJ analisaServentiasCNJ = new AnalisaServentiasCNJ(BaseEmAnaliseEnum.PJE,1);
		analisaServentiasCNJ.diagnosticarServentiasPjeInexistentes();
		
		analisaServentiasCNJ = new AnalisaServentiasCNJ(BaseEmAnaliseEnum.PJE,2);
		analisaServentiasCNJ.diagnosticarServentiasPjeInexistentes();
	}
}
