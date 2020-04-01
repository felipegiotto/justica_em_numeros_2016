select
  pdi.id_pessoa,
  
  /*** documento ***/
  /* codigoDocumento */
  pdi.nr_documento_identificacao nr_documento,
  /* emissorDocumento */
  coalesce(ds_orgao_expedidor, 'Não informado') ds_emissor,
  /* tipoDocumento */
  pdi.cd_tp_documento_identificacao,
  
  pdi.ds_nome_pessoa,
  pdi.in_principal
from tb_pess_doc_identificacao pdi
where pdi.id_pessoa = ANY(:ids_pessoas)
  AND pdi.in_ativo = 'S'