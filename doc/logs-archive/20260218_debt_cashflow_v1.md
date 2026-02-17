# Debt & Cashflow v1

## Context
Introduce debt and cashflow metrics with explainable outputs, structured advice, and warnings. Missing inputs degrade gracefully and warnings are surfaced for observability.

## Metrics Definitions
`metrics.debtCashflowV1` includes:
- `debtMetrics`: `totalDebt`, `monthlyDebtPayment`, `dti`, `debtToAssets`, `interestWeightedRate`, `warnings`, `explanations`
- `cashflowMetrics`: `monthlyIncome`, `monthlyExpense`, `monthlySurplus`, `surplusRate`, `emergencyFundMonths`, `warnings`, `explanations`
- `combined`: `stressLevel`, `stressReasons`, `warnings`

Each `explanations[]` item contains `rawValue`, `threshold`, and `detail`.

## Threshold Config Keys
Read from the active score rule set (fallback to defaults when missing):
- `DTI_HIGH`, `DTI_MED`
- `SURPLUS_RATE_MIN`
- `EMERGENCY_MONTHS_LOW`, `EMERGENCY_MONTHS_OK`
- `DEBT_TO_ASSETS_HIGH`
- `LIQUID_ASSET_ESTIMATE_RATIO`

## Advice Rules
`adviceV2.debtCashflowAdviceV1` returns:
- DTI high → reduce debt pressure
- Surplus rate low/negative → cashflow stop-loss
- Emergency fund low → build reserves
- Debt-to-assets high → limit new debt

## Degrade / Warnings
Missing inputs do not throw; warnings include:
- `MISSING_DEBT_ITEMS`
- `MISSING_CASHFLOW_INCOME`
- `MISSING_CASHFLOW_EXPENSE`
- `MISSING_LIQUID_ASSETS`
- `EMERGENCY_FUND_ESTIMATED`
- `MISSING_TOTAL_ASSETS`

## How to Verify
```bash
cd backend
mvn clean test
```
Expect `BUILD SUCCESS` and `DebtCashflowV1Test` passing.

## Rollback
```bash
git revert <commit_sha>
```
Revert the debt/cashflow v1 commits in reverse order (core, admin debug, tests, docs).
