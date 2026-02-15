
# M6-3 Admin Dashboard Stats Ops Log_
> 日期：2026-02-16  
> 模块：HealthV2 Admin  
> 目标：为后台仪表盘提供“可直接画图/列表”的聚合统计数据输出（不依赖前端二次聚合）。

## 变更范围
- 新增 Admin 聚合查询 API：`GET /api/admin/dashboard/stats`
- 新增 VO/DTO：`AdminDashboardStatsVO / BucketVO / RecentReportVO / RecentAlertVO / AdminDashboardStatsQueryDTO`
- 新增 Service：`AdminDashboardStatsService` 及实现类 `AdminDashboardStatsServiceImpl`，负责聚合：
  - 用户数、报告数、预警数（最近 N 天）
  - 分数分桶（health/risk，固定区间 0-39, 40-59, 60-79, 80-100）
  - 最近报告列表（最近 N 条）
  - 最近预警列表（最近 N 条）
- 权限：仅 `ADMIN/OPS` 允许访问，普通用户 403（依赖既有 AdminRoleInterceptor）

## API 说明
- Path：`/api/admin/dashboard/stats`
- Method：GET
- Query：
  - `windowDays` (Integer, default 30, min 7, max 365)
  - `recentSize` (Integer, default 10, min 1, max 50)
- 返回：`Result.success(AdminDashboardStatsVO)`

## 聚合口径
- 时间范围：按 query `windowDays` 决定（默认近 30 天）
- 用户总数：`user` 表全量计数
- 报告来源表：`fc_health_report`
- 预警来源表：`fc_alert_record`
- 分桶规则：固定边界 `0-39`, `40-59`, `60-79`, `80-100`

## 安全与约束
- `/api/admin/**` 必须走 JWT + AdminRoleInterceptor
- 返回不包含敏感字段（例如不返回用户密码/手机号等）
- 查询参数超范围，Service 层会自动修正为边界值（min/max clamp）

## 编译验证
- `cd backend && mvn clean compile` → BUILD SUCCESS

## 已知限制
- 统计为近似聚合（不做复杂 OLAP）
- 大数据量时可能需要加索引/分页（后续优化项）

## 下一步
- 可扩展：趋势对比（与 M6-1 trend 联动）、导出（CSV/JSON）、缓存等
