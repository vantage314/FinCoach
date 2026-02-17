# Insurance Gap v1

## Context
Add an insurance gap assessment with configurable thresholds, explainable gaps, and structured advice. Missing inputs degrade gracefully with warnings and are visible in admin debug.

## Rules / Defaults
Config keys (score rule set):
- `INS_PREMIUM_RATIO_MAX` (default 0.10)
- `INS_LIFE_MULTIPLIER` (default 10)
- `INS_LIFE_DEPENDENT_BONUS_MULTIPLIER` (default 1)
- `INS_CRITICAL_ILLNESS_MULTIPLIER` (default 3)
- `INS_ACCIDENT_MULTIPLIER` (default 1)

Medical coverage is boolean in V1. If medical info is missing, mark as missing and warn.

## Priority Logic
Default priority: `MEDICAL > ACCIDENT > CI > LIFE`.  
If dependents or debt exist, LIFE is promoted near the top with a reason.

## Degrade / Warnings
Warnings include:
- `INSURANCE_INCOME_MISSING`
- `INSURANCE_PREMIUM_MISSING`
- `INSURANCE_CURRENT_MISSING:{type}`
- `MISSING_INSURANCE_MEDICAL_INFO`
- `MISSING_DEPENDENTS_INFO`

## How to Verify
```bash
cd backend
mvn clean test
```
Expect `BUILD SUCCESS` and `InsuranceGapV1Test` passing.

## Rollback
```bash
git revert <commit_sha>
```
Revert insurance gap v1 commits in reverse order (core, admin debug, tests, docs).
