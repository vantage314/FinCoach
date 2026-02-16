# Runbook: M7-2 Portfolio History Verification

**Date:** 2026-02-16
**Objective:** Verify that Portfolio Metrics (Sharpe, MDD) are populated using historical report data.

## 1. Prerequisites
- Backend running.
- User logged in (Token obtained).

## 2. Verification Steps

### Step 1: Generate History (Bootstrap)
Since we need history, we must generate multiple reports. Use PowerShell or separate calls.

**PowerShell Loop (Generate 6 reports):**
```powershell
$token = "YOUR_TOKEN"
for ($i=1; $i -le 6; $i++) { 
    Invoke-RestMethod -Uri "http://localhost:8080/api/app/health-reports/generate" -Method Post -Headers @{Authorization="Bearer $token"} -ContentType "application/json"
    Start-Sleep -Seconds 1
    Write-Host "Generated Report $i"
}
```
*Alternatively, use Postman Runner or manual CURLs.*

### Step 2: Generate Final Report & Check
Generate one last report to trigger the analysis on the history created above.

```bash
curl -X POST http://localhost:8080/api/app/health-reports/generate \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

### Step 3: Verify Metrics API
Fetch the latest metrics.

```bash
curl -X GET http://localhost:8080/api/app/portfolio/metrics/latest \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
```json
{
    "code": 200,
    "data": {
        "performance": {
            "sharpe": 0.0,         // Should be a number (or 0.0 if flat)
            "maxDrawdown": 0.0     // Should be a number
        },
        "source": "REPORT_NET_WORTH_APPROX",
        "warnings": [
            "HISTORY_BUILT_FROM_REPORTS_APPROX"
        ]
    }
}
```
*Note: If you just generated rapid reports with no asset value change, Sharpe might be 0 or NaN (handled as null), but `source` must be present.*

### Step 4: Database Check
```sql
SELECT metrics_json FROM fc_health_report ORDER BY id DESC LIMIT 1;
```
Verify `metrics_json` contains `"source": "REPORT_NET_WORTH_APPROX"`.
