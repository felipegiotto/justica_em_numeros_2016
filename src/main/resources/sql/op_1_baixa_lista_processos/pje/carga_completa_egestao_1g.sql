select 
    numero_unico as nr_processo,
	num_classe_interno as cd_classe_judicial,
	ioj.id_orgao_julgador as cd_orgao_julgador
from (
    select 
        pj.id_processo, pj.numero_unico,
        fn_get_classe_data(pj.id_processo, ?) as num_classe_interno,
        fn_get_fase_processo(pj.id_processo, ?) fase
    from tb_processos_judiciais pj 
) mx, 
tb_processos_hist_distrib phd, 
tb_info_orgao_julgador ioj
where 1=1 --REGRAS DE NEGOCIO:
    and (
        mx.fase <> 'ARQ' --não está arquivado no dia referência
        or mx.id_processo in (-- ou foi arquivado entre 2015 e o último dia do mês informado no parâmetro mes_ano_corte.
            select hf.id_processo
            from tb_processos_hist_fases hf
            where hf.cd_fase in ('ARQ')
            and hf.dta_ocorrencia between 
                to_timestamp('01/01/2015 00:00:00','DD/MM/YYYY HH24:MI:SS') 
                and ?::timestamp
        )
    )
and phd.id_orgao_julgador = ioj.id_orgao_julgador
and mx.id_processo = phd.id_processo
and dta_distribuicao = (select max(phd2.dta_distribuicao) from tb_processos_hist_distrib phd2 where phd.id_processo = phd2.id_processo)
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