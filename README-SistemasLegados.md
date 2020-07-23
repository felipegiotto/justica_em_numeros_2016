# Passos para permitir o envio de dados de processos do Sistema Judicial Legado

Observações: 
* A implementação foi realizada apenas para o tipo de carga COMPLETA. Em outro momento, faremos os ajustes para os outros tipos.
* Como as bases dos nossos sistemas legados no primeiro e no segundo grau são diferentes, para facilitar a integração do Sistema Judicial Legado com a ferramenta, foram geradas duas bases no postgres (uma para o primeiro e outra para o segundo grau) que armazenam as informações dos processos do Sistema Legado após um processamento via pentaho. Essa estrutura ainda está em construção, assim como as queries que serão utilizadas pelo extrator java. Cada Regional poderá criar sua própria estrutura de dados, basta ajustar as consultas presentes nos seguintes arquivos:
  * '/resources/sql/op_1_baixa_lista_processos/legado/carga_completa_migrados.sql'
  * '/resources/sql/op_1_baixa_lista_processos/legado/carga_completa_nao_migrados.sql'
  * '/resources/sql/op_2_gera_xmls/legado/*'

1. Três parâmetros precisam ser informados no arquivo 'config.properties':
  * url_legado_1g: Indica a base postgres que armazena as informações relacionadas aos processos no 1º Grau do Sistema Judicial Legado;
  * url_legado_2g Indica a base postgres que armazena as informações relacionadas aos processos no 2º Grau do Sistema Judicial Legado;
  * sistema_judicial: Especifica o sistema judicial (Pje e Legado) que será utilizado na construção dos XMLs. Os valores possíveis são:
    * APENAS_LEGADO: Os XMLs serão gerados apenas para os processos que forem recuperados do Sistema Judicial Legado e que não foram migrados para o PJe;
    * APENAS_PJE: Os XMLs serão gerados apenas para os processos que forem recuperados do PJe. Nenhuma informação é recuperada do Sistema Judicial Legado;
    * APENAS_PJE_COM_MIGRADOS_LEGADO: Os XMLs serão gerados apenas para os processos que forem recuperados do PJe. Informações de Movimentos e Complementos de processos que tiverem sido migrados do Sistema Judicial Legado para o PJe também serão recuperadas para um merge de informações;
    * TODOS: Os XMLs serão gerados para os processos que forem recuperados do PJe e do Sistema Judicial Legado. Informações de Movimentos e Complementos de processos que tiverem sido migrados do Sistema Judicial Legado para o PJe também serão recuperadas para um merge de informações.

2. Na primeira operação (Op_1_BaixaListaDeNumerosDeProcessos), o sistema gera uma ou mais listas de processos que serão armazenadas nas pastas 'output\COMPLETA\G1' para processos do primeiro grau e 'output\COMPLETA\G2' para processos do segundo grau. As lista são geradas de acordo com a configuração do parâmetro 'sistema_judicial'
  * No arquivo 'PJe_lista_processos.txt' são armazenados os números dos processos do PJe recuperados pelas queries 'resources/sql/op_1_baixa_lista_processos/pje/carga_completa_egestao_1g.sql e 'resources/sql/op_1_baixa_lista_processos/pje/carga_completa_egestao_2g.sql';
  * No arquivo 'Legado_lista_processos_migrados.txt' são armazenados os números dos processos do Sistema Judicial Legado que já foram migrados para o PJe e que são recuperados pela query 'resources/sql/op_1_baixa_lista_processos/legado/carga_completa_migrados.sql';
    * Essa query precisa ser ajustada pelo Regional, adequando ao modelo de dados do Sistema Legado.
  * No arquivo 'Legado_lista_processos_nao_migrados.txt' são armazenados os números dos processos do Sistema Judicial Legado que NÃO foram migrados para o PJe e que são recuperados pela query 'resources/sql/op_1_baixa_lista_processos/legado/carga_completa_nao_migrados.sql'.
    * Essa query precisa ser ajustada pelo Regional, adequando ao modelo de dados do Sistema Legado.

3. Na operação Op_2_GeraXMLsIndividuais, o sistema utiliza as listas geradas na primeira operação para gerar os XMLs. 
  * Supondo que o parâmetro 'sistema_judicial' tenha sido informado com o valor 'TODOS', para o primeiro grau: 
    * Os XMLs dos processos do PJe seriam armazenados na pasta '\output\COMPLETA\G1\xmls_individuais\PJe'. Se tais processos tiverem sido migrados do Sistema Judicial Legado, as informações de movimentos e complementos da base do Legado também serão recuperadas e incluídas no XML do processo. 
    * Os XMLs dos processos do Sistema Judicial Legado que NÃO foram migrados para o PJe seriam armazenados na pasta '\output\COMPLETA\G1\xmls_individuais\Legado'. 
  * Para que a operação Op_2_GeraXMLsIndividuais possa ser utilizada para o Sistema Legado, o Regional precisa criar as queries localizadas no diretório '/resources/sql/op_2_gera_xmls/legado/*'. Os SELECTs devem retornar colunas com os mesmos nomes e tipos dos respectivos SELECTs localizados no diretório '/resources/sql/op_2_gera_xmls/pje/*';
    * É possível notar que não foram criadas as queries '10_consulta_deslocamento_oj.sql' e 'consulta_genero_outra_instancia.sql' no diretório '/resources/sql/op_2_gera_xmls/legado/*'. Não existe histórico de deslocamento no legado do TRT6 e o sexo também não era informado nesse sistema. Desta forma, tais consultas não foram implementadas e esses itens foram tratados no código. Se o Sistema Legado do Regional possuir essas informações, tais consultas poderão ser criadas e o código da ferramenta ajustado.

Possíveis pontos de ajuste:
  * IdentificaDocumentosPessoa.java
    * Existe um arquivo de-para de documentos do PJe para o formato aceito pelo CNJ ('src/main/resources/tipos_de_documentos.properties'). Como os tipos do sistema legado do TRT6 já existem nesse arquivo, não foi criado um outro e a estrutura foi aproveitada para o legado. O ideal seria criar um arquivo similar para o legado, de modo de cada Regional preenchesse de acordo com a sua realidade. O código também precisaria ser ajustado. Outra possível solução seria a base intermediária do Legado já armazenar as informações no formato esperado pelo CNJ.
  * IdentificaGeneroPessoa.java
    * Para o sistema legado, o método 'preencherSexoPessoa', sempre informa que o sexo é 'Desconhecido', pois não temos essa informação em nossas bases. Se o Regional decidir implementar para o sistema legado uma consulta equivalente a '/resources/sql/op_2_gera_xmls/pje/consulta_genero_outra_instancia.sql', essa classe precisaria ser ajustada.
  * AnalisaAssuntosCNJ.java
    * Apenas Assuntos Nacionais do sistema legado serão enviados pelo TRT6. Se o Regional precisa enviar assuntos locais, o método 'getAssunto' deveria ser ajustado;
    * As informações dos 'assuntos' do sistema legado, salvas na base intermediária, devem estar com os valores esperados pelo CNJ. Logo, não é necessário aplicar o de-para de assuntos para o sistema legado.
  * AnalisaMovimentosCNJ.java
    * Apenas Movimentos Nacionais do sistema legado serão enviados pelo TRT6. Se o Regional precisa enviar movimentos locais, o método 'preencheDadosMovimentoCNJ' deveria ser ajustado;
    * As informações dos 'movimentos' do sistema legado, salvas na base intermediária, devem estar com os valores esperados pelo CNJ. Logo, não é necessário aplicar o de-para de movimentos para o sistema legado.
  * Op_2_GeraXMLsIndividuais.java
    * Não existe histórico de deslocamento no legado do TRT6 e órgão julgador dos movimentos é sempre o órgão julgador do processo. Se o Regional possuir esse histórico, uma consulta equivalete a '/resources/sql/op_2_gera_xmls/pje/10_consulta_deslocamento_oj.sql' precisaria ser implementada e três pontos nessa classe com a marcação 'FIXME' precisariam ser ajustados.
