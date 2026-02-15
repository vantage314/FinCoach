# M5-A Behavior Events Ops Log

> 日期：2026-02-15

## 背景与目标
- 将 `behaviorScore` 从静态分值改为基于近 30 天行为事件计算。
- 补齐用户执行闭环：生成报告 -> 再平衡确认 -> 行为分变化可解释。

## 变更范围
- 新增行为事件表 `fc_behavior_event`（M5-A migration）。
- 新增行为事件服务：记录事件、分页查询、近 30 天统计。
- 新增 App API：
  - `GET /api/app/behavior-events`
  - `POST /api/app/rebalance/confirm`
- 在报告生成成功后自动写入 `REPORT_GENERATE` 事件。
- `ScoreEngine` 行为分规则改为事件驱动（30 天窗口）。

## 评分规则（BehaviorScore）
- 基线：60
- 30 天内存在 `REBALANCE_CONFIRM`：`+15`（该步后分数上限 85）
- 30 天内 `REPORT_GENERATE >= 10`：`-10`
- 30 天内 `REBALANCE_CONFIRM >= 3`：`+5`
- 最终分数 `clamp(0..100)`
- 当 30 天事件数 `<3`：直接返回基线 60，并在 breakdown reason 标注“数据不足”。

## 安全与权限
- `/api/app/**` 不接收 `userId`，统一从 `UserContext` 获取。
- `rebalance/confirm` 校验 `reportId`：
  - 报告不存在：`404`
  - 报告不归属当前用户：`403`

## 兼容性说明
- 迁移脚本对早期环境做兼容处理：
  - 对既有 `fc_behavior_event` 调整 `event_type` 为 `VARCHAR(40)`
  - 补建索引 `idx_user_time`（存在则跳过）
