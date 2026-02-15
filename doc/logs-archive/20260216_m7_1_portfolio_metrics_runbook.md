# Runbook: M7-1 Portfolio Metrics

**Date:** 2026-02-16
**Objective:** Verify Portfolio Metrics (Sharpe, MaxDrawdown, Correlation) integration.

## 1. Environment Status
- **Build**: `mvn clean compile` (Must be SUCCESS)
- **Service**: Backend running on port 8080 (default).

## 2. Verification Steps

### Step 1: Generate Health Report
Trigger a new report generation to populate metrics.

```bash
# Replace YOUR_TOKEN with valid JWT
curl -X POST http://localhost:8080/api/app/report/generate \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```
**Expected Output:**
- HTTP 200 OK
- Report ID returned.

### Step 2: detailed Metrics Check via New API
Fetch the portfolio metrics from the latest report.

```bash
curl -X GET http://localhost:8080/api/app/portfolio/metrics/latest \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected JSON Response (M7-1 State):**
```json
{
  "code": 200,
  "data": {
    "performance": {
      "sharpe": null,
      "maxDrawdown": null
      // ... legacy fields ...
    },
    "correlation": {
      "matrix": null,
      "highPairs": []
    },
    "warnings": [
      "INSUFFICIENT_RETURNS_SERIES",
      "INSUFFICIENT_EQUITY_CURVE"
    ]
  }
}
```
*Note: `sharpe` and `maxDrawdown` are null because historical data is not yet hooked up (M7-2).*

### Step 3: Verify Correlation (Conditional)
If assets exist, `allocation` will be calculated. However, `correlation` requires `returnsByAssetKey` which is currently mocked as null. So `matrix` will likely be null or empty depending on exact implementation path validation.
In current impl: `INSUFFICIENT_RETURNS_SERIES` warning is guaranteed.

## 3. SQL Check
Validate data accumulation in DB.
```sql
SELECT id, metrics_json FROM fc_health_report ORDER BY id DESC LIMIT 1;
```
Check `metrics_json` column for `"portfolio": { ... }` structure.
