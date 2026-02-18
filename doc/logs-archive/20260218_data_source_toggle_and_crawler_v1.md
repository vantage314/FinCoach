# Data Source Toggle & Crawler Control (Wave2) - 2026-02-18

## Summary
- Added DB-backed data source mode toggle (`DEMO_DB` / `REALTIME`).
- Added DB-backed crawler job status (`PY_MARKET_CRAWLER`) with start/stop placeholders.
- Admin UI includes `/admin/data-source` control panel and a new “数据与证券” menu group.
- Demo import writes sample snapshots into `fc_portfolio_price_snapshot` and minimal ticker mappings.

## Backend Changes
### Migrations
- `backend/src/main/resources/db/migration/V20260218_03__create_fc_system_config.sql`
- `backend/src/main/resources/db/migration/V20260218_04__create_fc_job_status.sql`

### Entities / Mappers
- `FcSystemConfigEntity`, `FcJobStatusEntity`
- `FcSystemConfigMapper`, `FcJobStatusMapper`
- `FcPortfolioPriceSnapshotEntity` mapping aligned to `snap_date`, `portfolio_value`, `source`, `currency`

### Admin API
Base path: `/admin/api/data-source`
- `GET /status`
- `POST /mode` body: `{ "mode": "DEMO_DB" | "REALTIME" }`
- `POST /demo/import`
- `POST /realtime/start`
- `POST /realtime/stop`

### Logging
Structured events:
- `event=DATA_SOURCE_MODE_SWITCH`
- `event=DEMO_DATA_IMPORT`
- `event=PY_CRAWLER_START`
- `event=PY_CRAWLER_STOP`

## Frontend Changes
- New page: `frontend/src/views/admin/AdminDataSource.vue`
- New API: `frontend/src/api/adminDataSource.ts`
- Admin menu group “数据与证券” with placeholders:
  - `/admin/securities/mapping`
  - `/admin/securities/snapshots`
  - `/admin/securities/quality`
- New route: `/admin/data-source`

## Demo Import Details
- Inserts 20 days of snapshots for current admin user (fallback `userId=1`).
- If `fc_ticker_mapping` is empty, inserts minimal `STOCK/BOND/CASH` mappings.
- Re-runs are idempotent: existing `(user_id, snap_date)` rows are skipped.

## Test Coverage
- `DataSourceAdminServiceTest`:
  - default status
  - mode switch
  - demo import writes
- `mvn clean test` -> **BUILD SUCCESS** (91 tests).

## Verification (Manual)
Example curl (replace token):
```bash
curl -i -H "Authorization: Bearer <token>" http://localhost:5173/admin/api/data-source/status
curl -i -H "Authorization: Bearer <token>" -X POST -H "Content-Type: application/json" \
  -d '{"mode":"REALTIME"}' http://localhost:5173/admin/api/data-source/mode
curl -i -H "Authorization: Bearer <token>" -X POST http://localhost:5173/admin/api/data-source/demo/import
curl -i -H "Authorization: Bearer <token>" -X POST http://localhost:5173/admin/api/data-source/realtime/start
curl -i -H "Authorization: Bearer <token>" -X POST http://localhost:5173/admin/api/data-source/realtime/stop
```

## Rollback
Commits:
- `d5cb17d7` feat(db): add system config and job status tables
- `641ed38c` feat(admin-api): add data source toggle and crawler control apis
- `d311cd7e` feat(admin-ui): add data source page and menu placeholders
- `e9566cec` test: add coverage for data source admin
- `<docs-commit>` docs: archive data source toggle notes

Rollback (reverse order):
- `git revert <docs-commit>`
- `git revert e9566cec`
- `git revert d311cd7e`
- `git revert 641ed38c`
- `git revert d5cb17d7`
