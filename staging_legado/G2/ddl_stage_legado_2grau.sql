--------------------------------------------------------------
--------------------------------------------------------------
------------------------STAGE_LEGADO_2GRAU--------------------
--------------------------------------------------------------
----------------------------PESSOA----------------------------
--------------------------------------------------------------
-- SEQUENCE: stage_legado_2grau.pessoa_id_pessoa_seq

-- DROP SEQUENCE stage_legado_2grau.pessoa_id_pessoa_seq;

CREATE SEQUENCE stage_legado_2grau.pessoa_id_pessoa_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

ALTER SEQUENCE stage_legado_2grau.pessoa_id_pessoa_seq
    OWNER TO postgres;

-- Table: stage_legado_2grau.pessoa

-- DROP TABLE stage_legado_2grau.pessoa;

CREATE TABLE stage_legado_2grau.pessoa
(
    cd_pessoa character varying(24) COLLATE pg_catalog."default" NOT NULL,
    ds_nome character varying(255) COLLATE pg_catalog."default",
    in_tipo_pessoa character(1) COLLATE pg_catalog."default",
    nr_documento character varying(30) COLLATE pg_catalog."default",
    cd_tp_documento_identificacao character(3) COLLATE pg_catalog."default",
    nm_logradouro character varying(200) COLLATE pg_catalog."default",
    ds_complemento character varying(100) COLLATE pg_catalog."default",
    nm_bairro character varying(100) COLLATE pg_catalog."default",
    ds_municipio character varying(50) COLLATE pg_catalog."default",
    cd_estado character(2) COLLATE pg_catalog."default",
    nr_cep character varying(8) COLLATE pg_catalog."default",
    nr_oab character varying(24) COLLATE pg_catalog."default",
    is_advogado character(1) COLLATE pg_catalog."default",
    id_pessoa integer NOT NULL DEFAULT nextval('stage_legado_2grau.pessoa_id_pessoa_seq'::regclass),
    rg character varying(20) COLLATE pg_catalog."default",
    orgao_expedidor_rg character varying(30) COLLATE pg_catalog."default",
    nr_cgc character varying(30) COLLATE pg_catalog."default",
    data_nascimento date,
    ds_nome_genitora character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT pessoa_pkey PRIMARY KEY (cd_pessoa)
        USING INDEX TABLESPACE pje1tsd01
)
WITH (
    OIDS = FALSE
)
TABLESPACE pje1tsd01;

ALTER TABLE stage_legado_2grau.pessoa
    OWNER to postgres;

COMMENT ON COLUMN stage_legado_2grau.pessoa.cd_pessoa
    IS 'id que identifica a pessoa (cdparte ou chpatrono)';

COMMENT ON COLUMN stage_legado_2grau.pessoa.ds_nome
    IS 'nome da pessoa';

COMMENT ON COLUMN stage_legado_2grau.pessoa.in_tipo_pessoa
    IS 'indica o tipo da pessoa F ou J física ou jurídica';

COMMENT ON COLUMN stage_legado_2grau.pessoa.nr_documento
    IS 'número do documento';

COMMENT ON COLUMN stage_legado_2grau.pessoa.cd_tp_documento_identificacao
    IS 'tipo de documento (CPF, CPJ)';

COMMENT ON COLUMN stage_legado_2grau.pessoa.nm_logradouro
    IS 'Endereço da parte';

COMMENT ON COLUMN stage_legado_2grau.pessoa.ds_complemento
    IS 'complemento do endereço';

COMMENT ON COLUMN stage_legado_2grau.pessoa.nm_bairro
    IS 'bairro';

COMMENT ON COLUMN stage_legado_2grau.pessoa.ds_municipio
    IS 'municipio';

COMMENT ON COLUMN stage_legado_2grau.pessoa.cd_estado
    IS 'sigla do estado - 2 letras (PE, etc)';

COMMENT ON COLUMN stage_legado_2grau.pessoa.nr_cep
    IS 'cep do endereço';

COMMENT ON COLUMN stage_legado_2grau.pessoa.rg
    IS 'Número da identidade';

COMMENT ON COLUMN stage_legado_2grau.pessoa.orgao_expedidor_rg
    IS 'Órgão Expedidor da identidade';

COMMENT ON COLUMN stage_legado_2grau.pessoa.nr_cgc
    IS 'Número do CNPJ ';

COMMENT ON COLUMN stage_legado_2grau.pessoa.data_nascimento
    IS 'Data de nascimento da pessoa';

COMMENT ON COLUMN stage_legado_2grau.pessoa.ds_nome_genitora
    IS 'Nome da genitora';
--------------------------------------------------------------
--------------------------------------------------------------
--------------------------PROCESSO----------------------------
--------------------------------------------------------------
--------------------------------------------------------------
-- Table: stage_legado_2grau.processo

-- DROP TABLE stage_legado_2grau.processo;

CREATE TABLE stage_legado_2grau.processo
(
    nr_processo character varying(30) COLLATE pg_catalog."default",
    cd_processo numeric(20,0) NOT NULL,
    in_segredo_justica character(1) COLLATE pg_catalog."default",
    cd_classe_judicial character varying(15) COLLATE pg_catalog."default",
    in_recursal character(1) COLLATE pg_catalog."default",
    id_municipio_ibge character varying(7) COLLATE pg_catalog."default",
    dt_autuacao timestamp without time zone,
    cd_orgao_julgador integer,
    ds_orgao_julgador text COLLATE pg_catalog."default",
    ds_orgao_julgador_colegiado text COLLATE pg_catalog."default",
    vl_causa numeric(12,2),
    cd_proc_referencia numeric(20,0),
    nr_processo_ref character varying(30) COLLATE pg_catalog."default",
    cd_classe_judicial_ref character varying(15) COLLATE pg_catalog."default",
    ds_classe_judicial_ref character varying(100) COLLATE pg_catalog."default",
    grau character(1) COLLATE pg_catalog."default",
    nrcnj character varying(20) COLLATE pg_catalog."default",
    proc_localizado_siaj character(1) COLLATE pg_catalog."default" DEFAULT 'N'::bpchar,
    proc_hibrido character(1) COLLATE pg_catalog."default" DEFAULT 'N'::bpchar,
    proc_migrado character(1) COLLATE pg_catalog."default" DEFAULT 'N'::bpchar,
    proc_escopo_legado character(1) COLLATE pg_catalog."default" DEFAULT 'N'::bpchar,
    CONSTRAINT processo_pkey PRIMARY KEY (cd_processo)
        USING INDEX TABLESPACE pje1tsd01
)
WITH (
    OIDS = FALSE
)
TABLESPACE pje1tsd01;

ALTER TABLE stage_legado_2grau.processo
    OWNER to postgres;

COMMENT ON COLUMN stage_legado_2grau.processo.nr_processo
    IS 'processo com máscara';

COMMENT ON COLUMN stage_legado_2grau.processo.cd_processo
    IS 'processo sem máscara e to_number(nr_proceso)';

COMMENT ON COLUMN stage_legado_2grau.processo.in_segredo_justica
    IS 'se é segredo de justiça';

COMMENT ON COLUMN stage_legado_2grau.processo.cd_classe_judicial
    IS 'código da classe judicial (não é chave estrangeira)';

COMMENT ON COLUMN stage_legado_2grau.processo.in_recursal
    IS 'valor default para 1 grau será N';

COMMENT ON COLUMN stage_legado_2grau.processo.id_municipio_ibge
    IS 'código ibge do município';

COMMENT ON COLUMN stage_legado_2grau.processo.dt_autuacao
    IS 'data de autuação do processo';

COMMENT ON COLUMN stage_legado_2grau.processo.cd_orgao_julgador
    IS 'código da serventia cadastrada no sistema de produtividade';

COMMENT ON COLUMN stage_legado_2grau.processo.ds_orgao_julgador
    IS 'descrição da serventia cadastrada no sistema de produtividade';

COMMENT ON COLUMN stage_legado_2grau.processo.vl_causa
    IS 'valor da causa do processo';

COMMENT ON COLUMN stage_legado_2grau.processo.cd_proc_referencia
    IS 'processo referencia sem máscara e to_number(nr_proceso)';

COMMENT ON COLUMN stage_legado_2grau.processo.nr_processo_ref
    IS 'processo referencia com máscara';

COMMENT ON COLUMN stage_legado_2grau.processo.cd_classe_judicial_ref
    IS 'código da classe judicial do processo referencia';

COMMENT ON COLUMN stage_legado_2grau.processo.ds_classe_judicial_ref
    IS 'descrição da classe judicial (não existe no siaj)';

COMMENT ON COLUMN stage_legado_2grau.processo.grau
    IS 'grau de referência que o processo veio';

COMMENT ON COLUMN stage_legado_2grau.processo.nrcnj
    IS 'Número do processo sem a máscara';

COMMENT ON COLUMN stage_legado_2grau.processo.proc_hibrido
    IS 'Processo que está no escopo do PJe e que foi migrado';

COMMENT ON COLUMN stage_legado_2grau.processo.proc_migrado
    IS 'Processos que estão na clet. Foram migrados do legado para o PJe';

COMMENT ON COLUMN stage_legado_2grau.processo.proc_escopo_legado
    IS 'Processo do legado que faz parte do escopo';	
--------------------------------------------------------------
--------------------------------------------------------------
--------------------------PROCESSO_ASSUNTO--------------------
--------------------------------------------------------------
--------------------------------------------------------------
-- SEQUENCE: stage_legado_2grau.processo_assunto_cd_processo_assunto_seq

-- DROP SEQUENCE stage_legado_2grau.processo_assunto_cd_processo_assunto_seq;

CREATE SEQUENCE stage_legado_2grau.processo_assunto_cd_processo_assunto_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

ALTER SEQUENCE stage_legado_2grau.processo_assunto_cd_processo_assunto_seq
    OWNER TO postgres;

-- Table: stage_legado_2grau.processo_assunto

-- DROP TABLE stage_legado_2grau.processo_assunto;

CREATE TABLE stage_legado_2grau.processo_assunto
(
    in_assunto_principal character(1) COLLATE pg_catalog."default",
    cd_assunto_nacional character varying(30) COLLATE pg_catalog."default",
    cd_processo_assunto integer NOT NULL DEFAULT nextval('stage_legado_2grau.processo_assunto_cd_processo_assunto_seq'::regclass),
    cd_processo numeric(20,0),
    CONSTRAINT processo_assunto_pkey PRIMARY KEY (cd_processo_assunto)
        USING INDEX TABLESPACE pje1tsd01,
    CONSTRAINT fk_processo FOREIGN KEY (cd_processo)
        REFERENCES stage_legado_2grau.processo (cd_processo) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pje1tsd01;

ALTER TABLE stage_legado_2grau.processo_assunto
    OWNER to postgres;

COMMENT ON COLUMN stage_legado_2grau.processo_assunto.in_assunto_principal
    IS 'se o assunto é principal (S/N)';

COMMENT ON COLUMN stage_legado_2grau.processo_assunto.cd_assunto_nacional
    IS 'código do assunto nacional CNJ';

COMMENT ON COLUMN stage_legado_2grau.processo_assunto.cd_processo_assunto
    IS 'PK sequence da tabela processo_assunto';

--------------------------------------------------------------
--------------------------------------------------------------
------------------PROCESSO_MOVIMENTO--------------------------
--------------------------------------------------------------
--------------------------------------------------------------
-- SEQUENCE: stage_legado_2grau.processo_movimento_cd_processo_movimento_seq

-- DROP SEQUENCE stage_legado_2grau.processo_movimento_cd_processo_movimento_seq;

CREATE SEQUENCE stage_legado_2grau.processo_movimento_cd_processo_movimento_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

ALTER SEQUENCE stage_legado_2grau.processo_movimento_cd_processo_movimento_seq
    OWNER TO postgres;
	
-- Table: stage_legado_2grau.processo_movimento

-- DROP TABLE stage_legado_2grau.processo_movimento;

CREATE TABLE stage_legado_2grau.processo_movimento
(
    cd_processo_movimento integer NOT NULL DEFAULT nextval('stage_legado_2grau.processo_movimento_cd_processo_movimento_seq'::regclass),
    cd_movimento_cnj integer,
    ds_login character varying(20) COLLATE pg_catalog."default",
    is_magistrado_julgamento boolean,
    dt_atualizacao timestamp without time zone,
    tipo_historico character(6) COLLATE pg_catalog."default",
    jurisdicao character(30) COLLATE pg_catalog."default",
    aositprazo character(30) COLLATE pg_catalog."default",
    dtentrada date,
    nome_junta character(100) COLLATE pg_catalog."default",
    txobservacao character(200) COLLATE pg_catalog."default",
    cd_processo numeric(20,0),
    cdusuario character varying(30) COLLATE pg_catalog."default",
    cdjuizresp character varying(30) COLLATE pg_catalog."default",
    cd_magistrado character varying(20) COLLATE pg_catalog."default",
    descricao_localidade character varying(255) COLLATE pg_catalog."default",
    chhistorico character varying(10) COLLATE pg_catalog."default",
    CONSTRAINT processo_movimento_pkey PRIMARY KEY (cd_processo_movimento)
        USING INDEX TABLESPACE pje1tsd01,
    CONSTRAINT fk_processo FOREIGN KEY (cd_processo)
        REFERENCES stage_legado_2grau.processo (cd_processo) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pje1tsd01;

ALTER TABLE stage_legado_2grau.processo_movimento
    OWNER to postgres;

COMMENT ON COLUMN stage_legado_2grau.processo_movimento.descricao_localidade
    IS 'Descrição da localidade, utilizado no complemento 7 do movimento 123';

COMMENT ON COLUMN stage_legado_2grau.processo_movimento.chhistorico
    IS 'FK da tabela historico do siaj2';
-- Index: idx_proc_mov_cnj

-- DROP INDEX stage_legado_2grau.idx_proc_mov_cnj;

CREATE INDEX idx_proc_mov_cnj
    ON stage_legado_2grau.processo_movimento USING btree
    (cd_movimento_cnj ASC NULLS LAST)
    TABLESPACE pje1tsd01;
-- Index: idx_proc_mov_tipo_historico

-- DROP INDEX stage_legado_2grau.idx_proc_mov_tipo_historico;

CREATE INDEX idx_proc_mov_tipo_historico
    ON stage_legado_2grau.processo_movimento USING btree
    (tipo_historico COLLATE pg_catalog."default" ASC NULLS LAST)
    TABLESPACE pje1tsd01;
-- Index: idx_processo_movimento_01

-- DROP INDEX stage_legado_2grau.idx_processo_movimento_01;

CREATE INDEX idx_processo_movimento_01
    ON stage_legado_2grau.processo_movimento USING btree
    (cdusuario COLLATE pg_catalog."default" ASC NULLS LAST)
    TABLESPACE pje1tsd01;
-- Index: idx_processo_movimento_02

-- DROP INDEX stage_legado_2grau.idx_processo_movimento_02;

CREATE INDEX idx_processo_movimento_02
    ON stage_legado_2grau.processo_movimento USING btree
    (cdjuizresp COLLATE pg_catalog."default" ASC NULLS LAST)
    TABLESPACE pje1tsd01;
--------------------------------------------------------------
--------------------------------------------------------------
------------------PROCESSO_MOVIMENTO_COMPLEMENTO--------------
--------------------------------------------------------------
--------------------------------------------------------------
-- SEQUENCE: stage_legado_2grau.proc_mov_compl_cd_proc_mov_compl_seq

-- DROP SEQUENCE stage_legado_2grau.proc_mov_compl_cd_proc_mov_compl_seq;

CREATE SEQUENCE stage_legado_2grau.proc_mov_compl_cd_proc_mov_compl_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

ALTER SEQUENCE stage_legado_2grau.proc_mov_compl_cd_proc_mov_compl_seq
    OWNER TO postgres;
	
-- Table: stage_legado_2grau.processo_movimento_complemento

-- DROP TABLE stage_legado_2grau.processo_movimento_complemento;

CREATE TABLE stage_legado_2grau.processo_movimento_complemento
(
    cd_processo_movimento_complemento integer NOT NULL DEFAULT nextval('stage_legado_2grau.proc_mov_compl_cd_proc_mov_compl_seq'::regclass),
    cd_processo_movimento integer,
    cd_tipo_complemento character varying(255) COLLATE pg_catalog."default",
    cd_complemento character varying(255) COLLATE pg_catalog."default",
    ds_valor_complemento text COLLATE pg_catalog."default",
    CONSTRAINT processo_movimento_complemento_pkey PRIMARY KEY (cd_processo_movimento_complemento)
        USING INDEX TABLESPACE pje1tsd01,
    CONSTRAINT fk_processo_movimento FOREIGN KEY (cd_processo_movimento)
        REFERENCES stage_legado_2grau.processo_movimento (cd_processo_movimento) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pje1tsd01;

ALTER TABLE stage_legado_2grau.processo_movimento_complemento
    OWNER to postgres;

COMMENT ON COLUMN stage_legado_2grau.processo_movimento_complemento.cd_processo_movimento_complemento
    IS 'PK da tabela';

COMMENT ON COLUMN stage_legado_2grau.processo_movimento_complemento.cd_processo_movimento
    IS 'FK para tabela PROCESSO_MOVIMENTO';

COMMENT ON COLUMN stage_legado_2grau.processo_movimento_complemento.cd_tipo_complemento
    IS 'FK para sgt_consulta.complemento.seq_complemento';

COMMENT ON COLUMN stage_legado_2grau.processo_movimento_complemento.cd_complemento
    IS 'Dados do complemento';

COMMENT ON COLUMN stage_legado_2grau.processo_movimento_complemento.ds_valor_complemento
    IS 'Dados do complemento';
-- Index: idx_proc_mov_comp_tipo

-- DROP INDEX stage_legado_2grau.idx_proc_mov_comp_tipo;

CREATE INDEX idx_proc_mov_comp_tipo
    ON stage_legado_2grau.processo_movimento_complemento USING btree
    (cd_tipo_complemento COLLATE pg_catalog."default" ASC NULLS LAST)
    TABLESPACE pje1tsd01;
--------------------------------------------------------------
--------------------------------------------------------------
------------------PROCESSO_DESLOCAMENTO-----------------------
--------------------------------------------------------------
--------------------------------------------------------------
-- SEQUENCE: stage_legado_2grau.processo_deslocamento_cd_processo_deslocamento_seq

-- DROP SEQUENCE stage_legado_2grau.processo_deslocamento_cd_processo_deslocamento_seq;

CREATE SEQUENCE stage_legado_2grau.processo_deslocamento_cd_processo_deslocamento_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

ALTER SEQUENCE stage_legado_2grau.processo_deslocamento_cd_processo_deslocamento_seq
    OWNER TO postgres;
	
-- Table: stage_legado_2grau.processo_deslocamento

-- DROP TABLE stage_legado_2grau.processo_deslocamento;

CREATE TABLE stage_legado_2grau.processo_deslocamento
(
    cd_processo numeric(20,0),
    cd_processo_deslocamento integer NOT NULL DEFAULT nextval('stage_legado_2grau.processo_deslocamento_cd_processo_deslocamento_seq'::regclass),
    id_oj_origem integer,
    ds_oj_origem text COLLATE pg_catalog."default",
    id_oj_destino integer,
    ds_oj_destino text COLLATE pg_catalog."default",
    dt_deslocamento timestamp without time zone,
    dt_retorno timestamp without time zone,
    id_municipio_origem character varying(7) COLLATE pg_catalog."default",
    id_municipio_destino character varying(7) COLLATE pg_catalog."default",
    CONSTRAINT processo_deslocamento_pkey PRIMARY KEY (cd_processo_deslocamento)
        USING INDEX TABLESPACE pje1tsd01,
    CONSTRAINT "FK_processo" FOREIGN KEY (cd_processo)
        REFERENCES stage_legado_2grau.processo (cd_processo) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
        NOT VALID
)
WITH (
    OIDS = FALSE
)
TABLESPACE pje1tsd01;

ALTER TABLE stage_legado_2grau.processo_deslocamento
    OWNER to postgres;

COMMENT ON TABLE stage_legado_2grau.processo_deslocamento
    IS 'Tabela que armazena o deslocamento de um processo pelos órgãos julgadores';

COMMENT ON CONSTRAINT "FK_processo" ON stage_legado_2grau.processo_deslocamento
    IS 'Referência para a tabela Processo';

--------------------------------------------------------------
--------------------------------------------------------------
--------------------------PROCESSO_PARTE----------------------
--------------------------------------------------------------
--------------------------------------------------------------
-- SEQUENCE: stage_legado_2grau.processo_parte_tmp_id_processo_parte_seq

-- DROP SEQUENCE stage_legado_2grau.processo_parte_tmp_id_processo_parte_seq;

CREATE SEQUENCE stage_legado_2grau.processo_parte_tmp_id_processo_parte_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

ALTER SEQUENCE stage_legado_2grau.processo_parte_tmp_id_processo_parte_seq
    OWNER TO postgres;

-- Table: stage_legado_2grau.processo_parte

-- DROP TABLE stage_legado_2grau.processo_parte;

CREATE TABLE stage_legado_2grau.processo_parte
(
    cd_processo_parte character varying(68) COLLATE pg_catalog."default" NOT NULL,
    cd_pessoa character varying(24) COLLATE pg_catalog."default",
    in_participacao character(1) COLLATE pg_catalog."default",
    ds_tipo_parte_representante character varying(50) COLLATE pg_catalog."default",
    cd_proc_parte_representante character varying(68) COLLATE pg_catalog."default",
    cd_processo numeric(20,0),
    in_recebe_intimacao character(1) COLLATE pg_catalog."default",
    in_parte_principal character(1) COLLATE pg_catalog."default",
    id_processo_parte integer NOT NULL DEFAULT nextval('stage_legado_2grau.processo_parte_tmp_id_processo_parte_seq'::regclass),
    CONSTRAINT processo_parte_pkey PRIMARY KEY (cd_processo_parte)
        USING INDEX TABLESPACE pje1tsd01,
    CONSTRAINT fk_pessoa FOREIGN KEY (cd_pessoa)
        REFERENCES stage_legado_2grau.pessoa (cd_pessoa) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_processo FOREIGN KEY (cd_processo)
        REFERENCES stage_legado_2grau.processo (cd_processo) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pje1tsd01;

ALTER TABLE stage_legado_2grau.processo_parte
    OWNER to postgres;

COMMENT ON COLUMN stage_legado_2grau.processo_parte.cd_processo_parte
    IS 'PK formado pela junção do cd_processo+cd_pessoa';

COMMENT ON COLUMN stage_legado_2grau.processo_parte.cd_pessoa
    IS 'FK para a tabela pessoa';

COMMENT ON COLUMN stage_legado_2grau.processo_parte.in_participacao
    IS 'Indica se a parte é A ou P ativa ou passiva';

COMMENT ON COLUMN stage_legado_2grau.processo_parte.ds_tipo_parte_representante
    IS 'descrição do tipo da parte (ADVOGADO, ETC) (aplicável apenas para o representante)';

COMMENT ON COLUMN stage_legado_2grau.processo_parte.cd_proc_parte_representante
    IS 'Auto relacionamento com PROCESSO_PARTE';

COMMENT ON COLUMN stage_legado_2grau.processo_parte.cd_processo
    IS 'FK para a tabela processo';

COMMENT ON COLUMN stage_legado_2grau.processo_parte.in_parte_principal
    IS 'Indica se a parte é parte Principal S/N';