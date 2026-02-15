# M6-1 Trend & Compare Runbook

> 变量约定：`$TOKEN` 为登录后的 Bearer Token，`$BASE` 默认为 `http://localhost:8080`

## 1) 生成 6 次报告（最短脚本）

```bash
for i in 1 2 3 4 5 6; do
  curl -s -X POST \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{}' \
    "$BASE/api/app/health-reports/generate" >/dev/null
done
```

PowerShell 等价脚本：

```powershell
1..6 | ForEach-Object {
  curl.exe -s -X POST `
    -H "Authorization: Bearer $TOKEN" `
    -H "Content-Type: application/json" `
    -d "{}" `
    "$BASE/api/app/health-reports/generate" | Out-Null
}
```

## 2) 调用趋势接口

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "$BASE/api/app/health-reports/trend?points=6"
```

## 3) 返回字段检查
- `data.points`: 请求点数（默认 6）。
- `data.reports`: 时间升序数组，每项包含：
  - `reportId`
  - `reportDate`
  - `scores.health/risk/behavior`
  - `metrics.dti/emergencyMonths/netWorth/sharpe/maxDrawdown`
- `data.trend`:
  - `labels`：与 `reports` 一一对应（长度恒等于 `reports.length`）
  - `health/risk/behavior/dti/emergencyMonths/netWorth/sharpe/maxDrawdown`：长度恒等于 `reports.length`，缺失填 `null`
  - `reportCount`
  - `scoreDelta`（health/risk/behavior）
  - `metricDelta`（dti/emergencyMonths/netWorth/sharpe/maxDrawdown）
- `data.execution`:
  - `rebalanceConfirmCount30d`
  - `lastRebalanceConfirmAt`

## 4) 可为 null 的字段说明
- `metrics.sharpe`、`metrics.maxDrawdown`、对应 `metricDelta` 可能为 `null`。
- 原因：performance 指标在 `HISTORY/PARAM` 路径切换或样本不足时可能缺失。
- 其他 metrics 字段在历史报告 JSON 缺字段或解析失败时也允许 `null`（容错设计）。
