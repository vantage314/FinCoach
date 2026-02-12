**  
  
- `backend/src/main/java/com/fincoach/core/controller/UserController.java`  
- `backend/src/main/java/com/fincoach/core/controller/dto/UserDTO.java`  
- `backend/src/main/java/com/fincoach/core/service/UserService.java`  
- `backend/src/main/java/com/fincoach/core/service/impl/UserServiceImpl.java`  
- `frontend/src/api/user.ts`  
- `frontend/src/store/modules/user.ts`  
- `frontend/src/views/user/Profile.vue`

**关键代码说明**  
- 后端新增 `GET /api/user/profile`，返回 `username/nickname/email`。  
- 前端 payload 使用 `nickname/email`，并在 Profile 挂载时拉取 profile 数据。  
- Store 补充 `email` 字段与本地存储恢复。

**验证方式与结果**  
- `npm run build`：成功。  
- `/api/user/profile` 需带 Token 访问，后端未运行，未做实请求验证（需后续人工验证）。

---

## 4) 修复投资计划 SELL 现金回流
**问题现象**  
SELL 执行仅减少持仓，现金账户未增加，资产闭环不完整。

**根因分析**  
`executePlan` SELL 分支缺少回款逻辑。

**修改文件**  
- `backend/src/main/java/com/fincoach/core/service/impl/InvestmentPlanServiceImpl.java`

**关键代码说明**  
- SELL 非现金类资产时回流到现金账户（找不到则新建现金资产）。  
- 记录回款交易（`DEPOSIT`）。

**验证方式与结果**  
- `pwsh -File .\\test_plan_only.ps1`：失败（后端服务未启动，`localhost:8080` 拒绝连接）。  
- `mvn clean package`：失败（环境缺少 JDK 17）。

---

## 5) 敏感配置环境变量化
**问题现象**  
`application.yml` 中包含明文 DB 密码和 API Key。

**根因分析**  
敏感配置未环境变量化，存在泄露风险。

**修改文件**  
- `backend/src/main/resources/application.yml`  
- `doc/bugfix/20260211_env_setup.md`

**关键代码说明**  
- 使用 `${DB_URL}` / `${DB_USERNAME}` / `${DB_PASSWORD}` / `${DEEPSEEK_API_KEY}` 读取敏感值。  
- 新增环境变量说明文档。

**验证方式与结果**  
- `mvn clean package`：失败（环境缺少 JDK 17）。

---

## 破坏性变更与回滚方法
**破坏性变更**  
- 后端敏感配置改为环境变量读取，未配置会导致运行时连接失败。  
- AuthInterceptor 停止注册，仅使用 JwtInterceptor + UserContext。

**回滚方法**  
- 回滚对应 commit：  
  - `git revert bf51c6db`  
  - `git revert 3dcba678`  
  - `git revert 097d9d31`  
  - `git revert 8cbb4d29`  
  - `git revert 7c6344e3`

---

## 验证命令汇总
- 后端：`mvn clean package`（失败：缺少 JDK 17）  
- 前端：`npm run build`（成功）  
- 计划测试：`pwsh -File .\\test_plan_only.ps1`（失败：后端未启动）
