select 
    p.nr_processo 							as nr_processo,
    pm.cd_processo_movimento 				as id_processo_evento, 
    (
		select 
			case 
				when mm.visibilidade_externa = 'S' then true else false
			end as visibilidade_externa
		from sgt_consulta.movimentos mm
		where mm.cod_movimento = pm.cd_movimento_cnj
	) 										as in_visibilidade_externa,
    pm.cd_movimento_cnj  					as cd_movimento_cnj,
    cast (NULL as character varying(1000)) 	as ds_texto_final_interno,
    (
		select mm.movimento from sgt_consulta.movimentos mm
		where mm.cod_movimento = pm.cd_movimento_cnj
	) 										as ds_movimento,
	pm.ds_login 							as ds_login,
	cast(null as int) 						as id_magistrado,
	false 									as is_magistrado_julgamento,
    pm.dt_atualizacao						as dt_atualizacao	
from
	stage_legado_1grau.processo p, stage_legado_1grau.processo_movimento pm
where
	1=1
	and p.cd_processo = pm.cd_processo
	and p.nr_processo = ANY(:numeros_processos)
		