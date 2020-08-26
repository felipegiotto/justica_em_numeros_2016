SELECT 
	null as dt_juntada, 
	null as ds_login, 
	p.nr_processo as nr_processo 
FROM 
	stage_legado_2grau.processo p
WHERE 
	1=1
	and p.nr_processo is not null
	and p.nr_processo = ANY(:numeros_processos)