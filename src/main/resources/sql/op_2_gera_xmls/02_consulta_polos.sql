select distinct 
    pp.in_participacao
    
    /*** polo ***/
    /* polo */
    --case when pp.in_participacao = 'A' then 'AT' else case when pp.in_participacao = 'P' then 'PA' else 'TJ' end end in_polo_participacao
from tb_processo_parte pp 
WHERE 1=1
    and pp.in_situacao='A'
    --and pp.id_tipo_parte not in (7,3,10,47,78,189,200,210,211) -- advogado e demais repesentantes
    and pp.id_processo_trf = :id_processo
ORDER BY pp.in_participacao