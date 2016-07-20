            select 
              /*** documento ***/
              /* codigoDocumento */
              regexp_replace(pdi.nr_documento_identificacao, '[_\.\-/]','','g') nr_documento,
              /* emissorDocumento */
              coalesce(ds_orgao_expedidor, 'Não informado') ds_emissor,
              /* tipoDocumento */
              --tpd.tp_doc_selo tp_documento,
              pdi.cd_tp_documento_identificacao,
              
              pdi.ds_nome_pessoa
            from tb_pess_doc_identificacao pdi
            /*
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
              */
            where 1=1
              and pdi.id_pessoa = :id_pessoa