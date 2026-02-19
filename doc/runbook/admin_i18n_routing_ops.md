# Admin i18n & Routing Ops Runbook

## Add New Admin Labels (i18n)
1. Open `frontend/src/utils/labelMap.ts`.
2. Add new key/value pairs in `adminLabelMap`.
3. Use `getAdminLabel(key)` or helpers (e.g., `formatEnabledText`) in admin views.
4. Keep keys consistent with backend payload fields (camel case keys used in UI).

Example:
```ts
export const adminLabelMap = {
  NewKey: '新字段中文',
};
```

## Keep /admin and /app Boundaries Correct
- Admin-only views must live under `/admin/*` routes.
- User views must live under `/app/*` routes.
- Never link from admin layout to `/app/*` routes.
- Never link from user layout to `/admin/*` routes.
- Router guard rules:
  - `/admin/*` requires ADMIN role.
  - `/app/*` requires USER role (admin-only token is denied).

If roles change, update `frontend/src/router/index.ts` guard logic accordingly.

## Chrome DevTools Scan Artifacts
Drop artifacts under:
```
doc/qa/chrome-scans/YYYYMMDD_HHMM/
```
Suggested contents:
- `console.log`
- `network.har`
- `screenshots/`
- `notes.md`

See `doc/qa/chrome-scans/README.md` for the folder convention.
