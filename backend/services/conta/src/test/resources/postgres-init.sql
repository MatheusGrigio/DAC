CREATE COLLATION IF NOT EXISTS "pt-BR-x-icu" (
    provider = icu,
    locale = 'pt-BR'
);

CREATE SCHEMA IF NOT EXISTS conta_command;
CREATE SCHEMA IF NOT EXISTS conta_query;
