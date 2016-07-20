== Sobre o projeto ==

Ferramenta para extrair, do PJe, os XMLs para o Selo Justiça em Números 2016 do CNJ.

Author: felipe.giotto@trt4.jus.br



== Instruções ==

Criar um arquivo "config.properties", na raiz do projeto, a partir do arquivo "config.properties_modelo", preenchendo os dados corretos.

Abrir o pacote "br.jus.trt4.justica_em_numeros_2016.tasks", analisar as classes nele contidas e executar as operações desejadas.



== Características / Pendências ==

* Órgão Julgador/Serventia: O arquivo XSD do CNJ orienta a utilização dos códigos e nomes de órgãos julgadores das serventias judiciárias cadastradas no 
  Módulo de Produtividade Mensal (Resolução CNJ nº 76/2009). Essas serventias incluem tanto os gabinetes (OJs dos processos)
  quanto as turmas e seções especializadas (OJCs do processo). A rotina utilizará, entretanto, SEMPRE o número e o nome da serventia
  do Órgão Julgador (gabinete), ignorando o OJC.
  PARA AVALIAR: verificar preenchimento do elemento orgaoJulgador pois, por exemplo, um processo de 1º grau com recurso ao 2º grau constará, 
                no XML da 1ª instância, no orgaoJulgador referente à vara ou ao tribunal? Atualmente consta como VARA.
  
* Processo/Sigilo e Movimento/Sigilo: O arquivo XSD do CNJ orienta a utilização de diferentes níveis de sigilo (de 0 a 5), para o processo
  e para os movimentos. Como o PJe não possui essa distinção, serão utilizados somente os valores "0" (sem sigilo) ou "5" (sigilo absoluto).

* Parte/Interesse público: não estão sendo tratados os casos em que a parte é considerada um interesse público abstrato cuja defesa 
  está a cargo do Ministério Público ou da Defensoria Pública, conforme campo "interessePublico" das partes do processo.
  O XSD não especifica se esse atributo é obrigatório.

* Parte/Relacionamentos: não estão sendo tratados os casos de representação e assistência dos pais, representação ou substituição 
  processual em ações coletivas, tutela e curatela. Esse campo é opcional no XSD.
  
* Parte: não estão sendo tratados os casos de representação ou substituição processual em ações coletivas, tutela e curatela.

* Parte/Advogado: não está sendo preenchido o elemento opcional 'advogado'.

* Assuntos: Decidir o que fazer quando o processo não tiver assunto cadastrado! Atualmente o processo fica sem assunto no XML,
  apesar do assunto estar marcado como obrigatório. 
  OBS: está sendo gerado um warning nos logs quando processo não tiver assunto.

* Movimentos/Complementos: Decidir o que fazer quando os complementos não tiverem código!
  OBS: está sendo gerado um warning nos logs quando movimento não tiver complemento.



== Referências ==

Essa ferramenta foi desenvolvida a partir de diversas fontes, ex:
* Script recebidos do TRT14 (Felypp De Assis Oliveira), gravados na pasta "documentacao/referencias/trt14"
* Página do CNJ:
  * http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/2016-06-02-17-51-25
  * http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros
  * http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros
* Wiki do CSJT: https://pje.csjt.jus.br/documentacao/index.php/Scripts_estatisticas_regionais
* E-mail "Re: Selo Justiça em Números" (Felypp, atual)
* E-mail "Justiça em Números", de 2015 (Crisostomo Koling)
* E-mail "Justiça em Números - XML 2015" (Felipe Cesar Stanzani Fonseca)
