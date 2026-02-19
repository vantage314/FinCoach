# Build Warnings Ops Runbook

## What Was Fixed in Wave4.11
- Added route-level lazy loading and vendor chunk splitting to reduce large bundles.
- Switched Sass to modern compiler API to remove legacy JS API deprecation warnings.
- Renamed Vite config to ESM (`vite.config.mts`) to remove CJS Node API deprecation warnings.

## How To Validate
1. Run build:
```
cd frontend
pnpm -s build
```
2. Save full output to:
```
doc/qa/build-warnings/YYYYMMDD_HHMM/build_output.txt
```
3. Note remaining warnings (if any) in the output file.

## Chunk-Size Warnings
- Route-level lazy loading and `manualChunks` are already enabled in `frontend/vite.config.mts`.
- If warnings for >500 kB remain, document them and confirm acceptable in the ops log.
- Last resort (only if required): set `build.chunkSizeWarningLimit` with justification.
  - Keep changes documented in this runbook and in the ops log.
