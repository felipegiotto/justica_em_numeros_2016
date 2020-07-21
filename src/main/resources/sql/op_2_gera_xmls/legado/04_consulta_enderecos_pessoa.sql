SELECT
	'' nm_logradouro,
	'' nr_endereco,
	'' ds_complemento,
	'' as nm_bairro,
	'' ds_municipio,
	'' cd_estado,
	'' nr_cep,
	'' id_municipio_ibge,
	'' id_processo_parte
FROM 
	dual
WHERE
	0 < ANY(:id_processo_parte)