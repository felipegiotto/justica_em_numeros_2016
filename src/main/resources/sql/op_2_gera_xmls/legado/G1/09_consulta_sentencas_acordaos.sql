SELECT 
	CAST(null AS TIMESTAMP) as dt_juntada, 
	CAST(null AS CHARACTER VARYING (100)) as ds_login,
	p.nr_processo as nr_processo 
FROM 
	stage_legado_1grau.processo p
WHERE 
	1=1
	and p.nr_processo is not null
	and p.nr_processo = ANY(:numeros_processos)
ORDER BY dt_juntada