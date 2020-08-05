SELECT
	p.nr_processo as nr_processo,
	pa.in_assunto_principal as in_assunto_principal,
    pa.cd_assunto_nacional as cd_assunto_trf
FROM 
	legado_1grau.processo_assunto pa, legado_1grau.processo p
WHERE
	pa.cd_processo = p.cd_processo 
	p.nr_processo = ANY(:numeros_processos)