# Ops Log: M7-2 Portfolio History (Approximate)

**Date:** 2026-02-16
**Author:** FinCoach AI Agent
**Status:** Completed

## 1. Context
M7-1 established the Portfolio Analyzer engine but lacked historical data, resulting in null Sharpe/MDD metrics. M7-2 implements a "bootstrap" mechanism to construct approximate historical data from the accumulated `FcHealthReport` history, allowing the system to output valid risk metrics without waiting for external market data integration.

## 2. Changes
### 2.1 History Builder (`PortfolioHistoryBuilder`)
- **Logic**: Queries the last 24 `FcHealthReport` entities for the user.
- **Equity Curve**: Extracted from `metrics.portfolio.netWorth` (or `totalAssets` fallback).
  - Invalid points (null or <=0) are retained in curve but flagged with `INVALID_EQUITY_POINT`.
- **Return Series**: calculated as `(Equity_t / Equity_t-1) - 1`.
  - **Refined**: Skips calculation if previous or current equity is invalid (does NOT assume 0%). 
  - Adds `RETURN_POINT_SKIPPED` warning if gaps exist.
- **Asset Proxy**: Uses **Allocation Drift** (`Alloc_t - Alloc_t-1`) instead of value change to avoid cashflow bias.
  - Adds `CORR_PROXY_ALLOCATION_DRIFT` warning to indicate this is a behavioral proxy, not true correlation.

### 2.2 Service Integration (`HealthReportV2ServiceImpl`)
- Injected `PortfolioHistoryBuilder`.
- **Flow**:
  1. Calculate current snapshot (NetWoth, Allocation).
  2. Call `builder.buildFromRecentReports(...)` to get `PortfolioInput`.
  3. Run `PortfolioAnalyzer.analyze(input)`.
  4. Enrich `metrics.portfolio` with:
     - `performance` (Sharpe, MDD from analyzer)
     - `source`: `"REPORT_NET_WORTH_APPROX"` (Explicitly marking data origin)
     - `warnings`: Merges builder warnings (e.g., `EQUITY_GAP`) with analyzer warnings (e.g., `INSUFFICIENT_RETURNS_SERIES`) plus `HISTORY_BUILT_FROM_REPORTS_APPROX`.

### 2.3 API Output Upgrade
- **GET /api/app/portfolio/metrics/latest** now returns:
```json
{
  "source": "REPORT_NET_WORTH_APPROX",
  "performance": {
    "sharpe": 1.25,          // Real value if valid returns > 2
    "maxDrawdown": 0.05      // Real value if equity points > 2
  },
  "warnings": [
    "HISTORY_BUILT_FROM_REPORTS_APPROX",
    "CORR_PROXY_ALLOCATION_DRIFT" // New warning
  ]
   ...
}
```

## 3. Assumptions & Limitations
- **Approximation**: Relies on "Report Net Worth".
- **Data Points**: Requires at least 2 **valid** data points to calculate any return.
- **Correlation**: Uses allocation drift. If user doesn't rebalance, correlation is 0 (or undefined). This is a known limitation until M7-3 (Real Market Data).

## 4. Verification
- **Build**: `mvn clean compile` passed.
- **Git Status**: Clean.
