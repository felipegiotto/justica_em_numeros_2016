SELECT hist.id_oj_origem, 
  upper(to_ascii(oj_origem.ds_orgao_julgador)) as ds_oj_origem, 
  hist.id_oj_destino,
  upper(to_ascii(oj_destino.ds_orgao_julgador)) as ds_oj_destino, 
  hist.dt_deslocamento, 
  hist.dt_retorno,
  ib_origem.id_municipio_ibge as id_municipio_origem,
  ib_destino.id_municipio_ibge as id_municipio_destino
  
FROM tb_hist_desloca_oj hist
INNER JOIN tb_orgao_julgador oj_origem ON (oj_origem.id_orgao_julgador = hist.id_oj_origem)
INNER JOIN tb_orgao_julgador oj_destino ON (oj_destino.id_orgao_julgador = hist.id_oj_destino)

LEFT  JOIN tb_jurisdicao_municipio jm_origem ON (jm_origem.id_jurisdicao = oj_origem.id_jurisdicao AND jm_origem.in_sede = 'S')
LEFT  JOIN tb_municipio_ibge ib_origem ON (ib_origem.id_municipio = jm_origem.id_municipio)
LEFT  JOIN tb_jurisdicao_municipio jm_destino ON (jm_destino.id_jurisdicao = oj_destino.id_jurisdicao AND jm_destino.in_sede = 'S')
LEFT  JOIN tb_municipio_ibge ib_destino ON (ib_destino.id_municipio = jm_destino.id_municipio)

WHERE hist.id_processo_trf = :id_processo
  AND hist.dt_deslocamento IS NOT NULL
  AND hist.dt_retorno IS NOT NULL
ORDER BY hist.dt_deslocamento