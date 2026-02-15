# HealthV2 M5-B Notifications Ops Log

## 背景与目标
- 在不改变既有预警判定逻辑的前提下，将预警结果旁路推送到用户通知中心。
- 让用户在 `/api/app/**` 侧直接看到未读预警，并支持单条/全部已读。

## 变更范围
- 新增通知表迁移：`sql/migration/20260215_health_v2_m5b_notifications.sql`
- 新增通知基础层：`FcNotificationEntity`、`FcNotificationMapper`、`NotificationService`
- 新增用户端通知 API：分页查询、单条已读、全部已读
- 在 `HealthReportV2ServiceImpl` 的 `alert_record` 写入成功后旁路写通知

## API 清单
- `GET /api/app/notifications?isRead=0|1|null&size=20&page=1`
- `PUT /api/app/notifications/{id}/read`
- `PUT /api/app/notifications/read-all`

## 权限与安全
- `/api/app/**` 不接受 `userId` 参数，全部来自 `UserContext.getCurrentUserId()`
- 单条已读：
  - 通知不存在 -> `404`
  - 通知不属于当前用户 -> `403`
- 列表查询与全部已读均只作用于当前登录用户

## 返回结构
- 列表接口统一返回：
  - `list`
  - `page`
  - `size`
  - `total`
  - `unreadCount`

## 预警接入策略
- 保持原 `alert_record` 逻辑不变，仅在成功插入后追加通知写入。
- 通知 payload 统一包含：
  - `ruleKey`
  - `threshold`
  - `currentValue`
  - `reportId`

## 已知限制
- 文案模板当前使用规则映射与简短拼接，后续可接入模板中心进行多语言/个性化扩展。
