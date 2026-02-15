
# HealthV2 M6-3 自测指南 — Admin Dashboard Stats

## 1. 编译启动
```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

## 2. 造数（至少 6 次报告 + 触发 1 次预警）
生成报告（沿用你现有接口）：
```bash
for i in 1 2 3 4 5 6; do
  curl -s -X POST -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" \
    -d '{}' "$BASE/api/app/health-reports/generate" >/dev/null
done
```

触发预警（DTI/应急金不足等，按你 M4 预警规则挑一个最容易触发的）：
```bash
# 模拟高负债触发 DTI 预警
curl -s -X POST -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" \
  -d '{"month":"2026-03","income":10000,"monthlyDebtPayment":6000}' \
  "$BASE/api/app/cashflows" >/dev/null

curl -s -X POST -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" \
  -d '{}' "$BASE/api/app/health-reports/generate" >/dev/null
```

## 3. 调用 Admin Stats
```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/api/admin/dashboard/stats?windowDays=30"
```

## 4. 验收点
- `scoreDistribution.healthScoreBuckets`：至少一个桶 count > 0
- `recent.recentReports`：返回条数符合预期（例如默认 10）
- `recent.recentAlerts`：至少 1 条（如果你刚触发了预警）
- 普通用户访问 admin 接口：403
