# M5-C Import/Export Runbook

> 变量约定：`$TOKEN` 为登录后的 Bearer Token，`$BASE` 默认为 `http://localhost:8080`

## 1) 7 个 curl 示例

```bash
# 模板下载（3）
curl -H "Authorization: Bearer $TOKEN" "$BASE/api/app/import/templates/assets.csv" -o assets.csv
curl -H "Authorization: Bearer $TOKEN" "$BASE/api/app/import/templates/liabilities.csv" -o liabilities.csv
curl -H "Authorization: Bearer $TOKEN" "$BASE/api/app/import/templates/cashflows.csv" -o cashflows.csv

# CSV 导入（3）
curl -X POST -H "Authorization: Bearer $TOKEN" -F "file=@assets.csv" "$BASE/api/app/import/assets"
curl -X POST -H "Authorization: Bearer $TOKEN" -F "file=@liabilities.csv" "$BASE/api/app/import/liabilities"
curl -X POST -H "Authorization: Bearer $TOKEN" -F "file=@cashflows.csv" "$BASE/api/app/import/cashflows"

# 报告导出（1）
curl -H "Authorization: Bearer $TOKEN" "$BASE/api/app/health-reports/100/export?format=md"
```

## 2) 3 份最短 CSV 示例

### assets.csv
```csv
type,name,amount,currency,riskLevel,asOfDate
CASH,Wallet,5000,CNY,LOW,2026-02-15
```

### liabilities.csv
```csv
type,principal,interestRate,remainingMonths,monthlyPayment,prepayPenaltyJson
MORTGAGE,800000,4.2,300,4200,{"fee":0}
```

### cashflows.csv
```csv
month,income,fixedExpense,variableExpense,monthlyDebtPayment,notes
2026-02-15,20000,6000,2500,3000,regular
```

## 3) 含错误行示例与预期返回

示例 `assets.csv`（第二行错误）：
```csv
type,name,amount,currency,riskLevel,asOfDate
CASH,Wallet,5000,CNY,LOW,2026-02-15
STOCK,TechETF,-1,CNY,HIGH,2026-02-15
```

调用：
```bash
curl -X POST -H "Authorization: Bearer $TOKEN" -F "file=@assets.csv" "$BASE/api/app/import/assets"
```

预期返回（示意）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "successCount": 1,
    "failCount": 1,
    "failures": [
      { "row": 3, "reason": "amount 必须大于0", "raw": "STOCK,TechETF,-1,CNY,HIGH,2026-02-15" }
    ]
  }
}
```

## 4) export md 输出检查要点

- 顶部必须包含报告标题与时间。
- 必须有三分：`Health` / `Risk` / `Behavior`。
- 必须包含 `Metrics` 分组（按 key 分节输出）。
- 必须包含 `Advice` 分组；若有 summary，应在 Advice 中可见。
