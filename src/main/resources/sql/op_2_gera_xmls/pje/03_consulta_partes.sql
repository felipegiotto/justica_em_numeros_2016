select 
  proc.nr_processo, pp.in_participacao,
  pp.id_pessoa,
  
  /*** pessoa ***/
  /* nome */
  ul.ds_nome,
  ul.ds_nome_consulta,
  /* tipoPessoa */
  pes.in_tipo_pessoa,
  /* sexo */
  pf.in_sexo,
  
  pp.id_processo_parte,
  ppr.id_parte_representante,
  tprep.ds_tipo_parte ds_tipo_parte_representante,
  
  pf.dt_nascimento, pf.dt_obito, pf.nm_genitora, pf.nm_genitor
from tb_processo_parte pp
inner join tb_processo proc on (pp.id_processo_trf = proc.id_processo)
inner join tb_processo_trf ptrf on (proc.id_processo = ptrf.id_processo_trf)
inner join tb_usuario_login ul on (pp.id_pessoa = ul.id_usuario) 
left join tb_pessoa pes on (pp.id_pessoa = pes.id_pessoa) 
left join tb_pessoa_fisica pf on (pf.id_pessoa_fisica = pes.id_pessoa)

LEFT JOIN tb_proc_parte_represntante ppr ON (pp.id_processo_parte = ppr.id_processo_parte AND ppr.in_situacao = 'A')
LEFT JOIN tb_tipo_parte tprep ON (tprep.id_tipo_parte = ppr.id_tipo_representante)

WHERE 1=1
  and pp.in_situacao='A'
  and proc.nr_processo = ANY(:numeros_processos)
  
  -- Reforça a condição da data de autuação, pois um bug no PJe fez com que houvesse mais que um processo com mesma numeração (um com data de autuação e outro sem)
  and ptrf.dt_autuacao IS NOT NULL
  
ORDER BY pp.in_participacao