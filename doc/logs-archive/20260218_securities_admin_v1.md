# Securities Admin v1 (Wave3) - 2026-02-18

## Summary
- Implemented securities admin APIs for mapping CRUD, snapshots browsing, and data quality checks.
- Added three admin pages: 证券映射 / 行情快照 / 数据质量.
- Hooked “导入演示数据” to snapshots page to form a demo-ready loop.

## Backend Changes
### Admin API
Base path: `/admin/api/securities`
- `GET /mapping?keyword=&enabled=&page=&size=`
- `POST /mapping` (create)
- `PUT /mapping/{id}` (update)
- `POST /mapping/{id}/toggle?enabled=true|false`
- `DELETE /mapping/{id}` (soft disable)
- `GET /snapshots?assetKey=&startDate=&endDate=&page=&size=` (response: `items[]` + `summary{assetsCount,sampleSize,startDate,endDate,latestDate}`)
- `GET /quality` (response: `missingMappings[]`, `snapshotCoverage{assetsCount,daysCovered,latestDate,lagDays,issues[]}`, `anomalies[]`, `recommendations[]`)

### Structured Logs
- `event=SEC_MAPPING_CREATE/UPDATE/TOGGLE`
- `event=SEC_SNAPSHOT_QUERY`
- `event=SEC_DATA_QUALITY`

### Data Quality Defaults (fc_system_config)
- `SNAPSHOT_MIN_DAYS` (default `20`)
- `SNAPSHOT_MAX_LAG_DAYS` (default `3`)
- `ANOMALY_CHANGE_PCT` (default `0.2`)

## Frontend Changes
- New pages: `frontend/src/views/admin/AdminSecuritiesMapping.vue`, `frontend/src/views/admin/AdminSecuritiesSnapshots.vue`, `frontend/src/views/admin/AdminSecuritiesQuality.vue`
- API wrapper: `frontend/src/api/adminSecurities.ts`
- Routes wired to real pages (replace placeholders): `/admin/securities/mapping`, `/admin/securities/snapshots`, `/admin/securities/quality`
- Snapshots page integrates demo import via `/admin/api/data-source/demo/import`

## Test Coverage
- `AdminSecuritiesMappingControllerTest`: create + list + toggle
- `AdminSecuritiesQualityTest`: missing mapping + insufficient snapshots issues
- `mvn clean test` -> **BUILD SUCCESS**

## Verification (Manual)
1) Start backend + frontend (no runtime required here; unit tests cover server).
2) Example curl (replace token):
```bash
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/admin/api/securities/mapping?page=1&size=20"

curl -X POST -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"keyword":"STOCK","ticker":"SPY.US","market":"US","priority":100,"enabled":1}' \
  "http://localhost:8080/admin/api/securities/mapping"

curl -X POST -H "Authorization: Bearer <token>" \
  "http://localhost:8080/admin/api/securities/mapping/1/toggle?enabled=false"

curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/admin/api/securities/snapshots?page=1&size=20"

curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/admin/api/securities/quality"
```

3) UI behavior:
- Mapping 新增一条后，列表出现新记录。
- Snapshots 点击“导入演示数据”，summary/sampleSize 更新。
- Quality 页面可看到缺失项/覆盖不足或滞后提示。

## Real Execution Evidence
Environment: local backend `8080`, frontend `5173`, admin user `admin_wave3`.

1) `GET http://localhost:5173/admin/api/data-source/status`
- HTTP 200
- Response (truncated):
```json
{"code":200,"data":{"mode":"DEMO_DB","job":{"status":"STOPPED","updatedAt":"2026-02-18T22:17:26.042197900"}}}
```

2) `POST http://localhost:5173/admin/api/data-source/demo/import`
- HTTP 200
- Response (truncated):
```json
{"code":200,"data":{"insertedSnapshots":20,"skippedSnapshots":0,"insertedMappings":3,"message":"demo import completed"}}
```

3) `GET http://localhost:5173/admin/api/securities/snapshots?page=1&size=5`
- HTTP 200
- Response (truncated):
```json
{"code":200,"data":{"items":[{"date":"2026-02-18","assetKey":"DEMO_DB","price":"110800.000000"}],"summary":{"assetsCount":1,"startDate":"2026-01-30","endDate":"2026-02-18","latestDate":"2026-02-18"}}}
```

4) `GET http://localhost:5173/admin/api/securities/quality`
- HTTP 200
- Response (truncated):
```json
{"code":200,"data":{"missingMappings":[],"snapshotCoverage":{"issues":["没有快照数据","没有可用资产标识"]},"anomalies":[],"recommendations":["导入演示数据或启动抓取以补齐行情快照"]}}
```

## Expected Changes After Demo Import
- Snapshots: 列表出现 DEMO_DB 快照记录，`summary.assetsCount` 与 `latestDate` 有值。
- Quality: 返回结构化 JSON（可能包含缺失映射或覆盖不足/滞后提示）。

## Rollback
Commits (reverse order):
1. `git revert <docs-commit>`
2. `git revert <test-commit>`
3. `git revert <ui-commit>`
4. `git revert <api-commit>`
