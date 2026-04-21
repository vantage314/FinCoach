# 暗色主题改造 — 本地验证手册 (Runbook)

## 1. 启动前端

```bash
cd d:\project\FinCoach\frontend
npm run dev
```

前端默认运行在 http://localhost:5173

## 2. 检查页面清单

打开浏览器，逐页检查以下页面的暗色效果：

### 2.1 认证页

| 页面 | 路由 | 关注点 |
|------|------|--------|
| 登录页 | `/login` | 输入框暗色、glass 卡片、glow 光晕 |

### 2.2 用户端页面

| 页面 | 路由 | 关注点 |
|------|------|--------|
| 体检/报告 | `/app/diagnosis` | 整体暗色、图表配色、卡片玻璃拟态 |
| 市场中心 | `/app/market` | 搜索框暗色、表格暗色、分页器 |
| 资产全景 | `/app/asset/analysis` | 卡片暗色、输入框 |
| 账本管理 | `/app/asset/manage` | 表格/输入框 |
| 资产总览 | `/app/dashboard` | 卡片数值对比度、表格暗色 |
| 智能投资 | `/app/investment` | 表格暗色 |
| 投资计划 | `/app/plan` | 弹窗暗色、表格 |
| AI 咨询 | `/app/chat` | 输入框暗色 |
| 个人中心 | `/app/user/profile` | 表单/Tab 暗色 |

### 2.3 管理后台页面

| 页面 | 路由 | 关注点 |
|------|------|--------|
| 仪表盘 | `/admin/dashboard` | 指标卡片对比度、表格暗色 |
| 用户管理 | `/admin/users` | 搜索框、表格、分页器全暗色 |
| 预警管理 | `/admin/alerts` | 表格、状态标签暗色 |
| Debug 快照 | `/admin/debug` | 卡片、表格暗色 |
| 证券映射 | `/admin/securities/mapping` | 表格、筛选器暗色 |
| 行情快照 | `/admin/securities/snapshots` | 表格暗色 |
| 数据质量 | `/admin/securities/quality` | 表格暗色 |
| 数据源 | `/admin/data-source` | 表格、Tab 暗色 |

## 3. 逐项检查标准

### ✅ 必须通过
- [ ] 找不到纯白输入框 (`el-input` 背景非白色)
- [ ] 找不到纯白表格区域 (表头/表体/hover 全暗色)
- [ ] 分页器暗色化 (按钮/页码/下拉都暗色)
- [ ] 弹窗/对话框暗色 (Dialog/MessageBox 背景非白)
- [ ] 仪表盘数值清晰可读 (标题/数值/说明三层对比)
- [ ] Focus 状态可辨 (键盘 Tab 切换时边框有发光)
- [ ] 下拉菜单暗色 (el-dropdown/el-select 弹出暗色)
- [ ] Card 卡片暗色 (背景半透明，有边框)

### ❌ 已知例外
- ECharts 图表内嵌 JS 颜色字符串（如 `'#94a3b8'`）未替换为 CSS 变量（ECharts 不支持 CSS 变量），但视觉上已与暗色背景协调

## 4. 构建验证

```bash
npm run build
```

应输出 0 error。

## 5. E2E 测试

```bash
npx playwright test
```

应通过 `e2e/app.smoke.spec.ts` 和 `e2e/admin.smoke.spec.ts`。
