#!/bin/bash
set -euo pipefail

create_schema_user() {
  local role="$1"
  local password="$2"
  local schema="$3"

  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<SQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${role}') THEN
    CREATE ROLE ${role} LOGIN PASSWORD '${password}';
  END IF;
END
\$\$;

GRANT USAGE, CREATE ON SCHEMA ${schema} TO ${role};
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA ${schema} TO ${role};
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA ${schema} TO ${role};
ALTER DEFAULT PRIVILEGES IN SCHEMA ${schema} GRANT ALL ON TABLES TO ${role};
ALTER DEFAULT PRIVILEGES IN SCHEMA ${schema} GRANT ALL ON SEQUENCES TO ${role};
ALTER ROLE ${role} SET search_path TO ${schema};
SQL
}

create_schema_user "$POSTGRES_CLIENTE_USER" "$POSTGRES_CLIENTE_PASSWORD" "cliente"
create_schema_user "$POSTGRES_GERENTE_USER" "$POSTGRES_GERENTE_PASSWORD" "gerente"
create_schema_user "$POSTGRES_CONTA_COMMAND_USER" "$POSTGRES_CONTA_COMMAND_PASSWORD" "conta_command"
create_schema_user "$POSTGRES_CONTA_QUERY_USER" "$POSTGRES_CONTA_QUERY_PASSWORD" "conta_query"
