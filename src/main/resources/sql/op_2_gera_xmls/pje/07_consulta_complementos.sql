SELECT
	cs.id_movimento_processo,
    tc.cd_tipo_complemento, 
    tc.ds_nome,
	CASE WHEN (tc.cd_tipo_complemento = '16') AND (pe.dt_atualizacao <  (SELECT installed_on FROM pje_adm.tb_schema_version tsv WHERE "version" LIKE '%2.2.3%' AND state = 'SUCCESS' ORDER BY installed_on DESC LIMIT 1)) 
		 		--tipo de audiência. Até a versão 2.2.2 o PJe preenchia erroneamente o código do complemento com o id_tipo_audiencia da tb_tipo_audiencia
				THEN  COALESCE( (SELECT cd_sigla_tipo_audiencia FROM tb_tipo_audiencia ta WHERE ta.id_tipo_audiencia::TEXT = cs.ds_texto LIMIT 1), 
								 cs.ds_texto, '')
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto is NULL)) AND cs.ds_valor_complemento = 'Petição (outras)' THEN '57' --No TRT-7 todos os complementos estão vazios e não tem registro na tb_elemento_dominio
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto is NULL)) AND cs.ds_valor_complemento = 'Indicação de Data de Diligência Pericial' THEN '7557' --Erro na nomenclatura, que deveria ser Indicação de Data de Realização de Diligência Pericial
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto is NULL)) AND cs.ds_valor_complemento = 'Apresentação de Renúncia de Procuração' THEN '7423' --Erro na nomenclatura, que deveria ser Apresentação de Renúncia de Procuração/Substabelecimento
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto is NULL)) AND cs.ds_valor_complemento = 'Apresentação de Renúncia de Procuração' THEN '7424' --Erro na nomenclatura, que deveria ser Apresentação de Revogação de Procuração/Substabelecimento
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto is NULL)) AND cs.ds_valor_complemento = 'Requisição Antecipada de Honorários Periciais' THEN '7570' --Erro na base (cd_tipo_complemento = 19) quando deveria ser 4		 
		 --quando vier nulo ou vazio o código no tipo de complemento dinamico, busca pelo nome na tabela elemento dominio
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto is NULL)) AND tc.tp_tipo_complemento = 'D' THEN
		 	COALESCE (
				 		(SELECT ed.cd_glossario FROM tb_elemento_dominio ed 
						 	WHERE ed.id_dominio = cs.id_tipo_complemento 
						 	  AND ed.ds_valor = cs.ds_valor_complemento LIMIT 1)
				 	  , '')
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto is NULL)) AND tc.tp_tipo_complemento = 'L' THEN 
			--quando vier nulo ou vazio nos tipos de complemento livre. Nesses tipos de complemento o campo tc.cd_tipo_complemento possui o mesmo valor do campo tb_dominio.cd_glossario
			--Os complementos do tipo livre nao possuem registro na tabela tb_elemento_dominio e o complemento 5067 nao tem registro na tabela tb_dominio, por isso usar a tb_tipo_complemento.cd_tipo_complemento
		 	COALESCE(tc.cd_tipo_complemento, '')
		 ELSE cs.ds_texto
     END AS cd_complemento,
    regexp_replace(cs.ds_valor_complemento, '[\r\n]', '') AS ds_valor_complemento
FROM tb_complemento_segmentado cs
INNER JOIN tb_tipo_complemento tc ON (tc.id_tipo_complemento = cs.id_tipo_complemento)
INNER JOIN tb_processo_evento pe on (pe.id_processo_evento = cs.id_movimento_processo)
WHERE 1=1
    AND cs.id_movimento_processo = ANY(:id_movimento_processo)
    AND tc.cd_tipo_complemento is not null
    AND tc.ds_nome is not null
    AND
    (0=1
      OR '' <> trim(cs.ds_texto) 
      OR cs.ds_valor_complemento is not null
    )
