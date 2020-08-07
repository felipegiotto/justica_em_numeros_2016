SELECT
    nr_processo as nr_processo,
    CAST(substr(nr_processo, 1,7) AS INT) nr_sequencia,
    CAST(substr(nr_processo, 12,4) AS INT) nr_ano,
	CAST(substr(nr_processo, 22,4) AS INT) nr_origem_processo,
    COALESCE (p.in_segredo_justica, '0') as in_segredo_justica,
    cd_classe_judicial,
    null ds_classe_judicial, -- essa informação não vai para o xml
	p.in_recursal,
    CAST(id_municipio_ibge AS INT) AS id_municipio_ibge, --  cast
    dt_autuacao, 
    cd_orgao_julgador, -- código do órgão julgador já no formato do CNJ
    ds_orgao_julgador, --nome do órgão já no formato do CNJ
	null ds_orgao_julgador_colegiado, -- atributo apenas para o 2 grau
    null vl_causa, -- esse campo não é devidamente preenchido no SIAJ
    substr(nr_processo_ref,1,7) || '-' || substr(nr_processo_ref,8,2) || '.' || substr(nr_processo_ref,10,4) || '.5.06.' || substr(nr_processo_ref, 17,4) as nr_processo_ref,
    CAST(cd_classe_judicial_ref AS INT) AS cd_classe_judicial_ref,
    null as ds_classe_judicial_ref   
FROM
    legado_1grau.processo p 
where  nr_processo_ref is not null
	p.nr_processo = ANY(:numeros_processos)