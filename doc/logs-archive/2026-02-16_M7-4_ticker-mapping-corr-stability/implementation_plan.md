    # Implementation Plan - M7-4

    ## 1. Config & Registry (Ticker Mapping)
    - [x] `backend/src/main/resources/config/ticker-mapping.yml` (Default aliases)
    - [x] `TickerMappingRegistry` (Component, loads YAML, optionally DB)
    - [x] `PortfolioHistoryBuilderMarket` update to use Registry.

    ## 2. DB Mapping (fc_ticker_mapping)
    - [x] SQL Migration: `fc_ticker_mapping` table.
    - [x] Entity: `FcTickerMappingEntity`
    - [x] Mapper: `FcTickerMappingMapper`
    - [x] Integration in Registry (Primary source).

    ## 3. Correlation Stability
    - [x] `ReturnSeriesAligner` (Helper class).
    - [x] Update `PortfolioAnalyzer` to use Aligner.
    - [x] Add Warnings for exclusions.
    - [x] Guard correlation by gap ratio (`CORR_GAP_RATIO_TOO_HIGH`).

    ## 4. Snapshot Replay
    - [x] Update `PortfolioHistoryFacade`: Check DB first if `preferCache=true`.
    - [x] Config: `fincoach.portfolio.snapshot.preferCache`.

    ## 5. Admin Debug API
    - [x] `AdminMarketDebugController` (`/api/admin/portfolio/market-debug`).
    - [x] Service method to expose internal state (or cache last state).
    - [x] Normalize debug response via DTO schema + mapper.
    - [x] Gate debug capture via `X-Debug-Market` + admin + config.

    ## 6. Testing
    - [x] Unit Tests for Registry, Aligner.
    - [x] MockMVCTest for Admin API.

    ## 7. Admin Ticker Mapping CRUD
    - [x] Admin CRUD endpoints (page/save/enable/disable/reload).
    - [x] Registry reload uses in-memory snapshot (no per-request DB hit).

    ## 8. CI Gate
    - [x] GitHub Actions workflow (JDK 21, compile + test, Maven cache).
    - [x] Local script `scripts/ci-backend.sh`.
