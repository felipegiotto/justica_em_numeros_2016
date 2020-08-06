SELECT
    p.id_pessoa,
    p.nr_documento,	
    'Não informado' ds_emissor,    
	p.cd_tp_documento_identificacao,
    p.ds_nome,
    'S' in_principal
FROM
    legado_1grau.pessoa p
where 
	p.id_pessoa = ANY(:ids_pessoas) 	
