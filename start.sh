#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

# This Compose build has no --parallel flag; build one service at a time to save RAM.
while IFS= read -r svc; do
  [[ -z "$svc" ]] && continue
  echo "==> docker compose build ${svc}"
  docker compose build "$svc"
done < <(docker compose config --services)

docker compose up -d
