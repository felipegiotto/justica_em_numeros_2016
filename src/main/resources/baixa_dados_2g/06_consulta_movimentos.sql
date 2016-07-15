      SELECT 
        pe.id_processo_evento, 
        
        /*** movimento ***/
        /* dataHora */
        to_char(pe.dt_atualizacao, 'yyyymmddhh24miss') AS dta_ocorrencia, -- TODO: Verificar formato da data
        /* nivelSigilo */
        case when pe.in_visibilidade_externa = true then 0 else 5 end AS in_visibilidade_externa, -- TODO: Verificar formato do sigilo
        /* identificadorMovimento */
        -- pe.id_processo_evento,
        
        /*** movimentoNacional ***/
        /* codigoNacional */
        pe.id_evento AS cd_movimento_cnj
      FROM tb_processo_evento pe 
      INNER JOIN tb_evento ev ON 1=1
        AND ev.id_evento = pe.id_evento
        AND ev.in_ativo  = 'S' 
      WHERE 1=1
        AND pe.id_processo_evento_excludente IS NULL
        AND pe.id_processo = :id_processo
      ORDER BY pe.dt_atualizacao ASC