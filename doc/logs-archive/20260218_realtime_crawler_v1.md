# Realtime Crawler v1 - 2026-02-18

## Summary
- Added safe async Python crawler runner with structured JSON line parsing and job logs.
- Added DAEMON / RUN_ONCE modes with interval-based scheduling and correct status transitions.
- Aligned demo import asset keys for quality checks (STOCK / BOND / CASH).
- Demo import now cleans legacy `DEMO_DB` snapshots/mappings; quality ignores legacy keys.
- Admin Data Source UI shows last crawler log and respects RUNNING state.

## Backend Changes
- Demo import writes `assetKey` (source) as `STOCK`, `BOND`, `CASH` and upserts by user/date.
- Import pre-clean: delete `DEMO_DB` snapshots and disable mapping (`enabled=0`).
- New runner abstraction:
  - `CrawlerRunner` interface + `DefaultCrawlerRunner` using `ProcessBuilder` (no shell string).
  - Script whitelist: `scripts/python/crawler.py`.
- Job status extended with `last_log` (<= 2000 chars), `last_error` (<= 500).
- `DataSourceAdminServiceImpl` supports DAEMON scheduling with heartbeat updates per batch.
- JWT now includes `roles` claim for frontend admin checks.

## Crawler Modes & Config
`fc_system_config` keys (auto-upsert defaults):
- `CRAWLER_MODE` = `DAEMON` (default)
- `CRAWLER_INTERVAL_SECONDS` = `10` (default)
- `CRAWLER_MAX_BATCHES` = `0` (0 = infinite loop, RUN_ONCE uses 1)

Behavior:
- `RUN_ONCE`: run 1 batch then STOPPED (`last_end_at` set).
- `DAEMON`: run every interval until stop; heartbeat updates every batch.

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
Note: for validation, `CRAWLER_INTERVAL_SECONDS` was set to `3` to observe heartbeat updates.

1) `POST http://localhost:5173/admin/api/data-source/mode` (`{"mode":"REALTIME"}`)
- HTTP 200
- Response (truncated):
```json
{"code":200,"data":{"mode":"REALTIME","crawlerMode":"DAEMON","crawlerIntervalSeconds":3,"job":{"status":"STOPPED"}}}
```

2) `POST http://localhost:5173/admin/api/data-source/realtime/start`
- HTTP 200
- Response (truncated):
```json
{"code":200,"data":{"status":"RUNNING","message":"start requested"}}
```

3) `GET http://localhost:5173/admin/api/data-source/status` (immediately)
- HTTP 200
- Response (truncated):
```json
{"code":200,"data":{"mode":"REALTIME","crawlerMode":"DAEMON","job":{"status":"RUNNING","lastHeartbeatAt":"2026-02-18T23:46:19"}}}
```

4) `GET http://localhost:5173/admin/api/data-source/status` (after ~4s)
- HTTP 200
- Response (truncated):
```json
{"code":200,"data":{"job":{"status":"RUNNING","lastHeartbeatAt":"2026-02-18T23:46:31"}}}
```

5) `POST http://localhost:5173/admin/api/data-source/realtime/stop`
- HTTP 200
- Response (truncated):
```json
{"code":200,"data":{"status":"STOPPED","message":"stop requested"}}
```

6) `GET http://localhost:5173/admin/api/data-source/status` (within 1 interval)
- HTTP 200
- Response (truncated):
```json
{"code":200,"data":{"job":{"status":"STOPPED","lastEndAt":"2026-02-18T23:46:44"}}}
```

## Demo Import Check
`POST http://localhost:5173/admin/api/data-source/demo/import`
- HTTP 200
- Response (truncated):
```json
{"code":200,"data":{"insertedSnapshots":0,"skippedSnapshots":20,"insertedMappings":0}}
```

`GET http://localhost:5173/admin/api/securities/quality`
- HTTP 200
- Response (truncated):
```json
{"code":200,"data":{"missingMappings":[],"legacyDemoKeyIssues":[]}}
```

## Expected Changes After Demo Import
- Snapshots list contains `STOCK` / `BOND` / `CASH` asset keys.
- Legacy `DEMO_DB` snapshots/mapping are removed/disabled before import.
- Quality endpoint returns structured JSON without `missingMappings: DEMO_DB`.

## Rollback
Revert commits (reverse order):
1. `git revert <docs-commit>`
2. `git revert <test-commit>`
3. `git revert <admin-ui-commit>`
4. `git revert <crawler-commit>`
5. `git revert <db-commit>`
6. `git revert <frontend-fix-commit>`
