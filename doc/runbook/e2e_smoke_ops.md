# E2E Smoke Runbook

## 目的
快速验证 Admin 与 App 关键路径是否可用，并收集失败时的截图/trace/video。

## 前置条件
- 后端服务已启动并可访问（含 admin/app API）。
- 前端已启动或可访问部署环境。
- 已安装 Playwright（`pnpm exec playwright install`）。

## 环境变量
请在本地终端设置（不要提交到仓库）：
- `E2E_ADMIN_USER` / `E2E_ADMIN_PASS`
- `E2E_USER_USER` / `E2E_USER_PASS`
- `E2E_BASE_URL`（可选，默认 `http://localhost:5173`）

## 运行命令
```powershell
cd frontend
pnpm exec playwright test
```

## 失败产物归档
Playwright 默认输出目录：`frontend/test-results/`

当测试失败时：
1. 创建归档目录：`doc/qa/e2e-runs/YYYYMMDD_HHMM/`
2. 复制失败用例的 `trace.zip`、截图(`*.png`)、视频(`*.webm`) 到归档目录
3. 若需复盘：
   - `pnpm exec playwright show-trace frontend/test-results/<case>/trace.zip`

## 常见问题
- 报错 `Missing env var`：未设置账号密码环境变量。
- 404/403：检查后端是否启动、权限账号是否正确。