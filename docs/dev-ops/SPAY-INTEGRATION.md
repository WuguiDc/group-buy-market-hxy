# 拼团 UI 与 s-pay-mall 对接

本次修改的是两个本地工程：`group-buy-market-hxy` 与同级 `s-pay-mall-ddd-market-hxy`。按本地 IDEA 联调方式使用：拼团服务监听 8091，支付商城监听 8080；前端使用 IDEA 内置 HTTP 预览。本节不要求重新部署容器。

## 活动 ID 的前置改动

- `GoodsMarketResponseDTO` 新增顶层 `Long activityId`。
- `MarketIndexController#queryGroupBuyMarketConfig` 从本次试算命中的活动取得 ID，并写入 `data.activityId`，即使 `teamList` 为空也返回。
- 商城 `CreatePayRequestDTO`、`ShopCartEntity` 原本已有 `activityId`，保留字段；修正 `AliPayController#createPayOrder`，将 `activityId/teamId/marketTypeVO` 一并传给购物车。
- 前端开团与参团使用接口返回的活动 ID，不再使用写死的 `100123` 或 URL 参数代替。
- 单购只传 `marketType: 0`，不传 `activityId/teamId`；商城控制器也会清除单购的营销参数。

```text
商品试算命中的活动ID
  → GoodsMarketResponseDTO.activityId
  → 浏览器 create_pay_order 请求
  → CreatePayRequestDTO.activityId
  → ShopCartEntity.activityId
  → AbstractOrderService.lockMarketPayOrder(...)
  → ProductPort / 拼团服务锁单
```

## 前端流程

DDD 商城 `docs/dev-ops/nginx/html/login.html` 登录成功后会直接跳到拼团商品购买页，不再回到旧的键盘演示首页。DDD 首页也增加了「进入拼团购买」入口：未登录先去登录页，已登录直接进入拼团页。

两个 DDD 页面共用 `js/mall-navigation.js` 配置。目标默认是当前主机名下的 `:63342/group-buy-market-hxy/docs/dev-ops/nginx/html/index.html#product`，沿用 `Path=/` 的 `loginToken` Cookie，不通过 URL 传递身份。建议两个页面统一使用 `localhost`；也支持全程使用 `127.0.0.1`，不要在两者之间切换。IDEA 需同时能访问两个工程的前端文件；如果项目名或预览端口不同，在该导航配置文件中修改目标。

前端唯一完整源码为 `docs/dev-ops/nginx/html/index.html`，商品、扫码登录与支付确认仍在一个 HTML 文件中。`docs/ui/index.html` 兼容旧入口，截图与测试保留在 `docs/ui`。

1. 微信登录：GET 商城 `/api/v1/login/weixin_qrcode_ticket`，展示 `showqrcode?ticket=...`。每 3 秒检查 `/api/v1/login/check_login?ticket=...`；`0003` 继续等待，成功后把返回身份写入 `loginToken` Cookie，有效期 30 天并返回商品页。离开登录页、刷新二维码或登录成功都会停止旧轮询；页面等待 3 分钟后提示刷新二维码。
2. 查询营销信息：POST 拼团 `/api/v1/gbm/index/query_group_buy_market_config`，使用 Cookie 中的身份作为 `userId`。旧版 `username` 演示登录不再作为登录依据。
3. 下单：单购、开团、参团均 POST 商城 `/api/v1/alipay/create_pay_order`。队伍 ID 取当前行，活动 ID 取 `data.activityId`。
4. 商城返回支付宝 HTML 表单。页面仅解析表单与隐藏字段，忽略其中的脚本和事件属性，保留页面原有元素；网关必须是 HTTPS 支付宝沙箱域名。
5. 支付确认弹窗从表单 `biz_content.total_amount/out_trade_no` 展示实际待支付金额及商城订单号。用户点击「前往支付宝沙箱付款」才在新标签页提交支付表单。
6. 「我已支付，刷新进度」只重新查询进度，不由浏览器锁单或提交结算，也不会直接显示支付成功。商城在支付宝验签回调后发起营销结算；订单支付或成团状态以服务端为准。

示例下单参数：

```json
{"userId":"微信登录返回的身份","productId":"9890001","marketType":1,"teamId":null,"activityId":100123}
```

其中 `100123` 仅为示例应答；前端代码读取实际返回值。单购参数为：

```json
{"userId":"微信登录返回的身份","productId":"9890001","marketType":0}
```

## 服务端配套修正

- 商城营销结算网关从误写的锁单 URL 改为 `/api/v1/gbm/trade/settlement_market_pay_order`。
- 商城 `/api/v1/alipay/group_buy_notify` 使用 `@RequestBody` 绑定 JSON 订单列表。
- 营销结算 HTTP/业务失败不再被 `ProductPort` 静默忽略，便于支付回调识别失败并重试。
- 演示商品 `9890001` 在商城商品查询中的原价改为 100，与拼团商品数据一致；其他原演示商品仍保持原配置。正式商品数据变动应以商品服务为准。
- 查询待支付商城订单时加入营销类型条件，避免单购与拼团复用彼此的支付单。
- 商城下单校验活动 ID、用户、商品和营销类型，并返回业务错误码供前端展示。
- 拼团统计原 SQL 按队伍分组却选择用户及交易单号，导致 MySQL `ONLY_FULL_GROUP_BY` 报错；统计只需要队伍 ID，改为 `select distinct team_id`，保留严格 SQL 模式。我的订单查询同时读取 `status`，用于支付状态展示。

## 本地配置与可选 Nginx 代理

本地 IDEA 联调直接访问 Java 端口，不依赖 Nginx。下面是配置文件中保留的可选代理规则，后续确有部署需要时再使用：

| 地址 | 当前上游 |
| --- | --- |
| `/api/v1/gbm/...` | Docker `group-buy-market-hxy:8091` |
| `/api/v1/login/...` | Windows 商城 `host.docker.internal:8080` |
| `/api/v1/alipay/...` | Windows 商城 `host.docker.internal:8080` |
| `/api/v1/weixin/...` | Windows 商城 `host.docker.internal:8080` |

前端默认同源请求，可用 `?apiBase=...&payApiBase=...&goodsId=...` 分别覆盖两个服务地址与商品 ID。没有提供有效活动 ID 时禁用拼团购买并提示更新后端，仍可单购。

商城 `application-dev.yml` 的 `app.config.group-buy-market.notify-url` 为 `http://127.0.0.1:8080/api/v1/alipay/group_buy_notify`，供本地拼团服务通知商城。

`alipay.return_url` 为 `http://localhost:63342/group-buy-market-hxy/docs/dev-ops/nginx/html/index.html`，支付后回到 IDEA HTTP 预览页面。保留已有的 `alipay.notify_url`、微信配置、Natapp 和支付密钥；外部回调仍需你配置的隧道保持在线。前端在 IDEA 的 63342 端口会直接请求本地 8080/8091 服务，其他 HTTP 地址默认同源；可使用 URL 参数覆盖。改用其他前端服务器时，同步调整回跳地址。

## 启动与验证

1. 在 IDEA 运行或重启 **group-buy-market-hxy**，使用本地 8091 端口，让新增的 `activityId` 和统计 SQL 生效。确认该端口可供本地进程使用。
2. 在 IDEA 重新启动 **s-pay-mall-ddd-market-hxy**，使用本地 8080 端口，让控制器、网关及配置改动生效。Maven 编译不会自动更新运行中的旧进程。
3. 在 IDEA 中右键 `docs/dev-ops/nginx/html/index.html`，选择浏览器预览（HTTP 63342）；先微信扫码登录，再单购、开团或参团，确认订单后前往支付宝沙箱。
4. 在支付宝沙箱完成支付，等待商城验签回调及营销结算，再刷新页面。不要使用前端按钮代替回调结算。

自动化验证不会连接外部支付平台或写真实订单：

- 拼团工程：`MarketIndexHttpContractTest` 检查顶层 `activityId` 与原有数据。
- 商城工程：`AliPayHttpContractTest` 检查活动、队伍、营销类型透传、单购参数、业务错误和 JSON 回调；`ProductPortSettlementContractTest` 检查结算失败传播。
- `node docs/ui/verify.cjs`：内存接口桩和浏览器验证。用非默认活动 ID `876543` 测试空队伍开团及参团；拦截支付宝表单提交，检查单购参数、登录轮询、30 天 Cookie、金额与脚本隔离。截图为合约数据，不是真实付款凭据。
- `node docs/ui/verify-live.cjs`：已部署拼团/Nginx 的只读查询检查，使用明确的测试 Cookie；不执行真实扫码、下单或结算。
- `node docs/ui/verify-navigation.cjs`：跨工程本地页面跳转测试，验证 DDD 登录后直接进入拼团页、Cookie 沿用、无重复扫码，以及 localhost/127.0.0.1 两种主机名。

本次未通过真实微信扫码和支付宝沙箱完成付款，不将自动化合约测试等同于外部支付回调的端到端验证。
