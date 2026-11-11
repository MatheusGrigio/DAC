#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
docker compose up -d
for i in $(seq 1 90); do
  if curl -sf http://localhost:3000/health >/dev/null; then
    echo "GATEWAY_UP"
    break
  fi
  sleep 2
done
curl -sf http://localhost:3000/health || { echo "Gateway não subiu"; exit 1; }
cd backend/contract-tests
python3 -m pip install -q -r requirements.txt
python3 -m pytest -v --tb=line
cd "$ROOT/backend/gateway"
if command -v npm >/dev/null; then
  npm test
fi
