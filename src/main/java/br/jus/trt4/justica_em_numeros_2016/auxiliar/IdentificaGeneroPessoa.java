package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.jus.cnj.modeloDeTransferenciaDeDados.ModalidadeGeneroPessoa;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoPessoa;
import br.jus.cnj.modeloDeTransferenciaDeDados.TipoQualificacaoPessoa;
import br.jus.trt4.justica_em_numeros_2016.enums.BaseEmAnaliseEnum;

/**
 * Classe que visa contornar problema de ausência de informação de sexo no PJe.
 * 
 * O PJe possui uma falha na remessa de processos entre instâncias, que faz com que grande parte das
 * pessoas sejam remetidas sem informação de gênero (masculino / feminino). Por isso, o cadastro
 * das pessoas, no 2o Grau, muitas vezes não possui essa informação.
 * 
 * Através dessa classe, a informação do gênero, quando ausente, será localizada na outra instância.
 * 
 * @author fgiotto
 */
public class IdentificaGeneroPessoa implements AutoCloseable {

	private static final Logger LOGGER = LogManager.getLogger(IdentificaGeneroPessoa.class);
	private NamedParameterStatement nsConsultaGeneroOutraInstancia;
	private HashMap<String, ModalidadeGeneroPessoa> cacheGenerosOutraInstancia = new HashMap<>();
	private Connection conexaoBasePrincipalOutraInstancia;

	public IdentificaGeneroPessoa(int grau, BaseEmAnaliseEnum baseEmAnalise) {
		
		// Abre conexão com o outro banco de dados do PJe, para localizar o sexo de pessoas que podem
		// estar sem essa informação
		// OBS: só busca o sexo na outra instância se o parâmetro estiver habilitado nas configurações
		if (Auxiliar.getParametroBooleanConfiguracao(Parametro.contornar_falta_de_genero, false)) {
			try {
				conexaoBasePrincipalOutraInstancia = Auxiliar.getConexao(grau, baseEmAnalise);
				
				String caminhoArquivo = "src/main/resources/sql/op_2_gera_xmls/" 
						+ Auxiliar.getPastaResources(baseEmAnalise, grau) 
						+ "/consulta_genero_outra_instancia.sql";
		
				String sqlConsultaGeneroOutraInstancia = Auxiliar.lerConteudoDeArquivo(caminhoArquivo);
				nsConsultaGeneroOutraInstancia = new NamedParameterStatement(conexaoBasePrincipalOutraInstancia, sqlConsultaGeneroOutraInstancia);
			} catch (SQLException | IOException ex) {
				LOGGER.warn("Não foi possível abrir conexão com a outra instância do " + baseEmAnalise.getDescricao() + " (" + grau + "G). O parâmetro 'contornar_falta_de_genero' não terá efeito!");
			}
		}
	}


	public void preencherSexoPessoa(TipoPessoa pessoa, String sexo, String nomeConsulta, BaseEmAnaliseEnum baseEmAnaliseEnum) throws SQLException {
		
		// Se o sexo veio preenchido corretamente no cadastro, grava na pessoa.
		if (!StringUtils.isBlank(sexo) && !"D".equals(sexo)) {
			pessoa.setSexo(ModalidadeGeneroPessoa.valueOf(sexo));
			return;
		} 
		
		// Se não for uma pessoa física, não adianta tentar localizar o sexo na outra instância
		if (!TipoQualificacaoPessoa.FISICA.equals(pessoa.getTipoPessoa())) {
			pessoa.setSexo(ModalidadeGeneroPessoa.D);
			return;
		} 
		
		// Se o gênero da pessoa não estiver no banco de dados, tenta localizar na outra instância, 
		// contando que todas as informações necessárias estejam disponíveis (nome e documento).
		// Comportamento implementado apenas para o PJe
		String documentoPrincipal = pessoa.getNumeroDocumentoPrincipal();
		if (!StringUtils.isBlank(documentoPrincipal) && nsConsultaGeneroOutraInstancia != null) {
			
			// Verifica se a informação do gênero ainda não está no cache
			String chaveCache = documentoPrincipal + "|" + nomeConsulta;
			if (!cacheGenerosOutraInstancia.containsKey(chaveCache)) {
				
				// Sincroniza chamadas ao gerar XMLs em várias threads, pois pode ocorrer problema de concorrência.
				synchronized (nsConsultaGeneroOutraInstancia) {
					
					// Gênero não está em cache, faz pesquisa na outra instância
					nsConsultaGeneroOutraInstancia.setString("nome_consulta", nomeConsulta);
					nsConsultaGeneroOutraInstancia.setString("documento", documentoPrincipal);
					try (ResultSet rs = nsConsultaGeneroOutraInstancia.executeQuery()) { // TODO: Otimizar acessos repetidos
						if (rs.next()) {
							ModalidadeGeneroPessoa modalidadeSexo = ModalidadeGeneroPessoa.valueOf(rs.getString("in_sexo"));
							cacheGenerosOutraInstancia.put(chaveCache, modalidadeSexo);
							// LOGGER.debug("Sexo da pessoa " + pessoa.getNome() + " foi identificado na outra instância (" + grau + "G): " + sexo);
						} else {
							cacheGenerosOutraInstancia.put(chaveCache, ModalidadeGeneroPessoa.D);
							// LOGGER.debug("Sexo da pessoa " + pessoa.getNome() + " não existe em nenhuma instância");
						}
					}
				}
			}
			pessoa.setSexo(cacheGenerosOutraInstancia.get(chaveCache));
			
		} else {
			
			// Se realmente não foi possível consultar o sexo na outra instância, grava como DESCONHECIDO.
			pessoa.setSexo(ModalidadeGeneroPessoa.D);
		}

	}

	
	@Override
	public void close() {
		
		if (nsConsultaGeneroOutraInstancia != null) {
			try {
				nsConsultaGeneroOutraInstancia.close();
				nsConsultaGeneroOutraInstancia = null;
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando consulta 'nsConsultaGeneroOutraInstancia': " + e.getLocalizedMessage(), e);
			}
		}
		
		if (conexaoBasePrincipalOutraInstancia != null) {
			try {
				conexaoBasePrincipalOutraInstancia.close();
				conexaoBasePrincipalOutraInstancia = null;
			} catch (SQLException e) {
				LOGGER.warn("Erro fechando 'conexaoBasePrincipalOutraInstancia': " + e.getLocalizedMessage(), e);
			}
		}
	}
}
