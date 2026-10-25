#!/usr/bin/env bash
set -euo pipefail
APP_SCRIPTS="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# 仅停止本项目的应用与 Nginx，不操作已有 MySQL/Redis 或数据卷。
bash "$APP_SCRIPTS/compose.sh" stop
