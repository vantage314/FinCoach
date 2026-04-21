# 暗色主题改造操作日志 (ops_log)

**日期**: 2026-03-01
**执行人**: AI 前端重构工程师

## 操作概要

| 步骤 | 操作 | 涉及文件数 |
|------|------|-----------|
| 1 | Design Tokens 体系重建 | 1 (`variables.scss`) |
| 2 | Element Plus 全组件暗色覆盖 | 1 (`element-overwrite.scss`) |
| 3 | 通用样式类 fc-common | 1 (`fc-common.scss`) |
| 4 | Fc* Wrapper 组件 | 6 (`FcCard/FcTable/FcInput/FcSelect/FcDatePicker/index.ts`) |
| 5 | 入口文件更新 | 3 (`main.ts`, `style.css`, `App.vue`) |
| 6 | 布局组件迁移 | 4 (`AdminLayout/UserLayout/UserTopLayout/AuthLayout`) |
| 7 | 页面硬编码样式批量替换 | 38 个 Vue 文件 |

**总计涉及 ~54 个文件**

## 核心变更

### 新增文件
- `src/theme/fc-common.scss` — 通用页面/卡片/指标样式类
- `src/components/fc/FcCard.vue` — 暗色卡片 Wrapper
- `src/components/fc/FcTable.vue` — 暗色表格 Wrapper
- `src/components/fc/FcInput.vue` — 暗色输入框 Wrapper
- `src/components/fc/FcSelect.vue` — 暗色下拉框 Wrapper
- `src/components/fc/FcDatePicker.vue` — 暗色日期 Wrapper
- `src/components/fc/index.ts` — 统一注册入口

### 重构文件
- `src/theme/variables.scss` — 扩展为完整 Design Tokens 体系（30+ CSS 变量）
- `src/theme/element-overwrite.scss` — 从 2 个组件覆盖扩展至 30+ 组件全链路暗色覆盖
- `src/main.ts` — 样式引入顺序优化 + Fc* 全局注册

### 批量替换模式
- `linear-gradient(135deg, #0f172a, #1e293b)` → `var(--fc-bg-gradient)`
- `rgba(30, 41, 59, 0.6)` → `var(--fc-panel)`
- `#94a3b8` → `var(--fc-text-muted)`
- `rgba(255, 255, 255, 0.08)` → `var(--fc-border)`
- `#fff` → `var(--fc-text)`（仅 style 块内）

## 未修改内容
- 不涉及任何 `.ts`/`.js` 业务逻辑文件
- 不涉及 API 调用、路由、Store
- 不涉及后端代码
- 不涉及数据库结构
- ECharts 图表内嵌的 JS 颜色字符串（如 `'#94a3b8'`）保持不动，因为 ECharts 不支持 CSS 变量
