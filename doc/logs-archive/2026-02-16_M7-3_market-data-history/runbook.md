# Runbook: M7-3 Market Data History Verification

**Date:** 2026-02-16
**Objective:** Verify market-data driven portfolio metrics and fallback mechanism.

## 1. Prerequisites
- Backend running.
- Network access to Stooq (or mock/fallback if offline).

## 2. Verification Steps

### Step 1: Verification with Market Data (Happy Path)
1. Ensure user has assets with valid Tickers (e.g., 'AAPL.US', '000001.SS' map if needed, or just standard tickers).
   - *Note: Stooq symbols might need specific suffix.*
2. Generate Report: `POST /api/app/health-reports/generate`
3. Check Metrics: `GET /api/app/portfolio/metrics/latest`
   - Expect: `"source": "MARKET_DATA_DAILY_CLOSE"`
   - Expect: `"warnings"` does NOT contain `MARKET_DATA_FALLBACK_...` (unless partial).

### Step 2: Verification of Fallback (Unhappy Path)
1. Disconnect network OR use a user with NO tickers / unknown tickers.
2. Generate Report.
3. Check Metrics.
   - Expect: `"source": "REPORT_NET_WORTH_APPROX"`
   - Expect: `"warnings"` contains `MARKET_DATA_FALLBACK_TO_REPORT_APPROX`.

### Step 3: SQL Check
```sql
SELECT count(*) FROM fc_portfolio_price_snapshot WHERE user_id = ?;
```
*Note: If DB migration failed (due to environment access), this table might not exist. The application handles this gracefully by skipping persistence.*

### Step 4: Fallback & Warning Verification
1. **Ticker Resolution**:
   - Create a portfolio with "贵州茅台" or "AAPL" (no suffix).
   - Check response warnings for `MARKET_DATA_SYMBOL_NORMALIZED`.
   - Result: `600519.SS` or `AAPL.US` should be used.
2. **Missing Ticker**:
   - Create a portfolio with "Unknown Asset".
   - Check warnings for `POSITION_TICKER_UNRESOLVED`.
   - Check `source`: `REPORT_NET_WORTH_APPROX` (if all fail).

### Step 5: Snapshot Persistence
- Default config: `fincoach.portfolio.snapshot.enabled=false`.
- Verify warning: `SNAPSHOT_PERSIST_SKIPPED` in logs/response (if warnings propagated).
- To enable: Add `fincoach.portfolio.snapshot.enabled=true` to `application.properties`.
- Check DB: `SELECT * FROM fc_portfolio_price_snapshot` (Note: Requires DB access).
