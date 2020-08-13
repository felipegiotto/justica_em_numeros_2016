SELECT
	p.nr_processo,
    pp.in_participacao,	
	pe.id_pessoa, 
    case when pe.ds_nome is null then '' else pe.ds_nome end as ds_nome,
    case when pe.ds_nome is null then '' else pe.ds_nome end as ds_nome_consulta,
    pe.in_tipo_pessoa   AS in_tipo_pessoa,
    'D' AS in_sexo, -- não possuímos tal informação
    (select max(pp2.id_processo_parte) from stage_legado_1grau.processo_parte pp2 where pp.cd_processo = pp2.cd_processo and pp.cd_pessoa = pp2.cd_pessoa)  AS id_processo_parte,
    (select pp2.id_processo_parte from stage_legado_1grau.processo_parte pp2 where pp2.cd_processo_parte = pp.cd_proc_parte_representante ) AS id_parte_representante, 
    pp.ds_tipo_parte_representante AS ds_tipo_parte_representante,
    CAST(null AS DATE) AS dt_nascimento, -- não possuímos tal informação
    CAST(null AS TIMESTAMP) AS dt_obito, -- não possuímos tal informação
    null AS nm_genitora,-- não possuímos tal informação
    null AS nm_genitor -- não possuímos tal informação	
FROM
    stage_legado_1grau.processo p, 
	stage_legado_1grau.pessoa pe, 
	stage_legado_1grau.processo_parte pp
where 
	1=1
	and p.cd_processo = pp.cd_processo
	and pp.cd_pessoa = pe.cd_pessoa
	and p.nr_processo = ANY(:numeros_processos)