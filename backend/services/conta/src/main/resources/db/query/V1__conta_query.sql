CREATE TABLE conta (
    numero VARCHAR(4) PRIMARY KEY,
    cpf_cliente VARCHAR(11) NOT NULL UNIQUE,
    cpf_gerente VARCHAR(11) NOT NULL,
    saldo NUMERIC(19, 4) NOT NULL,
    data_criacao DATE NOT NULL
);

CREATE INDEX conta_gerente_idx ON conta (cpf_gerente);

CREATE TABLE movimentacao (
    id UUID PRIMARY KEY,
    numero_conta VARCHAR(4) NOT NULL REFERENCES conta (numero) ON DELETE CASCADE,
    data_hora TIMESTAMP NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    valor NUMERIC(19, 4) NOT NULL,
    origem_numero VARCHAR(4),
    origem_cpf VARCHAR(11),
    origem_nome VARCHAR(120),
    destino_numero VARCHAR(4),
    destino_cpf VARCHAR(11),
    destino_nome VARCHAR(120)
);

CREATE INDEX movimentacao_conta_data_idx ON movimentacao (numero_conta, data_hora);

CREATE TABLE projecao_aplicada (
    evento_id UUID PRIMARY KEY
);
