WITH versao AS (SELECT MAX(installed_on) AS data_max_versao_223
					FROM pje_adm.tb_schema_version tsv
	                WHERE "version" LIKE '%2.2.3%')
SELECT
	cs.id_movimento_processo,	
	tc.tp_tipo_complemento,
	tc.cd_tipo_complemento, 
    tc.ds_nome,
	CASE WHEN (tc.cd_tipo_complemento = '16') AND (pe.dt_atualizacao <  versao.data_max_versao_223) 
		 		--tipo de audiência. Até a versão 2.2.2 o PJe preenchia erroneamente o código do complemento com o id_tipo_audiencia da tb_tipo_audiencia
				THEN  COALESCE( (SELECT cd_sigla_tipo_audiencia FROM tb_tipo_audiencia ta WHERE ta.id_tipo_audiencia::TEXT = cs.ds_texto LIMIT 1), 
								 cs.ds_texto, '')
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto IS NULL)) AND cs.ds_valor_complemento = 'Petição (outras)' THEN '57' --Todos os complementos estão vazios e não tem registro na tb_elemento_dominio em pelo menos um dos Regionais (TRT-7)
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto IS NULL)) AND cs.ds_valor_complemento = 'Indicação de Data de Diligência Pericial' THEN '7557' --Erro na nomenclatura, que deveria ser Indicação de Data de Realização de Diligência Pericial
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto IS NULL)) AND cs.ds_valor_complemento = 'Apresentação de Renúncia de Procuração' THEN '7423' --Erro na nomenclatura, que deveria ser Apresentação de Renúncia de Procuração/Substabelecimento
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto IS NULL)) AND cs.ds_valor_complemento = 'Apresentação de Revogação de Procuração' THEN '7424' --Erro na nomenclatura, que deveria ser Apresentação de Revogação de Procuração/Substabelecimento
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto IS NULL)) AND cs.ds_valor_complemento = 'Requisição Antecipada de Honorários Periciais' THEN '7570' --Erro na base (cd_tipo_complemento = 19) quando deveria ser 4
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto IS NULL)) AND cs.ds_valor_complemento = 'Notificação' THEN '7223' --Erro na base: cd_tipo_complemento 5 e 5013 não possuem o tipo Notificação na tb_elemento_dominio
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto IS NULL)) AND cs.ds_valor_complemento = 'Planilha de Atualização de Cálculos' THEN '7389' --Erro na nomenclatura, que deveria ser Planilha de Atualização de Cálculo
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto IS NULL)) AND cs.ds_valor_complemento = 'Planilha de Cálculos' THEN '7390' --Erro na nomenclatura, que deveria ser Planilha de Cálculo
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto IS NULL)) AND cs.ds_valor_complemento = 'Citação' THEN '7373' --Erro na nomenclatura, que deveria ser Mandado de Citação
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto IS NULL)) AND cs.ds_valor_complemento = 'Pauta de Julgamento' THEN '7006' --Erro na nomenclatura, que deveria ser pauta de julgamento
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto IS NULL)) AND cs.ds_valor_complemento = 'Cumprida' THEN '7179' --Erro na nomenclatura, que deveria ser cumprido com finalidade atingida
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto IS NULL)) AND cs.ds_valor_complemento = 'Não cumprida' THEN '7182' --Erro na nomenclatura, que deveria ser não cumprido
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto IS NULL)) AND cs.ds_valor_complemento ILIKE 'c_ntral de ma%d%do%' THEN '7476' --Erro na nomenclatura, que deveria ser Central de Mandados
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto IS NULL)) AND cs.ds_valor_complemento ILIKE 'r_u' THEN '7036' --Erro na nomenclatura, que deveria ser réu
		 --quando vier nulo ou vazio o código no tipo de complemento dinamico, busca pelo nome na tabela elemento domiínio
		 --Não foi possível tratar o complemento 5060, pois não está sendo preenchido a partir do domínio da tb_elemento_dominio, como se fosse do tipo texto livre
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto IS NULL)) AND tc.tp_tipo_complemento = 'D' THEN
		 	COALESCE (
				 		(SELECT ed.cd_glossario 
				 		 	FROM tb_elemento_dominio ed
				 		 		INNER JOIN tb_dominio dominio ON dominio.id_dominio = ed.id_dominio 
				 		 										AND dominio.cd_glossario = tc.cd_tipo_complemento 
							 WHERE UPPER(ed.ds_valor) = UPPER(cs.ds_valor_complemento) --Usando o UPPER por causa do complemento do tipo 7 (destino), que possui muitos erros de caixa alta
						 	 LIMIT 1)
				 	  , '')
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto IS NULL)) AND tc.tp_tipo_complemento = 'L' THEN 
			--quando vier nulo ou vazio nos tipos de complemento livre. Nesses tipos de complemento o campo tc.cd_tipo_complemento possui o mesmo valor do campo tb_dominio.cd_glossario
			--Os complementos do tipo livre nao possuem registro na tabela tb_elemento_dominio e o complemento 5067 nao tem registro na tabela tb_dominio, por isso usar a tb_tipo_complemento.cd_tipo_complemento
		 	COALESCE(tc.cd_tipo_complemento, '')
		 WHEN ((cs.ds_texto = '') OR (cs.ds_texto IS NULL)) AND tc.tp_tipo_complemento IN ('A', 'M', 'I', 'P') THEN 
			--quando vier nulo ou vazio nos tipos de complemento data, monetário, identificador e processo. Nesses tipos de complemento o campo tc.cd_tipo_complemento possui o mesmo valor do campo tb_dominio.cd_glossario
			--Os complementos desse tipo nao possuem registro na tabela tb_elemento_dominio
		 	COALESCE(tc.cd_tipo_complemento, '')
		 ELSE cs.ds_texto
	END AS cd_complemento,
	REGEXP_REPLACE(cs.ds_valor_complemento, '[\r\n]', '') AS ds_valor_complemento
FROM versao, tb_complemento_segmentado cs
INNER JOIN tb_tipo_complemento tc ON (tc.id_tipo_complemento = cs.id_tipo_complemento)
INNER JOIN tb_processo_evento pe on (pe.id_processo_evento = cs.id_movimento_processo)
WHERE 1=1
    AND cs.id_movimento_processo = ANY(:id_movimento_processo)
    AND tc.cd_tipo_complemento IS NOT null
    AND tc.ds_nome IS NOT null
    AND
    (0=1
      OR (cs.ds_texto IS NOT NULL AND TRIM(cs.ds_texto) <> '')
      OR (cs.ds_valor_complemento IS NOT NULL AND TRIM(cs.ds_valor_complemento) <> '')
    )