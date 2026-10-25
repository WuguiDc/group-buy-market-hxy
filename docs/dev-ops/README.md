# WSL Docker 部署

部署对象为当前 `group-buy-market-hxy`，前端使用 Nginx，后端使用 Java 8 容器。复用本机已经运行的 `mysql`、`redis` 和 `dev-ops_my-network` 网络。

## 原脚本判断

原样执行不能可靠部署当前工程，已修正应用部署链路：

| 原配置问题 | 本次处理 |
| --- | --- |
| `build.sh`、`start.sh` 和 Compose 镜像名不一致 | 统一为 `system/group-buy-market-hxy-app:1.0-SNAPSHOT` |
| 旧构建依赖执行时所在目录及事先存在的 JAR | 使用脚本自身目录；支持 Docker 内源码构建和已有 JAR 两种方式 |
| JVM 参数位于 `-jar` 后 | 使用 exec 形式入口，JVM 参数交由 `JAVA_TOOL_OPTIONS` |
| 应用默认连接 `127.0.0.1:13306/16379` | 容器通过 `mysql:3306`、`redis:6379` 通信 |
| 原版脚本数据库名为 `big_market` | 使用本工程 `group_buy_market` |
| 前端缺少代理配置 | Nginx 提供页面并代理完整 `/api/` 路径 |
| 通知 URL 直接使用浏览器地址 | 默认回调后端自身 `http://127.0.0.1:8091/api/v1/test/group_buy_notify`，避免容器内回调误连 80 端口 |
| 应用停止脚本直接停止固定旧容器 | 仅停止本 Compose 项目的应用、Nginx |

原来的 `docker-compose-environment*.yml` 保留用于参考，本次启动脚本不会执行它们。当前 SQL 目录包含多个带 `DROP TABLE` 的完整课程快照，不应为了部署应用重新导入整个目录；现有 MySQL/Redis 容器和数据卷直接复用。

## 文件位置

```text
docs/dev-ops/
├── .env.example                    # 配置模板
├── .env                            # 本地配置，已被 Git 忽略
├── docker-compose-app.yml           # 应用 + Nginx，连接已有外部网络
├── docker-compose-app-prebuilt.yml  # 使用已有 JAR 构建的覆盖文件
├── app/
│   ├── compose.sh                   # 统一 Compose 项目名、配置与路径
│   ├── start.sh
│   └── stop.sh
└── nginx/
    ├── conf.d/default.conf          # 静态页面、/api/ 代理及健康检查
    └── html/index.html              # 唯一完整前端源码
```

`docs/ui` 保留设计截图、原始素材、浏览器验证脚本和原入口跳转。前端不再打包进 Spring Boot JAR。

## 首次准备

在 **WSL Ubuntu** 终端执行（Windows 工程对应 `/mnt/d/...`）：

```bash
cd /mnt/d/code_java_2026_6/group-buy-market-hxy
docker info
docker compose version
docker network inspect dev-ops_my-network >/dev/null
docker ps --format '{{.Names}}\t{{.Status}}'
```

确保现有 `mysql` 和 `redis` 运行，并已接入上述网络。当前机器的 `docs/dev-ops/.env` 已按现有数据库生成；不用覆盖。迁移到其他机器时：

```bash
cp docs/dev-ops/.env.example docs/dev-ops/.env
# 编辑 .env，填写已有 MySQL 密码、数据库、网络和端口。
```

默认前端仅绑定本机 80 端口，后端仅绑定本机 8091；若端口冲突，在 `.env` 调整 `FRONT_PORT/APP_PORT`。如需手机或局域网访问，可将 `FRONT_BIND` 改成 `0.0.0.0` 并使用 Windows 对应地址与端口。

## 构建并启动

### 方式一：复用 Windows 已打包 JAR

这是当前机器可直接使用的方式。修改 Java 后先重新打包；仅修改 Nginx 前端不需要重新打 JAR。

Windows PowerShell（需要 JDK 8）：

```powershell
$env:JAVA_HOME = 'C:\Users\hxy\.jdks\corretto-1.8.0_482'
mvn -pl group-buy-market-hxy-app -am clean package -DskipTests
```

然后在 WSL 执行：

```bash
cd /mnt/d/code_java_2026_6/group-buy-market-hxy
bash docs/dev-ops/app/start.sh --jar
```

### 方式二：全部在 Docker 内从源码构建

无需在 WSL 安装 JDK/Maven，但必须能下载 Maven 基础镜像及 Maven 依赖：

```bash
bash docs/dev-ops/app/start.sh --source
```

不传参数时也使用 `--source`。基础镜像可通过 `.env` 的 `JAVA_BASE_IMAGE/MAVEN_BASE_IMAGE/NGINX_IMAGE` 替换为自己已验证可访问的镜像地址。Docker Hub 认证或镜像拉取失败时可先 `docker pull`，或使用 `--jar` 方式，不影响已有数据库。

已有应用镜像时，不重新构建：

```bash
bash docs/dev-ops/app/start.sh --no-build
```

## 访问与运维

- 前端：[http://localhost/](http://localhost/)。`/ui/index.html` 会跳转到新入口。
- API：浏览器请求同源 `/api/v1/...`，Nginx 转发到 `group-buy-market-hxy:8091`，不需要写死 Windows/WSL 的 IP。
- 后端调试：[http://localhost:8091/](http://localhost:8091/)；没有根路径控制器时返回 404 属于预期，API 使用完整路径。
- Nginx 存活检查：[http://localhost/healthz](http://localhost/healthz)。此检查只表示 Nginx 可用；业务可用性通过营销查询接口判断。

```bash
# 状态、配置、日志
bash docs/dev-ops/app/compose.sh ps
bash docs/dev-ops/app/compose.sh config --quiet
bash docs/dev-ops/app/compose.sh logs --tail=100 group-buy-market-hxy nginx

# 修改 html 后浏览器刷新即可；修改 nginx 配置后：
bash docs/dev-ops/app/compose.sh exec nginx nginx -t
bash docs/dev-ops/app/compose.sh exec nginx nginx -s reload

# 仅停止本项目应用与 Nginx，保留 MySQL/Redis
bash docs/dev-ops/app/stop.sh
```

应用文件日志保存在 Compose 管理的 `app-logs` 卷，容器标准输出也有日志轮转。`.env` 不提交版本库，避免通过 `docker compose config` 完整输出密码，校验时使用 `config --quiet`。

## 业务接口只读检查

```bash
curl -sS http://localhost/api/v1/gbm/index/query_group_buy_market_config \
  -H 'Content-Type: application/json' \
  -d '{"userId":"xfg01","source":"s01","channel":"c01","goodsId":"9890001"}'
```

返回 HTTP 200 说明代理和接口可达；`code: "0000"` 才表示营销查询成功。如果返回业务错误，查看应用日志及活动、人群、商品数据，不要重新初始化数据库。支付二维码仍为课程演示；点击「支付完成」会提交后端结算。

## 本机验证结果

2026-10-24 已在 `Ubuntu-24.04-E` 的 WSL 2 / Docker Desktop Linux 引擎验证：

- JAR 构建部署脚本成功，应用和 Nginx 两个容器均为 `healthy`。
- Docker 内源码构建也已成功：Maven 基础镜像预拉取后，7 个 Maven 模块全部构建通过，不依赖 WSL 本地 JDK/Maven。
- Nginx `nginx -t` 通过，Windows 访问首页返回 HTTP 200；旧 `/ui/index.html` 入口返回 302。
- 真实营销查询经 Nginx 返回 `code: "0000"`，原价 100、优惠 10、拼团价 90，确认可连接现有 MySQL 与 Redis。
- 浏览器只读验证通过：Cookie 登录、同源 POST 代理、接口金额及参团人数渲染；未创建或结算订单。实测截图保存在 `docs/ui/screenshots/04-WSL部署实测.png`。
- 当前数据库返回部分 2025 年历史队伍，其有效期已结束，页面会禁用这些队伍的参团入口；此现象不是代理或部署失败。

真实环境浏览器验证可使用 `node docs/ui/verify-live.cjs`，同前端合约测试一样，需要已有 Playwright，并支持 `PLAYWRIGHT_PATH/BROWSER_PATH/LIVE_URL` 环境变量。

配置依据：[Docker Compose 服务网络与外部网络](https://docs.docker.com/compose/how-tos/networking/)、[Nginx proxy_pass](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_pass)、[Eclipse Temurin 官方镜像](https://hub.docker.com/_/eclipse-temurin)。
