Select
    p.nr_processo as nr_processo,
   	pmc.cd_processo_movimento as id_movimento_processo,
    CAST(pmc.cd_tipo_complemento AS INT) AS cd_tipo_complemento, 
    (select cc.dsc_complemento from sgt_consulta.complemento  cc where ''||cc.seq_complemento = pmc.cd_tipo_complemento) as ds_nome,
    pmc.cd_complemento,
    pmc.ds_valor_complemento 
from legado_1grau.processo_movimento_complemento pmc, 
	legado_1grau.processo_movimento pm,
	legado_1grau.processo p
where
	1=1
	and pmc.cd_processo_movimento = pm.cd_processo_movimento
	and pm.cd_movimento_processo = ANY(:id_movimento_processo)
	and pm.cd_processo = p.cd_processo
	
	