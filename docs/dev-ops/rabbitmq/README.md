# RabbitMQ（WSL Docker）

在 WSL Ubuntu-24.04-E 中执行：

```bash
cd /mnt/d/code_java_2026_6/group-buy-market-hxy/docs/dev-ops
docker compose -f docker-compose-environment-aliyun.yml up -d --no-deps rabbitmq
docker compose -f docker-compose-environment-aliyun.yml ps rabbitmq
docker logs --tail 100 rabbitmq
```

此命令只启动 RabbitMQ。镜像为 `registry.cn-hangzhou.aliyuncs.com/xfg-studio/rabbitmq:3.12.9`，容器名和主机名均为 `rabbitmq`，重启策略为 `always`。

- 管理页面：<http://localhost:15672>，账号 `admin`，密码 `admin`。
- Windows 本地程序：主机 `localhost`，端口 `5672`，虚拟主机 `/`，账号密码 `admin/admin`。
- `dev-ops_my-network` 内的容器：主机 `rabbitmq`，端口 `5672`，虚拟主机 `/`，账号密码 `admin/admin`。
- `enabled_plugins` 启用 `rabbitmq_management`，无需手动进入容器启用管理插件。
- 数据保存在 Compose 命名卷 `dev-ops_rabbitmq-data`，重建容器会保留数据；固定主机名用于保持 RabbitMQ 节点名称稳定。

停止或再次启动此服务：

```bash
docker compose -f docker-compose-environment-aliyun.yml stop rabbitmq
docker compose -f docker-compose-environment-aliyun.yml up -d --no-deps rabbitmq
```
