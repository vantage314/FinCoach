# AdviceEngineV2 NPE Hotfix Regression Log (2026-02-18)

## Context
- Module: `AdviceEngineV2` (health report advice v2)
- Entry: `HealthReportV2ServiceImpl.generate` -> `AdviceEngineV2.build(...)`
- Trigger shape: missing cashflow/ratio inputs causing evidence values to be `null` (previously `Map.of(..., null, ...)` raised NPE).

## Fix Summary
- NPE fix already applied in commit `2ea00def` ("test(docs): add tests and update runbook/ops/plan").
- Root cause was `Map.of` rejecting `null` evidence values in `buildEmergencyAdvice/buildDebtAdvice/buildSurplusAdvice`.

## Regression Test
- Added: `AdviceEngineCashflowDebtTest#testBuildDoesNotThrowWhenEvidenceValuesMissing`
- Input shape: `assets=null`, `liabilities=null`, `cashflow=null`, `allocation=null`, `totalAssets=null`
- Assertions: `assertDoesNotThrow`, result object non-null, `advices` non-null.

## Verification
- Command: `cd backend && mvn clean test`
- Result: `Tests run: 47, Failures: 0, Errors: 0, Skipped: 0` + `BUILD SUCCESS`
- Notes: ByteBuddy/CDS warnings present; safe to ignore.

## Rollback
- `git revert <this_test_docs_commit_sha>`
