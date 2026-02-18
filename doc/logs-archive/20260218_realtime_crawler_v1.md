# Realtime Crawler v1 - 2026-02-18

## Summary
- Added safe async Python crawler runner with structured JSON line parsing and job logs.
- Aligned demo import asset keys for quality checks (STOCK / BOND / CASH).
- Admin Data Source UI shows last crawler log and respects RUNNING state.

## Backend Changes
- Demo import writes `assetKey` (source) as `STOCK`, `BOND`, `CASH` and upserts by user/date.
- New runner abstraction:
  - `CrawlerRunner` interface + `DefaultCrawlerRunner` using `ProcessBuilder` (no shell string).
  - Script whitelist: `scripts/python/crawler.py`.
- Job status extended with `last_log` (<= 2000 chars), `last_error` (<= 500).
- `DataSourceAdminServiceImpl` runs async job, streams lines, updates `last_log`, and upserts snapshots.
- JWT now includes `roles` claim for frontend admin checks.

## Script
- `scripts/python/crawler.py` outputs JSON lines:
  - `{"assetKey":"SPY.US","date":"YYYY-MM-DD","price":...}`

## Admin UI
- `/admin/data-source` shows `last_log` in a collapsible panel.
- Start is disabled when job is RUNNING; stop refreshes status.

## Tests
- `DataSourceAdminServiceTest`: FakeRunner validates upsert and status updates.
- `AdminSecuritiesQualityTest`: demo import no longer returns “没有快照数据”.
- `mvn clean test` -> **BUILD SUCCESS**.

## Real Execution Evidence
Environment: local backend `8080`, frontend `5173`, admin user `admin_rt`.

1) `POST http://localhost:5173/admin/api/data-source/mode` (`{"mode":"REALTIME"}`)
- HTTP 200
- Response (truncated):
```json
{"code":200,"data":{"mode":"REALTIME","job":{"status":"STOPPED"}}}
```

2) `POST http://localhost:5173/admin/api/data-source/realtime/start`
- HTTP 200
- Response (truncated):
```json
{"code":200,"data":{"status":"RUNNING","message":"start requested"}}
```

3) `GET http://localhost:5173/admin/api/data-source/status`
- HTTP 200
- Response (truncated):
```json
{"code":200,"data":{"mode":"REALTIME","job":{"status":"STOPPED","lastLog":"{\"assetKey\": \"SPY.US\"..."}}}
```

4) `GET http://localhost:5173/admin/api/securities/snapshots?page=1&size=5`
- HTTP 200
- Response (truncated):
```json
{"code":200,"data":{"items":[{"date":"2026-02-18","assetKey":"CASH"}...]}}
```

5) `POST http://localhost:5173/admin/api/data-source/realtime/stop`
- HTTP 200
- Response (truncated):
```json
{"code":200,"data":{"status":"STOPPED","message":"stop requested"}}
```

## Demo Import Check
`POST http://localhost:5173/admin/api/data-source/demo/import`
- HTTP 200
- Response (truncated):
```json
{"code":200,"data":{"insertedSnapshots":0,"skippedSnapshots":20,"insertedMappings":0}}
```

## Expected Changes After Demo Import
- Snapshots list contains `STOCK` / `BOND` / `CASH` asset keys (plus legacy `DEMO_DB` if historical data exists).
- Quality endpoint returns structured JSON with coverage info and recommendations.

## Rollback
Revert commits (reverse order):
1. `git revert <docs-commit>`
2. `git revert <test-commit>`
3. `git revert <admin-ui-commit>`
4. `git revert <crawler-commit>`
5. `git revert <db-commit>`
6. `git revert <frontend-fix-commit>`
