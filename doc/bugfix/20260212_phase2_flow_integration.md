# Phase 2 Flow Integration 修复记录 (2026-02-12)

## 概览
目标：打通“风险评估 -> 资产体检 -> 生成计划 -> 执行计划 -> 资产/体检刷新”闭环，并修复 health/check suggestions 序列化结构。
范围：最小必要改动，无新增依赖、无 UI 重构。

---

## 0) 关键链路定位
前端：
- 风险评估页：`frontend/src/views/risk/AssessmentWizard.vue`
- 风险结果页：`frontend/src/views/risk/AssessmentResult.vue`
- 体检页：`frontend/src/views/diagnosis/Index.vue`
- 健康卡片：`frontend/src/components/HealthScoreCard.vue`
- 计划页/执行：`frontend/src/views/plan/Index.vue`、`frontend/src/views/plan/components/PlanDetailDialog.vue`、`frontend/src/components/PlanDrawer.vue`
- 资产刷新：`frontend/src/store/modules/asset.ts`
- 体检/风险 API：`frontend/src/api/health.ts`、`frontend/src/api/risk.ts`

后端：
- 风险测评：`backend/src/main/java/com/fincoach/core/controller/RiskAssessmentController.java`、`.../service/impl/RiskAssessmentServiceImpl.java`
- 健康体检：`backend/src/main/java/com/fincoach/core/controller/HealthCheckController.java`、`.../service/impl/HealthCheckServiceImpl.java`
- 计划生成/执行：`backend/src/main/java/com/fincoach/core/controller/InvestmentPlanController.java`、`.../service/impl/InvestmentPlanServiceImpl.java`
- 资产服务：`backend/src/main/java/com/fincoach/core/controller/AssetItemController.java`、`.../service/impl/AssetItemServiceImpl.java`
- 交易记录：`backend/src/main/java/com/fincoach/core/service/TransactionService.java`
- AI 咨询：`backend/src/main/java/com/fincoach/core/controller/AiController.java`、`.../service/AiService.java`

---

## 1) 修复：health/check suggestions 返回结构
问题现象：
- /api/health/check 的 suggestions 在部分客户端表现为字符串列表（非对象数组）。

根因：
- suggestions 数据结构缺少兼容处理，且前端未对旧格式做容错。

修改文件：
- `backend/src/main/java/com/fincoach/core/controller/vo/HealthReportVO.java`
- `frontend/src/store/modules/health.ts`

关键修改：
- 增加 `HealthSuggestion` 无参构造，确保序列化稳定。
- 前端对 suggestions 做兼容解析（支持 `@{type=...; message=...}` 旧字符串）。

---

## 2) 打通链路：风险评估 -> 体检/计划
问题现象：
- 完成风险测评后，体检风险匹配维度仍固定为 0，链路未打通。

根因：
- Novice 模式下 riskMatchScore 始终为 0，未反映已有测评记录。

修改文件：
- `backend/src/main/java/com/fincoach/core/service/impl/HealthCheckServiceImpl.java`
- `frontend/src/views/risk/AssessmentWizard.vue`

关键修改：
- Novice 模式存在测评记录时，riskMatchScore 赋值为基础分（10），并增加提示语。
- 风险评估提交成功后，自动触发体检刷新并跳转到体检页。

### 2.1) 修正 riskMatchScore 映射评分
旧逻辑问题：
- Novice 模式固定 `riskMatchScore=10`，无法体现真实测评结果。

新逻辑：
- 基于最近一次风险测评 `riskLevel` 做映射：
  - conservative -> 5
  - steady -> 10
  - aggressive -> 15
- 若 `riskLevel` 缺失，使用 `totalScore` 回退：
  - <=20 -> 5
  - 21-35 -> 10
  - >35 -> 15

改动文件：
- `backend/src/main/java/com/fincoach/core/service/impl/HealthCheckServiceImpl.java`

提交：
- `8cd96f9d`

验证片段：
```
{"code":200,"message":"success","data":{"score":34,"level":"待优化","userType":"INVESTOR","liquidityScore":5,"riskMatchScore":5,"protectionScore":10,"diversityScore":14,"suggestions":[{"type":"warning","message":"⚠️ 流动资金不足，建议补充现金储备"},{"type":"success","message":"✅ 风险配置与偏好完美匹配"},{"type":"info","message":"💡 建议配置固定资产增强保障力"},{"type":"info","message":"💡 可增加资产类别进一步分散风险"}]}}
{"code":200,"message":"success","data":{"score":39,"level":"待优化","userType":"INVESTOR","liquidityScore":5,"riskMatchScore":10,"protectionScore":10,"diversityScore":14,"suggestions":[{"type":"warning","message":"⚠️ 流动资金不足，建议补充现金储备"},{"type":"warning","message":"⚠️ 配置过于保守，资产可能跑输通胀"},{"type":"info","message":"💡 建议配置固定资产增强保障力"},{"type":"info","message":"💡 可增加资产类别进一步分散风险"}]}}
{"code":200,"message":"success","data":{"score":44,"level":"一般","userType":"INVESTOR","liquidityScore":5,"riskMatchScore":15,"protectionScore":10,"diversityScore":14,"suggestions":[{"type":"warning","message":"⚠️ 流动资金不足，建议补充现金储备"},{"type":"warning","message":"⚠️ 配置过于保守，资产可能跑输通胀"},{"type":"info","message":"💡 建议配置固定资产增强保障力"},{"type":"info","message":"💡 可增加资产类别进一步分散风险"}]}}
```

---

## 3) 打通链路：执行计划 -> 资产更新 -> 体检刷新
问题现象：
- 执行计划后，资产和体检状态未自动刷新，闭环断裂。

修改文件：
- `frontend/src/components/PlanDrawer.vue`
- `frontend/src/store/modules/investmentPlan.ts`

关键修改：
- 计划执行成功后，联动刷新资产列表/汇总与体检报告。

---

## 4) 打通链路：AI 咨询接入用户画像
问题现象：
- AI 咨询上下文缺少用户画像，回答不够贴合。

修改文件：
- `backend/src/main/java/com/fincoach/core/controller/AiController.java`
- `backend/src/main/java/com/fincoach/core/service/AiService.java`

关键修改：
- AI Prompt 增加用户摘要：总资产、现金余额、风险等级、体检结果。

---

## 5) 数据库/迁移
- 本次未新增表结构。

---

## 验证方式与结果
### 后端构建
命令：
```
$env:JAVA_HOME="D:\software\jdk21"; $env:Path="$env:JAVA_HOME\bin;$env:Path"; cd backend; mvn clean package
```
结果：BUILD SUCCESS

### 后端启动
命令：
```
$env:JAVA_HOME="D:\software\jdk21"; $env:Path="$env:JAVA_HOME\bin;$env:Path";
$env:DB_URL="jdbc:mysql://localhost:3306/fincoach?useSSL=false&serverTimezone=UTC";
$env:DB_USERNAME="root"; $env:DB_PASSWORD="030314"; $env:DEEPSEEK_API_KEY="test_key";
cd backend; mvn spring-boot:run
```
日志片段：
```
Tomcat started on port 8080 (http) with context path ''
Started CoreApplication in 3.323 seconds
```

### 冒烟验证
1) 风险评估提交
```
POST /api/risk/assess
{"answers":{"q1":5,"q2":5,"q3":5,"q4":5,"q5":5}}
```
返回：
```
{"code":200,"message":"success","data":{"id":5,"totalScore":25,"riskLevel":"steady","label":"稳健理财师","description":"稳中求进，追求跑赢通胀","equityLimit":0.25,"createTime":"2026-02-12T12:15:20.1673363","actualRatio":0,"idealRatio":0.25,"gap":-0.25,"diagnosis":"📋 检测到您尚未录入资产，请先完善资产档案后再查看配置建议。"}}
```

2) 体检建议结构 (对象数组)
```
GET /api/health/check
```
返回：
```
{"code":200,"message":"success","data":{"score":50,"level":"一般","userType":"NOVICE","liquidityScore":50,"riskMatchScore":10,"protectionScore":0,"diversityScore":0,"suggestions":[{"type":"warning","message":"⚠️ 您目前资产主要集中在储蓄，虽然安全但难以跑赢通胀"},{"type":"info","message":"💡 建议并在留足3~6个月应急金后，尝试低风险理财"},{"type":"success","message":"✅ 已完成风险测评，系统将根据偏好给出配置建议"}]}}
```

3) 计划生成 + 执行
```
POST /api/plan/generate?planType=CONTRIBUTION&investMoney=5000
POST /api/plan/execute?planId=13
```
返回：
```
{"code":200,"message":"success","data":{"id":13,"planName":"智能定投计划-02121216","riskLevel":"steady","riskLabel":"稳健理财师","totalAmount":50000.00,"planType":"CONTRIBUTION","investMoney":5000,"status":"draft","createTime":"2026-02-12T12:16:08.0720186","items":[{"id":null,"action":"BUY","categoryId":2,"categoryName":"金融投资","subType":"农业银行","amount":5000.00,"currentRatio":0,"targetRatio":0,"reason":"基础安全垫构建 (强制稳健配置)。优选 R1 级资产【农业银行】，预期年涨幅 0.89%。"}]}}
{"code":200,"message":"执行成功，资产已更新","data":null}
```

4) 资产刷新
```
GET /api/asset/summary
```
返回：
```
{"code":200,"message":"success","data":{"totalAmount":50000.00,"categoryDistribution":{"固定资产":0.00,"金融投资":10.00,"现金":90.00},"investmentLimit":10000.00,"safetyThreshold":30000,"liquidityGap":0,"safetyProgress":100.0,"personaTag":"🌳 增值期"}}
```

### 前端构建
命令：
```
cd frontend
npm run build
```
结果：成功（有 Sass legacy API 警告，但不影响构建）。

---

## 兼容性影响与回滚
- 兼容性影响：
  - AI 服务依赖用户画像调用（若未登录或 DB 异常会自动降级为空摘要）。

- 回滚方式：
  - `git revert <phase2相关commit>`

---

## 备注
- 因依赖本地数据库，本次冒烟验证使用本机 `root/030314`。如环境不同，请按实际账号调整。
