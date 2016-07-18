DO language plpgsql $$
declare 
  proc    record;
  parte   record;
  polo    record;
  assunto record;
  documento record;
  mov     record;
  compl   record;
  -- controls
  fl_assunto integer;
  -- manual input
  cd_municipio_ibge_trt varchar := '1100205';
  dt_inicio_periodo date := to_date('01-01-2015', 'dd-mm-yyyy');
  dt_fim_periodo date := to_date('01-07-2016', 'dd-mm-yyyy');
begin
  -- XML header
  raise notice '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>';

  -- processos
  raise notice '<cnj:processos xmlns="http://www.cnj.jus.br/intercomunicacao-2.2.2" xmlns:cnj="http://www.cnj.jus.br/replicacao-nacional">';
  
  for proc in 
  (
    SELECT 
      pt.id_processo_trf,
      pt.nr_sequencia,
      pt.nr_ano,
      pt.nr_origem_processo,

      /*** dadosBasicos ***/
      /* nivelSigilo */
      case when pt.in_segredo_justica = 'S' then 5 else 0 end as nivelSigilo, -- avaliar melhor
      /* numero */
      regexp_replace(p.nr_processo, '[\.\-]', '', 'g') nr_processo,
      /* classeProcessual */
      cj.cd_classe_judicial,
      /* codigoLocalidade */
      cd_municipio_ibge_trt id_municipio_ibge_origem,
      /* dataAjuizamento */
      to_char(pt.dt_autuacao, 'yyyymmddhh24miss') dt_autuacao,

      /*** orgaoJulgador ***/
      /* codigoOrgao */
      case when ps.id_processo_trf is null then serv_ojc.cod_serventia else serv_oj.cod_serventia end ds_sigla,
      /* nomeOrgao */
      case when ps.id_processo_trf is null then serv_ojc.nom_org_julg else serv_oj.nom_org_julg end ds_orgao_julgador,
      /* instancia */
      case when pt.nr_instancia = '2' then 'ORIG' else 'REV' end tp_instancia, 
      /* codigoMunicipioIBGE */
      cd_municipio_ibge_trt id_municipio_ibge_atual
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
    WHERE 1=1
      and p.nr_processo is not null
    -- limit 30 -- TEST ONLY!!
  )
  LOOP
    -- processo
    raise notice '<cnj:processo>';
    
    -- dadosBasicos
    raise notice '<dadosBasicos nivelSigilo="%" numero="%" classeProcessual="%" codigoLocalidade="%" dataAjuizamento="%">' 
      , proc.nivelSigilo, proc.nr_processo, proc.cd_classe_judicial, proc.id_municipio_ibge_origem, proc.dt_autuacao;

    FOR polo IN 
    (
      select distinct 
        pp.in_participacao,
        pp.id_tipo_parte,
        
        /*** polo ***/
        /* polo */
        case when pp.in_participacao = 'A' then 'AT' else case when pp.in_participacao = 'P' then 'PA' else 'TJ' end end in_polo_participacao
      from tb_processo_parte pp 
      WHERE 1=1
        and pp.in_situacao='A'
        and pp.id_tipo_parte <> 7 -- advogado
        and pp.id_processo_trf = proc.id_processo_trf
    )
    LOOP 
      -- polo
      raise notice '<polo polo="%">', polo.in_polo_participacao;

      /*
        IMPORTANTE: não estão sendo tratados os casos em que a parte é considerada um interesse público abstrato cuja defesa está a cargo do Ministério Público ou da Defensoria Pública
        IMPORTANTE: não estão sendo tratados os casos de representação e assistência dos pais, representação ou substituição processual em ações coletivas, tutela e curatela
        IMPORTANTE: não está sendo preenchido o elemento opcional 'advogado'
      */

      -- parte
      FOR parte IN 
      (
        select 
          pp.id_pessoa,
          
          /*** pessoa ***/
          /* nome */
          ul.ds_nome, 
          /* tipoPessoa */
          case when pes.in_tipo_pessoa = 'J' then 'juridica' else 'fisica' end in_tipo_pessoa, 
          /* sexo */
          coalesce(pf.in_sexo, 'D') tp_sexo, 
          /* numeroDocumentoPrincipal */
          (
            select lpad(regexp_replace(nr_documento_identificacao, '[_\.\-/]','','g'), 11 + case when cd_tp_documento_identificacao = 'CPJ' then 3 end, '0') as nr_doc  
            from tb_pess_doc_identificacao 
            where 1=1 
              and id_pessoa = pp.id_pessoa 
              and cd_tp_documento_identificacao IN ('CPF', 'CPJ', 'RIC')
            limit 1
          ) as nr_documento 
        from tb_processo_parte pp
        inner join tb_usuario_login ul on 1=1
          and pp.id_pessoa = ul.id_usuario 
        left join tb_pessoa pes on 1=1
          and pp.id_pessoa = pes.id_pessoa 
        left join tb_pessoa_fisica pf on 1=1
          and pf.id_pessoa_fisica = pes.id_pessoa
        WHERE 1=1
          -- and pp.in_segredo='N'
          and pp.in_situacao='A'
          and pp.id_tipo_parte <> 7 -- advogado
          and pp.in_participacao = polo.in_participacao
          and pp.id_processo_trf = proc.id_processo_trf
      )
      LOOP 

        raise notice '<parte>';

          -- pessoa
          IF parte.nr_documento IS NOT NULL THEN
            raise notice '<pessoa nome="%" tipoPessoa="%" sexo="%" numeroDocumentoPrincipal="%">'
            , parte.ds_nome, parte.in_tipo_pessoa, parte.tp_sexo, parte.nr_documento;
          ELSE 
            raise notice '<pessoa nome="%" tipoPessoa="%" sexo="%">'
            , parte.ds_nome, parte.in_tipo_pessoa, parte.tp_sexo;
          END IF;
          

          FOR documento IN 
          (
            select 
              /*** documento ***/
              /* codigoDocumento */
              regexp_replace(pdi.nr_documento_identificacao, '[_\.\-/]','','g') nr_documento,
              /* emissorDocumento */
              coalesce(ds_orgao_expedidor, 'Não informado') ds_emissor,
              /* tipoDocumento */
              tpd.tp_doc_selo tp_documento
            from tb_pess_doc_identificacao pdi
            inner join 
            (
              select 'CNH' tp_doc_pje, 'CNH' tp_doc_selo union 
              select 'CN' tp_doc_pje, 'CN' tp_doc_selo union 
              select 'PAS' tp_doc_pje, 'PAS' tp_doc_selo union 
              select 'RIC' tp_doc_pje, 'RIC' tp_doc_selo union 
              select 'CEI' tp_doc_pje, 'CEI' tp_doc_selo union 
              select 'NIT' tp_doc_pje, 'NIT' tp_doc_selo union 
              select 'OAB' tp_doc_pje, 'OAB' tp_doc_selo union 
              select 'RJC' tp_doc_pje, 'RJC' tp_doc_selo union 
              select 'RNE' tp_doc_pje, 'RGE' tp_doc_selo union 
              select 'RG' tp_doc_pje, 'CI' tp_doc_selo union 
              select 'TIT' tp_doc_pje, 'TE' tp_doc_selo union 
              select 'CTP' tp_doc_pje, 'CT' tp_doc_selo union 
              select 'PIS' tp_doc_pje, 'PIS_PASEP' tp_doc_selo union 
              select 'CEJ' tp_doc_pje, 'CEI' tp_doc_selo union 
              select 'CRP' tp_doc_pje, 'CP' tp_doc_selo 
            ) tpd on 1=1
              and tpd.tp_doc_pje = pdi.cd_tp_documento_identificacao
            where 1=1
              and pdi.id_pessoa = parte.id_pessoa
          )
          LOOP
            
            raise notice '<documento codigoDocumento="%" tipoDocumento="%" emissorDocumento="%" />'
              , documento.nr_documento, documento.tp_documento, documento.ds_emissor;
              
          END LOOP;

          raise notice '</pessoa>';
        raise notice '</parte>';
      END LOOP;

      raise notice '</polo>';
    END LOOP;
    
    -- assunto
    fl_assunto := 0;
    FOR assunto IN 
    (
      select 
        case when pa.in_assunto_principal = 'S' then 'true' else 'false' end in_assunto_principal,
        ss.cd_assunto_trf
      from tb_processo_assunto pa
      inner join tb_assunto_trf ss on 1=1
        and pa.id_assunto_trf = ss.id_assunto_trf
      where 1=1
        and pa.id_processo_trf = proc.id_processo_trf
        -- in (select id_processo from tb_processo where nr_processo='0000001-29.2015.5.14.0071') -- test only
    )
    LOOP
      fl_assunto := 1;

      raise notice '<assunto>'; -- principal="%">', assunto.in_assunto_principal;
        raise notice '<codigoNacional>%</codigoNacional>', assunto.cd_assunto_trf;
      raise notice '</assunto>';
    END LOOP;

    -- se não tiver assunto? 
    IF fl_assunto = 0 THEN 
      -- do something...
      raise notice '<assunto>'; 
        raise notice '<codigoNacional>2546</codigoNacional>'; -- Verbas Rescisórias
      raise notice '</assunto>';
    END IF;

    -- orgaoJulgador
    raise notice '<orgaoJulgador codigoOrgao="%" nomeOrgao="%" instancia="%" codigoMunicipioIBGE="%"/>' -- codigoMunicipioIBGE="1100205" -- <=== 2º grau!!!
      , proc.ds_sigla, proc.ds_orgao_julgador, proc.tp_instancia, proc.id_municipio_ibge_atual;
    
    raise notice '</dadosBasicos>';

    for mov in 
    (
      SELECT 
        pe.id_processo_evento, 
        
        /*** movimento ***/
        /* dataHora */
        to_char(pe.dt_atualizacao, 'yyyymmddhh24miss') AS dta_ocorrencia,
        /* nivelSigilo */
        case when pe.in_visibilidade_externa = true then 0 else 5 end AS in_visibilidade_externa,
        /* identificadorMovimento */
        -- pe.id_processo_evento,
        
        /*** movimentoNacional ***/
        /* codigoNacional */
        pe.id_evento AS cd_movimento_cnj
      FROM tb_processo_evento pe 
      INNER JOIN tb_evento ev ON 1=1
        AND ev.id_evento = pe.id_evento
        AND ev.in_ativo  = 'S' 
      WHERE 1=1
        AND pe.id_processo_evento_excludente IS NULL
        AND pe.id_processo = proc.id_processo_trf
      ORDER BY pe.dt_atualizacao ASC
    )
    loop

      raise notice '<movimento dataHora="%" nivelSigilo="%">', mov.dta_ocorrencia, mov.in_visibilidade_externa;
        raise notice '<movimentoNacional codigoNacional="%">', mov.cd_movimento_cnj;

        for compl in 
        (
          select 
            tc.cd_tipo_complemento, 
            tc.ds_nome,
            cs.ds_texto AS cd_complemento,
            regexp_replace(cs.ds_valor_complemento, '[\r\n]', '') AS nm_complemento
          from tb_complemento_segmentado cs
          inner join client.tb_tipo_complemento tc ON 1=1
            AND tc.id_tipo_complemento = cs.id_tipo_complemento
          where 1=1
            and cs.id_movimento_processo = mov.id_processo_evento
            and tc.cd_tipo_complemento is not null
            and tc.ds_nome is not null
            and
            (0=1
              or '' <> trim(cs.ds_texto) 
              or cs.ds_valor_complemento is not null
            )
        )
        loop

          IF '' = trim(compl.cd_complemento) THEN
            raise notice '<complemento>%:%:%</complemento>'
              , compl.cd_tipo_complemento, compl.ds_nome, compl.nm_complemento;
          ELSE
            raise notice '<complemento>%:%:%:%</complemento>'
              , compl.cd_tipo_complemento, compl.ds_nome, compl.cd_complemento, compl.nm_complemento;
          END IF;

        end loop;


        raise notice '</movimentoNacional>';
        -- <idDocumentoVinculado />
      raise notice '</movimento>';

    end loop;

    raise notice '</cnj:processo>';
  end loop;

  raise notice '</cnj:processos>';
end
$$;



