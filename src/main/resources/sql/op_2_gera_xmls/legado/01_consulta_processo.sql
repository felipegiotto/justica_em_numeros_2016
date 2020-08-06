SELECT
    nr_processo as nr_processo,
    cd_processo as id_processo,
    CAST(substr(nr_processo, 1,7) AS INT) nr_sequencia,
    CAST(substr(nr_processo, 12,4) AS INT) nr_ano,
	CAST(substr(nr_processo, 22,4) AS INT) nr_origem_processo,
    COALESCE (p.in_segredo_justica, '0') as in_segredo_justica,
    cd_classe_judicial,
    '' ds_classe_judicial, -- essa informação não vai para o xml
    id_municipio_ibge,
    dt_autuacao, 
    cd_orgao_julgador, -- código do órgão julgador já no formato do CNJ
    ds_orgao_julgador, --nome do órgão já no formato do CNJ
    '' ds_orgao_julgador_colegiado, -- atributo apenas para o 2 grau
    grau nr_instancia,
    null vl_causa, -- esse campo não é devidamente preenchido no SIAJ
	CAST(nr_processo_ref AS BIGINT) as id_proc_referencia,
    nr_processo_ref,
    cd_classe_judicial_ref,
    ds_classe_judicial_ref   
FROM
    legado_1grau.processo p
where  
	p.nr_processo = ANY(:numeros_processos)