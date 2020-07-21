SELECT
    id_pessoa,
    CASE
        WHEN numerodocumentoprincipalf IS NOT NULL THEN numerodocumentoprincipalf
        WHEN numerodocumentoprincipalj IS NOT NULL THEN numerodocumentoprincipalj
        ELSE NULL
    END as nr_documento,
    '' ds_emissor,
    CASE
        WHEN numerodocumentoprincipalf IS NOT NULL THEN 'CPF'
        WHEN numerodocumentoprincipalj IS NOT NULL THEN 'CPJ'
        ELSE NULL
    END as cd_tp_documento_identificacao,
    nome as ds_nome_pessoa,
    'S' in_principal
FROM
    partes_tmp
where id_pessoa = ANY(:ids_pessoas)  AND 
(numerodocumentoprincipalf is not null or numerodocumentoprincipalj is not null)