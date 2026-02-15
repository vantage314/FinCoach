# M6-2 Rebalance Confirm Integrity Runbook

> 变量约定：`$TOKEN` 为登录后的 Bearer Token，`$BASE` 默认为 `http://localhost:8080`

## 1) 生成报告并获取 actionsHash

```bash
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{}' "$BASE/api/app/health-reports/generate"

curl -H "Authorization: Bearer $TOKEN" \
  "$BASE/api/app/health-reports/latest"
```

获取 `actionsHash`（示例用 jq）：

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE/api/app/health-reports/latest" | jq -r ".data.advice.rebalance.actionsHash"
```

## 2) confirm 成功示例

```bash
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{
    "reportId": 123,
    "actionsHash": "sha256:YOUR_HASH",
    "notes": "已按建议调整",
    "executedActions": [
      {"action":"BUY","type":"BOND","amount":3000}
    ]
  }' "$BASE/api/app/rebalance/confirm"
```

预期：
- 返回 `code=200`
- 行为事件 `REBALANCE_CONFIRM` 写入成功

## 3) confirm 失败示例（hash 不匹配）

```bash
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{
    "reportId": 123,
    "actionsHash": "sha256:WRONG_HASH",
    "notes": "force fail",
    "executedActions": []
  }' "$BASE/api/app/rebalance/confirm"
```

预期：
- `400`
- `message=REBALANCE_HASH_MISMATCH`

## 4) 验证行为事件包含 actionsHash

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "$BASE/api/app/behavior-events?page=1&size=20&type=REBALANCE_CONFIRM"
```

检查：
- `data.list[0].metaJson` 中包含 `actionsHash`
