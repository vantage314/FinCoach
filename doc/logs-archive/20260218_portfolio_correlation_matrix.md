# Portfolio Correlation Matrix (2026-02-18)

## Context
Provide a stable, explainable correlation matrix output for portfolio analysis, with clear warnings on missing/insufficient data.

## Algorithm
- Pearson correlation.
- Outputs symmetric matrix with diagonal = 1.
- Values rounded to 4 decimals for serialization stability.

## Data requirements
- Requires per-asset return series from `PortfolioHistoryFacade` inputs.
- Minimum points per asset: 10.

## Degrade strategy
- Asset with points < 10 is dropped and emits warning `CORR_INSUFFICIENT_POINTS:{assetCode}`.
- If remaining assets < 2, emit `CORR_NOT_ENOUGH_ASSETS` and return empty matrix.
- If no return series available at all, emit `CORR_HISTORY_UNAVAILABLE`.

## Warnings
- `CORR_NOT_ENOUGH_ASSETS`
- `CORR_INSUFFICIENT_POINTS`
- `CORR_HISTORY_UNAVAILABLE`

## How to verify
- `cd backend && mvn clean test`

## Rollback
- `git revert <commit_sha>`
