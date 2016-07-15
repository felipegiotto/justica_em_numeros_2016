-- Trecho de SQL copiado do script do TRT14: pje2grau_script.sql

SELECT 
      p.nr_processo as numero_completo_processo,
      pt.id_processo_trf,
      pt.nr_sequencia,
      pt.nr_ano,
      pt.nr_origem_processo,

      /*** dadosBasicos ***/
      /* nivelSigilo */
      case when pt.in_segredo_justica = 'S' then 5 else 0 end as nivelSigilo, -- TODO: avaliar melhor
      /* numero */
      regexp_replace(p.nr_processo, '[\.\-]', '', 'g') nr_processo, -- TODO: Revisar o motivo do número do processo estar sem os pontos e traços
      /* classeProcessual */
      cj.cd_classe_judicial,
      /* codigoLocalidade */
      :cd_municipio_ibge_trt id_municipio_ibge_origem,
      /* dataAjuizamento */
      to_char(pt.dt_autuacao, 'yyyymmddhh24miss') dt_autuacao,  -- TODO: Definir formato para mostrar a data/hora da autuação
/*
      / *** orgaoJulgador *** /
      -- codigoOrgao 
      case when ps.id_processo_trf is null then serv_ojc.cod_serventia else serv_oj.cod_serventia end ds_sigla,
      -- nomeOrgao
      case when ps.id_processo_trf is null then serv_ojc.nom_org_julg else serv_oj.nom_org_julg end ds_orgao_julgador,
      -- instancia   */
      case when pt.nr_instancia = '2' then 'ORIG' else 'REV' end tp_instancia, 
      /* codigoMunicipioIBGE */
      :cd_municipio_ibge_trt id_municipio_ibge_atual 
    FROM tb_processo_trf pt
    INNER JOIN tb_processo p ON 1=1
     and p.id_processo = pt.id_processo_trf
    INNER JOIN tb_classe_judicial cj ON 1=1
      and cj.id_classe_judicial = pt.id_classe_judicial
    /*
    INNER JOIN tb_jurisdicao_municipio jm ON 1=1
      and jm.id_jurisdicao = oj.id_jurisdicao
      AND jm.in_sede        = 'S'
    INNER JOIN tb_municipio_ibge ib ON 1=1
      and ib.id_municipio = jm.id_municipio
    */
      /*
    LEFT JOIN tb_orgao_julgador oj ON 1=1
      and oj.id_orgao_julgador = pt.id_orgao_julgador
    LEFT JOIN tb_orgao_julgador_colgiado ojc ON 1=1
      and ojc.id_orgao_julgador_colegiado = pt.id_orgao_julgador_colegiado
    LEFT JOIN 
    (
      -- exemplo de preenchimento: 
      -- <int> cod_serventia: o código da serventia no CNJ
      -- <int> num_org_julg: o código do órgão julgador no PJe
      -- <string> nom_org_julg: nome da serventia no CNJ
      select 30405 cod_serventia, 48 num_org_julg, 'GABINETE DO DESEMBARGADOR DO TRABALHO CARLOS AUGUSTO GOMES LÔBO' nom_org_julg
    ) serv_oj on 1=1
      and serv_oj.num_org_julg  = oj.id_orgao_julgador
    LEFT JOIN
    (
      -- exemplo de preenchimento: 
      -- <int> cod_serventia: o código da serventia no CNJ
      -- <int> num_org_julg: o código do órgão julgador no PJe
      -- <string> nom_org_julg: nome da serventia no CNJ 
    ) serv_ojc on 1=1
      and serv_ojc.num_org_julg = ojc.id_orgao_julgador_colegiado
    INNER JOIN 
    (
      -- item 92.220
      select distinct pr.id_processo_trf
      from tb_processo_trf pr 
      inner join tb_processo_evento ad on 1=1
        and ad.id_processo = pr.id_processo_trf
        and ad.id_processo_evento_excludente IS NULL
      where 1=1
        and ad.dt_atualizacao >= dt_inicio_periodo
        and ad.dt_atualizacao <  dt_fim_periodo 
    ) pc on 1=1
      and pc.id_processo_trf = pt.id_processo_trf
    LEFT JOIN 
    (
      select ps.id_processo_trf 
      from tb_pauta_sessao ps
      inner join tb_tipo_situacao_pauta sp on 1=1
        and sp.in_classificacao = 'A'
        and sp.in_ativo = 'S'
        and sp.id_tipo_situacao_pauta = ps.id_tipo_situacao_pauta
      where 1=1
        -- and ps.id_processo_trf = 2001
    ) ps on 1=1
      and ps.id_processo_trf = pt.id_processo_trf 
      */
    WHERE 1=1
      and length(p.nr_processo) = 25