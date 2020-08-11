SELECT
    nr_processo
FROM
    stage_legado_1grau.processo
WHERE migrado = true
and proc_localizado_siaj = 'S'