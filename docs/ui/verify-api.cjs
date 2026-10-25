// 浏览器接口合约测试：内存 HTTP 桩，不访问真实数据库或支付渠道。
const {chromium}=require(process.env.PLAYWRIGHT_PATH||'playwright');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http'),assert=require('node:assert/strict');
const {pathToFileURL}=require('node:url');
const API={market:'/api/v1/gbm/index/query_group_buy_market_config',lock:'/api/v1/gbm/trade/lock_market_pay_order',settle:'/api/v1/gbm/trade/settlement_market_pay_order'};
const frontend=path.resolve(__dirname,'../dev-ops/nginx/html/index.html');
const html=fs.readFileSync(frontend),calls=[],orders=new Map(),issues=[];
let marketMode='normal',failLock=false,failSettle=false,loseSettle=false;
const fixture=user=>({goods:{goodsId:'9890001',originalPrice:100,deductionPrice:10.25,payPrice:89.75},teamList:marketMode==='null'?null:marketMode==='empty'?[]:[
  ...[...orders.values()].filter(o=>o.userId===user).slice(-1).map(o=>({userId:user,teamId:o.teamId||'created-team',activityId:100123,targetCount:3,lockCount:2,completeCount:o.paid?1:0,validTimeCountdown:'00:09:59',outTradeNo:o.outTradeNo,tradeOrderStatus:o.paid?1:0})),
  {userId:'xfg02',teamId:'team-A',activityId:100123,targetCount:3,lockCount:1,completeCount:0,validTimeCountdown:'00:05:49'},
  {userId:marketMode==='xss'?'<img onerror=alert(1)>':'xfg08',teamId:'team-B',activityId:100123,targetCount:3,lockCount:2,completeCount:1,validTimeCountdown:'00:05:49'}
],teamStatistic:{allTeamCount:7,allTeamCompleteCount:1,allTeamUserCount:9}});
const server=http.createServer(async(req,res)=>{
  if(!req.url.startsWith('/api/')){res.setHeader('Content-Type','text/html; charset=utf-8');res.end(html);return}
  try{
    assert.equal(req.method,'POST');assert.match(req.headers['content-type'],/application\/json/);
    let input='';for await(const chunk of req)input+=chunk;const body=JSON.parse(input);calls.push({url:req.url,body});res.setHeader('Content-Type','application/json');
    const success=data=>res.end(JSON.stringify({code:'0000',info:'成功',data}));
    if(req.url===API.market){
      if(marketMode==='failure')return res.end(JSON.stringify({code:'E0002',info:'无拼团营销配置'}));
      if(marketMode==='http'){res.statusCode=503;res.end('{}');return}
      if(marketMode==='invalid'){res.end('not-json');return}
      if(marketMode==='null-body'){res.end('null');return}
      success(fixture(body.userId));
    }else if(req.url===API.lock){
      orders.set(body.outTradeNo,{...body,paid:false});
      if(failLock){failLock=false;res.statusCode=503;res.end('{}');return}
      success({orderId:'order-'+body.outTradeNo,deductionPrice:12.34,tradeOrderStatus:0});
    }else if(req.url===API.settle){
      if(failSettle){failSettle=false;res.end(JSON.stringify({code:'E0106',info:'订单交易时间不在拼团有效时间范围内'}));return}
      assert(orders.has(body.outTradeNo));orders.get(body.outTradeNo).paid=true;
      if(loseSettle){loseSettle=false;res.statusCode=503;res.end('{}');return}
      success({userId:body.userId,teamId:orders.get(body.outTradeNo).teamId||'created-team',activityId:100123,outTradeNo:body.outTradeNo});
    }else{res.statusCode=404;res.end('{}')}
  }catch(error){issues.push(error.message);res.statusCode=500;res.end('{}')}
});
async function login(page,name){
  await page.locator('#username').fill(name);await page.locator('#password').fill('not-a-real-password');await page.locator('.login-submit').click();await page.waitForURL(url=>url.hash==='#product');
  await page.waitForFunction(()=>document.querySelector('#index-price').textContent==='89.75');await page.waitForFunction(()=>!document.querySelector('#refresh-market').disabled);
}
async function refresh(page,mode){marketMode=mode;await page.locator('#refresh-market').click();await page.waitForFunction(()=>!document.querySelector('#refresh-market').disabled)}
(async()=>{
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;
  try{
    browser=await chromium.launch({executablePath:process.env.BROWSER_PATH||'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true});
    const context=await browser.newContext({viewport:{width:430,height:1100},deviceScaleFactor:2,isMobile:true,hasTouch:true,reducedMotion:'reduce'}),page=await context.newPage();page.on('pageerror',e=>issues.push(e.message));
    const base=`http://127.0.0.1:${server.address().port}`;await page.goto(base);
    assert.equal(calls.length,0);assert.equal(await page.locator('#index-price').textContent(),'--');
    assert(await page.locator('.slide img').evaluateAll(imgs=>imgs.length===3&&imgs.every(i=>i.complete&&i.naturalWidth>0)));
    await page.locator('.dot').nth(2).click();assert.equal(await page.locator('#slide-counter').textContent(),'3 / 3');
    await page.locator('#group-buy').click();await page.waitForURL(url=>url.hash==='#login');await page.locator('#toast').evaluate(el=>el.hidden=true);
    await page.screenshot({path:path.join(__dirname,'screenshots/02-登录页.png')});
    await login(page,'xfg01');
    assert((await context.cookies()).some(c=>c.name==='username'&&c.value==='xfg01'));assert(!(await page.evaluate(()=>JSON.stringify(localStorage))).includes('not-a-real-password'));
    assert.equal(calls[0].url,API.market);assert.deepEqual(calls[0].body,{userId:'xfg01',source:'s01',channel:'c01',goodsId:'9890001'});
    assert.equal(await page.locator('#deduction-price').textContent(),'10.25');assert.equal(await page.locator('#participant-count').textContent(),'9');
    assert.match(await page.locator('.group-status').first().textContent(),/2 人/);assert.match(await page.locator('.group-status').nth(1).textContent(),/1 人/);assert.equal(await page.locator('#groups img').count(),0);
    await page.locator('#toast').evaluate(el=>el.hidden=true);await page.screenshot({path:path.join(__dirname,'screenshots/01-商品详情页.png')});
    await page.locator('#single-buy').click();assert.equal(calls.filter(c=>c.url===API.lock).length,0);
    await page.locator('#groups .join').nth(1).evaluate(b=>{b.click();b.click()});await page.locator('#payment-overlay').waitFor({state:'visible'});assert.equal(calls.filter(c=>c.url===API.lock).length,1);
    const lock=calls.find(c=>c.url===API.lock).body;assert.equal(lock.teamId,'team-B');assert.equal(lock.userId,'xfg01');assert.equal(lock.activityId,100123);assert.match(lock.outTradeNo,/^\d+$/);assert.equal(lock.notifyUrl,'http://127.0.0.1:8091/api/v1/test/group_buy_notify');
    assert.equal(await page.locator('#pay-price').textContent(),'87.66');await page.locator('#toast').evaluate(el=>el.hidden=true);await page.screenshot({path:path.join(__dirname,'screenshots/03-弹出扫码支付页.png')});
    await page.locator('#cancel-payment').click();assert.equal(calls.filter(c=>c.url===API.settle).length,0);
    await page.reload();await page.waitForFunction(()=>document.querySelector('#index-price').textContent==='89.75');await page.locator('#group-buy').click();await page.locator('#payment-overlay').waitFor({state:'visible'});assert.equal(calls.filter(c=>c.url===API.lock).length,1);
    failSettle=true;await page.locator('#finish-payment').click();await page.locator('#payment-error').filter({hasText:'E0106'}).waitFor();assert(await page.locator('#payment-overlay').isVisible());assert(await page.locator('#success-content').isHidden());
    await page.evaluate(()=>{window.settlements=[];addEventListener('groupbuy:settled',e=>window.settlements.push(e.detail))});await page.locator('#finish-payment').click();await page.locator('#success-content').waitFor({state:'visible'});assert.equal(await page.evaluate(()=>window.settlements.length),1);
    const settlements=calls.filter(c=>c.url===API.settle);assert.equal(settlements[0].body.outTradeNo,lock.outTradeNo);assert.deepEqual(settlements[0].body,settlements[1].body);assert(Number.isFinite(Date.parse(settlements[0].body.outTradeTime)));
    await page.locator('#done-payment').click();await page.waitForFunction(()=>!document.querySelector('#refresh-market').disabled);assert.equal(await page.locator('#group-buy-label').textContent(),'已参与拼团');const count=calls.filter(c=>c.url===API.lock).length;await page.locator('#group-buy').click();assert.equal(calls.filter(c=>c.url===API.lock).length,count);
    await refresh(page,'xss');assert.equal(await page.locator('#groups img').count(),0);assert.match(await page.locator('#groups').textContent(),/<img onerror/);
    await refresh(page,'null');assert.match(await page.locator('#groups').textContent(),/做村里最靓的仔/);await refresh(page,'empty');assert.match(await page.locator('#groups').textContent(),/赶紧去开团/);
    for(const mode of ['failure','http','invalid','null-body']){await refresh(page,mode);assert(await page.locator('#group-buy').isDisabled());assert.equal(await page.locator('#index-price').textContent(),'--')}
    await refresh(page,'normal');await page.locator('#account').click();await login(page,'xfg03');failLock=true;
    await page.locator('#group-buy').click();await page.waitForFunction(()=>!document.querySelector('#group-buy').disabled);assert(await page.locator('#payment-overlay').isHidden());const failed=calls.filter(c=>c.url===API.lock).at(-1).body;assert.equal(failed.teamId,null);
    await page.locator('#group-buy').click();await page.locator('#payment-overlay').waitFor({state:'visible'});assert.equal(calls.filter(c=>c.url===API.lock).at(-1).body.outTradeNo,failed.outTradeNo);
    await page.locator('#cancel-payment').click();await page.evaluate(()=>{for(let i=localStorage.length-1;i>=0;i--){const key=localStorage.key(i);if(key.startsWith('gbm_order:'))localStorage.removeItem(key)}});
    await page.reload();await page.waitForFunction(()=>document.querySelector('#index-price').textContent==='89.75');await page.locator('#group-buy').click();await page.locator('#payment-overlay').waitFor({state:'visible'});assert.equal(calls.filter(c=>c.url===API.lock).at(-1).body.outTradeNo,failed.outTradeNo);
    loseSettle=true;await page.locator('#finish-payment').click();await page.locator('#success-content').waitFor({state:'visible'});assert(orders.get(failed.outTradeNo).paid,'lost settlement reply is reconciled via owner paid status');
    await page.locator('#done-payment').click();await page.setViewportSize({width:320,height:740});assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await page.locator('#account').click();assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await context.close();
    const offline=await browser.newContext({offline:true}),local=await offline.newPage();await local.goto(pathToFileURL(frontend).href);assert(await local.locator('.slide img').first().evaluate(img=>img.complete&&img.naturalWidth>0));await offline.close();assert.deepEqual(issues,[]);
    console.log('PASS: POST/JSON contracts; data rendering; null/empty teams; cookie identity; no password storage; lock price; selected team; cancel/reload resume; same-number retry; server recovery; settlement failure/retry; paid state; XSS-safe text; API failures; mobile layout; offline assets. 3 screenshots use contract fixtures, not real database data.');
  }finally{if(browser)await browser.close();server.close()}
})().catch(error=>{console.error(error);process.exitCode=1});
