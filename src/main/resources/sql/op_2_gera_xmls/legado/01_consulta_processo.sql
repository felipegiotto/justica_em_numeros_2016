SELECT
    processo as nr_processo,
    '' id_processo,--provavelmente é para usar no pje
    '' nr_sequencia,
    '' nr_ano,
    '' nr_origem_processo,
    CASE WHEN nivelsigilo = 'S' THEN 1
     ELSE 0 END as in_segredo_justica,
    classeprocessual as cd_classe_judicial,
    '' ds_classe_judicial, -- essa informação não vai para o xml
    codigomunicipioibge as id_municipio_ibge,
    dataajuizamento as dt_autuacao, --YYYYMMDDHH24MMSS -- confirmar
    codigoorgao as cd_orgao_julgador, -- código do órgão julgador já no formato do CNJ
    NOMEORGAO AS ds_orgao_julgador, --nome do órgão já no formato do CNJ
    '' ds_orgao_julgador_colegiado, -- atributo apenas para o 2 grau
    1 nr_instancia,
    null vl_causa, -- esse campo não é devidamente preenchido no SIAJ
    null id_proc_referencia, -- esse campo não vai para o xml, apenas para consulta interna no pje
    chprocdep as nr_processo_ref,
    '' cd_classe_judicial_ref,
    '' ds_classe_judicial_ref   
FROM
    dados_basicos_tmp
where 
processo = ANY(:numeros_processos)