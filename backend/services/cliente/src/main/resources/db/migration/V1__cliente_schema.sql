CREATE TABLE solicitacao (
    cpf VARCHAR(11) PRIMARY KEY,
    nome VARCHAR(200) NOT NULL,
    email VARCHAR(200) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    salario NUMERIC(19, 4) NOT NULL,
    logradouro VARCHAR(200) NOT NULL,
    numero VARCHAR(20) NOT NULL,
    complemento VARCHAR(200),
    cep VARCHAR(8) NOT NULL,
    cidade VARCHAR(120) NOT NULL,
    uf VARCHAR(2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    motivo TEXT,
    data_hora_processamento TIMESTAMP,
    CONSTRAINT solicitacao_email_uk UNIQUE (email),
    CONSTRAINT solicitacao_status_ck CHECK (status IN ('PENDENTE', 'APROVADA', 'NAO_APROVADA'))
);

CREATE INDEX solicitacao_status_idx ON solicitacao (status);

CREATE TABLE cliente (
    cpf VARCHAR(11) PRIMARY KEY,
    nome VARCHAR(200) NOT NULL,
    email VARCHAR(200) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    salario NUMERIC(19, 4) NOT NULL,
    logradouro VARCHAR(200) NOT NULL,
    numero VARCHAR(20) NOT NULL,
    complemento VARCHAR(200),
    cep VARCHAR(8) NOT NULL,
    cidade VARCHAR(120) NOT NULL,
    uf VARCHAR(2) NOT NULL,
    CONSTRAINT cliente_email_uk UNIQUE (email)
);

CREATE TABLE saga_inbox (
    id BIGSERIAL PRIMARY KEY,
    saga_id VARCHAR(64) NOT NULL,
    tipo VARCHAR(80) NOT NULL,
    reply_json TEXT NOT NULL,
    CONSTRAINT saga_inbox_uk UNIQUE (saga_id, tipo)
);
