#!/usr/bin/env bash
set -euo pipefail
DEPLOY_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${GBM_ENV_FILE:-$DEPLOY_DIR/.env}"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "缺少 $ENV_FILE；请复制 .env.example 为 .env 并填写现有数据库密码。" >&2
  exit 1
fi
docker compose version >/dev/null
exec docker compose --project-name group-buy-market-hxy --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose-app.yml" "$@"
