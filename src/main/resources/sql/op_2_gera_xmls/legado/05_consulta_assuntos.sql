SELECT
	p.nr_processo as nr_processo,
	COALESCE( pa.in_assunto_principal, 'N') as in_assunto_principal,
    CAST(pa.cd_assunto_nacional AS INT) as cd_assunto_trf
FROM 
	legado_1grau.processo_assunto pa, legado_1grau.processo p
WHERE
	pa.cd_processo = p.cd_processo 
	and p.nr_processo = ANY(:numeros_processos)