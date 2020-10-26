SELECT p.nr_processo,
cj.cd_classe_judicial as cd_classe_judicial,
oj.id_orgao_julgador as cd_orgao_julgador
FROM tb_processo p
INNER JOIN tb_processo_trf ptrf ON (p.id_processo = ptrf.id_processo_trf)
LEFT  JOIN tb_classe_judicial cj ON (cj.id_classe_judicial = ptrf.id_classe_judicial)
LEFT  JOIN tb_orgao_julgador oj ON (oj.id_orgao_julgador = ptrf.id_orgao_julgador)
WHERE 1=1
  AND ptrf.dt_autuacao IS NOT NULL
  AND p.nr_processo IS NOT NULL
  AND EXISTS (
    SELECT 1 
    FROM tb_processo_evento pe 
    WHERE (p.id_processo = pe.id_processo) 
        AND (pe.dt_atualizacao BETWEEN ?::timestamp AND ?::timestamp)
        AND (pe.id_processo_evento_excludente IS NULL)
    LIMIT 1
)
