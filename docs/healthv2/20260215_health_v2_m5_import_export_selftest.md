# HealthV2 M5-C Import/Export Selftest

## 1. 模板下载验证

```bash
curl -i -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/app/import/templates/assets.csv
curl -i -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/app/import/templates/liabilities.csv
curl -i -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/app/import/templates/cashflows.csv
```

预期：
- HTTP 200
- `Content-Type: text/csv; charset=utf-8`
- `Content-Disposition` 文件名分别是 `assets.csv` / `liabilities.csv` / `cashflows.csv`

## 2. 资产导入（含错误行）

示例 `assets.csv`：

```csv
type,name,amount,currency,riskLevel,asOfDate
CASH,Wallet,5000,CNY,LOW,2026-02-15
STOCK,TechETF,-1,CNY,HIGH,2026-02-15
```

```bash
curl -X POST -H "Authorization: Bearer <TOKEN>" -F "file=@assets.csv" http://localhost:8080/api/app/import/assets
```

预期：
- HTTP 200
- `successCount=1`
- `failCount=1`
- `failures[0].row=3`，`reason` 提示 `amount` 不合法

## 3. 负债导入（含错误行）

示例 `liabilities.csv`：

```csv
type,principal,interestRate,remainingMonths,monthlyPayment,prepayPenaltyJson
MORTGAGE,800000,4.2,300,4200,{"fee":0}
CAR_LOAN,0,0.06,36,1200,{"fee":0}
```

```bash
curl -X POST -H "Authorization: Bearer <TOKEN>" -F "file=@liabilities.csv" http://localhost:8080/api/app/import/liabilities
```

预期：
- HTTP 200
- `successCount=1`
- `failCount=1`
- `failures[0].row=3`，`reason` 提示 `principal` 必须大于0

## 4. 现金流导入（含错误行）

示例 `cashflows.csv`：

```csv
month,income,fixedExpense,variableExpense,monthlyDebtPayment,notes
2026-02-15,20000,6000,2500,3000,ok
2026-13,20000,6000,2500,3000,bad_month
```

```bash
curl -X POST -H "Authorization: Bearer <TOKEN>" -F "file=@cashflows.csv" http://localhost:8080/api/app/import/cashflows
```

预期：
- HTTP 200
- 第一行月份归一到 `2026-02`
- 第二行失败，`failures` 中出现 `row=3` 与 month 格式错误

## 5. 报告导出 json / md

```bash
curl -H "Authorization: Bearer <TOKEN>" "http://localhost:8080/api/app/health-reports/<REPORT_ID>/export?format=json"
curl -H "Authorization: Bearer <TOKEN>" "http://localhost:8080/api/app/health-reports/<REPORT_ID>/export?format=md"
```

预期：
- `format=json` 返回完整报告对象（含 metrics/advice）
- `format=md` 返回 Markdown 文本（标题 + 三分 + Metrics + Advice）
- 他人 reportId 返回 403
- 不存在 reportId 返回 404
