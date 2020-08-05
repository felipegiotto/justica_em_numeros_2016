SELECT
    p.nr_processo,
	pp.in_participacao,	
    pe.id_pessoa, 
    pe.ds_nome,
    pe.ds_nome          AS ds_nome_consulta,
    pe.in_tipo_pessoa   AS in_tipo_pessoa,
    'D' AS in_sexo, -- não possuímos tal informação
    pp.id_processo_parte AS id_processo_parte,
    (select pp2.id_processo_parte from legado_1grau.processo_parte pp2 where pp2.cd_processo_parte = pp.cd_proc_parte_representante ) AS id_parte_representante, 
    pp.ds_tipo_parte_representante AS ds_tipo_parte_representante,
    null AS dt_nascimento, -- não possuímos tal informação
    null AS dt_obito, -- não possuímos tal informação
    null AS nm_genitora,-- não possuímos tal informação
    null AS nm_genitor -- não possuímos tal informação
FROM
    legado_1grau.processo p, 
	legado_1grau.pessoa pe, 
	legado_1grau.processo_parte pp
where 
	1=1
	and p.cd_processo = pp.cd_processo
	and pp.cd_pessoa = pe.cd_pessoa
	p.nr_processo = ANY(:numeros_processos)
	
	