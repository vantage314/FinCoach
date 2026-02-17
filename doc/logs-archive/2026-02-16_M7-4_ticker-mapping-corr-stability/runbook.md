# Runbook: M7-4 Verification

## 1. Happy Path Verification
### Ticker Mapping (File)
1. Add `贵州茅台: 600519.SS` to `ticker-mapping.yml`.
2. Request Portfolio Metrics with "贵州茅台".
3. Verify warning: `TICKER_MAPPING_HIT_ALIAS`.
4. Verify source: `MARKET_DATA_DAILY_CLOSE`.

### Ticker Mapping (DB)
1. Insert mapping:
   ```sql
   INSERT INTO fc_ticker_mapping(keyword, ticker, priority, enabled)
   VALUES ('茅台', '600519.SS', 100, 1);
   ```
2. Request Portfolio Metrics with "茅台".
3. Verify warning: `TICKER_MAPPING_SOURCE_DB`.
4. (Optional) Call admin reload to refresh cache if you updated DB:
   - `POST /api/admin/ticker-mapping/reload`

### Ticker Mapping (Admin CRUD)
1. Page query:
   - `GET /api/admin/ticker-mapping/page?keyword=AA&enabled=1&page=1&size=20`
2. Save (insert/update):
   - `POST /api/admin/ticker-mapping/save`
   - Body example:
     ```json
     {
       "keyword": "AAPL",
       "ticker": "AAPL.US",
       "priority": 100,
       "enabled": 1
     }
     ```
3. Enable/Disable:
   - `POST /api/admin/ticker-mapping/enable?id=123`
   - `POST /api/admin/ticker-mapping/disable?id=123`
4. Reload registry:
   - `POST /api/admin/ticker-mapping/reload`
5. Verify `TickerMappingRegistry.resolve` now hits DB and warnings include `TICKER_MAPPING_SOURCE_DB`.

### Correlation Stability
1. Create portfolio with assets having different trading calendars (e.g. CN vs US, or one with gaps).
2. Check `correlation.matrix`. Should be valid (no NaNs).
3. Check warnings: `CORR_SERIES_ALIGNED_INTERSECTION` or `CORR_SERIES_ALIGNED_RELAXED`.
4. If aligned points are too few, `correlation.matrix` should be null/empty and warnings include `CORR_INSUFFICIENT_POINTS`.
5. If alignment gap ratio is too low, `correlation.matrix` should be null/empty and warnings include `CORR_GAP_RATIO_TOO_HIGH`.

## 2. Admin Debug
1. Enable capture (admin only):
   - Set `fincoach.portfolio.debug.capture-enabled=true`
   - Call a portfolio metrics endpoint with header `X-Debug-Market: 1`
2. Call `GET /api/admin/portfolio/market-debug/latest`.
3. Review JSON response for:
   - `resolvedTickers`: Which rule applied (keyword -> ticker + source + warnings).
   - `marketFetch`: Success/Fail counts and failed symbols.
4. Verify fields:
   - `requestId`, `timestamp`, `userId`
   - `historyPath` and `historySource`
   - `cache.hit` / `cache.snapshotsCount`
   - `marketFetch.failedSymbols`
   - `correlation.effectivePoints` / `correlation.matrixEmitted`
   - `warnings`
5. If no snapshot exists yet, response should still be 200 and include warning `DEBUG_SNAPSHOT_EMPTY` (schema remains stable).

## 3. Fallback Scenarios
- **DB Down**: Should use `ticker-mapping.yml`. Warning `TICKER_MAPPING_DB_UNAVAILABLE_FALLBACK_FILE`.
- **All Mapping Fail**: Warning `POSITION_TICKER_UNRESOLVED` + Fallback to `REPORT_NET_WORTH_APPROX`.

## 4. Snapshot Cache (Prefer Cache)
1. Enable `fincoach.portfolio.snapshot.preferCache=true`.
2. Insert snapshot rows:
   ```sql
   INSERT INTO fc_portfolio_price_snapshot(user_id, snap_date, source, portfolio_value)
   VALUES (1, '2026-02-10', 'MARKET_DATA_DAILY_CLOSE', 100000),
          (1, '2026-02-11', 'MARKET_DATA_DAILY_CLOSE', 101000),
          (1, '2026-02-12', 'MARKET_DATA_DAILY_CLOSE', 100500)
   ON DUPLICATE KEY UPDATE
   source=VALUES(source), portfolio_value=VALUES(portfolio_value);
   ```
3. Call Portfolio Metrics.
4. Verify `source=SNAPSHOT_CACHE` and warnings include `SNAPSHOT_CACHE_HIT` and `SNAPSHOT_PREFER_CACHE_ENABLED`.

## 5. CI Gate (Local)
Run before pushing changes:
```bash
./scripts/ci-backend.sh
```
