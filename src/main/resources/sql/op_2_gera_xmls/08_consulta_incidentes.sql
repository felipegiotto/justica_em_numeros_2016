SELECT proc.nr_processo, classe.cd_classe_judicial
FROM tb_processo proc
INNER JOIN tb_processo_trf ptrf ON (proc.id_processo = ptrf.id_processo_trf)
LEFT JOIN tb_classe_judicial classe ON (classe.id_classe_judicial = ptrf.id_classe_judicial)
WHERE ptrf.id_proc_referencia = :id_proc_referencia
ORDER BY ptrf.id_processo_trf