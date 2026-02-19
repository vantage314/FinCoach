# Admin Debug 403 Manual Check (2026-02-19)

## Steps
1. Login as an admin user (role `ADMIN`).
2. Open `/admin/debug` and click "刷新".
3. Confirm the snapshot loads with HTTP 200 and no 403 in the network panel.
4. Use a non-admin token to call `GET /api/admin/portfolio/market-debug/latest` and confirm it returns 403.
