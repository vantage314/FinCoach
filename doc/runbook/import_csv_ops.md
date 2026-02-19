# CSV Import Runbook (Wave4.9)

## Purpose
为债务与现金流月度提供批量导入能力，支持行级错误回传与幂等更新。

## Endpoints
- POST `/api/app/debt/importCsv` (multipart, file)
- POST `/api/app/cashflow/importCsv` (multipart, file)

## Templates
Debt CSV:
```
debtType,apr,remainingBalance,monthlyPayment,termMonths,principal,startDate,endDate,externalKey
MORTGAGE,0.045,480000,3200,240,500000,2022-01-01,2042-01-01,loan-001
```

Cashflow CSV:
```
month,income,expense
2026-01,12000,8000
```

## Idempotency Rules
- Debt:
  - 当 `externalKey` 存在时，按 `(user_id, externalKey)` 查找并更新
  - 未提供 externalKey 时，按普通新增处理
- Cashflow:
  - 按 `(user_id, month)` upsert，重复月份会更新

## Response Payload
```
{
  "successRows": 3,
  "errorRows": 1,
  "totalRows": 4,
  "errors": [
    { "row": 2, "field": "apr", "message": "apr 必须大于等于0", "raw": "..." }
  ]
}
```

## Common Errors
- `debtType 不能为空`
- `apr 必须大于等于0`
- `remainingBalance 必须大于等于0`
- `month 格式需为 YYYY-MM 或 YYYY-MM-DD`

## Ops Checklist
1) 使用模板导出 CSV，确保逗号分隔、UTF-8 编码
2) 通过页面 “导入 CSV” 或 curl 上传
3) 查看导入结果与错误明细
4) 重复导入同一现金流月份应覆盖更新
5) Debt CSV 使用 externalKey 可避免重复

## Curl Examples
```
curl -X POST -F "file=@debt.csv" http://localhost:5173/api/app/debt/importCsv
curl -X POST -F "file=@cashflow.csv" http://localhost:5173/api/app/cashflow/importCsv
```
