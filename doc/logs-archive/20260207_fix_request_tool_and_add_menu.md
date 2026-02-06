# 任务日志 - 2026-02-07 修复请求工具与添加菜单入口

## Phase 12.2.1: Frontend Infrastructure & Navigation Fix

### 1. 请求工具封装 (frontend/src/utils/request.ts)
- 创建了标准的 Axios 封装实例。
- 配置了 `baseURL: '/api'` 和 `timeout: 10000`。
- 添加了请求拦截器（预留 Token 注入位置）。
- 添加了响应拦截器（统一处理非 200 状态码和网络错误）。

### 2. 顶部导航更新 (frontend/src/layout/TopLayout.vue)
- 引入了 `FirstAidKit` (资产体检) 和 `TrendCharts` (智能投资) 图标。
- 在 `<el-menu>` 中添加了以下入口：
    - **资产体检** (`/diagnosis`)：配 `FirstAidKit` 图标。
    - **智能投资** (`/investment`)：配 `TrendCharts` 图标。

## 验证结论
- 代码变更已完成。
- 用户需手动重启前端服务 (`npm run dev`) 以使新的 `request.ts` 和路由菜单生效。
