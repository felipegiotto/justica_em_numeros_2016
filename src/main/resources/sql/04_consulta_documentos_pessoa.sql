            select 
              /*** documento ***/
              /* codigoDocumento */
              regexp_replace(pdi.nr_documento_identificacao, '[_\.\-/]','','g') nr_documento,
              /* emissorDocumento */
              coalesce(ds_orgao_expedidor, 'Não informado') ds_emissor,
              /* tipoDocumento */
              pdi.cd_tp_documento_identificacao,
              
              pdi.ds_nome_pessoa
            from tb_pess_doc_identificacao pdi

            where pdi.id_pessoa = :id_pessoa