// 微信/支付宝/商城的本地接口桩；不发起真实登录、支付或数据库写入。
const {chromium}=require(process.env.PLAYWRIGHT_PATH||'playwright');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http'),assert=require('node:assert/strict');
const html=fs.readFileSync(path.resolve(__dirname,'../dev-ops/nginx/html/index.html'));
const calls=[],errors=[],submits=[];let ready=false,mode='normal',createMode='normal',order=0;
const market=()=>({activityId:876543,goods:{goodsId:'9890001',originalPrice:100,deductionPrice:10,payPrice:90},teamList:mode==='empty'?null:[{activityId:876543,userId:mode==='xss'?'<img onerror=alert(1)>':'friend01',teamId:'team-A',targetCount:3,lockCount:1,completeCount:0,validTimeCountdown:'00:05:49'}],teamStatistic:{allTeamCount:7,allTeamCompleteCount:1,allTeamUserCount:9}});
const server=http.createServer(async(req,res)=>{
  if(!req.url.startsWith('/api/')){res.setHeader('Content-Type','text/html; charset=utf-8');res.end(html);return}
  let body='';for await(const chunk of req)body+=chunk;body=body?JSON.parse(body):null;
  calls.push({url:req.url,method:req.method,body});res.setHeader('Content-Type','application/json');
  const ok=data=>res.end(JSON.stringify({code:'0000',data,info:'成功'}));
  if(req.url==='/api/v1/login/weixin_qrcode_ticket')return ok('ticket+a/&?');
  if(req.url.startsWith('/api/v1/login/check_login?')){
    assert.equal(new URL(req.url,'http://localhost').searchParams.get('ticket'),'ticket+a/&?');
    return ready?ok('wx_openid_123'):res.end(JSON.stringify({code:'0003',info:'未登录'}));
  }
  if(req.url==='/api/v1/gbm/index/query_group_buy_market_config'){
    if(mode==='failure')return res.end(JSON.stringify({code:'E0002',info:'无拼团配置'}));
    const data=market();if(mode==='missing-activity')delete data.activityId;return ok(data);
  }
  if(req.url==='/api/v1/alipay/create_pay_order'){
    if(createMode==='failure')return res.end(JSON.stringify({code:'E0006',info:'拼团队伍已满'}));
    const gateway=createMode==='bad-gateway'?'https://invalid.example/pay':'https://openapi-sandbox.dl.alipaydev.com/gateway.do';
    const biz=JSON.stringify({out_trade_no:String(++order).padStart(12,'0'),total_amount:body.marketType===0?'100.00':'78.90'});
    return ok(`<form method="post" action="${gateway}" onsubmit="window.hacked=true"><input name="biz_content" value='${biz}'><input name="sign" value="fixture-sign"></form><script>window.hacked=true</script>`);
  }
  res.statusCode=404;res.end('{}');
});
async function refresh(page,nextMode){mode=nextMode;await page.locator('#refresh-market').click();await page.waitForFunction(()=>!document.querySelector('#refresh-market').disabled)}
(async()=>{
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;
  try{
    browser=await chromium.launch({executablePath:process.env.BROWSER_PATH||'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true});
    const context=await browser.newContext({viewport:{width:430,height:1100},deviceScaleFactor:2,isMobile:true,hasTouch:true,reducedMotion:'reduce'});
    await context.addInitScript(()=>{const schedule=window.setTimeout;window.setTimeout=(fn,delay,...args)=>schedule(fn,delay===3000?30:delay,...args)});
    await context.route('https://mp.weixin.qq.com/**',route=>route.fulfill({contentType:'image/svg+xml',body:fs.readFileSync(path.join(__dirname,'reference/demo-qr.svg'),'utf8')}));
    await context.route('https://openapi-sandbox.dl.alipaydev.com/**',route=>{submits.push(route.request());return route.fulfill({contentType:'text/html',body:'Sandbox form intercepted for contract test'})});
    const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message));const base=`http://127.0.0.1:${server.address().port}`;
    await context.addCookies([{name:'username',value:'legacy_user',url:base}]);
    await page.goto(base);assert.equal(calls.length,0,'old username cookie cannot bypass Weixin login');
    await page.locator('#group-buy').click();await page.locator('#qr-code-img').waitFor({state:'visible'});
    assert((await page.locator('#qr-code-img').getAttribute('src')).includes('ticket%2Ba%2F%26%3F'));
    await page.waitForFunction(()=>document.querySelector('#login-status').textContent.includes('扫一扫'));
    await page.screenshot({path:path.join(__dirname,'screenshots/02-登录页.png')});
    ready=true;await page.waitForURL(url=>url.hash==='#product');await page.waitForFunction(()=>document.querySelector('#index-price').textContent==='90');
    const identity=(await context.cookies()).find(cookie=>cookie.name==='loginToken');assert.equal(identity.value,'wx_openid_123');assert(identity.expires>Date.now()/1000+29*86400);
    assert.equal(calls.find(call=>call.url.includes('/gbm/')).body.userId,'wx_openid_123');
    await page.locator('#toast').evaluate(el=>el.hidden=true);await page.screenshot({path:path.join(__dirname,'screenshots/01-商品详情页.png')});
    await page.evaluate(()=>{const form=document.createElement('form');form.id='unrelated-form';form.hidden=true;document.body.append(form)});
    await refresh(page,'empty');assert.match(await page.locator('#groups').textContent(),/赶紧去开团/);
    await page.locator('#group-buy').evaluate(button=>{button.click();button.click()});await page.locator('#payment-overlay').waitFor({state:'visible'});
    const creates=()=>calls.filter(call=>call.url==='/api/v1/alipay/create_pay_order');
    assert.equal(creates().length,1);assert.deepEqual(creates()[0].body,{userId:'wx_openid_123',productId:'9890001',marketType:1,teamId:null,activityId:876543});
    assert.equal(await page.locator('#pay-price').textContent(),'78.90','confirmation amount comes from signed payment form');
    assert.equal(await page.locator('#unrelated-form').count(),1);assert.equal(await page.evaluate(()=>window.hacked),undefined);
    await page.locator('#toast').evaluate(el=>el.hidden=true);await page.screenshot({path:path.join(__dirname,'screenshots/03-弹出扫码支付页.png')});
    const popupPromise=context.waitForEvent('page');await page.locator('#go-alipay').click();const popup=await popupPromise;await popup.waitForLoadState();assert.equal(submits.length,1);assert.equal(submits[0].method(),'POST');assert.equal(JSON.parse(new URLSearchParams(submits[0].postData()).get('biz_content')).total_amount,'78.90');await popup.close();
    await page.locator('#refresh-payment').click();await page.waitForFunction(()=>!document.querySelector('#refresh-payment').disabled);assert.match(await page.locator('#payment-status').textContent(),/回调为准/);
    await page.locator('#cancel-payment').click();await refresh(page,'normal');
    await page.locator('#groups .join').click();await page.locator('#payment-overlay').waitFor({state:'visible'});assert.equal(creates().at(-1).body.teamId,'team-A');assert.equal(creates().at(-1).body.activityId,876543);await page.locator('#cancel-payment').click();
    await page.locator('#single-buy').click();await page.locator('#payment-overlay').waitFor({state:'visible'});assert.deepEqual(creates().at(-1).body,{userId:'wx_openid_123',productId:'9890001',marketType:0});assert.equal(await page.locator('#pay-price').textContent(),'100.00');await page.locator('#cancel-payment').click();
    createMode='failure';await page.locator('#group-buy').click();await page.waitForFunction(()=>!document.querySelector('#group-buy').disabled);assert(await page.locator('#payment-overlay').isHidden());assert.match(await page.locator('#toast').textContent(),/E0006/);
    createMode='bad-gateway';await page.locator('#group-buy').click();await page.waitForFunction(()=>!document.querySelector('#group-buy').disabled);assert(await page.locator('#payment-overlay').isHidden());assert.match(await page.locator('#toast').textContent(),/沙箱网关/);createMode='normal';
    await refresh(page,'missing-activity');assert(await page.locator('#group-buy').isDisabled());assert(!(await page.locator('#single-buy').isDisabled()));
    await refresh(page,'xss');assert.equal(await page.locator('#groups img').count(),0);assert.match(await page.locator('#groups').textContent(),/<img onerror/);
    await refresh(page,'failure');assert(await page.locator('#group-buy').isDisabled());assert(await page.locator('#single-buy').isDisabled());await refresh(page,'normal');
    await page.setViewportSize({width:320,height:740});assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
    ready=false;await page.locator('#account').click();await page.locator('#qr-code-img').waitFor({state:'visible'});await page.locator('#back').click();await page.waitForURL(url=>url.hash==='#product');const checks=calls.filter(call=>call.url.includes('check_login')).length;await page.waitForTimeout(150);assert.equal(calls.filter(call=>call.url.includes('check_login')).length,checks,'leaving login stops polling');
    assert.equal(calls.filter(call=>call.url.includes('/gbm/trade/')).length,0,'browser never locks or settles directly');assert.deepEqual(errors,[]);
    console.log('PASS: Weixin ticket/poll/cookie; stop polling on exit; top-level activityId (non-default and empty teams); group/join/single payloads; duplicate-click guard; isolated Alipay POST form; signed form amount; no injected scripts; business errors; invalid gateway; missing activity; API errors; mobile layout; no direct browser lock/settlement. Sandbox network intercepted; no real payments.');
    await context.close();
    const ide=await browser.newContext({viewport:{width:430,height:932}}),ideCalls=[];
    await ide.route('http://localhost:63342/**',route=>route.fulfill({contentType:'text/html; charset=utf-8',body:html}));
    await ide.route('http://127.0.0.1:8091/**',route=>{ideCalls.push(route.request().url());return route.fulfill({contentType:'application/json',body:JSON.stringify({code:'0000',data:market()})})});
    await ide.route('http://127.0.0.1:8080/**',route=>{ideCalls.push(route.request().url());return route.fulfill({contentType:'application/json',body:JSON.stringify({code:'0000',data:'ide-ticket'})})});
    await ide.route('https://mp.weixin.qq.com/**',route=>route.fulfill({contentType:'image/svg+xml',body:fs.readFileSync(path.join(__dirname,'reference/demo-qr.svg'),'utf8')}));
    await ide.addCookies([{name:'loginToken',value:'ide-test-user',url:'http://localhost:63342'}]);
    const idePage=await ide.newPage();await idePage.goto('http://localhost:63342/group-buy-market-hxy/docs/dev-ops/nginx/html/index.html');
    await idePage.waitForFunction(()=>document.querySelector('#index-price').textContent==='90');
    await idePage.locator('#account').click();await idePage.locator('#qr-code-img').waitFor({state:'visible'});
    assert(ideCalls.includes('http://127.0.0.1:8091/api/v1/gbm/index/query_group_buy_market_config'));
    assert(ideCalls.includes('http://127.0.0.1:8080/api/v1/login/weixin_qrcode_ticket'));
    await ide.close();console.log('PASS: IDEA 63342 preview directly targets local Java ports 8091 and 8080.');
  }finally{if(browser)await browser.close();server.close()}
})().catch(error=>{console.error(error);process.exitCode=1});
