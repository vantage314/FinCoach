# 20260206_投资计划功能补齐与稳定性修复记录

## 任务目标
修复投资计划模块的闭环流程，确保计划生成、查询、执行全链路畅通。

## 修改内容

### 1. 后端增强 (Java)
- **`InvestmentPlanServiceImpl`**:
    - 在 `generatePlan` 方法中增加 `@Transactional` 事务支持。
    - 修复生成不落地问题：在生成建议后自动调用 `savePlan` 持久化，并赋予默认名称。
    - `getHistory` 增加健壮性代码：对 `riskLevel` 和 `status` 进行 `null` 检查并设置 fallback 值。
- **`InvestmentPlanController`**:
    - 标准化 API 路径：`history` -> `list`。
    - 标准化执行接口参数：使用 `@RequestParam` 接收 `planId`。
- **`GlobalExceptionHandler`**:
    - 开发环境下返回真实的异常消息，弃用模糊的“系统繁忙”。

### 2. 前端优化 (Vue3)
- **`api/plan.ts`**: 更新接口路径以匹配后端。
- **`store/modules/investmentPlan.ts`**: 标准化参数传递，确保与 RESTful API 契合。
- **`views/plan/Index.vue`**: 兼容后端返回的数组及分页对象结构。

### 3. 测试与验证
- 编写并运行 `test_plan_only.ps1` 脚本。
- 自动化流转测试：`注册` -> `入金` -> `生成` -> `列表查询` -> `一键执行`。
- 结果：**Step 1-4 全部通过**，Plan ID 成功更新为 `executed` 状态。

## 遗留问题
- 无。
