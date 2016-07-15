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
          and pp.in_participacao = :in_participacao
          and pp.id_processo_trf = :id_processo
