# Sobre o projeto

Ferramenta para extrair, do PJe, os XMLs para o sistema DataJud do CNJ.

Esse sistema executa diversos passos incrementais para extrair os dados e deixar em um formato
compatível com o solicitado pelo CNJ. Cada um desses passos é representado por uma classe
no pacote "br.jus.trt4.justica_em_numeros_2016.tasks".

Também é possível unir os arquivos do PJe com arquivos XMLs de sistemas legados, para enviar
de uma vez só ao CNJ. 

DEPOIS de ler as instruções desse arquivo, leia o arquivo CHECKLIST_RESUMO.txt, que irá guiá-lo passo a 
passo.


Autor: felipe.giotto@trt4.jus.br



# Instruções básicas / Preparação do ambiente

IMPORTANTE: Confira sempre a saída do Console depois de cada passo executado, 
            prestando atenção especialmente nas linhas com "WARN" ou "ERROR". 
            Pode ser necessária alguma intervenção!

IMPORTANTE: Cada uma das tarefas (classes do pacote `br.jus.trt4.justica_em_numeros_2016.tasks`)
            possui uma explicação sobre o seu funcionamento. Recomendo a leitura de todos
            antes da execução das tarefas, para entender o funcionamento da ferramenta.
            
1. Crie um arquivo `config.properties`, na raiz do projeto, a partir do arquivo 
   `config_modelo.properties`, preenchendo os dados corretos.

2. Importe este projeto no Eclipse. É necessário ter as seguintes ferramentas instaladas:
  * Eclipse
  * Java 1.8 ou superior
  * Apache Maven (pode ser necessária configuração de proxy para baixar dependências)

3. Será preciso baixar manualmente a ferramenta "de-para" de movimentos e complementos, desenvolvida
   pelo TRT3. O repositório desta ferramenta está disponível em https://gitlab.trt15.jus.br/estatistica_cnj_jt/depara-jt-cnj ,
   mas deve ser solicitado o acesso no GitLab. Depois de baixar o código-fonte, acessar a
   pasta raiz do projeto "depara-jt-cnj" em um terminal e digitar: `mvn clean package install`.
   Pode ser preciso, também, atualizar o "pom.xml" deste projeto para referenciar novas versões do "depara-jt-cnj".

4. Leia as instruções do arquivo "CHECKLIST_RESUMO.txt", que conterá todos os
   passos que precisarão ser executados.
   


# Desenvolvimento - Perguntas frequentes

Se for necessário atualizar a estrutura de classes baseada em nova versão do arquivo XSD do CNJ:
1. Excluir os dados do package `br.jus.cnj.modeloDeTransferenciaDeDados`
2. No Eclipse, selecionar "File", "New", "Other", "JAXB Classes from Schema", selecionar o novo arquivo XSD, definir package de destino `br.jus.cnj.modeloDeTransferenciaDeDados`, confirmar.



O envio de dados ao CNJ via serviços REST depende da geração de uma keystore que contenha os certificados
de homologação e de produção do CNJ. Esta keystore fica gravada no arquivo `src/main/resources/certificados_rest_cnj/keystore/cnj.keystore`
e contém os certificados gravados na pasta `src/main/resources/certificados_rest_cnj/certificados`.
Se os certificados não estiverem corretos, ocorrerá um erro no envio:

```
PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target.
```

Nesse caso, se for preciso atualizar algum certificado da keystore, seguir os passos abaixo:
1. Gravar os novos certificados na pasta `src/main/resources/certificados_rest_cnj/certificados`
2. Em um terminal Linux, abrir a pasta `src/main/resources/certificados_rest_cnj`
3. Executar o script `_importar_certificados_para_keystore.sh`



# Características técnicas / Funcionamento avançado

* Cada operação gerará um log de operações, por padrão na pasta `output/<TIPO_CARGA_XML>/log` (isso pode ser
  alterado no arquivo de configurações). O log mais recente sempre se chamará "log_completo.log".
  Cada vez que uma nova operação for executada, o arquivo anterior será renomeado para utilizando a
  data/hora atual e um novo "log_completo.log" será criado.


* A ferramenta permite a extração dos dados do PJe para a carga COMPLETA e para a carga MENSAL, conforme
  instruções do CNJ em "http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/2016-06-02-17-51-25".
  As regras estão descritas a seguir:
  * Para a carga completa devem ser encaminhados a totalidade dos processos em tramitação em 31 de 
    julho de 2020, bem como daqueles que foram baixados de 1° de janeiro de 2015 até 31 de julho de 2020. 
  * Para a carga mensal devem ser transmitidos os processos que tiveram movimentação ou alguma 
    atualização no mês de agosto de 2020, com todos os dados e movimentos dos respectivos processos, 
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
  Esse mesmo padrão é utilizado no PJe-JT, na classe `IntercomunicacaoMNI222Service`.


* Processo/Documentos: Os documentos do processo NÃO DEVEM SER enviados nos arquivos XML, pois isso
  inviabilizaria a remessa. Essa informação foi confirmada por "Leandro Mendonça Andrade" 
  <leandro.andrade@cnj.jus.br> em 09/08/2016 às 08:39, no e-mail com assunto "Selo Justiça em 
  Números - Dúvida sobre TPUs": "Isso mesmo. Não é necessário enviar dados sobre 
  documento(tipoDocumento). Enviar somente dadosBasicos(tipoCabecalhoProcesso) e 
  movimento(tipoMovimentoProcessual).


* Processo/Classes,Movimentos,Assuntos: Ao identificar classes, movimentos e assuntos, a ferramenta
  verificará se as mesmas pertencem às TPUs "globais" do CNJ, sem levar em consideração somente as
  tabelas da Justiça do Trabalho. Isso foi esclarecido no e-mail "Selo Justiça em Números - Dúvida 
  sobre TPUs". 
  * Pergunta: Devo usar somente as tabelas da JT (baixadas do site do CNJ, no botão "Gerar Excel", 
              ao lado de "Justiça do Trabalho") ou devo usar as tabelas "completas" do CNJ?
  * Resposta: A tabela completa.
  
* DE-PARA de assuntos: Como o PJe possui assuntos da JT que não possuem correspondentes na tabela
    nacional do CNJ, é possível fazer com que determinados assuntos sejam convertidos para outros
    assuntos antes do envio ao CNJ. Isso  é possível, a partir do arquivo `src/main/resources/assuntos_de-para/assuntos_de-para.properties`.


* Parte: não estão sendo tratados os casos de representação ou substituição processual em ações 
  coletivas, tutela e curatela.


* Parte/Sexo: O PJe possui uma falha na remessa de processos entre instâncias, que faz com que grande parte das
  pessoas sejam remetidas sem informação de gênero/sexo (masculino / feminino). Por isso, o cadastro
  das pessoas, no 2o Grau, muitas vezes não possui essa informação.
  É possível, nas configurações, habilitar o parâmetro `contornar_falta_de_genero` para fazer com
  que a ferramenta tente consultar as informações do gênero na outra instância. 
  Observação: Esse comportamento também foi implementado para o sistema Legado.


* Parte/Interesse público: não estão sendo tratados os casos em que a parte é considerada um interesse 
  público abstrato cuja defesa está a cargo do Ministério Público ou da Defensoria Pública, conforme 
  campo "interessePublico" das partes do processo. No XSD, esse campo não é obrigatório.


* Parte/Relacionamentos: não estão sendo tratados os casos de representação e assistência dos pais, 
  representação ou substituição processual em ações coletivas, inventariantes, tutela e curatela. No XSD, esse campo 
  não é obrigatório. Ao executar a ferramenta "replicacao-client", do CNJ, esses dados não são 
  enviados, de qualquer forma.
  

* Parte/Documentos: somente estão sendo inseridos nos arquivos XML os documentos que possuem correspondência
  nos tipos do CNJ, conforme arquivo "tipos_de_documentos.properties". Os documentos do PJe que não possuírem
  correspondência serão ignorados, ex: "RGE".
  * OBS: está sendo gerado um warning nos logs ao encontrar um documento que não possua correspondência nas
       tabelas do CNJ
  * OBS: alguns documentos podem existir somente no PJe e não existirem no CNJ, por exemplo, PFP  (Programa 
       de Formação de Patrimônio). Esses tipos podem ser inseridos no arquivo "properties" sem nenhum
       correspondende do "lado" do CNJ, para que sejam descartados na hora de enviar o arquivo XML.


* Assuntos: Quando o processo não tiver assunto cadastrado no PJe, ficará sem assunto no arquivo XML.
  * OBS: Se nenhum dos assuntos do processo estiver marcado como principal no PJe, a ferramenta marcará
       o primeiro assunto como principal no arquivo XML (orientação de Jeferson, via
       e-mail, em 28 de julho de 2016 12:43, no e-mail "Processos sem assunto")
  * OBS: É possível definir um assunto "padrão", a ser inserido nos processos que não tiverem nenhum
       assunto cadastrado. Ver documentação dos parâmetros `assunto_padrao_1G` e `assunto_padrao_2G`.
  * OBS: Está sendo gerado um warning nos logs quando processo não tiver assunto.
  
  
* Assuntos CNJ: O arquivo XSD do CNJ explicita que os assuntos devem ser preenchidos de forma diferente
  se fizerem ou não parte das tabelas nacionais. Por isso, o arquivo `assuntos_cnj.csv`, 
  na pasta `src/main/resources/tabelas_cnj`, contém listas com todos os assuntos do CNJ.
  Lista extraída de https://www.cnj.jus.br/sgt/versoes.php?tipo_tabela=A
  * OBS: Foram filtradas somente os assuntos a partir do terceiro nível da planilha, conforme trecho (abaixo) do e-mail com assunto
       "Fwd: Envio dos dados xml - informações do CNJ - Prêmio CNJ de Qualidade - Portaria CNJ nº 88/2019 artigo 8º inciso II":
       b.5) campos `tipoAssuntoProcessual.codigoNacional` e/ou `tipoAssuntoLocal.codigoPaiNacional` a partir do terceiro nível ou no último nível das TPUs;


* Movimentos CNJ: A mesma regra dos assuntos (que devem fazer parte da lista do CNJ) também se aplica
  aos movimentos processuais. Por isso, a lista dos movimentos processuais do CNJ foi gravada no arquivo 
  `movimentos_cnj.csv`. na pasta `src/main/resources/tabelas_cnj`, depois de 
  ser extraída da ferramenta "replicacao-client", do CNJ.
  
  * Serão aplicadas regras especiais aos movimentos e seus complementos, utilizando a ferramenta "depara-jt-cnj".
    Essa ferramenta mapeia os dados do PJe para os dados que o CNJ espera receber.
  

# Dúvidas / Esclarecimentos / Pendências


* Quanto ao período de extração dos dados:
  * Pergunta enviada ao CNJ: 
    Há uma divergência, na página do CNJ, a respeito do período dos dados que precisam ser extraídos:
    * A página "http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/perguntas-frequentes" 
      descreve que devem ser extraídos dados de processos baixados a partir de 01/01/2016.
    * A página "http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/2016-06-02-17-51-25" 
      descreve que devem ser extraídos dados de processos baixados a partir de 01/01/2015.
  * Resposta de davi.borges@cnj.jus.br em 26/07/2016: A partir de 01/01/2015


* Quanto à forma de seleção dos processos a serem exportados
  * Pergunta enviada ao CNJ: 
    A orientação do CNJ, para a carga "completa" de processos, é esta: "Para a carga completa devem ser encaminhados a 
    totalidade dos processos em tramitação em 31 de julho de 2016, bem como daqueles que foram baixados de 1° de janeiro de  2015 até 31 de julho de 2016". 
    Gostaríamos de esclarecer exatamente o conceito de processos "em tramitação" e processos "baixados". 
    Em ambos os casos, o PJe gera movimentos processuais. Por isso, não seria suficiente localizar 
    todos os processos que possuem algum movimento processual no período?
  * Resposta de davi.borges@cnj.jus.br em 26/07/2016: Os conceitos são equivalentes.


* Quanto ao Órgão Julgador do processo:
  * Pergunta enviada ao CNJ:
    O arquivo XSD define somente um campo "orgaoJulgador" para cada processo. Ocorre que, no PJe, 
    existe tanto o conceito de "Órgão Julgador" (normalmente um gabinete) quanto "Órgão Julgador 
    Colegiado" (turmas ou seções especializadas). Estamos mandando, portanto, somente a informação 
    do gabinete. Nosso entendimento está correto?
  * Resposta de davi.borges@cnj.jus.br em 26/07/2016: Sim.


* Quanto à estrutura do arquivo XML:
  * Pergunta enviada ao CNJ:
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
  * Resposta de leandro.andrade@cnj.jus.br em 26/07/2016: 
    Seguir o modelo replicação-nacional.xsd. Esse encapsula o intercomunicação-2.2.2.xsd.
    O arquivo está disponível em "https://www.cnj.jus.br/owncloud/....." (ver "Instruções básicas", neste arquivo README)


* Processos sem assunto cadastrado (ou sem assunto principal) poderão gerar erros no envio dos dados
  ao CNJ. Por isso, é possível executar a consulta abaixo, para identificar processos que estejam
  sem assunto ou sem assunto principal:
```
        WITH processos AS (
           SELECT 
              proc.nr_processo, 
              EXISTS(SELECT 1 FROM tb_processo_assunto pa WHERE pa.id_processo_trf = proc.id_processo) as existe_assunto,
              EXISTS(SELECT 1 FROM tb_processo_assunto pa WHERE pa.id_processo_trf = proc.id_processo AND pa.in_assunto_principal='S') as existe_assunto_principal
           FROM tb_processo proc
           WHERE length(proc.nr_processo) = 25
              --AND EXISTS (SELECT 1 FROM tb_processo_evento pe WHERE proc.id_processo = pe.id_processo AND pe.dt_atualizacao BETWEEN '2015-01-01 00:00:00.000' AND '2016-12-31 23:59:59.999') -- Condição para que o processo seja exportado ao CNJ
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
        ORDER BY nr_processo
```
  
* Quanto ao formato das partes dos tipos "Ministério Público do Trabalho" e "Órgão Público"
  * Dúvida enviada ao CNJ em "3 de julho de 2017 15:36", com assunto "Dúvidas sobre envio de dados para Justiça em Números"
      Estou gerando os dados para o Selo Justiça em Números aqui no TRT4. Ocorre que o MNI 2.2, utilizado como referência, 
      permite somente o envio de dados de pessoas dos tipos "Física", "Jurídica", "Autoridade" e "Órgão de Representação".
      No PJe, os tipos são parecidos, mas ligeiramente diferentes: "Física", "Jurídica", "Autoridade", "MPT" e "Órgão Público". 
      Quanto aos três primeiros, vejo que eles tem correspondência direta, mas fiquei na dúvida sobre como tratar os outros dois! 
      Preciso, para cada pessoa do "lado" do PJe, encontrar um correspondente do "lado" do MNI.
      Vocês saberiam me informar qual o tipo correto, no padrão MNI, para enviar partes dos tipos "MPT" e "Órgão Público"?
  * Resposta do CNJ (estatistica@cnj.jus.br), em "28 de julho de 2017 18:10"
      Sugerimos enquadrar tanto o MPT como os órgãos públicos sem personalidade jurídica própria como “Órgãos de Representação”.


* Quanto ao campo "tipoRelacaoIncidental":
  * Dúvida enviada ao CNJ em "26 de mar. de 2020 15:29", com assunto "Fwd: Enc: Justiça em Números: Serviço Fora do Ar"
      Em qual dos processos (principal ou incidental) que eu devo preencher essa informação? 
      O processo principal que deve conter uma lista com os seus incidentais ou o processo incidental que deve referenciar o seu principal? 
      Outra coisa: esse campo normalmente indica uma associação entre um processo principal e um incidental. 
      Então, qual o valor deve ser preenchido (PP ou PI)?
  * Resposta do CNJ (rosfran.borges@cnj.jus.br):
      O tipoRelacaoIncidental serve para qualquer caso, seja para informar a existência de incidentais num processo principal, 
      seja pra informar um processo principal, num incidental. Desse modo o atributo tipoRelacao informa o que o processo
      informado é em relação ao processo atual;


* Quanto ao campo "siglaTribunal":
  * Dúvida enviada ao CNJ em "26 de mar. de 2020 15:29", com assunto "Fwd: Enc: Justiça em Números: Serviço Fora do Ar"
      Esse é o mesmo valor utilizado para autenticar na API (no nosso caso, TRT4)?
  * Resposta do CNJ (rosfran.borges@cnj.jus.br):
      Sim, siglaTribunal é o mesmo usado para logar;


* Quanto ao campo "magistradoProlator":
  * Dúvida enviada ao CNJ em "26 de mar. de 2020 15:29", com assunto "Fwd: Enc: Justiça em Números: Serviço Fora do Ar"
      Como devo identificar o Magistrado (nome, CPF ou outro campo)?
      A descrição fala sobre quem prolatou a sentença. Esse dado deve ser preenchido somente para os movimentos de prolação 
      de sentença ou para todos os movimentos processuais?
  * Resposta do CNJ (rosfran.borges@cnj.jus.br):
      Conforme indicado na própria documentação, magistradoProlator é um CPF;
      (Deve ser preenchido) só pra quem prolatou a sentença/acórdão;


* Quanto ao campo "descricaoComplemento":
  * Dúvida enviada ao CNJ em "26 de mar. de 2020 15:29", com assunto "Fwd: Enc: Justiça em Números: Serviço Fora do Ar"
      Devo preencher a descrição do complemento (ex: "motivo_da_remessa") ou efetivamente o conteúdo do complemento (ex: "em grau de recurso").
  * Resposta do CNJ (rosfran.borges@cnj.jus.br):
      Atributo descricaoComplemento seria motivo_da_remessa, não o valor de livre preenchimento;


# Referências

Essa ferramenta foi desenvolvida a partir de diversas fontes, ex:
* Página do CNJ com últimas orientações sobre o envio ao DataJud: https://www.cnj.jus.br/pesquisas-judiciarias/premio-cnj-de-qualidade/orientacoes-para-envio-via-servico-rest/
* Painel de qualificação dos dados enviados (mesmo usuário do envio de XMLs, senha informada pela área de estratégia): https://painel-qualificacao.cnj.jus.br/
* Painel de comparação (mesmo usuário do envio de XMLs, senha informada pela área de estratégia): https://painel-comparacao.prd.cnj.cloud/painel-comparativo 

Outras referências:
* Script recebidos do TRT14 (Felypp De Assis Oliveira), gravados na pasta "documentacao/referencias/trt14"
* Página do CNJ:
  * http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros/2016-06-02-17-51-25
  * http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros/selo-justica-em-numeros
  * http://www.cnj.jus.br/programas-e-acoes/pj-justica-em-numeros
* Wiki do CSJT: https://pje.csjt.jus.br/documentacao/index.php/Scripts_estatisticas_regionais
* E-mail "Re: Selo Justiça em Números" (Felypp, atual)
* E-mail "Justiça em Números", de 2015 (Crisostomo Koling)
* E-mail "Justiça em Números - XML 2015" (Felipe Cesar Stanzani Fonseca)
* http://cnj.jus.br/noticias/judiciario/789-acoes-e-programas/programas-de-a-a-z/eficiencia-modernizacao-e-transparencia/justica-em-numeros/selo-justica-em-numeros
* E-mail "Fwd: Enc: Justiça em Números: Serviço Fora do Ar" (Carlos Fontoura)
