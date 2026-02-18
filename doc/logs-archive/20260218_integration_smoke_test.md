# Integration Smoke Test - 2026-02-18

## Scope
- Target: login -> report page (7 cards) -> admin menu (ADMIN only) -> `/admin/alerts` list + ack -> `/admin/debug` summary
- Branch: `fix/frontend-backend-integration`

## Environment
- Backend: `http://localhost:8080`
- Frontend: `http://localhost:5173`
- DB: `fincoach` (MySQL, user `root`)

## Changes
### Backend
- `WebConfig` now intercepts `/admin/**` so JWT/UserContext applies to admin APIs.
- `AdminChecker` and `PermissionChecker` add RBAC-missing fallback (dev admin via `userId=1`).
- Added SQL scripts for local dev tables + seeds:
  - `backend/src/main/resources/sql/migration/20260218_alert_record_table.sql`
  - `backend/src/main/resources/sql/migration/20260218_alert_record_seed.sql`
  - `backend/src/main/resources/sql/migration/20260218_health_report_table.sql`
  - `backend/src/main/resources/sql/migration/20260218_health_report_seed.sql`

### Frontend
- User store: robust role extraction (JWT roles + profile fallback) + dev admin fallback for `userId=1`.
- Health store: prefer HealthReport v2 (`/api/app/health-reports/latest`) with mapping to existing UI shape; fallback to legacy `/api/health/check`.
- Report page: portfolio/advice fallbacks for `metrics.portfolio` and `advice.adviceV2`.
- Admin debug: correlation matrix folded by default.

## DB Actions (local)
Executed in shell:
- `mysql -uroot -p030314 fincoach < backend/src/main/resources/sql/migration/20260218_alert_record_table.sql`
- `mysql -uroot -p030314 fincoach < backend/src/main/resources/sql/migration/20260218_alert_record_seed.sql`
- `mysql -uroot -p030314 fincoach < backend/src/main/resources/sql/migration/20260218_health_report_table.sql`
- `mysql -uroot -p030314 fincoach < backend/src/main/resources/sql/migration/20260218_health_report_seed.sql`

## Smoke Steps & Evidence
### Backend tests
- `cd backend && mvn clean test` -> **BUILD SUCCESS** (88 tests).

### Backend run
- `mvn spring-boot:run` (JAVA_HOME set to JDK21) -> port **8080**.

### Frontend run
- `cd frontend && pnpm dev` -> port **5173**.

### Proxy curl (required)
- `curl -i http://localhost:5173/api/admin/portfolio/market-debug/latest?includeMatrix=true`
  - **401** `{"code":401,"message":"未登录或 Token 缺失"}`
- `curl -i http://localhost:5173/admin/api/alerts?limit=5&status=OPEN`
  - **401** `{"code":401,"message":"未登录或 Token 缺失"}`

### Login
- `POST /api/auth/login` (vantage1 / ******)
  - **200** `data=<JWT>` (token redacted)
- Wrong password (len>=6)
  - **401** `{"code":401,"message":"用户名或密码错误"}`

### Report v2 data (API)
- `GET /api/app/health-reports/latest`
  - **200** with `metrics.scores`, `metrics.portfolio.correlationMatrixSummary`, `metrics.rebalanceAdviceV1`, `metrics.debtCashflowV1`, `metrics.insuranceGapV1`, `metrics.alertsV1`, and `advice.adviceV2.*`.

### Admin alerts list + ack (proxy)
- `GET /admin/api/alerts?limit=5&status=OPEN` (Authorization: Bearer ...)
  - **200** `total=1`, items include `ALERT_RISK_HIGH`
- `POST /admin/api/alerts/ack` (Authorization + body `{alerts:[{alertId:2}]}`)
  - **200** `"ACKED:1"`

### Admin debug (proxy)
- `GET /api/admin/portfolio/market-debug/latest?includeMatrix=true` (Authorization)
  - **200** with `warnings=["DEBUG_SNAPSHOT_EMPTY"]` and empty summaries (expected in dev)

## Manual UI Smoke (recorded)
1) Login as `vantage1` -> success, redirected to report page.
2) Report page shows 7 module cards (scores / correlation / rebalance / debt cashflow / debt optimizer / insurance gap / alerts). Data populated from HealthReport v2 seed; cards render without errors.
3) Admin menu appears for ADMIN (`userId=1` dev fallback) and routes `/admin/alerts` + `/admin/debug` load.
4) `/admin/alerts` displays list and ACK works; `/admin/debug` shows summary + collapsible matrix section.

## Notes / Caveats
- Local DB lacks many v2 tables; admin + report flows rely on the minimal seed tables above.
- Admin fallback is **dev-only** (`userId=1`) when RBAC tables are missing.

## Rollback
- `git revert 74b75389`
- `git revert 308be86c`
- `git revert a94ed3d5`
- `git revert 3318c21b`
