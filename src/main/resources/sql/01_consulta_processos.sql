-- Trecho de SQL adaptado do script do TRT14: trt14_original__pje2grau_script.sql

SELECT 
      p.nr_processo as numero_completo_processo,
      pt.id_processo_trf,
      pt.nr_sequencia,
      pt.nr_ano,
      pt.nr_origem_processo,

      /*** dadosBasicos ***/
      /* nivelSigilo */
      case when pt.in_segredo_justica = 'S' then 5 else 0 end as nivelSigilo,
      /* numero */
      regexp_replace(p.nr_processo, '[\.\-]', '', 'g') nr_processo,
      /* classeProcessual */
      cj.cd_classe_judicial,
      /* codigoLocalidade */
      ib.id_municipio_ibge id_municipio_ibge_origem,
      /* dataAjuizamento */
      to_char(pt.dt_autuacao, 'yyyymmddhh24miss') dt_autuacao,
      
      /*** orgaoJulgador ***/
      upper(to_ascii(oj.ds_orgao_julgador)) as ds_orgao_julgador, upper(to_ascii(ojc.ds_orgao_julgador_colegiado)) as ds_orgao_julgador_colegiado,
      -- instancia
      pt.nr_instancia, 
      /* codigoMunicipioIBGE */
      ib.id_municipio_ibge id_municipio_ibge_atual,
      
      /* TRT4 */
      pt.vl_causa
    FROM tb_processo_trf pt
    INNER JOIN tb_processo p ON (p.id_processo = pt.id_processo_trf)
    INNER JOIN tb_classe_judicial cj ON (cj.id_classe_judicial = pt.id_classe_judicial)
    LEFT  JOIN tb_orgao_julgador oj ON (oj.id_orgao_julgador = pt.id_orgao_julgador)
    LEFT  JOIN tb_orgao_julgador_colgiado ojc ON (ojc.id_orgao_julgador_colegiado = pt.id_orgao_julgador_colegiado)
    INNER JOIN tb_jurisdicao_municipio jm ON (jm.id_jurisdicao = oj.id_jurisdicao AND jm.in_sede = 'S')
    INNER JOIN tb_municipio_ibge ib ON (ib.id_municipio = jm.id_municipio)
    INNER JOIN 
    (
      -- item 92.220
      select distinct pr.id_processo_trf
      from tb_processo_trf pr 
      inner join tb_processo_evento ad on 1=1
        and ad.id_processo = pr.id_processo_trf
        and ad.id_processo_evento_excludente IS NULL
      where :filtrar_por_movimentacoes = 1 
        and ad.dt_atualizacao >= :dt_inicio_periodo
        and ad.dt_atualizacao <  :dt_fim_periodo
      UNION
      select distinct pr.id_processo_trf
      from tb_processo_trf pr
      where :filtrar_por_movimentacoes = 0
    ) pc on 1=1
      and pc.id_processo_trf = pt.id_processo_trf
    WHERE 1=1
      and length(p.nr_processo) = 25