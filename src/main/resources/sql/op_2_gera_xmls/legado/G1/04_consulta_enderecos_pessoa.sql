select
	p.nm_logradouro 						as nm_logradouro,
	cast (null as character varying(15)) 	as nr_endereco,
	p.ds_complemento 						as ds_complemento,
	p.nm_bairro 							as nm_bairro,
	p.ds_municipio 							as ds_municipio,
	p.cd_estado 							as cd_estado,
	p.nr_cep 								as nr_cep,
	cast(null as character varying(7)) 		as id_municipio_ibge,
	pp.id_processo_parte 					as id_processo_parte
from 
	stage_legado_1grau.pessoa p, stage_legado_1grau.processo_parte pp
where
	1=1
	and p.cd_pessoa = pp.cd_pessoa 
	and pp.id_processo_parte = any(:id_processo_parte)