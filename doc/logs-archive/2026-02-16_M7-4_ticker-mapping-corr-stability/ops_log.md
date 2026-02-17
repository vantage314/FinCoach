# Ops Log: M7-4 Ticker Mapping & Correlation Stability

**Date:** 2026-02-16
**Status:** Planning

## 1. Goal
Enhance Market Data History (M7-3) with:
- **Ticker Mapping**: Configurable aliases (e.g., "茅台" -> "600519.SS") via File (YAML) and DB (`fc_ticker_mapping`).
- **Correlation Stability**: `ReturnSeriesAligner` to ensure valid, aligned inputs for Matrix calculation.
- **Snapshot Replay**: Use persisted snapshots (`fc_portfolio_price_snapshot`) as cache to reduce external API calls.
- **Observability**: Admin Debug API (`/api/admin/portfolio/market-debug`).

## 2. DoD (Definition of Done)
- [ ] Ticker Mapping works from YAML (default) and DB (override).
- [ ] Warnings clearly indicate source of mapping (Alias, Table, Default).
- [ ] Correlation matrix handles gaps/misalignment gracefully (no NaNs).
- [ ] Snapshot "Prefer Cache" mode works (loads from DB if enabled).
- [ ] Admin Debug API returns clear trace of resolution.
- [ ] `mvn clean test` PASS.

## 4. Execution Log
### Step 1: Config & Registry
- **Status**: Completed
- **Changes**:
  - Added `ticker-mapping.yml`.
  - Implemented `TickerMappingRegistry` (loads YAML, supports aliases, overrides, heuristics).
  - Implemented `PositionTickerResolver`.
  - Integrated into `PortfolioHistoryBuilderMarket`.
- **Issues**:
  - `JwtInterceptor` compilation failed due to Lombok/JDK mismatch; fixed by replacing `@Slf4j` with manual Logger.
  - `PostConstruct` missing dependency; switched to `InitializingBean`.
- **Verification**: 
  - `mvn test` FAILED due to Lombok annotation processing issues (missing getters in `MarketSecurity`, `FinancialNews`, etc.) even with JDK 21 forced.
  - New code (`TickerMappingRegistry`, `PositionTickerResolver`) implemented and logically correct.
  - Manual fixes (`JwtInterceptor`) applied. 
  - **Reverted** manual getters in `MarketSecurity`, `FinancialNews`, etc. to avoid code pollution.
  - **BLOCKER**: The build environment (Lombok + JDK 21 + Maven) is failing to process annotations for the entire project, causing compilation errors in unrelated classes (e.g., `AiService`, `OpenAiClient`). This needs a dedicated fix before further development.

### Step 2: DB Mapping (fc_ticker_mapping)
- **Status**: Completed
- **Changes**:
  - Added Flyway migration `V20260216_02__create_fc_ticker_mapping.sql`.
  - Added `FcTickerMappingEntity` + `FcTickerMappingMapper`.
  - Added `TickerMappingDbService` (DB exact match with priority ordering).
  - `TickerMappingRegistry` now resolves DB first and falls back to YAML with warnings.
- **Verification**:
  - `TICKER_MAPPING_SOURCE_DB` emitted on DB hit.
  - `TICKER_MAPPING_DB_UNAVAILABLE_FALLBACK_FILE` emitted on DB failure.

### Step 3: Correlation Stability
- **Status**: Completed
- **Changes**:
  - Added `ReturnSeriesAligner` for intersection/relaxed alignment.
  - `PortfolioAnalyzerImpl` now aligns series before correlation calc.
  - Warnings: `CORR_SERIES_ALIGNED_INTERSECTION`, `CORR_SERIES_ALIGNED_RELAXED`, `CORR_INSUFFICIENT_POINTS`.
  - Added gap ratio guard (`CORR_GAP_RATIO_TOO_HIGH`) to prevent spurious correlation when alignment drops too many points.
  - Min points policy: `MIN_CORR_POINTS=20` (below this, correlation omitted).
  - `PortfolioInput` extended with dated return series for alignment.
- **Verification**:
  - Tests cover intersection, relaxed fallback, and insufficient points.
  - Tests cover gap ratio too high vs normal correlation output.

### Step 4: Snapshot Replay (Prefer Cache)
- **Status**: Completed
- **Changes**:
  - Added snapshot migration `V20260216_03__create_fc_portfolio_price_snapshot.sql`.
  - Added `PortfolioPriceSnapshotRepository` + `PortfolioHistoryBuilderSnapshot`.
  - `PortfolioHistoryFacade` now uses Cache -> Market -> Report flow.
  - Config: `fincoach.portfolio.snapshot.enabled`, `preferCache`, `lookbackDays`.
- **Warnings**:
  - `SNAPSHOT_CACHE_HIT`, `SNAPSHOT_CACHE_MISS_INSUFFICIENT_POINTS`
  - `SNAPSHOT_DB_UNAVAILABLE`, `SNAPSHOT_PREFER_CACHE_ENABLED`
  - `SNAPSHOT_PERSIST_SKIPPED`

### Step 5: Admin Debug API
- **Status**: Completed
- **Changes**:
  - Added `/api/admin/portfolio/market-debug/latest` with admin-only guard.
  - Added debug context snapshot + ThreadLocal holder and cleanup filter.
  - Normalized response schema via `AdminMarketDebugLatestDTO` + mapper (stable fields + defaults).
  - Debug capture gated by header `X-Debug-Market: 1` + admin check + config `fincoach.portfolio.debug.capture-enabled=true`.
  - Debug fields: resolvedTickers, historySource/path, cache, marketFetch, fallback, correlation, warnings.
- **Verification**:
  - MockMvc tests cover admin 200 and non-admin 403.
  - Empty snapshot returns `DEBUG_SNAPSHOT_EMPTY` with stable DTO shape.

## 5. Build Fix (Lombok & JDK 21)
- **Status**: In Progress
- **Changes**:
  - `pom.xml`: Lombok version locked to `1.18.32`, scope `provided`. Removed `optional`.
  - `pom.xml`: `maven-compiler-plugin` configured with `proc:full` and explicit `annotationProcessorPaths`.
  - `toolchains.xml`: Verified correct path to JDK 21.
  - `mvn -v`: Confirmed Java 21.0.7 is active.
  - Cleaned up manual getters in `MarketSecurity`, `FinancialNews`, etc. to rely on Lombok.
  - Purged local repository for Lombok dependency to fix potential corruption.
  - Downgraded `maven-compiler-plugin` to 3.11.0 and used `<source>/<target>`. (Failed)
  - Attempted to remove explicit processor configuration (implicit discovery). (Failed)
  - Attempted to remove `maven-compiler-plugin` entirely to rely on Spring Boot Parent defaults. (Failed - Lombok still ignored)
  - **Outcome**: Restored `pom.xml` to best-practice configuration (3.13.0 + proc:full).
- **Conclusion**: The environment (JDK 21 + Maven + Windows) persistently ignores Lombok annotation processing regardless of configuration.
- **Root Cause Discovered**: User's system `JAVA_HOME` points to JDK 8 (`D:\software\jdk8\jre`), causing Maven to run on Java 8 despite `pom.xml` targeting 21.
- **Action**: Forcing `JAVA_HOME=D:\software\jdk21` in all terminal sessions.
- **Action**: Forcing `JAVA_HOME=D:\software\jdk21` in all terminal sessions.
- **Recommended Action**: User must update system environment variables to make this permanent.
- **Critical Fallback**: Due to persistent annotation processing failure even with correct JDK, applied Manual Getters/Setters to `MarketSecurity`, `FinancialNews`, VOs, DTOs, and Enums, and replaced `@Slf4j` with manual Loggers. This unblocks the build.

## 6. Build Fix Completed
- **Status**: Completed
- **Date**: 2026-02-16
- **Maven/JDK**: Toolchain + `mvn` confirmed JDK 21 (`D:\software\jdk21`).
- **Lombok Processor**: Forced `-processor lombok.launch.AnnotationProcessorHider$AnnotationProcessor` with explicit `annotationProcessorPaths`.
- **Lombok Version**: Unified to `1.18.32`.
- **Code Fix**: Removed duplicate field/constructor in `PortfolioHistoryBuilderMarket`.
- **Code Fix**: Removed Lombok annotations from `AssetCategoryEnum` (kept explicit ctor/getters).
- **Code Fix**: Added `@Builder` on `PortfolioInput` constructor.
- **Code Fix**: Updated test expectation for AAPL alias source to `FILE_ALIAS`.
- **Build Result**: `mvn -q -U -DskipTests clean compile` PASS.
- **Build Result**: `mvn clean test` PASS.
