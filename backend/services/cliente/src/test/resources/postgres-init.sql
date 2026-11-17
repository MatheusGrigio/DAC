CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE COLLATION IF NOT EXISTS "pt-BR-x-icu" (
    provider = icu,
    locale = 'pt-BR'
);

CREATE SCHEMA IF NOT EXISTS cliente;
