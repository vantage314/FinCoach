# M6-1 Trend & Compare Ops Log

> 日期：2026-02-15

## 背景
- M1~M5 已支持多次生成体检报告，但缺少“时间序列聚合”输出。
- 本次新增趋势聚合层与 App API，便于前端绘制分数/指标变化并展示执行力。

## 变更范围
- 新增 `HealthReportTrendService#getTrend(userId, points)` 聚合接口。
- 新增 VO：
  - `HealthReportTrendPointVO`
  - `HealthReportTrendVO`
- 新增 API：
  - `GET /api/app/health-reports/trend?points=6`

## 聚合口径
- 报告数据来源：`fc_health_report`，按 `report_date desc` 取最近 `points` 条。
- 返回排序：按时间升序（time-ascending）。
- metrics 仅抽取：
  - `cashflow.dti`
  - `cashflow.emergencyMonths`
  - `portfolio.netWorth`
  - `portfolio.performance.sharpe`
  - `portfolio.performance.maxDrawdown`
- 执行信息来源：`fc_behavior_event` 近 30 天 `REBALANCE_CONFIRM`
  - `rebalanceConfirmCount30d`
  - `lastRebalanceConfirmAt`

## 输出格式补充
- `trend.labels` 与 `reports` 一一对应，长度固定为 `reports.length`，来源 `reportDate`。
- `trend.health/risk/behavior/dti/emergencyMonths/netWorth/sharpe/maxDrawdown` 均为长度 `N` 的数组，缺失位置填 `null`，不跳过。
- `scoreDelta` / `metricDelta` 仅在 `N>=2` 且首尾均有值时计算，否则为 `null`。

## 容错约定
- metrics JSON 解析失败或字段缺失时，不抛错，相关字段返回 `null`。
- 报告不足 2 条时，`scoreDelta` / `metricDelta` 中各字段为 `null`。

## 安全与约束
- `/api/app/**` 不接收 `userId`，统一从 `UserContext` 获取。
- `points` 默认 6，范围限制 `2..24`，超出返回 `400`。
