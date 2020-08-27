select
	p.nr_processo 							as nr_processo,
	coalesce( pa.in_assunto_principal, 'N') as in_assunto_principal,
    cast(pa.cd_assunto_nacional as int) 	as cd_assunto_trf
from 
	stage_legado_2grau.processo_assunto pa, stage_legado_2grau.processo p
where
	pa.cd_processo = p.cd_processo 
	and p.nr_processo = any(:numeros_processos)
order by cd_assunto_trf