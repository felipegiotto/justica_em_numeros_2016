      select 
        case when pa.in_assunto_principal = 'S' then 'true' else 'false' end in_assunto_principal,
        ss.cd_assunto_trf
      from tb_processo_assunto pa
      inner join tb_assunto_trf ss on 1=1
        and pa.id_assunto_trf = ss.id_assunto_trf
      where 1=1
        and pa.id_processo_trf = :id_processo