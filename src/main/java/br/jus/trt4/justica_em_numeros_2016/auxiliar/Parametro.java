package br.jus.trt4.justica_em_numeros_2016.auxiliar;

/**
 * Lista todos os parâmetros que podem ser lidos do arquivo "config.properties".
 * 
 * Foi criado um "enum" para facilitar a identificação de parâmetros inválidos no
 * arquivo de configurações e também para localizar atributos depreciados.
 * 
 * @author fgiotto
 */
public enum Parametro {
	gerar_xml_1G,
	gerar_xml_2G,
	url_jdbc_1g, 
	url_jdbc_2g,
	assunto_padrao_1G,
	assunto_padrao_2G,
	arquivo_serventias_cnj,
	dia_padrao_para_arquivos_xml,
	pasta_saida_padrao,
	tipo_carga_xml,
	baixa_incremental,
	codigo_municipio_ibge_trt,
	contador_inicial_xmls_unificados,
	url_jdbc_egestao_1g, 
	url_jdbc_egestao_2g, 
	configuracao_proxy,
	sigla_tribunal, 
	jar_replicacao_nacional_cnj,
	contornar_falta_de_genero;
}
