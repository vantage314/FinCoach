# Bugfix Phase 1 修复记录

## 概览
本次修复仅针对已确认问题进行最小改动，未引入新依赖、未进行大规模重构。

---

## 1) 统一后端鉴权机制
问题现象：
controller 内 userId 获取方式混用，部分接口在拦截器通过后仍拿不到 userId；未登录返回格式不统一。

根因分析：
AuthInterceptor 与 JwtInterceptor 并存，Controller 仍依赖 request attribute，未统一到 UserContext ThreadLocal。

修改文件：
- `backend/src/main/java/com/fincoach/core/config/AuthInterceptor.java`
- `backend/src/main/java/com/fincoach/core/interceptor/JwtInterceptor.java`
- `backend/src/main/java/com/fincoach/interceptor/JwtInterceptor.java`
- `backend/src/main/java/com/fincoach/core/controller/AssetItemController.java`
- `backend/src/main/java/com/fincoach/core/controller/HealthCheckController.java`
- `backend/src/main/java/com/fincoach/core/controller/RiskAssessmentController.java`
- `backend/src/main/java/com/fincoach/core/controller/InvestmentPlanController.java`

关键代码说明：
- 取消 AuthInterceptor 注册（保留类，不实现 WebMvcConfigurer）。
- Controller 获取 userId 统一为 `UserContext.getCurrentUserId()`。
- JwtInterceptor 统一返回 `Result` JSON 的 401。

验证方式与结果：
- 见“Phase 1 JDK21 构建与运行验证”。

---

## 2) 统一前端 request 封装
问题现象：
`src/api/request.ts` 与 `src/utils/request.ts` 并存，错误处理行为不一致。

根因分析：
多套封装导致业务层 try/catch 行为不一致。

修改文件：
- `frontend/src/api/ai.ts`
- `frontend/src/api/invest.ts`
- `frontend/src/api/market.ts`
- `frontend/src/utils/request.ts`

关键代码说明：
- 所有 API 统一引用 `src/api/request.ts`。
- `utils/request.ts` 变为薄封装，转调 api/request。

验证方式与结果：
- `npm run build`：成功。

---

## 3) 修复用户中心 Profile 模块
问题现象：
Profile 页面读取 `userStore.email` 但 store 不存在该字段；update payload 映射错误；后端缺少 `GET /api/user/profile`。

根因分析：
Profile 闭环不完整，前后端字段映射不一致。

修改文件：
- `backend/src/main/java/com/fincoach/core/controller/UserController.java`
- `backend/src/main/java/com/fincoach/core/controller/dto/UserDTO.java`
- `backend/src/main/java/com/fincoach/core/service/UserService.java`
- `backend/src/main/java/com/fincoach/core/service/impl/UserServiceImpl.java`
- `frontend/src/api/user.ts`
- `frontend/src/store/modules/user.ts`
- `frontend/src/views/user/Profile.vue`

关键代码说明：
- 新增 `GET /api/user/profile`，返回 username/nickname/email。
- update payload 使用 `nickname/email`。
- Profile 挂载时主动拉取 profile 并同步 store。

验证方式与结果：
- `/api/user/profile` 访问成功（见“Phase 1 JDK21 构建与运行验证”）。

---

## 4) 修复投资计划 SELL 现金回流
问题现象：
SELL 执行仅减少持仓，现金账户未回流，资产闭环不完整。

根因分析：
`executePlan` 的 SELL 分支缺少回款逻辑。

修改文件：
- `backend/src/main/java/com/fincoach/core/service/impl/InvestmentPlanServiceImpl.java`

关键代码说明：
- SELL 非现金类资产时回流到现金账户（无则创建）。
- 记账类型为 `DEPOSIT`。

验证方式与结果：
- 仅编译验证（见“Phase 1 JDK21 构建与运行验证”）。
- `test_plan_only.ps1`：后端未启动时失败；本次验证不覆盖业务脚本执行。

---

## 5) 敏感配置环境变量化
问题现象：
`application.yml` 中包含明文 DB 密码和 API Key。

根因分析：
敏感配置未环境变量化。

修改文件：
- `backend/src/main/resources/application.yml`
- `doc/bugfix/20260211_env_setup.md`

关键代码说明：
- 使用 `${DB_URL}` / `${DB_USERNAME}` / `${DB_PASSWORD}` / `${DEEPSEEK_API_KEY}`。
- 增加环境变量说明文档。

验证方式与结果：
- 见“Phase 1 JDK21 构建与运行验证”。

---

## Phase 1 JDK21 构建与运行验证
JDK 环境：21（Temurin 21.0.7）

修改后的编译配置：
- `backend/pom.xml` 使用 `<source>17</source>` 与 `<target>17</target>`，并显式清空 `maven.compiler.release` 以避免 JDK21 下的 release 冲突。

Maven 构建日志片段：
```
[INFO] --- compiler:3.11.0:compile (default-compile) @ core ---
[INFO] Compiling 88 source files with javac [debug target 17] to target\classes
[INFO] --- jar:3.3.0:jar (default-jar) @ core ---
[INFO] Building jar: D:\project\FinCoach\backend\target\core-0.0.1-SNAPSHOT.jar
[INFO] --- spring-boot:3.2.2:repackage (repackage) @ core ---
[INFO] BUILD SUCCESS
```

成功启动日志片段：
```
Tomcat started on port 8080 (http) with context path ''
Started CoreApplication in 3.398 seconds
```

健康检查返回内容（带 Token）：
```
{"code":200,"message":"success","data":{"score":50,"level":"一般","userType":"NOVICE","liquidityScore":50,"riskMatchScore":0,"protectionScore":0,"diversityScore":0,"suggestions":["@{type=warning; message=⚠️ 您目前资产主要集中在储蓄，虽然安全但难以跑赢通胀}","@{type=info; message=💡 建议并在留足3~6个月应急金后，尝试低风险理财}"]}}
```

Profile 返回内容（带 Token）：
```
{"code":200,"message":"success","data":{"username":"phase1_user","nickname":"phase1_user","email":null,"phone":null,"oldPassword":null,"newPassword":null}}
```

---

## 破坏性变更与回滚方法
破坏性变更：
- 后端敏感配置改为环境变量读取，未配置会导致运行时连接失败。
- AuthInterceptor 停止注册，仅使用 JwtInterceptor + UserContext。

回滚方法：
- `git revert bf51c6db`
- `git revert 3dcba678`
- `git revert 097d9d31`
- `git revert 8cbb4d29`
- `git revert 7c6344e3`

---

## 验证命令汇总
- 后端构建：`mvn clean package`（JDK21，成功）
- 后端启动：`mvn spring-boot:run`（成功）
- 前端构建：`npm run build`（成功）
- 冒烟验证：`/api/health/check` 与 `/api/user/profile`（带 Token）
