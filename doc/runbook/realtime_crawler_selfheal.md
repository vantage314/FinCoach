# Realtime Crawler Self-Heal Runbook (Wave4.2)

## Purpose
- Provide operational guidance for REALTIME crawler DAEMON mode:
  - health checks
  - stale detection
  - recover workflow
  - config defaults

## Key Endpoints
- GET  /admin/api/data-source/realtime/health
- POST /admin/api/data-source/realtime/recover
- POST /admin/api/data-source/realtime/start
- POST /admin/api/data-source/realtime/stop
- GET  /admin/api/data-source/status

## Stale Logic
- isStale = (status == RUNNING) && lastHeartbeatAt != null && (now - lastHeartbeatAt) > staleThresholdSeconds

## Config
- crawler.staleThresholdSeconds (default: 30)
- crawler.logMaxChars (default: 4000)
- crawler.selfHealEnabled (default: true)

## Observability Fields
- `recentEvents` keeps the latest 20 events (tail ring):
  - `ts`, `type`, `msg`
- Counters:
  - `staleCount`, `recoverCount`, `restartCount`
- `lastErrorAt` tracks the last failure timestamp.

## Log Truncation
- `last_log` keeps the newest tail up to `logMaxChars` (older head is dropped).

## Troubleshooting
- If RUNNING but heartbeat stops updating:
  - call /realtime/health
  - if isStale=true, call /realtime/recover
  - check last_log truncation and DB timestamps
