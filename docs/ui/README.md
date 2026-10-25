# 小傅哥拼团 · H5 前端与后端对接

完整前端已迁移到 [`../dev-ops/nginx/html/index.html`](../dev-ops/nginx/html/index.html)，该文件包含 HTML、CSS、JavaScript、商品图片与课程演示二维码。当前目录的 `index.html` 是兼容旧链接的跳转页，截图、素材和验证脚本仍在本目录。图片可离线展示；查询商品、锁单和结算需要连接 Java 后端。

## 启动和打开

1. 参考 [`../dev-ops/README.md`](../dev-ops/README.md) 在 WSL Docker 中启动后端与 Nginx，复用现有 MySQL/Redis；确保商品 `9890001`、活动 `100123` 和 `s01/c01` 配置有效。
2. 已有 Maven 打包 JAR 时，在 WSL 工程根目录执行 `bash docs/dev-ops/app/start.sh --jar`。
3. 无本地 JDK/Maven 时可执行 `bash docs/dev-ops/app/start.sh --source`，通过 Docker 构建源码。
4. 浏览器打开 [http://localhost/](http://localhost/)。Nginx 提供前端页面并将 `/api/` 转发后端，前端不再打包到 Spring Boot JAR。
5. 点击「开团购买」进入登录页，填写 `xfg01` 等用户 ID 和任意非空演示密码。页面显示后端返回的优惠和队伍，再进行开团、参团与结算。

也可以双击当前目录的 `index.html`，会跳到 Nginx 目录内的页面，本地文件默认调用 `http://127.0.0.1:8091`。使用其他独立静态服务器时，需指定 `?apiBase=http://127.0.0.1:8091`；使用已配置的 Nginx 时无需指定。前端在 JSON 请求体中传递 `userId`。

## 三个独立画面

| 页面 | 原稿入口 | 单独截图 |
| --- | --- | --- |
| 商品详情页 | [index.html#product](index.html#product) | [01-商品详情页.png](screenshots/01-商品详情页.png) |
| 登录页 | [index.html#login](index.html#login) | [02-登录页.png](screenshots/02-登录页.png) |
| 弹出扫码支付页 | 登录后点击「开团购买」或「参与拼团」 | [03-弹出扫码支付页.png](screenshots/03-弹出扫码支付页.png) |

商品与登录页可通过 hash 分别打开。支付画面需要成功锁单；直接访问 `#payment` 会回到商品页，避免使用虚构交易单号提交结算。三张截图分别保存，使用接口合约测试数据生成，不代表真实数据库中的当前数据。

## 体验流程

1. 商品页包含 3 张轮播图，支持自动轮播、点击圆点、触摸切换。图片均来自指定的商品图片地址。
2. 未登录时展示登录提示和价格占位，不使用虚构用户试算优惠。点击购买进入登录页。
3. 用户 ID 使用 1–32 位字母、数字、下划线或横线，直接作为后端 `userId`。登录写入有效期 7 天的 `username` Cookie 并返回商品页；使用 `localStorage.gbm_identity` 提供本地文件和 Cookie 受限时的回退。密码不保存，当前后端没有登录鉴权接口。
4. 查询接口成功后才开放交易。开团请求的 `teamId` 为 `null`，参团传入对应行的 `teamId/activityId`。锁单返回成功后展示支付弹窗；金额使用锁单返回的折扣与原价计算，避免继续使用过期的页面试算金额。
5. 点击「我已支付，支付完成」调用后端结算接口。服务端确认成功后才展示成功画面，触发 `window` 的 `groupbuy:settled` 事件（`detail` 为结算应答），并刷新队伍和统计。不会仅根据锁单人数判断已经成团。
6. 取消支付只关闭弹窗，不提交结算、不释放名额；后端没有取消/释放锁单接口。待支付记录以 `gbm_order:` 为前缀按接口地址、渠道、商品、活动和用户隔离存储，刷新后可继续支付。新生成的 `outTradeNo` 固定为 12 位数字，与数据库字段长度一致；旧版缓存中尚未锁单成功的超长单号会自动清理。锁单网络重试使用相同 `outTradeNo`；服务端返回的「我的订单」也可用于恢复未支付单号。
7. 「我的订单」使用新增的 `tradeOrderStatus` 区分待支付、已支付与关闭状态。结算应答丢失时，刷新查询到同一交易单已支付，才恢复成功画面；业务失败保留弹窗与可重试单号。
8. 队伍剩余人数使用 `targetCount - lockCount`，倒计时以接口 `validTimeCountdown` 为基准；过期或满员的其他队伍不可参与。`teamList` 为 `null` 或 `[]` 时展示“小伙伴，赶紧去开团吧，做村里最靓的仔。”。

当前后端只实现了拼团交易，没有原价单购接口，因此「单独购买」展示接口原价并提示“暂未开放”，不会调用拼团接口伪造单购。二维码编码为 `DEMO:GROUP-BUY-NO-PAYMENT`，没有真实微信/支付宝支付能力；按课程要求，通过点击「支付完成」调用真实结算接口。

## 接口与字段映射

三个接口均为 `POST`、`Content-Type: application/json`，以 `code === "0000"` 判断业务成功。

| 接口 | 路径 | 主要请求参数 |
| --- | --- | --- |
| 查询营销配置 | `/api/v1/gbm/index/query_group_buy_market_config` | `userId/source/channel/goodsId` |
| 锁单 | `/api/v1/gbm/trade/lock_market_pay_order` | 查询参数加 `activityId/teamId/outTradeNo/notifyUrl` |
| 支付结算 | `/api/v1/gbm/trade/settlement_market_pay_order` | `userId/source/channel/outTradeNo/outTradeTime` |

| 页面位置 | 数据来源 |
| --- | --- |
| 拼团价、开团按钮价格 | `data.goods.payPrice` |
| 单独购买原价 | `data.goods.originalPrice` |
| 直降、立省金额 | `data.goods.deductionPrice` |
| 参与人数 | `data.teamStatistic.allTeamUserCount` |
| 开团、成团统计 | `allTeamCount/allTeamCompleteCount` |
| 队伍列表 | `data.teamList` |
| 队伍成员、交易单号 | `userId/outTradeNo` |
| 我的交易支付状态 | `tradeOrderStatus`：`0` 待支付、`1` 已支付、`2` 已关闭 |
| 付款弹窗金额 | `originalPrice - 锁单返回的 deductionPrice` |

商品标题和详情文案保留 UI 原稿内容，当前 DTO 没有返回商品名称或详情。没有写死优惠价格、参团人数或成团人数。

页面配置位于 Nginx 目录前端文件的内嵌脚本开头 `CONFIG`；也可用 URL 查询参数覆盖：`apiBase/goodsId/activityId/source/channel/notifyUrl`。默认分别为同源（本地文件为 `http://127.0.0.1:8091`）、`9890001`、`100123`、`s01`、`c01`。通知地址默认使用后端自身 `http://127.0.0.1:8091/api/v1/test/group_buy_notify`，避免 Nginx 80 端口在后端容器内无法访问。空队伍时使用配置的活动 ID，参团使用接口返回的活动 ID。`outTradeTime` 使用带 UTC 时区的 ISO 时间。

接口失败、非 JSON 应答、空应答或超时会显示错误，不会静默切回示例数据。下单和结算期间禁用重复提交。

## 原始素材

- `reference/原始线框图.png`：用户提供的原始线框图。
- `reference/mybatis-03.png`：指定商品图的本地副本；页面通过 CSS 展示书籍部分。
- `reference/demo-qr.svg`：内嵌演示二维码的源文件。
- 商品图来源：https://bugstack.cn/images/article/product/book/mybatis-03.png?raw=true

## 验证

后端 HTTP 测试使用 MockMvc 与 Mock 服务，不启动 Spring 应用、不连接数据库，验证真实控制器映射、JSON 绑定、ISO 时间解析、成功结果、参数与业务错误，以及「我的订单」状态字段：

```powershell
mvn -pl group-buy-market-hxy-app -am test "-Dtest=MarketTradeHttpContractTest,MarketIndexHttpContractTest" -DfailIfNoTests=false -DskipTests=false
```

`verify.cjs` 调用 `verify-api.cjs`，是可选的浏览器合约测试与截图脚本，不参与页面运行。使用 Node.js 20+ 和已有 Playwright，设置 `PLAYWRIGHT_PATH` 为包路径；可通过 `BROWSER_PATH` 指定浏览器（默认本机 Edge）。执行 `node docs/ui/verify.cjs`，脚本会启动内存接口桩并在完成后关闭，不调用真实后端或写数据库。

检查范围：POST/JSON 合约、字段渲染、空列表、Cookie 登录与密码不落盘、锁单金额、选队、重复点击、取消不结算、刷新恢复、网络失败复用单号、服务端订单恢复、结算失败重试与应答丢失恢复、已支付状态、接口异常、文本注入防护、320px 布局与离线图片。截图为 430px 手机宽度、2 倍像素。

前期已通过后端编译、5 个 MockMvc HTTP 测试和浏览器合约测试。迁移后浏览器测试已改为读取 Nginx 目录的真实前端文件。真实联调可依次登录 `xfg01/xfg02/xfg03` 开团与参团；如出现 `E0102/E0103/E0007`，按返回检查活动有效期、参与次数或人群配置，无需清空订单表。Docker 部署与只读业务检查见部署说明。
