SELECT
    p.nr_processo_ref as nr_processo_referencia,
    p.nr_processo as nr_processo,
    p.cd_processo as  id_processo, 
    p.cd_classe_judicial as cd_classe_processual,
    null as ds_classe_processual -- essa informação não vai para o xml
FROM 
    legado_1grau.processo p
WHERE 
     p.nr_processo = ANY(:numeros_processos)