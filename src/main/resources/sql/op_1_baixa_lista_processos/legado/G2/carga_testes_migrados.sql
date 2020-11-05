SELECT
    nr_processo
FROM
    stage_legado_2grau.processo
WHERE 
	1=1
	and proc_localizado_siaj 	= 'S'
	and proc_hibrido			= 'S'
	and cd_orgao_julgador is not null -- foi decidido que serão desconsiderados os processo que não puderam ter o orgao julgador mapeado para serventia por fazer parte da chave
ORDER BY random()
LIMIT 100
