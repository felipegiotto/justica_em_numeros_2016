select 
    numero_unico as nr_processo
from (
    select 
        pj.id_processo, pj.numero_unico,
        fn_get_classe_orig_ou_rec_data(pj.id_processo, to_date('31/07/2020','DD/MM/YYYY')) as num_classe_interno,
        fn_get_proc_status_data(pj.id_processo, to_date('31/07/2020','DD/MM/YYYY')) as status
    from tb_processos_judiciais pj
) mx
where 1=1 --REGRAS DE NEGOCIO:
    and (
        mx.status not in ('B','ARQ') --não está arquivado ou baixado no dia referência
        or id_processo in (-- ou foi arquivado entre 2015 e 30/06/2020.
            select 
                phe.id_processo
            from tb_processos_hist_estado phe
            where
                phe.in_evento in ('B','ARQ') --Itens 92220 e 92221
                and phe.dta_ocorrencia between 
                    to_timestamp('01/01/2015 00:00:00','DD/MM/YYYY HH24:MI:SS') 
                    and to_timestamp('31/07/2020 23:59:59','DD/MM/YYYY HH24:MI:SS')
        )
    )
    and (
        fn_valida_classe_item (92220, cast(mx.num_classe_interno as integer))
        or fn_valida_classe_item (92221, cast(mx.num_classe_interno as integer))
    )
--order by 1;
