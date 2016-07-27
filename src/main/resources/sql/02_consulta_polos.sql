select distinct 
    pp.in_participacao,
    pp.id_tipo_parte,
    
    /*** polo ***/
    /* polo */
    case when pp.in_participacao = 'A' then 'AT' else case when pp.in_participacao = 'P' then 'PA' else 'TJ' end end in_polo_participacao
from tb_processo_parte pp 
WHERE 1=1
    and pp.in_situacao='A'
    and pp.id_tipo_parte <> 7 -- advogado
    and pp.id_processo_trf = :id_processo
