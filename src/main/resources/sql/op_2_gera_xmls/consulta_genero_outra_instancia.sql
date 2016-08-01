SELECT pf.in_sexo
FROM tb_usuario_login ul
LEFT JOIN tb_pess_doc_identificacao doc ON (doc.id_pessoa = ul.id_usuario)
INNER JOIN tb_pessoa_fisica pf ON (pf.id_pessoa_fisica = ul.id_usuario)
WHERE 1=1
  AND doc.in_principal = 'S' 
  AND doc.in_ativo = 'S'
  AND ul.ds_nome_consulta = :nome_consulta
  AND regexp_replace(doc.nr_documento_identificacao, '[\.\-]', '', 'g') = :documento
  AND pf.in_sexo != 'D'
