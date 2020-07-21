SELECT
	SUBSTR(nrcnj,0,7) || '-' || SUBSTR(nrcnj,8,2) || '.' || SUBSTR(nrcnj,10,4)
|| '.' || SUBSTR(nrcnj,14,1) || '.' || SUBSTR(nrcnj,15,2) || '.' || SUBSTR(nrcnj,17,4) as nr_processo,
	'N' as in_assunto_principal,
    a.codigonacional as cd_assunto_trf
FROM 
	assunto_tmp a
WHERE
	SUBSTR(nrcnj,0,7) || '-' || SUBSTR(nrcnj,8,2) || '.' || SUBSTR(nrcnj,10,4)
|| '.' || SUBSTR(nrcnj,14,1) || '.' || SUBSTR(nrcnj,15,2) || '.' || SUBSTR(nrcnj,17,4) = ANY(:numeros_processos)