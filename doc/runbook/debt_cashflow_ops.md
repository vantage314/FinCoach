# Debt + Cashflow Ops Runbook (Wave4.5)

## Data Definitions
- Debt types: `MORTGAGE`, `CAR`, `CONSUMER`, `CREDITCARD`, `OTHER`
- Month format: `YYYY-MM` (stored as string, e.g. `2026-02`)

## How to Input Data
### Debt
- Use `/api/debt/upsert` with:
  - `debtType`, `principal`, `apr`, `remainingBalance`
  - `monthlyPayment` optional; computed for installment loans if missing
- Soft delete via `/api/debt/delete` (sets `is_active=false`)

### Cashflow
- Use `/api/cashflow/upsertMonth` with:
  - `month`, `income`, `expense`
  - `net` is computed as `income - expense`
- Query range with `/api/cashflow/months?from=YYYY-MM&to=YYYY-MM`

## Scoring Integration (HealthV2)
- DTI (debt-to-income):
  - `totalMonthlyDebtPayment / avgMonthlyIncome`
  - Warnings: `DTI_WARN`, `DTI_DANGER`, or `DTI_INSUFFICIENT_INCOME`
- Emergency fund months:
  - `liquidCash / avgMonthlyExpense`
  - Warnings: `EMERGENCY_FUND_UNKNOWN`, `EMERGENCY_FUND_INSUFFICIENT_EXPENSE`, `EMERGENCY_FUND_LOW`
- Cashflow:
  - Uses last 3 months average
  - Warning: `CASHFLOW_INSUFFICIENT_DATA`, `CASHFLOW_NEGATIVE`

## Advice Suggestions
### Debt Suggestions
Triggered by:
- Negative net cashflow -> `CASHFLOW_STABILIZE`
- Emergency fund below target -> `EMERGENCY_FUND`
- DTI above thresholds -> `DTI_RISK`
- Active debts sorted by APR (AVALANCHE) -> `DEBT_PRIORITY`

### Cashflow Suggestions
Triggered by:
- Insufficient data (<2 months) -> `CASHFLOW_STABILIZE`
- Negative net -> `CASHFLOW_STABILIZE`
- Positive net + emergency fund below target -> `EMERGENCY_FUND`
- Positive net + emergency fund met -> `REBALANCE_ALLOCATE`

## Config Defaults (Advice Ruleset)
- `EMERGENCY_FUND_MONTHS_TARGET` = 3
- `DTI_WARN` = 0.35
- `DTI_DANGER` = 0.50
- `DEBT_STRATEGY` = AVALANCHE

## Troubleshooting
- `CASHFLOW_ADVICE_INSUFFICIENT_DATA`: add at least 2 months of cashflow.
- `DEBT_ADVICE_INSUFFICIENT_INCOME`: income missing or zero.
- `DEBT_MISSING_APR`: one or more debts missing APR.
- `EMERGENCY_FUND_UNKNOWN`: cash assets missing or zero.
- `RULE_PARAM_PARSE_ERROR`: rule param invalid, fallback used.
