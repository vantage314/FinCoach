# Advice Warnings Observability (2026-02-18)

## Context
AdviceEngineV2 now emits input-quality warnings. We need these warnings to be observable for ops/debug without exposing sensitive data.

## What changed
- Added structured log event `ADVICE_V2_WARNINGS` in `HealthReportV2ServiceImpl.generate`.
- Added admin read-only stats API: `GET /admin/api/advice/warnings/stats?limit=200`.

## How to verify
- `cd backend && mvn clean test`
- Curl example:
  - `curl -s "http://localhost:8080/admin/api/advice/warnings/stats?limit=200"`
- Latest run: `Tests run: 50, Failures: 0, Errors: 0, Skipped: 0` + `BUILD SUCCESS`

## Rollback
- `git revert c5871402 4d387f8e fb9eb002`

## Security note
- Log and API only expose warning codes and truncated detail strings; no asset detail or PII is emitted.
