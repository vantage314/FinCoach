# Layout Split: User vs Admin (2026-02-18)

## Summary
- User routes moved under `/app/*` with `UserLayout` (left menu + content).
- Admin routes live under `/admin/*` with `AdminLayout` (left menu + content).
- `/admin` proxy fixed to only forward `/admin/api`.

## Route Structure
- User
  - `/app/diagnosis`
  - `/app/dashboard`
  - `/app/market`
  - `/app/asset/analysis`
  - `/app/asset/manage`
  - `/app/plan`
  - `/app/investment`
  - `/app/chat`
  - `/app/user/profile`
- Admin
  - `/admin/alerts`
  - `/admin/debug`
- Legacy redirects
  - `/` -> `/app/diagnosis`
  - `/diagnosis` -> `/app/diagnosis`
  - `/dashboard` -> `/app/dashboard`
  - `/market` -> `/app/market`

## Layouts & Menus
- `UserLayout` uses classic left menu; only user features shown.
- `AdminLayout` uses classic left menu; only admin features shown.
- Admin entry in user side: “进入后台” (visible only when `isAdmin=true`).

## Guard & Login Flow
- Unauthenticated access to `/app/*` or `/admin/*` redirects to `/login`.
- Authenticated non-admin access to `/admin/*` redirects to `/403`.
- Login redirect
  - ADMIN -> `/admin/alerts`
  - User -> `/app/diagnosis`

## Proxy Fix
- Vite proxy no longer forwards `/admin/*` page routes.
- Only `/admin/api/*` and `/api/*` are proxied to backend.

## Verification
- `/admin/debug` opens on `5173` and no backend log for `/admin/debug`.
- `curl -i http://localhost:5173/admin/api/alerts?limit=1&status=OPEN` reaches backend (401/403 allowed).
- Login paths
  - User -> `/app/diagnosis`
  - Admin -> `/admin/alerts`

## Rollback
- `git revert d2d05727` (layout/routes/guard)
- `git revert 2b9b3de7` (proxy fix)
- Revert this docs commit if needed.
