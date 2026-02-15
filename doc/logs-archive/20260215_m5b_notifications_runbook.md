# M5-B Notifications Runbook

## 1. 触发预警并检查通知

```bash
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"month":"2026-03","income":10000,"monthlyDebtPayment":6000}' \
  http://localhost:8080/api/app/cashflows

curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{}' http://localhost:8080/api/app/health-reports/generate

curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/app/notifications?isRead=0&size=20&page=1"
```

预期：
- 列表返回 `code=200`
- `data.list` 中出现 `type=ALERT` 通知
- `data.unreadCount` 大于等于 `1`

## 2. 标记已读

```bash
curl -X PUT -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/app/notifications/<ID>/read
```

预期：
- 返回 `已标记已读`
- 再查未读列表时该通知不在 `isRead=0` 结果中

## 3. 全部标记已读

```bash
curl -X PUT -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/app/notifications/read-all
```

预期：
- 返回 `已全部标记已读`
- `unreadCount` 归零

## 4. 异常验证
- 越权读取/标记他人通知：`403`
- 通知 ID 不存在：`404`
- `isRead` 仅允许 `0/1/null`，其他值返回 `400`
