CREATE TABLE gerente (
    cpf VARCHAR(11) PRIMARY KEY,
    nome VARCHAR(200) NOT NULL,
    email VARCHAR(200) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL,
    CONSTRAINT gerente_email_uk UNIQUE (email)
);

CREATE INDEX gerente_ativo_idx ON gerente (ativo);

CREATE TABLE saga_inbox (
    id BIGSERIAL PRIMARY KEY,
    saga_id VARCHAR(64) NOT NULL,
    tipo VARCHAR(80) NOT NULL,
    reply_json TEXT NOT NULL,
    CONSTRAINT saga_inbox_uk UNIQUE (saga_id, tipo)
);
