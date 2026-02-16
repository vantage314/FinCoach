# Ops Log: M7-3 Market Data Driven Portfolio History

**Date:** 2026-02-16
**Status:** Completed

## 1. Goal
Upgrade Portfolio Metrics to use real market data for Volatility and Correlation calculations, falling back to "Report Net Worth" approximation only when market data is unavailable.

## 2. Architecture
- **Provider**: `StooqMarketDataProvider` (MVP, Daily Close, No API Key).
  - Fetches CSV from Stooq.
  - Returns `Map<LocalDate, BigDecimal>`.
- **Builder**: `PortfolioHistoryBuilderMarket`.
  - **Ticker Resolution**: Maps names to tickers (e.g. "茅台" -> "600519.SS"). Returns `POSITION_TICKER_UNRESOLVED` if failed.
  - **Symbol Normalization**: 
    - 6 digits starting with 6 -> .SS
    - 6 digits starting with 0/3 -> .SZ
    - Alpha only -> .US
    - Records `MARKET_DATA_SYMBOL_NORMALIZED` warning.
  - **Robustness**: Catches provider errors, returns empty series instead of failing. Warns `MARKET_DATA_HTTP_ERROR`.
  - **Persistence**: Controlled by `fincoach.portfolio.snapshot.enabled` (default false). Warns `SNAPSHOT_PERSIST_SKIPPED` if disabled/failed.
- **Facade**: `PortfolioHistoryFacade`.
  - Tries `buildFromMarketData`.
  - If fails or insufficient points (<2), falls back to `PortfolioHistoryBuilder` (Report Approx).
  - Sets `source` to `MARKET_DATA_DAILY_CLOSE` or `REPORT_NET_WORTH_APPROX`.
- **Storage**: `fc_portfolio_price_snapshot` (Entity/Mapper created, but DB migration skipped due to env access issues).

## 3. Schema
- `fc_portfolio_price_snapshot`: Stores daily snapshots (Not yet populated in MVP).

## 4. Key Changes
- Refactored `HealthReportV2ServiceImpl` to use `PortfolioHistoryFacade`.
- Added Unit Tests for Provider and Builder.
