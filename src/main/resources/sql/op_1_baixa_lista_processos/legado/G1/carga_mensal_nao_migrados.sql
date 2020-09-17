select 
	nr_processo
from 
	stage_legado_1grau.processo 
where 
	1=1
	and proc_localizado_siaj 	= 'S'
	and proc_escopo_legado 		= 'S'
	and proc_hibrido			= 'N'
	and cd_orgao_julgador is not null -- foi decidido que serão desconsiderados os processo que não puderam ter o orgao julgador mapeado para serventia por fazer parte da chave