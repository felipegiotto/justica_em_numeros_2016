SELECT
    p.id_pessoa,
    p.nr_documento,	
    'Não informado' ds_emissor,    
	case 
		when p.nr_documento is not null then p.cd_tp_documento_identificacao
		else null
	end as cd_tp_documento_identificacao,
    p.ds_nome AS ds_nome_pessoa,
    'S' in_principal
FROM
    stage_legado_1grau.pessoa p
where 
	p.id_pessoa = ANY(:ids_pessoas) 	
