# M5-A Behavior Events Runbook

> 变量约定：`$TOKEN` 为登录后的 Bearer Token，`$BASE` 默认为 `http://localhost:8080`

## 1) 生成报告并写入 REPORT_GENERATE

```bash
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{}' "$BASE/api/app/health-reports/generate"
```

预期：
- 返回 `code=200`
- 报告生成成功
- 行为事件中新增 `REPORT_GENERATE`

## 2) 查询行为事件（分页 / 类型过滤）

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "$BASE/api/app/behavior-events?page=1&size=20"

curl -H "Authorization: Bearer $TOKEN" \
  "$BASE/api/app/behavior-events?page=1&size=20&type=REBALANCE_CONFIRM"
```

预期：
- 返回 `data.list/page/size/total`
- 第二个请求仅返回 `REBALANCE_CONFIRM`

## 3) 确认再平衡（写 REBALANCE_CONFIRM）

```bash
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{
    "reportId": 123,
    "notes": "已按建议加仓债券",
    "executedActions": [
      {"action":"BUY","type":"BOND","amount":3000},
      {"action":"SELL","type":"STOCK","amount":1000}
    ]
  }' "$BASE/api/app/rebalance/confirm"
```

预期：
- `reportId` 缺失：`400`
- 报告不存在：`404`
- 越权：`403`
- 成功后写入 `REBALANCE_CONFIRM`，`amount=4000`

## 4) 验证行为分解释

```bash
curl -H "Authorization: Bearer $TOKEN" "$BASE/api/app/health-reports/latest"
```

检查：
- `data.metrics.scoreBreakdown.behavior` 中 `reason` 包含 30 天规则说明
- 事件不足 3 条时，reason 明确提示“基线 60 / 数据不足”
