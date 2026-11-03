# 商城系统近七天运行情况报告

统计窗口：2026-10-26 19:52:50 至 2026-11-02 19:52:50（北京时间，滚动七天）。
数据来源：通过 Codex 的 Grafana MCP 查询 Grafana 中的 Prometheus 数据源（UID：PBFA97CFB590B2093）。
本次操作：只读查询，未修改监控配置、仪表盘或告警。

## 结论

当前商城应用可以正常采集，短时观测下未见持续资源饱和。**现有数据不足以形成完整七天的稳定性、业务性能或可用率结论。** 查询七天历史时，仅发现 2026-11-02 晚间约 31 分钟的采集数据；其余时间应标记为“无监控数据”，不能判断为运行正常，也不能判断为宕机。

HTTP 数据几乎全部来自监控接口，不能据此证明下单、支付、结算等业务链路正常或推断生产容量。

## 数据覆盖与在线情况

| 项目 | 实测结果 | 解读 |
|---|---|---|
| Prometheus 采集记录 | 11 月 2 日 19:21:57—19:52:39 | 七天窗口内仅有这一小段历史 |
| 应用目标采集记录 | 11 月 2 日 19:21:59—19:52:42 | 目标为 host.docker.internal:8091 |
| 首次采集成功 | 11 月 2 日 19:33:45 | 之前的已记录样本均为失败，之后均为成功 |
| 当前应用状态 | UP | 指标端点可被采集 |
| 应用 up 样本 | 129 次，其中成功 80 次、失败 49 次 | 样本成功率 62.02%，不是七天可用率，也不是按时长计算的可用率 |
| Prometheus 自监控 up 样本 | 129/129 成功 | 仅代表已经记录到的样本 |
| 当前应用进程启动时间 | 11 月 2 日 19:33:37 | 来源 process_start_time_seconds，和日志中的业务就绪时间含义不同 |
| 当前进程运行时长 | 约 19 分 5 秒 | 截至统计时间的最近采集值 |
| 已观测进程重启 | uptime 指标未见重置 | 不包含采集建立前、失败期间或缺失期间的启动尝试 |

初始采集失败与前面排查到的应用未启动、RedissonClient 注入冲突在时间上相符；仅凭 up 指标不能逐次判定失败原因。首次成功后约 19 分钟的已记录采集样本均为 UP。

## 请求与性能

| 指标 | 实测结果 | 适用范围 |
|---|---:|---|
| 当前进程累计 HTTP 请求 | 83 次 | 不是完整七天业务请求数 |
| GET /actuator/prometheus，200 | 82 次 | 主要是监控采集流量 |
| GET /**，404 | 1 次 | /** 是归一化 URI 标签，无法从该指标还原原始 URL |
| 5xx | 当前已暴露的请求计数中未见 5xx 系列 | 不能推广为七天没有异常 |
| 观测期间平均响应时间 | 约 124.3 ms | 由请求耗时与请求数的 increase 比值计算，几乎全部是监控接口 |
| 指标记录到的最大请求耗时 | 约 311.8 ms | http_server_requests_seconds_max 在观测窗口的最大值 |
| P95/P99 | 无法计算 | 未发现 http_server_requests_seconds_bucket 等所需分布数据 |

当前没有可用于评估下单、支付、库存占用和结算接口的有效业务压测样本；单次 404 也不能代表业务失败率。

## JVM 与资源

| 指标 | 当前或观测范围 | 判断 |
|---|---:|---|
| 应用进程 CPU | 平均 0.52%，最高 1.23% | 观测期进程整体 CPU 利用率较低 |
| 应用报告的系统 CPU | 平均 9.74%，最高 46.06% | 属于应用所在 Windows 主机视角，不是各 Docker 容器 CPU |
| JVM 堆内存当前使用 | 123.8 MiB | 约占堆上限 1.53% |
| JVM 堆内存峰值 | 374.2 MiB | 按 15 秒网格观测的峰值，约占堆上限 4.61% |
| JVM 堆上限 | 7.92 GiB | 是最大值，不代表已经实际占用 |
| JVM 非堆内存当前使用 | 93.6 MiB | 短时间数据不足以判断泄漏 |
| JVM 活跃线程 | 当前 98，已采样最高 101 | 未见持续增长证据 |
| BLOCKED 线程 | 已采样最高 1 | 存在短时阻塞样本，不能据此认定死锁 |
| Tomcat 忙线程 | 当前 1 / 配置上限 20 | 当前没有线程池饱和迹象 |
| GC 次数 | 当前进程累计 4 次 | 来自多个 action/cause 序列求和 |
| GC 累计停顿 | 当前进程累计约 125 ms | 包含启动阶段的累计计数 |
| GC 最大停顿 | 已记录最高 93 ms | 短时样本中未见持续长停顿 |
| 应用工作目录所在 D 盘 | 剩余约 78.6 GiB，空闲约 26.2% | 不代表 WSL 虚拟磁盘或 Docker 数据盘剩余空间 |

这些结果只支持“本次短时、低业务负载观测未出现明显持续资源压力”，不能证明无内存泄漏、无性能瓶颈或已满足生产容量要求。

## 数据库连接池与日志计数

- HikariPool-1 当前连接数为 10，配置最大连接数为 10。总连接数达到池大小不等于连接池耗尽，空闲连接也计入总数。
- 活跃连接采样峰值为 0，等待连接采样峰值为 0，当前进程累计连接获取超时为 0。查询间隙内的瞬时借还连接可能不会被采到，不能推断从未访问数据库。
- Logback 当前进程累计 INFO 167 条，WARN/ERROR 计数均为 0。它不覆盖全部 Logback 内部状态输出；此前 Logstash 的连接失败告警不能因此被排除。
- Grafana 当前只有 Prometheus 数据源，未接入日志或链路追踪数据源。本报告没有通过 Grafana 查询到 ELK 日志、业务异常堆栈或分布式追踪。

## 监控配置发现

### 1. JVM 仪表盘标签与指标不一致

现有 JVM (Micrometer) 仪表盘的 Application 变量使用 label_values(application)，面板普遍过滤 application="$application"。
实际应用指标带有 app="group-buy-market-app" 和 job="group-buy-market-app"，但没有 application 标签；查询带非空 application 标签的 JVM 指标返回空结果。因此该仪表盘存在无数据的配置原因，不能将空白面板理解为资源使用为零。

后续可统一添加 application 指标标签，或把仪表盘的变量和面板改为使用 app/job 标签；本次没有执行修改。

### 2. 部分面板引用当前没有的指标

仪表盘包含 process_memory_rss_bytes、process_memory_vss_bytes、process_files_open_files 等查询，当前发现的是 process_resident_memory_bytes、process_virtual_memory_bytes、process_open_fds 等名称。还有 Jetty/Linux 相关面板，与当前 Windows + Tomcat 环境未必适配，需要逐一校验，不能把这些空白面板作为故障证据。

### 3. 告警规则和组件级监控不足

Grafana 告警规则列表为空。这表示没有配置该类规则，不表示七天内没有故障。
当前仅发现商城应用和 Prometheus 自身两个采集目标，未见 MySQL、Redis、RabbitMQ、Docker 容器的独立采集目标，也未见 node/container 指标。组件健康不能由 JVM 健康替代。

## 后续处理顺序

1. 持续保留至少完整七天的采集数据，并单独统计有数据时段与采集中断时段。
2. 统一 JVM 仪表盘标签、变量和指标名称，使现有数据能够正确展示。
3. 建立应用采集失败、监控缺失、HTTP 5xx、持续高资源使用和连接池等待等告警；阈值需结合持续时间、流量和业务目标制定，避免用低流量下的比例误报。
4. 为关键组件补充独立指标；在有代表性的业务请求下补充成功率、延迟分布和容量验证。
5. 若继续使用 ELK，单独检查 Logstash/Kibana 与日志入库状态；Prometheus UP 不能证明日志链路正常。

## 复查入口与口径

- [Grafana 仪表盘列表](http://localhost:4000/dashboards)
- [JVM 仪表盘：本次七天窗口](http://localhost:4000/d/d26890e8-8a26-4b9e-9a9d-dc30cd795aba/jvm-micrometer?orgId=1&from=1793015570000&to=1793620370000)
- [Prometheus 采集目标](http://localhost:9090/targets)
- 原始查询结果与统计表达式：同目录 2026-11-02-grafana-7day-evidence.json。

关键表达式：

```promql
up[7d]
count_over_time(up{job="group-buy-market-app"}[7d])
sum_over_time(up{job="group-buy-market-app"}[7d])
sum by(uri,status,method) (http_server_requests_seconds_count{job="group-buy-market-app"})
avg_over_time(process_cpu_usage{job="group-buy-market-app"}[7d])
max_over_time(process_cpu_usage{job="group-buy-market-app"}[7d])
count(jvm_memory_used_bytes{job="group-buy-market-app",application!=""})
```

七天范围查询首先用于确认历史覆盖；资源统计仅基于实际存在的应用样本。计数器当前值与 increase 估计增量分开使用，空序列不自动当成零；未采集的时段不纳入“运行正常”结论。
