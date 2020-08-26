SELECT
    nr_processo
FROM
    stage_legado_2grau.processo
WHERE 
	1=1
	and proc_localizado_siaj 	= 'S'
	and proc_hibrido			= 'S'
