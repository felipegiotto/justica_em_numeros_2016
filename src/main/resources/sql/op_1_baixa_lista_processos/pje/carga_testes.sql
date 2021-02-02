SELECT nr_processo
FROM tb_processo p
INNER JOIN tb_processo_trf ptrf ON (p.id_processo = ptrf.id_processo_trf AND ptrf.dt_autuacao IS NOT NULL)
WHERE nr_ano = 2016
ORDER BY random()
LIMIT 100
