#!/usr/bin/env bash
# Sobe containers ja buildados e segura a sessao WSL (sem rebuild).
set -euo pipefail
cd "$(dirname "$0")"
docker compose up -d
echo "Frota no ar: http://localhost:3000/health — mantenha este terminal aberto."
exec sleep infinity
