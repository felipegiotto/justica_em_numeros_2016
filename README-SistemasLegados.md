# Passos para permitir o envio de dados de processos do Sistema Judicial Legado

Observações: 
* Como as bases dos nossos sistemas legados no primeiro e no segundo grau são diferentes, para facilitar a integração do Sistema Judicial Legado com a ferramenta, foram geradas duas bases no postgres (uma para o primeiro e outra para o segundo grau) que armazenam as informações dos processos dos Sistemas Legados, após um processamento via pentaho. O DDL e os arquivos do pentaho criados pelo TRT6 se encontram na pasta 'staging_legado'. Cada Regional poderá criar sua própria estrutura de dados, basta ajustar as consultas presentes nos seguintes arquivos:
  * '/resources/sql/op_1_baixa_lista_processos/legado/*'
  * '/resources/sql/op_2_gera_xmls/legado/*'

1. Três parâmetros precisam ser informados no arquivo 'config.properties':
  * url_legado_1g: Indica a base postgres que armazena as informações relacionadas aos processos no 1º Grau do Sistema Judicial Legado;
  * url_legado_2g Indica a base postgres que armazena as informações relacionadas aos processos no 2º Grau do Sistema Judicial Legado;
  * sistema_judicial: Especifica o sistema judicial (Pje e Legado) que será utilizado na construção dos XMLs. Os valores possíveis são:
    * APENAS_LEGADO: Os XMLs serão gerados apenas para os processos que forem recuperados do Sistema Judicial Legado e que não foram migrados para o PJe;
    * APENAS_PJE: Os XMLs serão gerados apenas para os processos que forem recuperados do PJe. Nenhuma informação é recuperada do Sistema Judicial Legado;
    * APENAS_PJE_COM_MIGRADOS_LEGADO: Os XMLs serão gerados apenas para os processos que forem recuperados do PJe. Informações de Movimentos e Complementos de processos que tiverem sido migrados do Sistema Judicial Legado para o PJe também serão recuperadas das bases legadas para um merge de informações;
    * TODOS: Os XMLs serão gerados para os processos que forem recuperados do PJe e do Sistema Judicial Legado. Informações de Movimentos e Complementos de processos que tiverem sido migrados do Sistema Judicial Legado para o PJe também serão recuperadas das bases legadas para um merge de informações.

2. Na primeira operação (Op_1_BaixaListaDeNumerosDeProcessos), o sistema gera uma ou mais listas de processos que serão armazenadas nas pastas 'output\COMPLETA\G1' para processos do primeiro grau e 'output\COMPLETA\G2' para processos do segundo grau. As lista são geradas de acordo com a configuração do parâmetro 'sistema_judicial'
  * No arquivo 'PJe_lista_processos.txt' são armazenados os números dos processos do PJe recuperados por uma das duas queries a seguir: 'resources/sql/op_1_baixa_lista_processos/pje/carga_completa_egestao_1g.sql' e  'resources/sql/op_1_baixa_lista_processos/pje/carga_completa_egestao_2g.sql';
  * No arquivo 'Legado_lista_processos_migrados.txt' são armazenados os números dos processos do Sistema Judicial Legado que já foram migrados para o PJe e que são recuperados pela query 'resources/sql/op_1_baixa_lista_processos/legado/../carga_completa_migrados.sql';
    * Essa query precisa ser ajustada pelo Regional, adequando ao seu modelo de dados do Sistema Legado.
  * No arquivo 'Legado_lista_processos_nao_migrados.txt' são armazenados os números dos processos do Sistema Judicial Legado que NÃO foram migrados para o PJe e que são recuperados pela query 'resources/sql/op_1_baixa_lista_processos/legado/../carga_completa_nao_migrados.sql'.
    * Essa query precisa ser ajustada pelo Regional, adequando ao seu modelo de dados do Sistema Legado.

3. Na operação Op_2_GeraEValidaXMLsIndividuais, o sistema utiliza as listas geradas na primeira operação para gerar os XMLs. 
  * Supondo que o parâmetro 'sistema_judicial' tenha sido informado com o valor 'TODOS', para o primeiro grau: 
    * Os XMLs dos processos do PJe seriam armazenados na pasta '\output\COMPLETA\G1\xmls_individuais\PJe'. Se tais processos tiverem sido migrados do Sistema Judicial Legado, as informações de movimentos e complementos da base do Legado também serão recuperadas e incluídas no XML do processo. 
    * Os XMLs dos processos do Sistema Judicial Legado que NÃO foram migrados para o PJe seriam armazenados na pasta '\output\COMPLETA\G1\xmls_individuais\Legado'. 
  * Para que a operação Op_2_GeraEValidaXMLsIndividuais possa ser utilizada para o Sistema Legado, o Regional precisa criar as queries localizadas no diretório '/resources/sql/op_2_gera_xmls/legado/*'. Os SELECTs devem retornar colunas com os mesmos nomes e tipos dos respectivos SELECTs localizados no diretório '/resources/sql/op_2_gera_xmls/pje/*';
    * A implementação da query '10_consulta_deslocamento_oj.sql' é opcional para o sistema legado. Tal consulta deve ser implementada para o 1º Grau quando o parâmetro 'possui_deslocamento_oj_legado_1g' tiver o valor SIM. E deve ser implementada para o 2º Grau quando o parâmetro 'possui_deslocamento_oj_legado_2g' tiver o valor SIM. Ler os comentários a respeito dos parâmetros no arquivo `config_modelo.properties` para mais informações.

Possíveis pontos de ajuste:
  * AnalisaAssuntosCNJ.java
    * Apenas Assuntos Nacionais do sistema legado serão enviados. Se o Regional deseja enviar assuntos locais, o método 'getAssunto' deve ser ajustado;
    * As informações dos 'assuntos' do sistema legado, salvas na base intermediária, devem estar com os valores esperados pelo CNJ. Logo, o de-para de assuntos não será aplicado para o sistema legado.
  * AnalisaMovimentosCNJ.java
    * Apenas Movimentos Nacionais do sistema legado serão enviados. Se o Regional deseja enviar movimentos locais, o método 'preencheDadosMovimentoCNJ' deve ser ajustado;
    * As informações dos 'movimentos' do sistema legado, salvas na base intermediária, devem estar com os valores esperados pelo CNJ. Logo, o de-para de movimentos não será aplicado para o sistema legado.
