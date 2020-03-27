SELECT 
    pe.id_processo_evento, 
    
    /*** movimento ***/
    /* dataHora */
    to_char(pe.dt_atualizacao, 'yyyymmddhh24miss') AS dta_ocorrencia,
    /* nivelSigilo */
    case when pe.in_visibilidade_externa = true then 0 else 5 end AS in_visibilidade_externa,
    /* identificadorMovimento */
    -- pe.id_processo_evento,
    
    /*** movimentoNacional ***/
    /* codigoNacional */
    pe.id_evento AS cd_movimento_cnj,
    
    /* TRT4 */
    pe.ds_texto_final_interno,
    ep.ds_movimento,
	ul.ds_login,
	pm.id as id_magistrado,
	
	-- Para identificar se é um movimento do Magistrado, referente a um Julgamento
	case when (ev.ds_caminho_completo ilike 'Magistrado|Julgamento%') then true else false end as is_magistrado_julgamento,
	pe.dt_atualizacao
FROM tb_processo_evento pe 
INNER JOIN tb_evento ev ON (ev.id_evento = pe.id_evento AND ev.in_ativo  = 'S') 
LEFT  JOIN tb_evento_processual ep on ev.id_evento=ep.id_evento_processual
LEFT  JOIN tb_usuario_login ul on (ul.id_usuario = pe.id_usuario AND pe.id_usuario <> 0) -- Para identificar o usuário responsável pelo MOVIMENTO
LEFT  JOIN tb_pessoa_magistrado pm on (pm.id = ul.id_usuario)                            -- Para identificar se o usuário responsável pelo MOVIMENTO é Servidor ou Magistrado
WHERE 1=1
    AND pe.id_processo_evento_excludente IS NULL
    AND pe.id_processo = :id_processo
ORDER BY pe.dt_atualizacao ASC