# M6-3 Admin Dashboard Stats Ops Log (Step 1)

> 日期：2026-02-15

## 背景/目的
- 为后台仪表盘提供统一的统计返回结构（VO/DTO），用于后续接入执行闭环与关键分层数据。

## 新增 VO/DTO
- `AdminDashboardStatsVO`
  - `overview`: userCount / reportCount / alertTriggerCount / notificationUnreadCount
  - `scoreDistribution`: healthScoreBuckets / riskScoreBuckets
  - `recent`: recentReports / recentAlerts
  - `meta`: generatedAt / windowDays
- `BucketVO`: label / count
- `RecentReportVO`: reportId / userId / reportDate / healthScore / riskScore / behaviorScore
- `RecentAlertVO`: id / userId / ruleKey / triggerAt / status / payload
- `AdminDashboardStatsQueryDTO`: windowDays(7..365) / recentSize(1..50)

## 约束
- Step 1 仅定义结构，不接入查询、不修改 controller。
- 不引入新依赖，不改 SQL/表结构。

## 下一步（Step 2）数据源
- `fc_health_report`（报告统计 / 分布 / 最近报告）
- `fc_alert_record`（预警统计 / 最近预警）
- `user` 或 `sys_user`（用户数）
- `fc_notification`（未读通知数，如需）
