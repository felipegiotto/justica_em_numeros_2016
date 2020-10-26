select 
    numero_unico as nr_processo,
    num_classe_interno as cd_classe_judicial,
	ioj.id_orgao_julgador as cd_orgao_julgador
from (
    select 
        pj.id_processo, pj.numero_unico,
        fn_get_classe_orig_ou_rec_data(pj.id_processo, ?) as num_classe_interno,
        fn_get_proc_status_data(pj.id_processo, ?) as status
    from tb_processos_judiciais pj
) mx,
tb_processos_hist_distrib phd, 
tb_info_orgao_julgador ioj
where 1=1 --REGRAS DE NEGOCIO:
and (
    mx.status not in ('B','ARQ') --não está arquivado ou baixado no dia referência
    or mx.id_processo in (-- ou foi arquivado entre 2015 e o último dia do mês informado no parâmetro mes_ano_corte.
        select 
            phe.id_processo
        from tb_processos_hist_estado phe
        where
            phe.in_evento in ('B','ARQ') --Itens 92220 e 92221
            and phe.dta_ocorrencia between 
                to_timestamp('01/01/2015 00:00:00','DD/MM/YYYY HH24:MI:SS') 
                and ?::timestamp
    )
)
and (
    fn_valida_classe_item (92220, cast(mx.num_classe_interno as integer))
    or fn_valida_classe_item (92221, cast(mx.num_classe_interno as integer))
)
and phd.id_orgao_julgador = ioj.id_orgao_julgador
and mx.id_processo = phd.id_processo
and dta_distribuicao = (select max(phd2.dta_distribuicao) from tb_processos_hist_distrib phd2 where phd.id_processo = phd2.id_processo)
--order by 1;
