# 拼团商城前端

完整前端位于 [Nginx HTML](../dev-ops/nginx/html/index.html)，当前目录的 index.html 仅跳转到该文件。HTML、CSS、JS 和商品图片保存在一个运行文件中。

当前流程已升级为：微信扫码登录 → 查询拼团优惠 → 支付商城下单 → 支付宝沙箱付款 → 后端回调结算。浏览器不再直接调用拼团锁单、结算接口。

详细的 activityId 透传、两项目改动、接口与启动步骤见 [商城对接说明](../dev-ops/SPAY-INTEGRATION.md)。Docker 部署见 [部署说明](../dev-ops/README.md)。

## 页面与截图

- [商品详情页](../dev-ops/nginx/html/index.html#product) · [截图](screenshots/01-商品详情页.png)
- [微信扫码登录页](../dev-ops/nginx/html/index.html#login) · [截图](screenshots/02-登录页.png)
- 支付确认弹窗：成功创建商城支付单后展示 · [截图](screenshots/03-弹出扫码支付页.png)

登录和支付需要 HTTP 服务；本地开发请在 IDEA 中用浏览器预览打开 HTML（63342 端口），页面自动调用本地 8080/8091 服务。截图使用接口合约数据与演示二维码，不代表真实扫码或付款。

## 核心接口

| 用途 | 接口 |
| --- | --- |
| 微信二维码 | GET /api/v1/login/weixin_qrcode_ticket |
| 检查扫码登录 | GET /api/v1/login/check_login?ticket=... |
| 商品营销试算 | POST /api/v1/gbm/index/query_group_buy_market_config |
| 商城创建支付单 | POST /api/v1/alipay/create_pay_order |

前端登录身份使用 loginToken Cookie，有效期 30 天。活动 ID 来自试算返回的 data.activityId，单购不携带活动和队伍参数。金额以商城支付表单为准。支付结果由支付宝回调和后端确认。

## 验证脚本

使用 Node.js 20+ 和已有 Playwright。可设置 PLAYWRIGHT_PATH、BROWSER_PATH 指定包与浏览器路径。

- node docs/ui/verify.cjs：执行 verify-mall.cjs 本地接口桩测试，自动更新 3 张截图，不调用真实微信、支付宝或数据库。
- node docs/ui/verify-live.cjs：真实部署的只读查询验证，使用测试身份 Cookie；支持 LIVE_URL、LIVE_USER_ID。不会创建或结算订单，也不代替真实微信扫码验证。
- verify-api.cjs 保留为新测试的兼容入口。

原始线框图和商品素材仍在 reference/。04-WSL部署实测.png 属于先前部署时的只读实测截图，不作为本次真实支付验证。
