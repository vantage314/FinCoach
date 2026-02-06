# 任务日志 - 2026-02-06 修复 API 与 组件报错
## 内容：补全 API 定义、修复变量提升、检查 ECharts 配置

### 1. API 修复 (Fix API Export)
- 文件：`frontend/src/api/plan.ts`
- 修改：
    - 补全了 `generateInvestmentPlan` 和 `executePlan` 的导出。
    - 保留了 `InvestmentPlanVO` 等接口定义以维持类型安全。
    - 兼容了 `generatePlan` 别名。

### 2. 组件修复 (Fix Variable Hoisting)
- 文件：`frontend/src/components/HealthScoreCard.vue`
- 修改：
    - 将 `animateScore` 函数定义移动到 `watch` 之前，解决了 `ReferenceError`。
    - 确保了 `watch` 的 `immediate: true` 能正确调用函数。

### 3. ECharts 警告检查 (ECharts Warning)
- 文件：`InvestorOverview.vue` / `SecurityDetail.vue`
- 检查结果：
    - `InvestorOverview.vue` 已正确使用 modular import 引入并注册了 `GridComponent`。
    - `SecurityDetail.vue` 使用全量 `import * as echarts`，默认包含 Grid 组件。
    - 确认无须额外修复，警告应已消除（或源于缓存/HMR）。

### 4. 总结
本次修复解决了前端核心流程（生成/执行计划）的 API 缺失阻碍，以及明显的运行时 JS 错误。
