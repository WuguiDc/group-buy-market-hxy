// 两个本地工程的前端跳转验证，所有后端与微信请求均被拦截。
const {chromium}=require(process.env.PLAYWRIGHT_PATH||'playwright');
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const mallRoot='D:/code_java_2026_6/s-pay-mall-ddd-market-hxy/docs/dev-ops/nginx/html';
const groupPath='/group-buy-market-hxy/docs/dev-ops/nginx/html/index.html';
const mallPath='/s-pay-mall-ddd-market-hxy/docs/dev-ops/nginx/html/';
const groupHtml=fs.readFileSync(path.resolve(__dirname,'../dev-ops/nginx/html/index.html'),'utf8');
(async()=>{
  const browser=await chromium.launch({executablePath:process.env.BROWSER_PATH||'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true});
  try{
    for(const hostname of ['localhost','127.0.0.1']){
      const context=await browser.newContext(),calls=[],errors=[];let loginChecks=0;
      await context.addInitScript(()=>{const original=setTimeout;window.setTimeout=(callback,delay,...args)=>original(callback,delay===3000?30:delay,...args)});
      await context.route('**/*',route=>{
        const request=route.request(),url=new URL(request.url());
        if(url.pathname===groupPath)return route.fulfill({contentType:'text/html; charset=utf-8',body:groupHtml});
        if(url.port==='63342'&&url.pathname.startsWith(mallPath)){
          const name=url.pathname.slice(mallPath.length);
          if(['index.html','login.html','js/mall-navigation.js'].includes(name))return route.fulfill({contentType:name.endsWith('.js')?'application/javascript':'text/html; charset=utf-8',body:fs.readFileSync(path.join(mallRoot,name),'utf8')});
        }
        if(url.pathname.includes('/api/'))calls.push({url:url.href,body:request.postDataJSON()});
        const ok=data=>route.fulfill({contentType:'application/json',body:JSON.stringify({code:'0000',data})});
        if(url.pathname.endsWith('/weixin_qrcode_ticket'))return ok('navigation-ticket');
        if(url.pathname.endsWith('/check_login'))return ++loginChecks===1?route.fulfill({contentType:'application/json',body:'{"code":"0003","info":"未登录"}'}):ok('openid_test+special');
        if(url.pathname.endsWith('/query_group_buy_market_config'))return ok({activityId:876543,goods:{goodsId:'9890001',originalPrice:100,deductionPrice:10,payPrice:90},teamList:[],teamStatistic:{allTeamCount:0,allTeamCompleteCount:0,allTeamUserCount:0}});
        if(url.hostname==='mp.weixin.qq.com'||url.pathname.match(/\.(png|ico)$/))return route.fulfill({contentType:'image/svg+xml',body:'<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200"/>'});
        return route.fulfill({status:404,body:'Not found'});
      });
      const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message));
      const base='http://'+hostname+':63342';
      await page.goto(base+mallPath+'index.html');assert((await page.locator('#group-buy-entry').getAttribute('href')).endsWith('login.html'));
      await page.locator('#group-buy-entry').click();await page.waitForURL(url=>url.pathname===groupPath&&url.hash==='#product');
      await page.waitForFunction(()=>document.querySelector('#index-price').textContent==='90');
      const identity=(await context.cookies()).find(cookie=>cookie.name==='loginToken');assert.equal(decodeURIComponent(identity.value),'openid_test+special');assert.equal(identity.path,'/');assert(identity.expires>Date.now()/1000+29*86400);
      assert.equal(new URL(page.url()).hostname,hostname);assert(!page.url().includes('loginToken='));
      assert.equal(calls.find(call=>call.url.includes('query_group_buy_market_config')).body.userId,'openid_test+special');
      assert.equal(calls.filter(call=>call.url.includes('weixin_qrcode_ticket')).length,1,'group page reuses login rather than requesting a second QR code');
      await page.goto(base+mallPath+'login.html');await page.waitForURL(url=>url.pathname===groupPath);assert.equal(calls.filter(call=>call.url.includes('weixin_qrcode_ticket')).length,1,'already logged-in DDD login page goes directly to group page');
      await page.goto(base+mallPath+'index.html');assert.equal(await page.locator('#group-buy-entry').getAttribute('href'),base+groupPath+'#product');
      assert.deepEqual(errors,[]);await context.close();
    }
    console.log('PASS: DDD entry -> Weixin login -> group purchase page; same-host 30-day Cookie shared across project paths; no token in URL; no second login; existing login redirects directly. Tested localhost and 127.0.0.1. No real API or payment calls.');
  }finally{await browser.close()}
})().catch(error=>{console.error(error);process.exitCode=1});
