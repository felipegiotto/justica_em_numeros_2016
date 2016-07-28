========== Sobre o projeto ==========

Ferramenta para extrair, do PJe, os XMLs para o Selo Justiça em Números 2016 do CNJ.

Também é possível unir esses arquivos do PJe com arquivos XMLs de sistemas legados, para enviar
de uma vez só ao CNJ. 

Author: felipe.giotto@trt4.jus.br



========== Instruções ==========

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
   gerados no passo anterior, em lotes de 5000 processos. Esses arquivos poderão ser enviados ao CNJ.
   OBS: Serão gerados arquivos na pasta "output\xmls_unificados".
   IMPORTANTE: Essa classe unificará TODOS os arquivos XML da referida pasta, inclusive os que 
               foram gerados em execuções anteriores!! 
               Por isso, antes de gerar o arquivo XML definitivo para enviar ao CNJ, recomenda-se 
               limpar a pasta "ouput", e seguir todos os passos novamente, desde o início.

5. Seguir as instruções do arquivo "Op_4_ValidaEnviaArquivosCNJ.txt", para execução da ferramenta
   "replicacao-client", fornecida pelo CNJ, para validar os XMLs gerados e enviar ao Conselho.



Funcionamento "avançado":

* Essa ferramenta também permite a unificação de arquivos XML de processos de sistemas legados.
  Obviamente, esses arquivos devem ser gerados utilizando alguma outra ferramenta. 
  Para tanto, grave os arquivos XML dos sistemas legados na pasta 
  "output\Xg\xmls_individuais\NOME_SISTEMA" (onde X representa a instância - "1" ou "2" - e 
  NOME_SISTEMA pode ser qualquer identificador) antes de executar o passo 4, acima.


* Se a estrutura do arquivo XSD do CNJ for alterado, gravar o novo arquivo XSD na pasta 
  "src/main/resources" e executar o método "main" da classe "Op_0_ParseArquivoXSD".
  Provavelmente será necessário alterar a lógica das rotinas que leem e gravam arquivos XML.



========== Características ==========


* A ferramenta permite a extração dos dados do PJe para a carga COMPLETA e para a carga MENSAL, conforme
  instruções do CNJ em "http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/2016-06-02-17-51-25".
  As regras estão descritas a seguir:
  * Para a carga completa devem ser encaminhados a totalidade dos processos em tramitação em 31 de 
    julho de 2016, bem como daqueles que foram baixados de 1° de janeiro de 2015 até 31 de julho de 2016. 
  * Para a carga mensal devem ser transmitidos os processos que tiveram movimentação ou alguma 
    atualização no mês de agosto de 2016, com todos os dados e movimentos dos respectivos processos, 
    de forma a evitar perda de algum tipo de informação.
  
  
* Processo/Órgão Julgador: O arquivo XSD do CNJ orienta a utilização dos códigos e nomes de órgãos 
  julgadores das serventias judiciárias cadastradas no Módulo de Produtividade Mensal (Resolução 
  CNJ nº 76/2009). A rotina utilizará SEMPRE o número e o nome da serventia do Órgão Julgador 
  (gabinete), ignorando o OJC.
  
  
* Processo/Classe Processual: O arquivo XSD do CNJ orienta a utilização de classes processuais padronizadas,
  conforme "Resolução 46". Por isso, se alguma classe processual for diferente das definidas pelo CNJ,
  será gerado um WARNING. De qualquer forma, mesmo com o aviso, o código da classe será inserido no XML. 
  
  
* Processo/Sigilo e Movimento/Sigilo: O arquivo XSD do CNJ orienta a utilização de diferentes níveis 
  de sigilo (de 0 a 5), para o processo e para os movimentos. Como o PJe não possui essa distinção, 
  serão utilizados somente os valores "0" (sem sigilo) ou "5" (sigilo absoluto).


* Parte: não estão sendo tratados os casos de representação ou substituição processual em ações 
  coletivas, tutela e curatela.


* Parte/Interesse público: não estão sendo tratados os casos em que a parte é considerada um interesse 
  público abstrato cuja defesa está a cargo do Ministério Público ou da Defensoria Pública, conforme 
  campo "interessePublico" das partes do processo. No XSD, esse campo não é obrigatório.


* Parte/Relacionamentos: não estão sendo tratados os casos de representação e assistência dos pais, 
  representação ou substituição processual em ações coletivas, tutela e curatela. No XSD, esse campo 
  não é obrigatório.
  
  
* Parte/Advogado: não está sendo preenchido o elemento opcional 'advogado'.


* Parte/Documentos: somente estão sendo inseridos nos arquivos XML os documentos que possuem correspondência
  nos tipos do CNJ, conforme arquivo "tipos_de_documentos.properties". Os documentos do PJe que não possuírem
  correspondência serão ignorados, ex: "RGE".
  OBS: está sendo gerado um warning nos logs ao encontrar um documento que não possuem correspondência.


* Assuntos: Quando o processo não tiver assunto cadastrado no PJe, ficará sem assunto no arquivo XML.
  OBS: Se nenhum dos assuntos do processo estiver marcado como principal no PJe, a ferramenta marcará
       o primeiro assunto como principal no arquivo XML (orientação de Jeferson, via
       e-mail, em 28 de julho de 2016 12:43, no e-mail "Processos sem assunto")
  OBS: Éstá sendo gerado um warning nos logs quando processo não tiver assunto.
  TODO: Definir o que fazer com processos sem assunto, já que os arquivos são negados no CNJ.
  
  
* Assuntos CNJ: O arquivo XSD do CNJ explicita que os assuntos devem ser preenchidos de forma diferente
  se fizerem ou não parte das tabelas nacionais. Por isso, os arquivos "assuntos_1g.csv" e "assuntos_2g.csv", 
  na pasta "src/main/resources/tabelas_cnj", contém listas com todos os assuntos do CNJ. Essa lista
  foi baixada de http://www.cnj.jus.br/sgt/versoes.php?tipo_tabela=A .


* Movimentos CNJ: A mesma regra dos assuntos (que devem fazer parte da lista do CNJ) também se aplica
  aos movimentos processuais. Por isso, a lista dos movimentos processuais do CNJ foi gravada nos arquivos 
  "movimentos_1g.csv" e "movimentos_2g.csv". na pasta "src/main/resources/tabelas_cnj", depois de 
  serem baixadas de http://www.cnj.jus.br/sgt/versoes.php?tipo_tabela=M .
  
  
* Movimentos/Complementos: Alguns complementos de movimentos possuem código, outros não. A página do CNJ
  "http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/2016-06-02-17-51-25"
  exibe um exemplo de complemento com código e sem código: Ex.: no movimento 123, seria
      18:motivo_da_remessa:38:em grau de recurso
      7:destino:1ª Vara Cível



========== Dúvidas / Esclarecimentos / Pendências ==========

* TODO: Documentos do tipo "RGE". Esclarecer o que fazer.

* TODO: Classe processual que não existe nas tabelas do CNJ: 11886 - RECURSO ORDINÁRIO EM PROCEDIMENTO SUMARÍSSIMO

* TODO: Definir como tratar partes de tipos que não estejam mapeadas no CNJ, ex: 
  * O representante da parte 'CLINSUL MAO DE OBRA E REPRESENTACAO LTDA' (id_processo_parte=173621) possui um tipo de parte que ainda não foi tratado: ADMINISTRADOR
  * O representante da parte 'FERNANDA CRISTINA DE SOUZA LIMA' (id_processo_parte=2780966) possui um tipo de parte que ainda não foi tratado: ASSISTENTE
  * O representante da parte 'LUIZ PAULO FERREIRA' (id_processo_parte=466618) possui um tipo de parte que ainda não foi tratado: CURADOR
  * O representante da parte 'VILMAR DOS REIS' (id_processo_parte=2220300) possui um tipo de parte que ainda não foi tratado: REPRESENTANTE
  * O representante da parte 'NISYO STROIEKE' (id_processo_parte=245410) possui um tipo de parte que ainda não foi tratado: INVENTARIANTE
  * O representante da parte 'MELISSA CAVALHEIRO GRAVINA' (id_processo_parte=2436733) possui um tipo de parte que ainda não foi tratado: TUTOR
  * Comando para identificar problemas com tipos de parte desconhecidos:
    grep "O representante da parte" justica_em_numeros.log | grep --invert-match ": ADMINISTRADOR" | grep --invert-match ": ASSISTENTE" | grep --invert-match ": CURADOR" | grep --invert-match ": REPRESENTANTE" | grep --invert-match ": INVENTARIANTE" | grep --invert-match ": TUTOR" | less
  
* Comando para identificar warnings nos arquivos de log:
  grep " WARN " justica_em_numeros.log | grep --invert-match "'RGE'" | grep --invert-match "EM PROCEDIMENTO SUMAR" | grep --invert-match "GABINETE JUDICIARIO" | grep --invert-match "foi gerado na base" | grep --invert-match "sem assunto" | grep --invert-match "representante da parte" | grep --invert-match "'PFP'" | grep --invert-match "connection has been closed" | less  
  
* Quanto ao período de extração dos dados:
  Pergunta enviada ao CNJ: 
    Há uma divergência, na página do CNJ, a respeito do período dos dados que precisam ser extraídos:
    * A página "http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/perguntas-frequentes" 
      descreve que devem ser extraídos dados de processos baixados a partir de 01/01/2016.
    * A página "http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/2016-06-02-17-51-25" 
      descreve que devem ser extraídos dados de processos baixados a partir de 01/01/2015.
  Resposta de davi.borges@cnj.jus.br em 26/07/2016: A partir de 01/01/2015


* Quanto à forma de seleção dos processos a serem exportados
  Pergunta enviada ao CNJ: 
    A orientação do CNJ, para a carga "completa" de processos, é esta:
      Para a carga completa devem ser encaminhados a totalidade dos processos em tramitação em 31 de 
      julho de 2016, bem como daqueles que foram baixados de 1° de janeiro de  2015 até 31 de julho de 2016. 
    Gostaríamos de esclarecer exatamente o conceito de processos "em tramitação" e processos "baixados". 
    Em ambos os casos, o PJe gera movimentos processuais. Por isso, não seria suficiente localizar 
    todos os processos que possuem algum movimento processual no período?
  Resposta de davi.borges@cnj.jus.br em 26/07/2016: Os conceitos são equivalentes.


* Quanto ao Órgão Julgador do processo:
  Pergunta enviada ao CNJ:
    O arquivo XSD define somente um campo "orgaoJulgador" para cada processo. Ocorre que, no PJe, 
    existe tanto o conceito de "Órgão Julgador" (normalmente um gabinete) quanto "Órgão Julgador 
    Colegiado" (turmas ou seções especializadas). Estamos mandando, portanto, somente a informação 
    do gabinete. Nosso entendimento está correto?
  Resposta de davi.borges@cnj.jus.br em 26/07/2016: Sim.


* Parte/Advogado: analisar como devem ser tratados os procuradores, que hoje estão sendo ignorados no arquivo 
  "03_consulta_partes.sql"
  
  
* Quanto à cumulatividade dos arquivos enviados:
  Pergunta enviada ao CNJ e respostas de leandro.andrade@cnj.jus.br em 26/07/2016:
    O CNJ disponibilizou a ferramenta "replicacao-client" para que os tribunais possam validar seus 
    arquivos XML e enviar ao CNJ. Temos algumas dúvidas a respeito deste procedimento:
    * Como faremos para diferenciar os arquivos que foram enviados somente para testes e os arquivos 
      que devem ser apreciados pelo CNJ? Temos como fazer uma limpeza desses dados antigos quando a 
      rotina estiver pronta?
      R: A carga final de todos os processos deve iniciar no máximo até dia 10/08. Vocês podem enviar os 
         dados de teste. Caso queira fazer uma limpeza dos dados antigos, basta enviar um e-mail para
         rosfran.borges@cnj.jus.br, leonardo.borges@cnj.jus.br e leandro.andrade@cnj.jus.br
    * No TRT4, temos processos no PJe (1G e 2G), no inFOR e no NovaJus4. Qual o procedimento que 
      devemos seguir? Acredito que o correto seja gerar vários arquivos XML para cada um dos 4 sistemas
      (já que são muitos processos) e rodar, uma vez, o aplicativo "replicacao-client", para enviar 
      todos ao CNJ. Estou correto?
      R: A quantidade de XML fica a seu critério. O sistema processa todos e separa em 5000 processos.
    * A página "http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/2016-06-02-17-51-25" 
      fala que devemos gravar os arquivos seguindo o padrão "<Sigla do Tribunal>_<Grau de Jurisdição>_<data e hora de transmissão>". 
      Como temos diversos sistemas, como será feita esta diferenciação?
      R: O correto é utilizar o seguinte padrão: <SIGLA_TRIBUNAL>_<GRAU_JURISDICAO>_<DIAMESANO>. 
         Não se preocupe quando a diversidade de sistemas, basta criar os arquivos com a descrição 
         diferente, por exemplo, TJRS_1_20072016-X.zip, em que "X" é um número sequencial e incremental.


* Quanto à estrutura do arquivo XML:
  Pergunta enviada ao CNJ:
    Há uma divergência, na página do CNJ, a respeito da estrutura dos arquivos XML que deve ser seguida:
    * As páginas "http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/perguntas-frequentes" 
      e "http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/2016-06-02-17-51-25" 
      falam somente que deve ser utilizado o XSD de intercomunicação-2.2.2, sem definir o elemento "raiz". 
      Nesse caso, subentende-se que deve ser utilizado um objeto "intercomunicacao".
    * O manual de replicação (manual-replicacao-nacional_1.3.docx) fala que deve ser utilizado um 
      elemento raiz "processos", conforme arquivo "replicacao-nacional.xsd". (http://www.cnj.jus.br/intercomunicacao-2.2.2)
    * A página "http://www.cnj.jus.br/tecnologia-da-informacao/comite-nacional-da-tecnologia-da-informacao-e-comunicacao-do-poder-judiciario/modelo-nacional-de-interoperabilidade/arquivos-do-modelo-nacional-de-interoperabilidade" 
      contém, ainda, um link "Modelo Nacional de Interoperabilidade (Compactado)", que aponta para o 
      arquivo "intercomunicacao-2.2.2.zip". Dentro desse arquivo, há uma JAR "cnj-interop-2.2.2.jar", 
      que possui ainda uma terceira estrutura de classes para gerar os arquivos XML.
  Resposta de leandro.andrade@cnj.jus.br em 26/07/2016: 
    Seguir o modelo replicação-nacional.xsd. Esse encapsula o intercomunicação-2.2.2.xsd.
    O arquivo está disponível em "https://www.cnj.jus.br/owncloud/index.php/s/KwPOp6wENVBS6Vi"


* Processos sem assunto cadastrado (ou sem assunto principal) poderão gerar erros no envio dos dados
  ao CNJ. Por isso, é possível executar a consulta abaixo, para identificar processos que estejam
  sem assunto ou sem assunto principal:
        WITH processos AS (
           SELECT 
              proc.nr_processo, 
              EXISTS(SELECT 1 FROM tb_processo_assunto pa WHERE pa.id_processo_trf = proc.id_processo) as existe_assunto,
              EXISTS(SELECT 1 FROM tb_processo_assunto pa WHERE pa.id_processo_trf = proc.id_processo AND pa.in_assunto_principal='S') as existe_assunto_principal
           FROM tb_processo proc
           WHERE length(proc.nr_processo) = 25
              AND EXISTS (SELECT 1 FROM tb_processo_evento pe WHERE proc.id_processo = pe.id_processo AND pe.dt_atualizacao BETWEEN '2015-01-01 00:00:00.000' AND '2016-12-31 23:59:59.999') -- Condição para que o processo seja exportado ao CNJ
        )
        SELECT nr_processo, 
          CASE WHEN NOT existe_assunto THEN 
            'NENHUM ASSUNTO' 
          ELSE 
            CASE WHEN NOT existe_assunto_principal THEN 
              'SEM ASSUNTO PRINCIPAL'
            ELSE
              'OK'
            END
          END as problema
        FROM processos
        WHERE existe_assunto = false OR existe_assunto_principal = false
  


========== Referências ==========

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
