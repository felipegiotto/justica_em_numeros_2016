SELECT
    substr(nr_processo_ref,1,7) || '-' || substr(nr_processo_ref,8,2) || '.' || substr(nr_processo_ref,10,4) || '.5.06.' || substr(nr_processo_ref, 17,4) as nr_processo_referencia,
    p.nr_processo as nr_processo,
    CAST(p.cd_classe_judicial AS INT) as cd_classe_processual,
	p.in_recursal,
    null as ds_classe_processual -- essa informação não vai para o xml
FROM 
    stage_legado_1grau.processo p
WHERE 
     p.nr_processo = ANY(:numeros_processos)