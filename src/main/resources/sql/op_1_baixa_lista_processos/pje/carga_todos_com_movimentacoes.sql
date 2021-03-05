SELECT nr_processo
FROM tb_processo p
INNER JOIN tb_processo_trf ptrf ON (p.id_processo = ptrf.id_processo_trf)
WHERE 1=1
  AND ptrf.dt_autuacao IS NOT NULL
  AND nr_processo IS NOT NULL
  AND EXISTS (
    SELECT 1 
    FROM tb_processo_evento pe 
    WHERE (p.id_processo = pe.id_processo) 
        AND (pe.dt_atualizacao BETWEEN '2015-01-01 00:00:00.000' AND ?::timestamp)
        AND (pe.id_processo_evento_excludente IS NULL)
    LIMIT 1
)