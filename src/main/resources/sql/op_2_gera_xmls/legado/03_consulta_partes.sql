SELECT
    p.nrcnj       AS nr_nrcnj,
    CASE
        WHEN polo = 'AT' THEN 'A'
        ELSE 'P'
    END AS in_participacao,
    '' id_pessoa, -- informacao que só interessa ao pje
    nome          AS ds_nome,
    nome          AS ds_nome_consulta,
    tipo_pessoa   AS in_tipo_pessoa,
    'D' AS in_sexo, -- não possuímos tal informação
    '' AS id_nrcnj_parte, -- essa informação é do pje
    '' AS id_parte_representante,  -- essa informação é do pje
    '' AS ds_tipo_parte_representante, -- essa informação ainda será preenchida, vou ajustar o pentaho
    '' AS dt_nascimento, -- não possuímos tal informação
    '' AS dt_obito, -- não possuímos tal informação
    '' AS nm_genitora,-- não possuímos tal informação
    '' AS nm_genitor -- não possuímos tal informação
FROM
    partes_tmp p
where SUBSTR(nrcnj,0,7) || '-' || SUBSTR(nrcnj,8,2) || '.' || SUBSTR(nrcnj,10,4)
|| '.' || SUBSTR(nrcnj,14,1) || '.' || SUBSTR(nrcnj,15,2) || '.' || SUBSTR(nrcnj,17,4) = ANY(:numeros_processos)