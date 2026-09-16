CREATE TABLE evento (
    id UUID PRIMARY KEY,
    objeto_id VARCHAR(4) NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    payload JSONB NOT NULL,
    versao INT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    CONSTRAINT evento_versao_uk UNIQUE (objeto_id, versao)
);

CREATE INDEX evento_objeto_idx ON evento (objeto_id, versao);

CREATE TABLE saga_inbox (
    id BIGSERIAL PRIMARY KEY,
    saga_id VARCHAR(64) NOT NULL,
    tipo VARCHAR(80) NOT NULL,
    reply_json TEXT NOT NULL,
    CONSTRAINT saga_inbox_uk UNIQUE (saga_id, tipo)
);
