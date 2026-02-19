# E2E Smoke 运行说明

本目录用于说明 Playwright 冒烟测试的运行方式与产物存放约定。

## 运行前准备
- 确保后端与前端服务已启动（本地或测试环境）。
- 配置环境变量（勿提交到仓库）：
  - E2E_ADMIN_USER / E2E_ADMIN_PASS
  - E2E_USER_USER / E2E_USER_PASS
- 可选：E2E_BASE_URL（默认 http://localhost:5173）

## 运行方式
```powershell
cd frontend
pnpm exec playwright test
```

## 产物与归档
- Playwright 默认输出：`frontend/test-results/`
- 归档目录：`doc/qa/e2e-runs/YYYYMMDD_HHMM/`

建议：
- 仅在失败时，将 `test-results/` 中对应用例的 `trace.zip`、`*.png`、`*.webm` 复制到归档目录。
- 归档目录用于留存复盘，不纳入 Git 版本控制。