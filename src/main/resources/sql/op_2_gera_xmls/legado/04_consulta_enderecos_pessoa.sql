SELECT
	p.nm_logradouro,
	null as nr_endereco,
	p.ds_complemento,
	p.nm_bairro,
	p.ds_municipio,
	p.cd_estado,
	p.nr_cep,
	CAST(NULL AS INT ) AS id_municipio_ibge,
	pp.id_processo_parte
FROM 
	legado_1grau.pessoa p, legado_1grau.processo_parte pp
WHERE
	1=1
	and p.cd_pessoa = pp.cd_pessoa 
	pp.id_processo_parte < ANY(:id_processo_parte)