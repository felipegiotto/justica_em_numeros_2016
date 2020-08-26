select
    pmc.cd_processo_movimento 				as id_movimento_processo,
	pmc.cd_tipo_complemento  				as cd_tipo_complemento, 
	(
		select cc.dsc_complemento 
		from sgt_consulta.complemento  cc 
		where ''||cc.seq_complemento = pmc.cd_tipo_complemento
	) 										as ds_nome,
	pmc.cd_complemento 						as cd_complemento,
    pmc.ds_valor_complemento 				as ds_valor_complemento	
from stage_legado_2grau.processo_movimento_complemento pmc, 
	stage_legado_2grau.processo_movimento pm,
	stage_legado_2grau.processo p
where
	1=1
	and pmc.cd_processo_movimento = pm.cd_processo_movimento
	and pm.cd_processo_movimento = any(:id_movimento_processo)
	and pm.cd_processo = p.cd_processo