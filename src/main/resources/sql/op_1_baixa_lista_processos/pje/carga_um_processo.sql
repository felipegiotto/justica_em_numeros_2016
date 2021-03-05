SELECT p.nr_processo
FROM tb_processo p
INNER JOIN tb_processo_trf ptrf ON (p.id_processo = ptrf.id_processo_trf)
WHERE p.nr_processo = ?
