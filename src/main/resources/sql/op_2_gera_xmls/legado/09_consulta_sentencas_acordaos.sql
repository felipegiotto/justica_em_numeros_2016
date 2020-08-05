SELECT 
	p.dtautuacao as dt_juntada, 
	p.ds_login as ds_login, 
	p.nr_processo as nr_processo 
FROM 
	legado_1grau.processo p
WHERE 
	1=1
	and p.nr_processo = ANY(:numeros_processos)