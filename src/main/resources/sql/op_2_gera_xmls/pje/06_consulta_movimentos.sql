SELECT
	proc.nr_processo,
    pe.id_processo_evento, 
    
    /*** movimento ***/
    /* nivelSigilo */
    pe.in_visibilidade_externa,
    --case when pe.in_visibilidade_externa = true then 0 else 5 end AS in_visibilidade_externa,
    /* identificadorMovimento */
    -- pe.id_processo_evento,
    
    /*** movimentoNacional ***/
    /* codigoNacional */
    --O campo id_evento da tabela tb_processo_evento não contém o código correto para alguns movimentos da JT
    CAST(COALESCE(ep.cd_evento, '0') AS INTEGER) AS cd_movimento_cnj,
    
    /* TRT4 */
    pe.ds_texto_final_interno,
    ep.ds_movimento,
	ul.ds_login,
	pm.id as id_magistrado,
	
	-- Para identificar se é um movimento do Magistrado, referente a um Julgamento
	case when (ev.ds_caminho_completo ilike 'Magistrado|Julgamento%') then true else false end as is_magistrado_julgamento,
	pe.dt_atualizacao
FROM tb_processo_evento pe 
INNER JOIN tb_processo proc ON (proc.id_processo = pe.id_processo)
INNER JOIN tb_processo_trf ptrf on (proc.id_processo = ptrf.id_processo_trf)
INNER JOIN tb_evento ev ON (ev.id_evento = pe.id_evento AND ev.in_ativo  = 'S') 
LEFT  JOIN tb_evento_processual ep on ev.id_evento=ep.id_evento_processual
LEFT  JOIN tb_usuario_login ul on (ul.id_usuario = pe.id_usuario AND pe.id_usuario <> 0) -- Para identificar o usuário responsável pelo MOVIMENTO
LEFT  JOIN tb_pessoa_magistrado pm on (pm.id = ul.id_usuario)                            -- Para identificar se o usuário responsável pelo MOVIMENTO é Servidor ou Magistrado
WHERE 1=1
    AND pe.id_processo_evento_excludente IS NULL
    AND proc.nr_processo = ANY(:numeros_processos)
    
    -- Reforça a condição da data de autuação, pois um bug no PJe fez com que houvesse mais que um processo com mesma numeração (um com data de autuação e outro sem)
    AND ptrf.dt_autuacao IS NOT NULL
    
ORDER BY pe.dt_atualizacao ASC