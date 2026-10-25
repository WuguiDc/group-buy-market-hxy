// 已部署 Nginx/Java 的只读浏览器检查，不创建或结算订单。
const {chromium}=require(process.env.PLAYWRIGHT_PATH||'playwright');
const assert=require('node:assert/strict');
const path=require('node:path');
(async()=>{
  const browser=await chromium.launch({executablePath:process.env.BROWSER_PATH||'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true});
  try{
    const context=await browser.newContext({viewport:{width:430,height:1100},deviceScaleFactor:2,reducedMotion:'reduce'});
    const page=await context.newPage(),errors=[],trades=[];
    page.on('pageerror',error=>errors.push(error.message));
    page.on('request',request=>{if(/\/trade\//.test(request.url()))trades.push(request.url())});
    const base=process.env.LIVE_URL||'http://127.0.0.1';
    assert.equal((await page.goto(base)).status(),200);
    await page.locator('#group-buy').click();await page.locator('#username').fill('xfg01');await page.locator('#password').fill('readonly-check');
    const response=page.waitForResponse(res=>res.url().endsWith('/api/v1/gbm/index/query_group_buy_market_config')&&res.request().method()==='POST');
    await page.locator('.login-submit').click();const result=await(await response).json();assert.equal(result.code,'0000');
    await page.waitForFunction(()=>!document.querySelector('#refresh-market').disabled&&document.querySelector('#index-price').textContent!=='--');
    assert.equal(Number(await page.locator('#index-price').textContent()),Number(result.data.goods.payPrice));
    assert.equal(Number(await page.locator('#participant-count').textContent()),Number(result.data.teamStatistic.allTeamUserCount));
    assert((await context.cookies()).some(cookie=>cookie.name==='username'&&cookie.value==='xfg01'));
    for(const row of await page.locator('#groups .group').all()){
      if((await row.textContent()).includes('拼团已结束'))assert(await row.locator('button').isDisabled());
    }
    await page.locator('#toast').evaluate(el=>el.hidden=true);
    await page.screenshot({path:path.join(__dirname,'screenshots/04-WSL部署实测.png')});
    assert.deepEqual(errors,[]);assert.deepEqual(trades,[]);
    console.log('PASS: actual Nginx page, cookie login, same-origin POST proxy, real Java/MySQL response and price rendering. No orders created or settled.');
  }finally{await browser.close()}
})().catch(error=>{console.error(error);process.exitCode=1});
