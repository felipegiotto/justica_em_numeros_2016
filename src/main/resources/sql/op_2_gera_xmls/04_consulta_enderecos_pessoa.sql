SELECT e.nm_logradouro, e.nr_endereco, e.ds_complemento, coalesce(cep.nm_bairro, e.nm_bairro) as nm_bairro, m.ds_municipio, uf.cd_estado, cep.nr_cep
FROM tb_processo_parte_endereco ppe
LEFT JOIN tb_endereco e USING (id_endereco)
LEFT JOIN tb_cep cep USING (id_cep)
LEFT JOIN tb_municipio m USING (id_municipio)
LEFT JOIN tb_estado uf USING (id_estado)
WHERE id_processo_parte = :id_processo_parte
  AND e.nm_logradouro IS NOT NULL AND cep.nr_cep IS NOT NULL