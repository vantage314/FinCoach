# Bugfix: 投资计划生成后未出现在历史列表中

## Bug 描述
用户点击“生成新计划”后，虽然前端显示生成成功，但在“我的投资计划”列表中找不到刚才生成的记录。

## Bug 原因
1. **持久化缺失**：后端 `InvestmentPlanServiceImpl.generatePlan` 方法仅计算了调仓建议并返回了 VO 对象，但没有将 `InvestmentPlan` 实体及其关联的 `PlanItem` 保存到数据库。
2. **状态未初始化**：生成的计划没有设置默认的状态（如 `draft` 或 `saved`），导致查询时可能被过滤。

## 解决方案
1. 在 `InvestmentPlanServiceImpl.generatePlan` 中增加持久化逻辑：
   - 为生成的计划生成默认名称。
   - 调用 `savePlan` 方法将计划及其明细保存到数据库。
2. 确保 `generatePlan` 方法开启 `@Transactional` 事务，保证主表和明细表保存的原子性。

## 学习总结
在实现生成类功能时，必须明确“草稿生成”与“持久化”的边界。如果用户期望立即看到记录，则必须在生成阶段完成入库操作。
