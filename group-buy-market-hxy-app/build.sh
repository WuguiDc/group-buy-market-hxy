#!/usr/bin/env bash
set -euo pipefail
APP_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd -- "$APP_DIR/../docs/dev-ops" && pwd)"
case "${1:---source}" in
  --source)
    bash "$DEPLOY_DIR/app/compose.sh" build group-buy-market-hxy
    ;;
  --jar)
    if [[ ! -s "$APP_DIR/target/group-buy-market-hxy-app.jar" ]]; then
      echo '缺少应用 JAR。先 Maven 打包，或改用 --source 在 Docker 中编译。' >&2
      exit 1
    fi
    bash "$DEPLOY_DIR/app/compose.sh" -f "$DEPLOY_DIR/docker-compose-app-prebuilt.yml" build group-buy-market-hxy
    ;;
  *) echo '用法：bash group-buy-market-hxy-app/build.sh [--source|--jar]' >&2; exit 2 ;;
esac
