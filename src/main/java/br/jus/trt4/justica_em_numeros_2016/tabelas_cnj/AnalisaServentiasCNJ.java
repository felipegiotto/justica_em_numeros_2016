package br.jus.trt4.justica_em_numeros_2016.tabelas_cnj;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.BenchmarkVariasOperacoes;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.DadosInvalidosException;
import br.jus.trt4.justica_em_numeros_2016.auxiliar.Parametro;
import br.jus.trt4.justica_em_numeros_2016.enums.BaseEmAnaliseEnum;

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
	private static Set<String> orgaosJulgadoresSemServentiasCadastradas = new TreeSet<>();
	
	public AnalisaServentiasCNJ(BaseEmAnaliseEnum baseEmAnaliseEnum) throws IOException {
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
					
					// Quebra cada linha em três partes: o nome do OJ/OJC no PJe, o código da serventia no CNJ e o nome da serventia no CNJ
					String[] partes = line.split(";");
					if (partes.length != 3) {
						throw new IOException("Inconsistência na linha " + linha + " do arquivo '" + arquivoServentias + "': a linha deve conter 3 campos, separados por ponto e vírgula: o nome do OJ/OJC no PJe, o código da serventia no CNJ e o nome da serventia no CNJ.");
					}
					String orgaoJulgadorPJe = partes[0];
					int codigoServentiaCNJ;
					try {
						codigoServentiaCNJ = Integer.parseInt(partes[1]);
					} catch (NumberFormatException ex) {
						throw new IOException("Inconsistência na linha " + linha + " do arquivo '" + arquivoServentias + "': o código da serventia deve ser um valor numérico inteiro.");
					}
					String nomeServentiaCNJ = partes[2];
					
					if (!StringUtils.isBlank(orgaoJulgadorPJe)) {
						
						// Verifica se não há OJs/OJCs declarados em duplicidade
						if (serventiasCNJ.containsKey(orgaoJulgadorPJe)) {
							LOGGER.warn("Inconsistência na linha " + linha + " do arquivo '" + arquivoServentias + "': o órgão julgador '" + orgaoJulgadorPJe + "' está definido mais de uma vez.");
						}
						
						// Adiciona o OJ/OJC na lista de serventias conhecidas
						ServentiaCNJ serventia = new ServentiaCNJ(codigoServentiaCNJ, nomeServentiaCNJ);
						serventiasCNJ.put(orgaoJulgadorPJe, serventia);
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

	public ServentiaCNJ getServentiaByOJ(String descricaoOrgao, int codigoOrgao, BaseEmAnaliseEnum baseEmAnaliseEnum, boolean obrigatorio) throws DadosInvalidosException {
		if (baseEmAnaliseEnum.isBasePJe()) {
			if (serventiasCNJ.containsKey(descricaoOrgao)) {
				return serventiasCNJ.get(descricaoOrgao);
			} else {
				
				//LOGGER.warn("Inconsistência no arquivo '" + arquivoServentias + "': não há nenhuma linha definindo o código e o nome da serventia para o OJ/OJC '" + nomePJe + "', do PJe. Para evitar interrupção da rotina, será utilizada uma serventia temporária.");
				//return new ServentiaCNJ("CODIGO_INEXISTENTE", "SERVENTIA INEXISTENTE");
				orgaosJulgadoresSemServentiasCadastradas.add(descricaoOrgao);
				if (obrigatorio) {
					throw new DadosInvalidosException("Inconsistência no arquivo '" + arquivoServentias + "'", "Não há nenhuma linha definindo o código e o nome da serventia para o OJ/OJC '" + descricaoOrgao + "', do PJe.");
				} else {
					return null;
				}
			}
		} else {
			//No sistema judicial legado, as informações de código e descrição já estarão com os
			//valores corretos na base intermediária
			return new ServentiaCNJ(codigoOrgao, descricaoOrgao);
		}
	}
	
	public static boolean mostrarWarningSeAlgumaServentiaNaoFoiEncontrada() {
		if (!orgaosJulgadoresSemServentiasCadastradas.isEmpty()) {
			LOGGER.warn("");
			LOGGER.warn("Há pelo menos um órgão julgador que não possui serventia cadastrada no arquivo " + getArquivoServentias().getName() + " (veja instruções na chave 'arquivo_serventias_cnj' do arquivo de configurações e cadastre as linhas abaixo):");
			for (String oj: orgaosJulgadoresSemServentiasCadastradas) {
				LOGGER.warn("* " + oj + ";<CODIGO SERVENTIA CNJ>;<NOME SERVENTIA CNJ>");
			}
			return true;
		}
		
		return false;
	}
	
	public boolean diagnosticarServentiasPjeInexistentes() throws SQLException, DadosInvalidosException {
		LOGGER.info("Iniciando diagnóstico de serventias inexistentes...");
		
		if (Auxiliar.deveProcessarSegundoGrau()) {
			diagnosticarServentiasPjeInexistentes(2);
		}
		
		if (Auxiliar.deveProcessarPrimeiroGrau()) {
			diagnosticarServentiasPjeInexistentes(1);
		}
		
		LOGGER.info("Finalizado diagnóstico de serventias inexistentes.");
		
		return AnalisaServentiasCNJ.mostrarWarningSeAlgumaServentiaNaoFoiEncontrada();
	}

	private void diagnosticarServentiasPjeInexistentes(int grau) throws SQLException, DadosInvalidosException {
		
		List<String> listaProcessos = Auxiliar.carregarListaProcessosDoArquivo(Auxiliar.getArquivoListaProcessosPje(grau));
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
			
			try (Connection conexao = Auxiliar.getConexao(grau, BaseEmAnaliseEnum.PJE)) {
				
				// Monta SQL para consultar os nomes dos OJs de todos os processos da lista, nessa instância
				// Sugestao TRT6, por causa de falha no PostgreSQL na conversão do caractere "º" para ASCII:
				//                StringBuilder sql = new StringBuilder("SELECT DISTINCT upper(to_ascii(replace (oj.ds_orgao_julgador, 'º', 'O'))) as ds_orgao_julgador " +
				//                Fonte: e-mail com assunto "Sugestões de alterações justica_em_numeros_2016" do TRT6
				//                Fonte: https://www.postgresql.org/message-id/20040607212810.15543.qmail@web13125.mail.yahoo.com
				String sql = "SELECT DISTINCT upper(to_ascii(oj.ds_orgao_julgador)) as ds_orgao_julgador " + 
						"FROM tb_processo proc " +
						"INNER JOIN tb_processo_trf ptrf ON (proc.id_processo = ptrf.id_processo_trf) " +
						"INNER JOIN tb_orgao_julgador oj USING (id_orgao_julgador) " +
						"WHERE proc.nr_processo IN (" + sqlNumerosProcessos + ")";
				BenchmarkVariasOperacoes.globalInstance().inicioOperacao("Consulta de serventias");
				try (ResultSet rs = conexao.createStatement().executeQuery(sql.toString())) {
					analisarExistenciaServentiasPje(rs);
				} finally {
					BenchmarkVariasOperacoes.globalInstance().fimOperacao();
				}
				
				// Monta SQL para consultar os nomes de todos os outros OJs que o processo já passou, com base na tabela "tb_hist_desloca_oj".
				// Esses dados serão utilizados para identificar o OJ que emitiu cada movimento processual.
				String sqlHistorico = "SELECT DISTINCT upper(to_ascii(oj.ds_orgao_julgador)) as ds_orgao_julgador " + 
						"FROM tb_hist_desloca_oj hdo " + 
						"INNER JOIN tb_processo proc ON (proc.id_processo = hdo.id_processo_trf) " + 
						"INNER JOIN tb_orgao_julgador oj ON (oj.id_orgao_julgador = hdo.id_oj_origem) " + 
						"WHERE proc.nr_processo IN (" + sqlNumerosProcessos + ") " + 
						"UNION " + 
						"SELECT DISTINCT upper(to_ascii(oj.ds_orgao_julgador)) as ds_orgao_julgador " + 
						"FROM tb_hist_desloca_oj hdo " + 
						"INNER JOIN tb_processo proc ON (proc.id_processo = hdo.id_processo_trf) " + 
						"INNER JOIN tb_orgao_julgador oj ON (oj.id_orgao_julgador = hdo.id_oj_destino) " + 
						"WHERE proc.nr_processo IN (" + sqlNumerosProcessos + ")";
				LOGGER.info("Consultando historicos de deslocamento...");
				BenchmarkVariasOperacoes.globalInstance().inicioOperacao("Consulta de historicos de deslocamento");
				try (ResultSet rs = conexao.createStatement().executeQuery(sqlHistorico.toString())) {
					analisarExistenciaServentiasPje(rs);
				} finally {
					BenchmarkVariasOperacoes.globalInstance().fimOperacao();
				}
				
			}
		}
	}
	
	private void analisarExistenciaServentiasPje(ResultSet rs) throws SQLException {
		while (rs.next()) {
			try {
				getServentiaByOJ(rs.getString("ds_orgao_julgador"), 0, BaseEmAnaliseEnum.PJE, false);
			} catch (DadosInvalidosException e) {
				// Não vai acontecer, por causa do parametro "false"
			}
		}
	}

	public static void main(String[] args) throws Exception {
		AnalisaServentiasCNJ analisaServentiasCNJ = new AnalisaServentiasCNJ(BaseEmAnaliseEnum.PJE);
		analisaServentiasCNJ.diagnosticarServentiasPjeInexistentes();
	}
}
