#!/usr/bin/env bash
set -euo pipefail
APP_SCRIPTS="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "$APP_SCRIPTS/../../.." && pwd)"
if [[ ! -f "$APP_SCRIPTS/../nginx/conf.d/default.conf" || ! -f "$APP_SCRIPTS/../nginx/html/index.html" ]]; then
  echo 'Nginx 配置或前端文件不可用，请确认已在 WSL 中挂载当前工程目录。' >&2
  exit 1
fi
bash "$APP_SCRIPTS/compose.sh" config --quiet
case "${1:---source}" in
  --source|--jar) bash "$PROJECT_ROOT/group-buy-market-hxy-app/build.sh" "${1:---source}" ;;
  --no-build) ;;
  *) echo '用法：bash docs/dev-ops/app/start.sh [--source|--jar|--no-build]' >&2; exit 2 ;;
esac
bash "$APP_SCRIPTS/compose.sh" up -d --no-build --wait --wait-timeout 150
bash "$APP_SCRIPTS/compose.sh" exec -T nginx nginx -t
bash "$APP_SCRIPTS/compose.sh" ps
echo '启动完成。默认前端地址：http://localhost/（可在 .env 修改 FRONT_PORT）。'
