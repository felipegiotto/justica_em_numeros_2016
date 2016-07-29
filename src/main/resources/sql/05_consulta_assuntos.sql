select 
    pa.in_assunto_principal,
    ss.cd_assunto_trf
from tb_processo_assunto pa
inner join tb_assunto_trf ss on (pa.id_assunto_trf = ss.id_assunto_trf)
inner join tb_processo proc on (pa.id_processo_trf = proc.id_processo)
where proc.nr_processo = :nr_processo
ORDER BY ss.cd_assunto_trf