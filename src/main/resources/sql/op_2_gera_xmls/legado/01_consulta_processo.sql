select
    nr_processo 											as nr_processo,
    cast(substr(nr_processo, 1,7) as int) 					as nr_sequencia,
    cast(substr(nr_processo, 12,4) as int) 					as nr_ano,
	cast(substr(nr_processo, 22,4) as int) 					as nr_origem_processo,
	case
		when p.in_segredo_justica = '1' then 's'
		else 'n' 
	end 													as in_segredo_justica,
    cd_classe_judicial,
    cast(null as character varying(100)) 					as ds_classe_judicial, -- essa informação não vai para o xml
	coalesce (p.in_recursal, 'n') 							as in_recursal,
    cast(id_municipio_ibge as character varying(7))			as id_municipio_ibge,
    dt_autuacao 											as dt_autuacao, 
    cd_orgao_julgador 										as cd_orgao_julgador, -- código do órgão julgador já no formato do cnj
    ds_orgao_julgador 										as ds_orgao_julgador, --nome do órgão já no formato do cnj
	cast(null as text) 										as ds_orgao_julgador_colegiado, -- atributo apenas para o 2 grau
    cast(null as numeric(12,2)) 							as vl_causa, -- esse campo não é devidamente preenchido no siaj
    (
		substr(nr_processo_ref,1,7) || '-' || 
		substr(nr_processo_ref,8,2) || '.' || 
		substr(nr_processo_ref,10,4) || '.5.06.' || 
		substr(nr_processo_ref, 17,4)
	) 														as nr_processo_ref,
    cast(cd_classe_judicial_ref as character varying(15))	as cd_classe_judicial_ref,
    cast(null as character varying(100)) 					as ds_classe_judicial_ref    
from
    stage_legado_1grau.processo p 
where p.nr_processo = any(:numeros_processos)