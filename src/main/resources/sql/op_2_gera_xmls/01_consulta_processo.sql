-- Trecho de SQL adaptado do script do TRT14: trt14_original__pje2grau_script.sql
SELECT 
      p.nr_processo,
      p.id_processo,
      pt.nr_sequencia,
      pt.nr_ano,
      pt.nr_origem_processo,

      /*** dadosBasicos ***/
  /* nivelSigilo */
  pt.in_segredo_justica,
  --case when pt.in_segredo_justica = 'S' then 5 else 0 end as nivelSigilo,
  /* classeProcessual */
  cj.cd_classe_judicial,
  cj.ds_classe_judicial,
  /* codigoLocalidade */
  ib.id_municipio_ibge,
  /* dataAjuizamento */
  pt.dt_autuacao,
  
  /*** orgaoJulgador ***/
  upper(to_ascii(oj.ds_orgao_julgador)) as ds_orgao_julgador, 
  upper(to_ascii(ojc.ds_orgao_julgador_colegiado)) as ds_orgao_julgador_colegiado,
  -- Sugestao TRT6, por causa de falha no PostgreSQL na conversão do caractere "º" para ASCII:
  -- upper(to_ascii(replace(oj.ds_orgao_julgador, 'º', 'O'))) as ds_orgao_julgador, upper(to_ascii(replace(ojc.ds_orgao_julgador_colegiado, 'º', 'O'))) as ds_orgao_julgador_colegiado,
  -- Fonte: e-mail com assunto "Sugestões de alterações justica_em_numeros_2016" do TRT6
  -- Fonte: https://www.postgresql.org/message-id/20040607212810.15543.qmail@web13125.mail.yahoo.com
  
  
  -- instancia
  pt.nr_instancia, 
 
  /* TRT4 */
  pt.vl_causa,
  pt.id_proc_referencia,
  pref.nr_processo as nr_processo_ref,
  cjref.cd_classe_judicial as cd_classe_judicial_ref,
  cjref.ds_classe_judicial as ds_classe_judicial_ref
FROM tb_processo_trf pt
INNER JOIN tb_processo p ON (p.id_processo = pt.id_processo_trf)
LEFT  JOIN tb_classe_judicial cj ON (cj.id_classe_judicial = pt.id_classe_judicial)
LEFT  JOIN tb_orgao_julgador oj ON (oj.id_orgao_julgador = pt.id_orgao_julgador)
LEFT  JOIN tb_orgao_julgador_colgiado ojc ON (ojc.id_orgao_julgador_colegiado = pt.id_orgao_julgador_colegiado)
LEFT  JOIN tb_jurisdicao_municipio jm ON (jm.id_jurisdicao = oj.id_jurisdicao AND jm.in_sede = 'S')
LEFT  JOIN tb_municipio_ibge ib ON (ib.id_municipio = jm.id_municipio)

LEFT JOIN tb_processo pref ON (pt.id_proc_referencia = pref.id_processo)
LEFT JOIN tb_processo_trf ptref ON (ptref.id_processo_trf = pref.id_processo)
LEFT  JOIN tb_classe_judicial cjref ON (cjref.id_classe_judicial = ptref.id_classe_judicial)


WHERE p.nr_processo = ANY(:numeros_processos)
