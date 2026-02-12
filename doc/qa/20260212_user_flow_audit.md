# FinCoach 用户链路巡检 + Bug 捕获 (2026-02-12)

## 概览
- 运行方式: Playwright (MCP 未检测到可用工具，已自动降级)
- 前端: http://localhost:5173
- 后端: http://localhost:8080
- 通过率: 5/9
- 失败: 3 | 阻塞: 0 | 部分通过: 1

## Top 5 问题
- P1 TC06 资产列表/统计（asset list/summary）: locator.click: Error: strict mode violation: locator('.el-dialog').locator('.el-select') resolved to 2 elements:
    1) <div class="el-select" data-v-813c081a="">…</div> aka locator('div').filter({ hasText: /^股票$/ }).nth(1)
    2) <div class="el-select" data-v-813c081a="">…</div> aka locator('div').filter({ hasText: /^输入代码或名称搜索$/ }).nth(1)

Call log:
[2m  - waiting for locator('.el-dialog').locator('.el-select')[22m

- P2 TC07 风险测评 -> 体检闭环（risk->health）: 未跳转至体检页，当前 URL=http://localhost:5173/risk/assessment; 体检页未显示健康分数
- P1 TC08 计划生成/执行闭环（plan generate/execute）: 定位失败: 计划投入金额输入框; locator.fill: Test timeout of 1800000ms exceeded.
Call log:
[2m  - waiting for locator('.el-dialog').locator('.el-input-number input').first()[22m

- P1 TC09 AI 咨询（/api/ai/chat）: page.goto: Target page, context or browser has been closed

## 用例明细
### TC01 首页（未登录）
- 状态: pass
- URL: http://localhost:5173/login
- 预期结果:
  - 未登录访问首页自动跳转登录页或展示访客态
  - 页面无空白/报错
- 实际结果:
  - 访问首页后自动跳转到登录页
  - 登录页表单可见
- 截图: doc/qa/screenshots/20260212/TC01_home.png

### TC02 登录
- 状态: pass
- URL: http://localhost:5173/dashboard
- 预期结果:
  - 登录成功进入首页 /dashboard
  - localStorage 写入 token
- 实际结果:
  - 默认账号 admin/123456 登录失败，尝试注册新账号
  - 注册新账号成功: qa_user_592945
  - 登录成功，token=已写入
- 截图: doc/qa/screenshots/20260212/TC02_login_success.png
- Console 错误/警告 (前 10 条):
  - [error] [api] Request failed {method: POST, url: /api/auth/login, status: 200, code: 500, message: 用户名或密码错误}
  - [error] 登录异常: Error: 用户名或密码错误
    at request.interceptors.response.use.response (http://localhost:5173/src/api/request.ts?t=1770886739646:40:31)
    at async Axios.request (http://localhost:5173/node_modules/.vite/deps/axios.js?v=3f0c2f8a:2229:14)
    at async http://localhost:5173/src/pages/auth/Login.vue?t=1770886739646:36:25
    at async validateField (http://localhost:5173/node_modules/.vite/deps/element-plus.js?v=3f0c2f8a:39633:11)
    at async handleLogin (http://localhost:5173/src/pages/auth/Login.vue?t=1770886739646:32:7)
  - [error] [api] Request failed {method: GET, url: /api/risk/latest, status: 200, code: 404, message: 尚未完成风险测评}
- Network 失败/异常 (前 10 条):
- api_code_error POST http://localhost:5173/api/auth/login 200 16ms
- api_code_error GET http://localhost:5173/api/risk/latest 200 44ms
- 关键接口响应摘要:
  - GET http://localhost:5173/api/asset/list status=200 code=200 body={"code":200,"message":"success","data":[]}
  - GET http://localhost:5173/api/asset/summary status=200 code=200 body={"code":200,"message":"success","data":{"totalAmount":0,"categoryDistribution":{"固定资产":0,"金融投资":0,"现金":0},"investmentLimit":0,"safetyThreshold":30000,"liquidityGap":30000,"safetyProgress":0,"personaTag":"🌱 蓄力期"}}
  - GET http://localhost:5173/api/health/check status=200 code=200 body={"code":200,"message":"success","data":{"score":50,"level":"一般","userType":"NOVICE","liquidityScore":50,"riskMatchScore":0,"protectionScore":0,"diversityScore":0,"suggestions":[{"type":"warning","message":"⚠️ 您目前资产主要集中在储蓄，虽然安全但难以跑赢通胀"},{"type":"info","message":"💡 建议并在留足3~6个月应急金后，尝试低风险理财"}]}}
  - GET http://localhost:5173/api/health/check status=200 code=200 body={"code":200,"message":"success","data":{"score":50,"level":"一般","userType":"NOVICE","liquidityScore":50,"riskMatchScore":0,"protectionScore":0,"diversityScore":0,"suggestions":[{"type":"warning","message":"⚠️ 您目前资产主要集中在储蓄，虽然安全但难以跑赢通胀"},{"type":"info","message":"💡 建议并在留足3~6个月应急金后，尝试低风险理财"}]}}
  - GET http://localhost:5173/api/risk/latest status=200 code=404 body={"code":404,"message":"尚未完成风险测评","data":null}

### TC03 市场列表（securities）
- 状态: pass
- URL: http://localhost:5173/market
- 预期结果:
  - 市场列表可加载，分页可用
- 实际结果:
  - 市场列表行数: 71
  - 选取样本标的: 航发动力 600893
- 截图: doc/qa/screenshots/20260212/TC03_market.png
- 关键接口响应摘要:
  - GET http://localhost:5173/api/invest/watchlist status=200 code=200 body={"code":200,"message":"success","data":[]}
  - GET http://localhost:5173/api/market/securities status=200 code=200 body={"code":200,"message":"success","data":{"records":[{"id":54,"name":"航发动力","code":"600893","type":"STOCK","currentPrice":51.97,"changePercent":5.89,"openPrice":50,"highPrice":50.8,"lowPrice":49,"riskLevel":"R4","sector":"A股","description":"航空发动机唯一上市平台","marketCap":null,"peRatio":null,"volume":31931788,"turnover":1583258506,"high52w":null,"low52w":null,"bid1Price":49.08,"bid1Vol":58600,"bid2Price":49.07,"bid2Vol":25800,"bid3Price":49.06,"bid3Vol":11500,"bid4Price":49.05,"bid4Vol":14500,"bid5Price":49.04,"bid5Vol":8800,"ask1Price":49.09,"ask1Vol":28700,"ask2Price":49.1,"ask2Vol":1500,"ask3Price":49.11,"ask3Vol":24100,"ask4Price":49.12,"ask4Vol":9600,"ask5Price":49.13,"ask5Vol":1700,"peTtm":null},{"id":18,"name":"阳光电源","code":"300274","type":"STOCK","currentPrice":156.55,"changePercent":2.86,"openPrice":154.71,"highPrice":155.49,"lowPrice":149.58,"riskLevel":"R4","sector":"A股","description":"光伏逆变器龙头企业","marketCap":null,"peRatio":null,"volume":47922307,"turnover":7296757022.25,"high52w":nul

### TC04 股票详情 + K线（kline/initChart）
- 状态: pass
- URL: http://localhost:5173/market/detail/600893?name=%E8%88%AA%E5%8F%91%E5%8A%A8%E5%8A%9B
- 预期结果:
  - 进入证券详情页
  - K 线区域正常渲染，无 Request failed
- 实际结果:
  - K线图 canvas 数量: 1
- 截图: doc/qa/screenshots/20260212/TC04_kline.png
- 关键接口响应摘要:
  - GET http://localhost:5173/api/invest/watchlist status=200 code=200 body={"code":200,"message":"success","data":[]}
  - GET http://localhost:5173/api/market/securities status=200 code=200 body={"code":200,"message":"success","data":{"records":[{"id":54,"name":"航发动力","code":"600893","type":"STOCK","currentPrice":51.97,"changePercent":5.89,"openPrice":50,"highPrice":50.8,"lowPrice":49,"riskLevel":"R4","sector":"A股","description":"航空发动机唯一上市平台","marketCap":null,"peRatio":null,"volume":31931788,"turnover":1583258506,"high52w":null,"low52w":null,"bid1Price":49.08,"bid1Vol":58600,"bid2Price":49.07,"bid2Vol":25800,"bid3Price":49.06,"bid3Vol":11500,"bid4Price":49.05,"bid4Vol":14500,"bid5Price":49.04,"bid5Vol":8800,"ask1Price":49.09,"ask1Vol":28700,"ask2Price":49.1,"ask2Vol":1500,"ask3Price":49.11,"ask3Vol":24100,"ask4Price":49.12,"ask4Vol":9600,"ask5Price":49.13,"ask5Vol":1700,"peTtm":null},{"id":18,"name":"阳光电源","code":"300274","type":"STOCK","currentPrice":156.55,"changePercent":2.86,"openPrice":154.71,"highPrice":155.49,"lowPrice":149.58,"riskLevel":"R4","sector":"A股","description":"光伏逆变器龙头企业","marketCap":null,"peRatio":null,"volume":47922307,"turnover":7296757022.25,"high52w":nul
  - GET http://localhost:5173/api/market/notices/600893 status=200 code=200 body={"code":200,"message":"success","data":[{"title":"关于召开2025年年度股东大会的通知","date":"2026-02-11"},{"title":"2025年第一季度业绩预告","date":"2026-02-07"},{"title":"关于控股股东增持股份计划的进展公告","date":"2026-01-31"},{"title":"关于分配2024年度现金股利的实施公告","date":"2026-01-23"}]}
  - GET http://localhost:5173/api/invest/watchlist status=200 code=200 body={"code":200,"message":"success","data":[]}
  - GET http://localhost:5173/api/market/detail/600893 status=200 code=200 body={"code":200,"message":"success","data":{"id":54,"name":"航发动力","code":"600893","type":"STOCK","currentPrice":51.97,"changePercent":5.89,"openPrice":50,"highPrice":50.8,"lowPrice":49,"riskLevel":"R4","sector":"A股","description":"航空发动机唯一上市平台","marketCap":null,"peRatio":null,"volume":31931788,"turnover":1583258506,"high52w":null,"low52w":null,"bid1Price":49.08,"bid1Vol":58600,"bid2Price":49.07,"bid2Vol":25800,"bid3Price":49.06,"bid3Vol":11500,"bid4Price":49.05,"bid4Vol":14500,"bid5Price":49.04,"bid5Vol":8800,"ask1Price":49.09,"ask1Vol":28700,"ask2Price":49.1,"ask2Vol":1500,"ask3Price":49.11,"ask3Vol":24100,"ask4Price":49.12,"ask4Vol":9600,"ask5Price":49.13,"ask5Vol":1700,"peTtm":null}}
  - GET http://localhost:5173/api/market/finance/600893 status=200 code=200 body={"code":200,"message":"success","data":{"profit_growth":"+8.3%","revenue":"22.22 亿","revenue_growth":"+12.5%","eps":1.73,"roe":"15.4%","profit":"4.00 亿"}}
  - GET http://localhost:5173/api/market/kline status=200 code=-- body=[{"day":"2024-11-22","open":"42.300","high":"42.550","low":"41.140","close":"41.180","volume":"16988436"},{"day":"2024-11-25","open":"41.160","high":"41.380","low":"39.650","close":"40.030","volume":"22214509"},{"day":"2024-11-26","open":"40.280","high":"41.180","low":"40.140","close":"40.450","volume":"14282620"},{"day":"2024-11-27","open":"40.070","high":"41.500","low":"39.900","close":"41.290","volume":"17213376"},{"day":"2024-11-28","open":"41.160","high":"42.140","low":"40.900","close":"40.970","volume":"14112388"},{"day":"2024-11-29","open":"40.910","high":"42.110","low":"40.840","close":"41.880","volume":"16754553"},{"day":"2024-12-02","open":"41.950","high":"42.370","low":"41.330","close":"41.800","volume":"26762329"},{"day":"2024-12-03","open":"41.810","high":"41.880","low":"41.130","close":"41.620","volume":"14414080"},{"day":"2024-12-04","open":"41.550","high":"41.700","low":"41.020","close":"41.360","volume":"13410800"},{"day":"2024-12-05","open":"41.170","high":"42.080","l

### TC05 自选（watchlist 增删查）
- 状态: pass
- URL: http://localhost:5173/investment
- 预期结果:
  - 标的可加入自选并在自选列表中可见
  - 刷新后仍可见（持久化）
- 实际结果:
  - 自选列表数量: 1
  - 自选列表包含标的 600893
  - 刷新后自选列表数量: 1
- 截图: doc/qa/screenshots/20260212/TC05_watchlist.png
- 关键接口响应摘要:
  - GET http://localhost:5173/api/market/notices/600893 status=200 code=200 body={"code":200,"message":"success","data":[{"title":"关于召开2025年年度股东大会的通知","date":"2026-02-11"},{"title":"2025年第一季度业绩预告","date":"2026-02-07"},{"title":"关于控股股东增持股份计划的进展公告","date":"2026-01-31"},{"title":"关于分配2024年度现金股利的实施公告","date":"2026-01-23"}]}
  - GET http://localhost:5173/api/invest/watchlist status=200 code=200 body={"code":200,"message":"success","data":[]}
  - GET http://localhost:5173/api/market/finance/600893 status=200 code=200 body={"code":200,"message":"success","data":{"profit_growth":"+8.3%","revenue":"22.22 亿","revenue_growth":"+12.5%","eps":1.73,"roe":"15.4%","profit":"4.00 亿"}}
  - GET http://localhost:5173/api/market/detail/600893 status=200 code=200 body={"code":200,"message":"success","data":{"id":54,"name":"航发动力","code":"600893","type":"STOCK","currentPrice":51.97,"changePercent":5.89,"openPrice":50,"highPrice":50.8,"lowPrice":49,"riskLevel":"R4","sector":"A股","description":"航空发动机唯一上市平台","marketCap":null,"peRatio":null,"volume":31931788,"turnover":1583258506,"high52w":null,"low52w":null,"bid1Price":49.08,"bid1Vol":58600,"bid2Price":49.07,"bid2Vol":25800,"bid3Price":49.06,"bid3Vol":11500,"bid4Price":49.05,"bid4Vol":14500,"bid5Price":49.04,"bid5Vol":8800,"ask1Price":49.09,"ask1Vol":28700,"ask2Price":49.1,"ask2Vol":1500,"ask3Price":49.11,"ask3Vol":24100,"ask4Price":49.12,"ask4Vol":9600,"ask5Price":49.13,"ask5Vol":1700,"peTtm":null}}
  - GET http://localhost:5173/api/market/kline status=200 code=-- body=[{"day":"2024-11-22","open":"42.300","high":"42.550","low":"41.140","close":"41.180","volume":"16988436"},{"day":"2024-11-25","open":"41.160","high":"41.380","low":"39.650","close":"40.030","volume":"22214509"},{"day":"2024-11-26","open":"40.280","high":"41.180","low":"40.140","close":"40.450","volume":"14282620"},{"day":"2024-11-27","open":"40.070","high":"41.500","low":"39.900","close":"41.290","volume":"17213376"},{"day":"2024-11-28","open":"41.160","high":"42.140","low":"40.900","close":"40.970","volume":"14112388"},{"day":"2024-11-29","open":"40.910","high":"42.110","low":"40.840","close":"41.880","volume":"16754553"},{"day":"2024-12-02","open":"41.950","high":"42.370","low":"41.330","close":"41.800","volume":"26762329"},{"day":"2024-12-03","open":"41.810","high":"41.880","low":"41.130","close":"41.620","volume":"14414080"},{"day":"2024-12-04","open":"41.550","high":"41.700","low":"41.020","close":"41.360","volume":"13410800"},{"day":"2024-12-05","open":"41.170","high":"42.080","l
  - POST http://localhost:5173/api/invest/watchlist/toggle status=200 code=200 body={"code":200,"message":"success","data":true}
  - GET http://localhost:5173/api/invest/watchlist status=200 code=200 body={"code":200,"message":"success","data":["600893"]}
  - GET http://localhost:5173/api/market/securities status=200 code=200 body={"code":200,"message":"success","data":{"records":[{"id":54,"name":"航发动力","code":"600893","type":"STOCK","currentPrice":51.97,"changePercent":5.89,"openPrice":50,"highPrice":50.8,"lowPrice":49,"riskLevel":"R4","sector":"A股","description":"航空发动机唯一上市平台","marketCap":null,"peRatio":null,"volume":31931788,"turnover":1583258506,"high52w":null,"low52w":null,"bid1Price":49.08,"bid1Vol":58600,"bid2Price":49.07,"bid2Vol":25800,"bid3Price":49.06,"bid3Vol":11500,"bid4Price":49.05,"bid4Vol":14500,"bid5Price":49.04,"bid5Vol":8800,"ask1Price":49.09,"ask1Vol":28700,"ask2Price":49.1,"ask2Vol":1500,"ask3Price":49.11,"ask3Vol":24100,"ask4Price":49.12,"ask4Vol":9600,"ask5Price":49.13,"ask5Vol":1700,"peTtm":null},{"id":18,"name":"阳光电源","code":"300274","type":"STOCK","currentPrice":156.55,"changePercent":2.86,"openPrice":154.71,"highPrice":155.49,"lowPrice":149.58,"riskLevel":"R4","sector":"A股","description":"光伏逆变器龙头企业","marketCap":null,"peRatio":null,"volume":47922307,"turnover":7296757022.25,"high52w":nul
  - GET http://localhost:5173/api/invest/watchlist status=200 code=200 body={"code":200,"message":"success","data":["600893"]}
  - GET http://localhost:5173/api/market/securities status=200 code=200 body={"code":200,"message":"success","data":{"records":[{"id":54,"name":"航发动力","code":"600893","type":"STOCK","currentPrice":51.97,"changePercent":5.89,"openPrice":50,"highPrice":50.8,"lowPrice":49,"riskLevel":"R4","sector":"A股","description":"航空发动机唯一上市平台","marketCap":null,"peRatio":null,"volume":31931788,"turnover":1583258506,"high52w":null,"low52w":null,"bid1Price":49.08,"bid1Vol":58600,"bid2Price":49.07,"bid2Vol":25800,"bid3Price":49.06,"bid3Vol":11500,"bid4Price":49.05,"bid4Vol":14500,"bid5Price":49.04,"bid5Vol":8800,"ask1Price":49.09,"ask1Vol":28700,"ask2Price":49.1,"ask2Vol":1500,"ask3Price":49.11,"ask3Vol":24100,"ask4Price":49.12,"ask4Vol":9600,"ask5Price":49.13,"ask5Vol":1700,"peTtm":null},{"id":18,"name":"阳光电源","code":"300274","type":"STOCK","currentPrice":156.55,"changePercent":2.86,"openPrice":154.71,"highPrice":155.49,"lowPrice":149.58,"riskLevel":"R4","sector":"A股","description":"光伏逆变器龙头企业","marketCap":null,"peRatio":null,"volume":47922307,"turnover":7296757022.25,"high52w":nul

### TC06 资产列表/统计（asset list/summary）
- 状态: fail
- URL: http://localhost:5173/asset/manage
- 预期结果:
  - 资产列表可见
  - 新增资产后统计刷新
- 异常/问题:
  - locator.click: Error: strict mode violation: locator('.el-dialog').locator('.el-select') resolved to 2 elements:
    1) <div class="el-select" data-v-813c081a="">…</div> aka locator('div').filter({ hasText: /^股票$/ }).nth(1)
    2) <div class="el-select" data-v-813c081a="">…</div> aka locator('div').filter({ hasText: /^输入代码或名称搜索$/ }).nth(1)

Call log:
[2m  - waiting for locator('.el-dialog').locator('.el-select')[22m

- 关键接口响应摘要:
  - GET http://localhost:5173/api/asset/list status=200 code=200 body={"code":200,"message":"success","data":[]}

### TC07 风险测评 -> 体检闭环（risk->health）
- 状态: partial
- URL: http://localhost:5173/risk/assessment
- 预期结果:
  - 风险测评提交后跳转体检页
  - 体检结果刷新
- 异常/问题:
  - 未跳转至体检页，当前 URL=http://localhost:5173/risk/assessment
  - 体检页未显示健康分数
- 截图: doc/qa/screenshots/20260212/TC07_risk_health.png

### TC08 计划生成/执行闭环（plan generate/execute）
- 状态: fail
- URL: http://localhost:5173/plan
- 预期结果:
  - 可生成计划并执行
  - 资产统计刷新
- 异常/问题:
  - 定位失败: 计划投入金额输入框
  - locator.fill: Test timeout of 1800000ms exceeded.
Call log:
[2m  - waiting for locator('.el-dialog').locator('.el-input-number input').first()[22m

- Console 错误/警告 (前 10 条):
  - [warning] [Vue warn]: Property "Plus" was accessed during render but is not defined on instance. 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< undefined > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< undefined > > 
  at <RouterView> 
  at <App>
  - [warning] [Vue warn]: Property "Plus" was accessed during render but is not defined on instance. 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
  - [warning] [Vue warn]: Property "Plus" was accessed during render but is not defined on instance. 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
  - [warning] [Vue warn]: Property "Plus" was accessed during render but is not defined on instance. 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
  - [warning] [Vue warn]: Property "step" was accessed during render but is not defined on instance. 
  at <ElDialogContent key=0 ref_key="dialogContentRef" ref=Ref< undefined >  ... > 
  at <ElFocusTrap loop="" trapped=true focus-start-el="container"  ... > 
  at <ElOverlay custom-mask-event="" mask=true overlay-class= [, el-modal-dialog, ]  ... > 
  at <BaseTransition onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave> onAfterLeave=fn<afterLeave>  ... > 
  at <Transition name="dialog-fade" onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave>  ... > 
  at <Teleport to="body" disabled=true > 
  at <ElDialog modelValue=true onUpdate:modelValue=fn title="生成投资计划"  ... > 
  at <CreatePlanDialog modelValue=true onUpdate:modelValue=fn onSuccess=fn<handlePlanCreated> > 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
  - [warning] [Vue warn]: Property "draft" was accessed during render but is not defined on instance. 
  at <ElDialogContent key=0 ref_key="dialogContentRef" ref=Ref< undefined >  ... > 
  at <ElFocusTrap loop="" trapped=true focus-start-el="container"  ... > 
  at <ElOverlay custom-mask-event="" mask=true overlay-class= [, el-modal-dialog, ]  ... > 
  at <BaseTransition onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave> onAfterLeave=fn<afterLeave>  ... > 
  at <Transition name="dialog-fade" onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave>  ... > 
  at <Teleport to="body" disabled=true > 
  at <ElDialog modelValue=true onUpdate:modelValue=fn title="生成投资计划"  ... > 
  at <CreatePlanDialog modelValue=true onUpdate:modelValue=fn onSuccess=fn<handlePlanCreated> > 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
  - [warning] [Vue warn]: Property "draft" was accessed during render but is not defined on instance. 
  at <ElDialogContent key=0 ref_key="dialogContentRef" ref=Ref< undefined >  ... > 
  at <ElFocusTrap loop="" trapped=true focus-start-el="container"  ... > 
  at <ElOverlay custom-mask-event="" mask=true overlay-class= [, el-modal-dialog, ]  ... > 
  at <BaseTransition onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave> onAfterLeave=fn<afterLeave>  ... > 
  at <Transition name="dialog-fade" onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave>  ... > 
  at <Teleport to="body" disabled=true > 
  at <ElDialog modelValue=true onUpdate:modelValue=fn title="生成投资计划"  ... > 
  at <CreatePlanDialog modelValue=true onUpdate:modelValue=fn onSuccess=fn<handlePlanCreated> > 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
  - [warning] [Vue warn]: Property "draft" was accessed during render but is not defined on instance. 
  at <ElDialogContent key=0 ref_key="dialogContentRef" ref=Ref< undefined >  ... > 
  at <ElFocusTrap loop="" trapped=true focus-start-el="container"  ... > 
  at <ElOverlay custom-mask-event="" mask=true overlay-class= [, el-modal-dialog, ]  ... > 
  at <BaseTransition onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave> onAfterLeave=fn<afterLeave>  ... > 
  at <Transition name="dialog-fade" onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave>  ... > 
  at <Teleport to="body" disabled=true > 
  at <ElDialog modelValue=true onUpdate:modelValue=fn title="生成投资计划"  ... > 
  at <CreatePlanDialog modelValue=true onUpdate:modelValue=fn onSuccess=fn<handlePlanCreated> > 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
  - [warning] [Vue warn]: Property "planName" was accessed during render but is not defined on instance. 
  at <ElDialogContent key=0 ref_key="dialogContentRef" ref=Ref< undefined >  ... > 
  at <ElFocusTrap loop="" trapped=true focus-start-el="container"  ... > 
  at <ElOverlay custom-mask-event="" mask=true overlay-class= [, el-modal-dialog, ]  ... > 
  at <BaseTransition onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave> onAfterLeave=fn<afterLeave>  ... > 
  at <Transition name="dialog-fade" onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave>  ... > 
  at <Teleport to="body" disabled=true > 
  at <ElDialog modelValue=true onUpdate:modelValue=fn title="生成投资计划"  ... > 
  at <CreatePlanDialog modelValue=true onUpdate:modelValue=fn onSuccess=fn<handlePlanCreated> > 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
  - [warning] [Vue warn]: Property "step" was accessed during render but is not defined on instance. 
  at <ElDialogContent key=0 ref_key="dialogContentRef" ref=Ref< undefined >  ... > 
  at <ElFocusTrap loop="" trapped=true focus-start-el="container"  ... > 
  at <ElOverlay custom-mask-event="" mask=true overlay-class= [, el-modal-dialog, ]  ... > 
  at <BaseTransition onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave> onAfterLeave=fn<afterLeave>  ... > 
  at <Transition name="dialog-fade" onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave>  ... > 
  at <Teleport to="body" disabled=true > 
  at <ElDialog modelValue=true onUpdate:modelValue=fn title="生成投资计划"  ... > 
  at <CreatePlanDialog modelValue=true onUpdate:modelValue=fn onSuccess=fn<handlePlanCreated> > 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
- 关键接口响应摘要:
  - GET http://localhost:5173/api/plan/list status=200 code=200 body={"code":200,"message":"success","data":[]}

### TC09 AI 咨询（/api/ai/chat）
- 状态: fail
- URL: http://localhost:5173/plan
- 预期结果:
  - AI 返回 Markdown 文本
  - Network 200，无前端报错
- 异常/问题:
  - page.goto: Target page, context or browser has been closed

## Bug 清单 (P0/P1/P2)
- P1 | TC06 资产列表/统计（asset list/summary）
  - 复现步骤: 见 TC06 用例执行流程
  - 疑似根因: locator.click: Error: strict mode violation: locator('.el-dialog').locator('.el-select') resolved to 2 elements:
    1) <div class="el-select" data-v-813c081a="">…</div> aka locator('div').filter({ hasText: /^股票$/ }).nth(1)
    2) <div class="el-select" data-v-813c081a="">…</div> aka locator('div').filter({ hasText: /^输入代码或名称搜索$/ }).nth(1)

Call log:
[2m  - waiting for locator('.el-dialog').locator('.el-select')[22m

  - 建议修复点: 检查前后端对应接口/页面渲染逻辑与权限校验
- P2 | TC07 风险测评 -> 体检闭环（risk->health）
  - 复现步骤: 见 TC07 用例执行流程
  - 证据: doc/qa/screenshots/20260212/TC07_risk_health.png
  - 疑似根因: 未跳转至体检页，当前 URL=http://localhost:5173/risk/assessment; 体检页未显示健康分数
  - 建议修复点: 检查前后端对应接口/页面渲染逻辑与权限校验
- P1 | TC08 计划生成/执行闭环（plan generate/execute）
  - 复现步骤: 见 TC08 用例执行流程
  - Console: [Vue warn]: Property "Plus" was accessed during render but is not defined on instance. 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< undefined > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< undefined > > 
  at <RouterView> 
  at <App> | [Vue warn]: Property "Plus" was accessed during render but is not defined on instance. 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
  - 疑似根因: 定位失败: 计划投入金额输入框; locator.fill: Test timeout of 1800000ms exceeded.
Call log:
[2m  - waiting for locator('.el-dialog').locator('.el-input-number input').first()[22m

  - 建议修复点: 检查前后端对应接口/页面渲染逻辑与权限校验
- P1 | TC09 AI 咨询（/api/ai/chat）
  - 复现步骤: 见 TC09 用例执行流程
  - 疑似根因: page.goto: Target page, context or browser has been closed
  - 建议修复点: 检查前后端对应接口/页面渲染逻辑与权限校验

## 附录
### 失败请求表 (status>=400 或 requestfailed)
- api_code_error POST http://localhost:5173/api/auth/login status=200 duration=16ms
- api_code_error GET http://localhost:5173/api/risk/latest status=200 duration=44ms

### Console 错误表
- [error] [api] Request failed {method: POST, url: /api/auth/login, status: 200, code: 500, message: 用户名或密码错误}
- [error] 登录异常: Error: 用户名或密码错误
    at request.interceptors.response.use.response (http://localhost:5173/src/api/request.ts?t=1770886739646:40:31)
    at async Axios.request (http://localhost:5173/node_modules/.vite/deps/axios.js?v=3f0c2f8a:2229:14)
    at async http://localhost:5173/src/pages/auth/Login.vue?t=1770886739646:36:25
    at async validateField (http://localhost:5173/node_modules/.vite/deps/element-plus.js?v=3f0c2f8a:39633:11)
    at async handleLogin (http://localhost:5173/src/pages/auth/Login.vue?t=1770886739646:32:7)
- [error] [api] Request failed {method: GET, url: /api/risk/latest, status: 200, code: 404, message: 尚未完成风险测评}
- [warning] [Vue warn]: Property "Plus" was accessed during render but is not defined on instance. 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< undefined > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< undefined > > 
  at <RouterView> 
  at <App>
- [warning] [Vue warn]: Property "Plus" was accessed during render but is not defined on instance. 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
- [warning] [Vue warn]: Property "Plus" was accessed during render but is not defined on instance. 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
- [warning] [Vue warn]: Property "Plus" was accessed during render but is not defined on instance. 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
- [warning] [Vue warn]: Property "step" was accessed during render but is not defined on instance. 
  at <ElDialogContent key=0 ref_key="dialogContentRef" ref=Ref< undefined >  ... > 
  at <ElFocusTrap loop="" trapped=true focus-start-el="container"  ... > 
  at <ElOverlay custom-mask-event="" mask=true overlay-class= [, el-modal-dialog, ]  ... > 
  at <BaseTransition onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave> onAfterLeave=fn<afterLeave>  ... > 
  at <Transition name="dialog-fade" onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave>  ... > 
  at <Teleport to="body" disabled=true > 
  at <ElDialog modelValue=true onUpdate:modelValue=fn title="生成投资计划"  ... > 
  at <CreatePlanDialog modelValue=true onUpdate:modelValue=fn onSuccess=fn<handlePlanCreated> > 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
- [warning] [Vue warn]: Property "draft" was accessed during render but is not defined on instance. 
  at <ElDialogContent key=0 ref_key="dialogContentRef" ref=Ref< undefined >  ... > 
  at <ElFocusTrap loop="" trapped=true focus-start-el="container"  ... > 
  at <ElOverlay custom-mask-event="" mask=true overlay-class= [, el-modal-dialog, ]  ... > 
  at <BaseTransition onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave> onAfterLeave=fn<afterLeave>  ... > 
  at <Transition name="dialog-fade" onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave>  ... > 
  at <Teleport to="body" disabled=true > 
  at <ElDialog modelValue=true onUpdate:modelValue=fn title="生成投资计划"  ... > 
  at <CreatePlanDialog modelValue=true onUpdate:modelValue=fn onSuccess=fn<handlePlanCreated> > 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
- [warning] [Vue warn]: Property "draft" was accessed during render but is not defined on instance. 
  at <ElDialogContent key=0 ref_key="dialogContentRef" ref=Ref< undefined >  ... > 
  at <ElFocusTrap loop="" trapped=true focus-start-el="container"  ... > 
  at <ElOverlay custom-mask-event="" mask=true overlay-class= [, el-modal-dialog, ]  ... > 
  at <BaseTransition onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave> onAfterLeave=fn<afterLeave>  ... > 
  at <Transition name="dialog-fade" onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave>  ... > 
  at <Teleport to="body" disabled=true > 
  at <ElDialog modelValue=true onUpdate:modelValue=fn title="生成投资计划"  ... > 
  at <CreatePlanDialog modelValue=true onUpdate:modelValue=fn onSuccess=fn<handlePlanCreated> > 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
- [warning] [Vue warn]: Property "draft" was accessed during render but is not defined on instance. 
  at <ElDialogContent key=0 ref_key="dialogContentRef" ref=Ref< undefined >  ... > 
  at <ElFocusTrap loop="" trapped=true focus-start-el="container"  ... > 
  at <ElOverlay custom-mask-event="" mask=true overlay-class= [, el-modal-dialog, ]  ... > 
  at <BaseTransition onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave> onAfterLeave=fn<afterLeave>  ... > 
  at <Transition name="dialog-fade" onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave>  ... > 
  at <Teleport to="body" disabled=true > 
  at <ElDialog modelValue=true onUpdate:modelValue=fn title="生成投资计划"  ... > 
  at <CreatePlanDialog modelValue=true onUpdate:modelValue=fn onSuccess=fn<handlePlanCreated> > 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
- [warning] [Vue warn]: Property "planName" was accessed during render but is not defined on instance. 
  at <ElDialogContent key=0 ref_key="dialogContentRef" ref=Ref< undefined >  ... > 
  at <ElFocusTrap loop="" trapped=true focus-start-el="container"  ... > 
  at <ElOverlay custom-mask-event="" mask=true overlay-class= [, el-modal-dialog, ]  ... > 
  at <BaseTransition onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave> onAfterLeave=fn<afterLeave>  ... > 
  at <Transition name="dialog-fade" onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave>  ... > 
  at <Teleport to="body" disabled=true > 
  at <ElDialog modelValue=true onUpdate:modelValue=fn title="生成投资计划"  ... > 
  at <CreatePlanDialog modelValue=true onUpdate:modelValue=fn onSuccess=fn<handlePlanCreated> > 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
- [warning] [Vue warn]: Property "step" was accessed during render but is not defined on instance. 
  at <ElDialogContent key=0 ref_key="dialogContentRef" ref=Ref< undefined >  ... > 
  at <ElFocusTrap loop="" trapped=true focus-start-el="container"  ... > 
  at <ElOverlay custom-mask-event="" mask=true overlay-class= [, el-modal-dialog, ]  ... > 
  at <BaseTransition onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave> onAfterLeave=fn<afterLeave>  ... > 
  at <Transition name="dialog-fade" onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave>  ... > 
  at <Teleport to="body" disabled=true > 
  at <ElDialog modelValue=true onUpdate:modelValue=fn title="生成投资计划"  ... > 
  at <CreatePlanDialog modelValue=true onUpdate:modelValue=fn onSuccess=fn<handlePlanCreated> > 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
- [warning] [Vue warn]: Property "step" was accessed during render but is not defined on instance. 
  at <ElDialogContent key=0 ref_key="dialogContentRef" ref=Ref< undefined >  ... > 
  at <ElFocusTrap loop="" trapped=true focus-start-el="container"  ... > 
  at <ElOverlay custom-mask-event="" mask=true overlay-class= [, el-modal-dialog, ]  ... > 
  at <BaseTransition onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave> onAfterLeave=fn<afterLeave>  ... > 
  at <Transition name="dialog-fade" onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave>  ... > 
  at <Teleport to="body" disabled=true > 
  at <ElDialog modelValue=true onUpdate:modelValue=fn title="生成投资计划"  ... > 
  at <CreatePlanDialog modelValue=true onUpdate:modelValue=fn onSuccess=fn<handlePlanCreated> > 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>
- [warning] [Vue warn]: Property "saving" was accessed during render but is not defined on instance. 
  at <ElDialogContent key=0 ref_key="dialogContentRef" ref=Ref< undefined >  ... > 
  at <ElFocusTrap loop="" trapped=true focus-start-el="container"  ... > 
  at <ElOverlay custom-mask-event="" mask=true overlay-class= [, el-modal-dialog, ]  ... > 
  at <BaseTransition onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave> onAfterLeave=fn<afterLeave>  ... > 
  at <Transition name="dialog-fade" onAfterEnter=fn<afterEnter> onBeforeLeave=fn<beforeLeave>  ... > 
  at <Teleport to="body" disabled=true > 
  at <ElDialog modelValue=true onUpdate:modelValue=fn title="生成投资计划"  ... > 
  at <CreatePlanDialog modelValue=true onUpdate:modelValue=fn onSuccess=fn<handlePlanCreated> > 
  at <Index onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > key="/plan" > 
  at <BaseTransition mode="out-in" appear=false persisted=false  ... > 
  at <Transition name="fade-transform" mode="out-in" > 
  at <RouterView> 
  at <ElMain class="main-content" > 
  at <ElContainer class="top-layout" > 
  at <TopLayout onVnodeUnmounted=fn<onVnodeUnmounted> ref=Ref< Proxy(Object) > > 
  at <RouterView> 
  at <App>

### 截图清单
- TC01: doc/qa/screenshots/20260212/TC01_home.png
- TC02: doc/qa/screenshots/20260212/TC02_login_success.png
- TC03: doc/qa/screenshots/20260212/TC03_market.png
- TC04: doc/qa/screenshots/20260212/TC04_kline.png
- TC05: doc/qa/screenshots/20260212/TC05_watchlist.png
- TC07: doc/qa/screenshots/20260212/TC07_risk_health.png