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
	url_webservice_cnj,
	assunto_padrao_1G,
	assunto_padrao_2G,
	arquivo_serventias_cnj,
	pasta_saida_padrao,
	tipo_carga_xml,
	baixa_incremental,
	codigo_municipio_ibge_trt,
	url_jdbc_egestao_1g, 
	url_jdbc_egestao_2g, 
	proxy_host,
	proxy_port,
	proxy_username,
	proxy_password,
	sigla_tribunal,
	password_tribunal,
	contornar_falta_de_genero;
}
