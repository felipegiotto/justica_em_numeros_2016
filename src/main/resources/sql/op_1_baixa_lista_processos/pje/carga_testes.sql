SELECT p.nr_processo,
cj.cd_classe_judicial as cd_classe_judicial,
oj.id_orgao_julgador as cd_orgao_julgador
FROM tb_processo p
INNER JOIN tb_processo_trf ptrf ON (p.id_processo = ptrf.id_processo_trf)
LEFT  JOIN tb_classe_judicial cj ON (cj.id_classe_judicial = ptrf.id_classe_judicial)
LEFT  JOIN tb_orgao_julgador oj ON (oj.id_orgao_julgador = ptrf.id_orgao_julgador)
WHERE p.nr_ano = 2016
ORDER BY random()
LIMIT 100
