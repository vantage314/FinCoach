# Debt Optimizer v1

## Context
Add a simple, explainable debt optimization plan (avalanche/snowball/mixed) based on debt list and monthly surplus. Outputs are attached to `adviceV2` with warnings and admin debug summary.

## Strategy (Avalanche / Snowball / Mixed)
- `AVALANCHE`: sort by interest rate (high → low).
- `SNOWBALL`: sort by principal (low → high).
- `MIXED`: debts with rates sorted by rate, debts without rates sorted by principal.

If all rates are missing, strategy falls back to `SNOWBALL`.

## Budget Rule
`budgetForExtraPayment = max(0, monthlySurplus * extraPayRatio)`  
Default `extraPayRatio=0.5` (configurable).

If `stressLevel=HIGH` or `monthlySurplus<=0`, budget is forced to 0 and PAY_DEBT_FIRST is recommended.

## Tradeoff Heuristic (Pay Debt vs Invest)
- Compare `maxDebtRate` vs `expectedReturn` with margin.
- `maxDebtRate >= expectedReturn + margin` → `PAY_DEBT_FIRST`
- `maxDebtRate <= expectedReturn - margin` → `INVEST_FIRST`
- otherwise → `BALANCED`

If expectedReturn or debt rates are missing: `INSUFFICIENT_DATA`.

## Degrade / Warnings
Warnings include:
- `DEBT_ITEMS_EMPTY`
- `DEBT_RATE_MISSING_ALL`
- `DEBT_RATE_PARTIAL`
- `DEBT_PAYOFF_ESTIMATE_UNAVAILABLE`
- `DEBT_OPTIMIZER_SURPLUS_NOT_POSITIVE`
- `TRADEOFF_EXPECTED_RETURN_MISSING`
- `TRADEOFF_DEBT_RATE_MISSING`

## How to Verify
```bash
cd backend
mvn clean test
```
Expect `BUILD SUCCESS` with `DebtOptimizerV1Test` passing.

## Rollback
```bash
git revert <commit_sha>
```
Revert debt optimizer v1 commits in reverse order (core, admin debug, tests, docs).
