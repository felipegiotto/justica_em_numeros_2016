select
	p.nr_processo 							as nr_processo,
    pp.in_participacao 						as in_participacao,	
	pe.id_pessoa 							as id_pessoa , 
    (
		case 
			when pe.ds_nome is null then '' 
			else pe.ds_nome 
		end 
	) 										as ds_nome,
	(
		case 
			when pe.ds_nome is null then '' 
			else upper(pe.ds_nome) 
		end 
	)										as ds_nome_consulta,
    pe.in_tipo_pessoa   					as in_tipo_pessoa,
    cast('D' as character(1))				as in_sexo, -- não possuímos tal informação
    (
		select max(pp2.id_processo_parte) 
		from stage_legado_1grau.processo_parte pp2 
		where 1=1
		and pp.cd_processo = pp2.cd_processo 
		and pp.cd_pessoa = pp2.cd_pessoa
	)  										as id_processo_parte,
    (
		select pp2.id_processo_parte 
		from stage_legado_1grau.processo_parte pp2 
		where 1=1
		and pp2.cd_processo_parte = pp.cd_proc_parte_representante 
	) 										as id_parte_representante, 
    pp.ds_tipo_parte_representante			as ds_tipo_parte_representante,
    cast(null as date) 						as dt_nascimento, -- não possuímos tal informação
    cast(null as timestamp) 				as dt_obito, -- não possuímos tal informação
    cast(null as character varying(255)) 	as nm_genitora,-- não possuímos tal informação
    cast(null as character varying(255)) 	as nm_genitor -- não possuímos tal informação	
from
    stage_legado_1grau.processo p, 
	stage_legado_1grau.pessoa pe, 
	stage_legado_1grau.processo_parte pp
where 
	1=1
	and p.cd_processo = pp.cd_processo
	and pp.cd_pessoa = pe.cd_pessoa
	and p.nr_processo = any(:numeros_processos)
ORDER BY pp.in_participacao