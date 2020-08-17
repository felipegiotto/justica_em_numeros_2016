select
    p.id_pessoa 								as id_pessoa,
    p.nr_documento 								as nr_documento,	
    cast('' as character varying) 	as ds_emissor,    
	(
		case 
			when p.nr_documento is not null then p.cd_tp_documento_identificacao
			else null
		end 
	)											as cd_tp_documento_identificacao,
    p.ds_nome 									as ds_nome_pessoa,
    cast('s' as character(1)) 					as in_principal
from
    stage_legado_1grau.pessoa p
where 
	p.id_pessoa = any(:ids_pessoas) 	
