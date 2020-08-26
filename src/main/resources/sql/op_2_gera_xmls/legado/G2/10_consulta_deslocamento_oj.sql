--Deve ser implementada quando o parâmetro 'possui_deslocamento_oj_legado_2g' tiver o valor SIM
SELECT
  p.nr_processo as nr_processo,
  pd.id_oj_origem, -- representa o código do órgão julgador de origem
  upper(pd.ds_oj_origem) as ds_oj_origem, 
  pd.id_oj_destino, -- representa o código do órgão julgador de destino
  upper(pd.ds_oj_destino) as ds_oj_destino, 
  pd.dt_deslocamento, 
  pd.dt_retorno,
  pd.id_municipio_origem as id_municipio_origem,
  pd.id_municipio_destino as id_municipio_destino
from 
	stage_legado_2grau.processo_deslocamento pd inner join stage_legado_2grau.processo p on pd.cd_processo = p.cd_processo
WHERE 
	p.nr_processo = ANY(:numeros_processos)
ORDER BY pd.dt_deslocamento