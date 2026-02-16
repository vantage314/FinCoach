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
  - Input: Current Positions (Symbol -> Quantity).
  - Output: `PortfolioInput` (Equity Curve, Returns, Asset Returns).
  - Logic: Skips gaps, warns on missing symbols.
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
