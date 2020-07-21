select
    proc.nr_processo,
    pa.in_assunto_principal,
    ss.cd_assunto_trf
from tb_processo_assunto pa
inner join tb_assunto_trf ss on (pa.id_assunto_trf = ss.id_assunto_trf)
inner join tb_processo proc on (pa.id_processo_trf = proc.id_processo)
inner join tb_processo_trf ptrf on (proc.id_processo = ptrf.id_processo_trf)
where proc.nr_processo = ANY(:numeros_processos)

-- Reforça a condição da data de autuação, pois um bug no PJe fez com que houvesse mais que um processo com mesma numeração (um com data de autuação e outro sem)
and ptrf.dt_autuacao IS NOT NULL

ORDER BY ss.cd_assunto_trf