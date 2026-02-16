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
- **Return Series**: calculated as `(Equity_t / Equity_t-1) - 1`.
  - Handles gaps or zero values by assuming 0% return.
- **Asset Proxy**: Uses change in "NetWorth * Allocation%" to approximate asset class returns for correlation matrix.

### 2.2 Service Integration (`HealthReportV2ServiceImpl`)
- Injected `PortfolioHistoryBuilder`.
- **Flow**:
  1. Calculate current snapshot (NetWoth, Allocation).
  2. Call `builder.buildFromRecentReports(...)` to get `PortfolioInput`.
  3. Run `PortfolioAnalyzer.analyze(input)`.
  4. Enrich `metrics.portfolio` with:
     - `performance` (Sharpe, MDD from analyzer)
     - `source`: `"REPORT_NET_WORTH_APPROX"` (Explicitly marking data origin)
     - `warnings`: Added `HISTORY_BUILT_FROM_REPORTS_APPROX`.

### 2.3 API Output Upgrade
- **GET /api/app/portfolio/metrics/latest** now returns:
```json
{
  "source": "REPORT_NET_WORTH_APPROX",
  "performance": {
    "sharpe": 1.25,          // Real value if history > 2pts
    "maxDrawdown": 0.05      // Real value if history > 2pts
  },
  "warnings": [
    "HISTORY_BUILT_FROM_REPORTS_APPROX"
  ]
   ...
}
```

## 3. Assumptions & Limitations
- **Approximation**: Relies on "Report Net Worth" which includes user deposits/withdrawals effectively as "performance" if not adjusted. This is a known limitation for this phase (M7-2).
- **Data Points**: Requires at least 2 reports to calculate any return, and ~12 for meaningful Sharpe.
- **Correlation**: Based on asset class balance changes, not underlying asset price correlation.

## 4. Verification
- **Build**: `mvn clean compile` passed.
- **Git Status**: Clean.
