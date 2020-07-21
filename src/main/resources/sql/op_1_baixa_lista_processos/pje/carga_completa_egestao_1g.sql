select 
    numero_unico as nr_processo
from (
    select 
        pj.id_processo, pj.numero_unico,
        fn_get_classe_data(pj.id_processo, to_date('31/07/2016','DD/MM/YYYY')) as num_classe_interno,
        fn_get_fase_processo(pj.id_processo, to_date('31/07/2016','DD/MM/YYYY')) fase
    from tb_processos_judiciais pj 
) mx
where 1=1 --REGRAS DE NEGOCIO:
    and (
        mx.fase <> 'ARQ' --não está arquivado no dia referência
        or id_processo in (-- ou foi arquivado entre 2015 e 30/07/2016.
            select hf.id_processo
            from tb_processos_hist_fases hf
            where hf.cd_fase in ('ARQ')
            and hf.dta_ocorrencia between 
                to_timestamp('01/01/2015 00:00:00','DD/MM/YYYY HH24:MI:SS') 
                and to_timestamp('30/07/2016 23:59:59','DD/MM/YYYY HH24:MI:SS')
        )
    )
    -- Comentado para considerar qualquer classe, senão as cartas seriam inoradas.
--  and mx.num_classe_interno in ( --somente classes das três contidas nas 3 fases conforme e-gestão
--      select rci.cd_classe_judicial 
--      from tb_regras_classes_itens  rci
--      where rci.num_item_pai in (
--          24,  -- Fase de Conhecimento
--          316, -- Fase de Liquidação
--          71   -- Fase de Execução
--      )
--  )
--order by 1;