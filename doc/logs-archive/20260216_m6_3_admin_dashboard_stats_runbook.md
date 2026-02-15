
# M6-3 Admin Dashboard Stats Runbook

> 变量：`$BASE=http://localhost:8080`，`$ADMIN_TOKEN` 为 ADMIN/OPS 登录后的 Bearer Token

## 0. 前置
- 已执行 HealthV2 相关 SQL（M1~M4 + 当前分支所需）
- 服务已启动：`cd backend && mvn spring-boot:run`

## 1) 获取管理员 Token
示例（按你项目实际登录接口调整）：
```bash
curl -s -X POST -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' \
  "$BASE/api/auth/login"
```

## 2) 调用 Dashboard Stats
```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/api/admin/dashboard/stats?windowDays=30&recentSize=10"
```

## 3) 返回检查要点
code=200

data 包含：
- `overview`: userCount, reportCount, alertTriggerCount, notificationUnreadCount
- `scoreDistribution`: healthScoreBuckets, riskScoreBuckets
- `recent`: recentReports, recentAlerts
- `meta`: generatedAt, windowDays

## 4) 权限验证（普通用户必须 403）
```bash
curl -i -H "Authorization: Bearer $USER_TOKEN" \
  "$BASE/api/admin/dashboard/stats?windowDays=30"
```

## 5) 常见排障
- 403：确认 token role=ADMIN/OPS；确认 WebConfig/JwtInterceptor 放行逻辑无误
- buckets 全 0：确认库中存在 `fc_health_report` 数据；先生成 3~6 次报告再测
