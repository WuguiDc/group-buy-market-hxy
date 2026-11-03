# WSL Docker 监控

在 WSL Ubuntu 中执行：

```bash
cd /mnt/d/code_java_2026_6/group-buy-market-hxy/docs/dev-ops
docker compose -p monitoring -f docker-compose-grafana-wsl.yml config --quiet
docker compose -p monitoring -f docker-compose-grafana-wsl.yml up -d prometheus grafana
docker compose -p monitoring -f docker-compose-grafana-wsl.yml ps
```

- Grafana：http://localhost:4000，使用已有 MySQL 数据库中的账号登录（当前有 admin 用户），保留该数据库原有密码。
- Prometheus：http://localhost:9090，采集状态见 http://localhost:9090/targets。
- Grafana 自动加载现有 provisioning 中的 Prometheus 数据源，内部地址为 `http://prometheus:9090`。
- Grafana 使用已存在的 MySQL `grafana` 数据库，容器内连接 `mysql:3306`；复用 `docs/dev-ops/.env` 的 `MYSQL_USER/MYSQL_PASSWORD`。Windows 本机访问 MySQL 的端口仍为 13306。
- Grafana 文件数据和 Prometheus 指标分别存放在 Compose 命名卷中；切换前的数据库备份保存在本目录 `backups/` 中，备份文件不提交 Git。
- 复用已有外部网络 `dev-ops_my-network`。
- 应用在 Windows IDEA 中运行时，采集目标为 `host.docker.internal:8091/actuator/prometheus`；应用未启动时该 target 显示 DOWN。
- 如果以后改为 Docker 应用，修改 `prometheus/prometheus-wsl.yml` 中目标为 `group-buy-market-hxy:8091`，并确保应用加入同一网络，然后重启 Prometheus。

## 可选的 Grafana MCP

在 Grafana 的 Administration → Users and access → Service accounts 中创建本实例的服务账号和令牌。仅查询时可使用 Viewer 角色。不要使用示例令牌或把令牌提交到 Git。

在 WSL 中交互输入令牌并启动（输入内容不会回显）：

```bash
read -rsp 'Grafana service account token: ' GRAFANA_SERVICE_ACCOUNT_TOKEN
echo
if [ -n "$GRAFANA_SERVICE_ACCOUNT_TOKEN" ]; then
  export GRAFANA_SERVICE_ACCOUNT_TOKEN
  docker compose -p monitoring -f docker-compose-grafana-wsl.yml --profile mcp up -d grafana-mcp
  unset GRAFANA_SERVICE_ACCOUNT_TOKEN
fi
docker logs --tail 100 grafana-mcp
```

MCP 配置使用 SSE，连接地址为 `http://localhost:8000/sse`，Grafana 内部地址为 `http://grafana:3000`。已通过镜像的 `-h` 确认支持 `-t sse -sse-address 0.0.0.0:8000`；默认只监听容器 localhost，因此必须显式指定监听地址。启动 MCP 还需要本实例有效的服务账号令牌。

重新创建 MCP 容器时需重新提供令牌；普通 `docker restart grafana-mcp` 保留创建时的环境变量。

停止基础监控服务：

```bash
docker compose -p monitoring -f docker-compose-grafana-wsl.yml stop prometheus grafana
```
