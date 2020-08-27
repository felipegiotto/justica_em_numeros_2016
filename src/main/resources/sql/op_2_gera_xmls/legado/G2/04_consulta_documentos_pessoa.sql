select
    p.id_pessoa 								as id_pessoa,
	p.ds_nome 									as ds_nome_pessoa,
    p.nr_documento 								as nr_documento,	
	(
		case 
			when p.nr_documento is not null then p.cd_tp_documento_identificacao
			else null
		end 
	)											as cd_tp_documento_identificacao,
	(
		case
			when p.cd_tp_documento_identificacao is not null and p.cd_tp_documento_identificacao in ('RG') then p.orgao_expedidor_rg
			else ''
		end 
	)											as ds_emissor,
	--Segundo o XSD, pode ser considerado com principal apenas CPF ou CNPJ
	(
		case
			when p.cd_tp_documento_identificacao is not null and p.cd_tp_documento_identificacao in ('RG') then 'N'
			else 'S'
		end 
	)											as in_principal
from
    stage_legado_2grau.pessoa p
where 
	1=1
	and p.id_pessoa = any(:ids_pessoas) 	
