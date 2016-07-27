select 
  pp.id_pessoa,
  
  /*** pessoa ***/
  /* nome */
  ul.ds_nome, 
  /* tipoPessoa */
  pes.in_tipo_pessoa,
  /* sexo */
  coalesce(pf.in_sexo, 'D') tp_sexo,
  
  pp.id_processo_parte,
  ppr.id_parte_representante,
  tprep.ds_tipo_parte ds_tipo_parte_representante
from tb_processo_parte pp
inner join tb_usuario_login ul on (pp.id_pessoa = ul.id_usuario) 
left join tb_pessoa pes on (pp.id_pessoa = pes.id_pessoa) 
left join tb_pessoa_fisica pf on (pf.id_pessoa_fisica = pes.id_pessoa)

LEFT JOIN tb_proc_parte_represntante ppr ON (pp.id_processo_parte = ppr.id_processo_parte)
LEFT JOIN tb_tipo_parte tprep ON (tprep.id_tipo_parte = ppr.id_tipo_representante)

WHERE 1=1
  and pp.in_situacao='A'
  and pp.in_participacao = :in_participacao
  and pp.id_processo_trf = :id_processo
