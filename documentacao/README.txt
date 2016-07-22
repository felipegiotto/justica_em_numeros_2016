== Sobre o projeto ==

Ferramenta para extrair, do PJe, os XMLs para o Selo Justiça em Números 2016 do CNJ.

Também é possível unir esses arquivos do PJe com arquivos XMLs de sistemas legados, para enviar
de uma vez só ao CNJ. 

Author: felipe.giotto@trt4.jus.br



== Instruções ==

Funcionamento "básico":

IMPORTANTE: Confira sempre a saída do Console, prestando atenção especialmente nas linhas 
            com "WARN" ou "ERROR". Pode ser necessária alguma intervenção!

1. Criar um arquivo "config.properties", na raiz do projeto, a partir do arquivo 
   "config.properties_modelo", preenchendo os dados corretos.

2. Executar o método "main" da classe "Op_1_BaixaListaDeNumerosDeProcessos", para localizar os números
   dos processos que precisarão ser exportados para arquivos XML.
   OBS: Os números dos processos serão gravados na pasta "output\Xg" (onde 'X' representa o número 
        da instância - '1' ou '2') no arquivo "lista_processos.txt".
   
3. Executar o método "main" da classe "Op_2_GeraXMLsIndividuais", para gerar os arquivos XML dos
   processos identificados no passo anterior.
   OBS: Será gerado um arquivo XML para cada processo, nas pastas "output\Xg\xmls_individuais\PJe",
        (onde 'X' representa o número da instância - '1' ou '2').
   
4. Executar o método "main" da classe "Op_3_UnificaArquivosXML", para unificar todos os arquivos XML
   gerados no passo anterior. Esses arquivos poderão ser enviados ao CNJ.
   OBS: Serão gerados arquivos na pasta "output\Xg", com nome "xmls_unificados.xml".
   IMPORTANTE: Essa classe unificará TODOS os arquivos XML da referida pasta, inclusive os que 
               foram gerados em execuções anteriores!! 
               Por isso, antes de gerar o arquivo XML definitivo para enviar ao CNJ, recomenda-se 
               limpar a pasta "ouput", e seguir todos os passos novamente, desde o início.
               


Funcionamento "avançado":

* Essa ferramenta também permite a unificação de arquivos XML de processos de sistemas legados.
  Obviamente, esses arquivos devem ser gerados utilizando alguma outra ferramenta. 
  Para tanto, grave os arquivos XML dos sistemas legados na pasta 
  "output\Xg\xmls_individuais\NOME_SISTEMA" (onde X representa a instância - "1" ou "2" - e 
  NOME_SISTEMA pode ser qualquer identificador) antes de executar o passo 4. 


* Se a estrutura do arquivo XSD do CNJ for alterado, gravar o novo arquivo XSD na pasta 
  "src/main/resources" e executar o método "main" da classe "Op_0_ParseArquivoXSD".
  Provavelmente será necessário alterar a lógica das rotinas que leem e gravam arquivos XML.



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
