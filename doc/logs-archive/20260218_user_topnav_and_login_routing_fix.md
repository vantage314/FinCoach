# User Top Nav + Login Routing Fix - 2026-02-18

## Summary
- Restored user-side top horizontal navigation for `/app/*`.
- Kept admin-side left navigation intact for `/admin/*`.
- Hardened admin role detection and login redirects to prevent regular users entering admin pages.

## Frontend Changes
- Added `UserTopLayout.vue` with `el-menu` horizontal top nav for `/app/*` routes.
- Router mounts `/app/*` under `UserTopLayout`, `/admin/*` under `AdminLayout`.
- “进入后台” entry only renders for `isAdmin` users on the top-right area.
- `isAdmin` derived strictly from JWT roles (`roles`/`authorities`/`role` includes `ADMIN`/`ROLE_ADMIN`).
- Login redirect: admin -> `/admin/alerts`, non-admin -> `/app/diagnosis`.
- Route guard blocks non-admin access to `/admin/*` with `/403`.
- Added `console.debug` to print parsed roles after login (no token contents).

## Verification (Manual)
- Visit `/app/diagnosis`: top horizontal menu is visible; no left sidebar.
- Visit `/admin/alerts`: left sidebar remains visible.
- Login checks:
  - `vantage1/123456` -> `/app/diagnosis`, and `/admin/alerts` -> `/403`.
  - Admin user -> `/admin/alerts`.

## Rollback
Revert commit:
- `git revert <fix-frontend-commit>`
