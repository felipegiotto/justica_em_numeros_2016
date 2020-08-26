select 
	nr_processo
from 
	stage_legado_2grau.processo 
where 
	1=1
	and proc_localizado_siaj 	= 'S'
	and proc_escopo_legado 		= 'S'
	and proc_hibrido			= 'N'