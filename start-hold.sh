#!/usr/bin/env bash
# Igual ao start.sh, mas mantém esta sessão WSL viva (evita SIGTERM nos containers).
set -euo pipefail
cd "$(dirname "$0")"

while IFS= read -r svc; do
  [[ -z "$svc" ]] && continue
  echo "==> docker compose build ${svc}"
  docker compose build "$svc"
done < <(docker compose config --services)

docker compose up -d
echo ""
echo "Frota no ar. Gateway: http://localhost:3000/health"
echo "Mantenha ESTE terminal aberto. Ctrl+C encerra o sleep (containers podem parar no WSL)."
exec sleep infinity
