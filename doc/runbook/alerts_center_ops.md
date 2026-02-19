# Alerts Center Runbook

## Overview
The alerts center aggregates key health and operational warnings into deduplicated alerts and in-app notifications.

## Alert Lifecycle
- OPEN: active alert
- ACKED: acknowledged by admin
- RESOLVED: resolved/closed

## Dedupe Behavior
- Same user + same alertType updates existing OPEN alert:
  - hit_count++
  - last_seen_at updated
  - latest message/meta stored

## Alert Sources
- HealthV2: DTI_DANGER/DTI_WARN, CASHFLOW_NEGATIVE, EMERGENCY_FUND_LOW/CRITICAL
- Insurance gap: INSURANCE_GAP_HIGH, PREMIUM_RATIO_WARN/DANGER
- Crawler: CRAWLER_STALE_DETECTED / CRAWLER_STALE_RECOVERED

## Severity
- INFO / WARN / DANGER

## Operations
- List alerts (admin): GET /admin/api/alerts/list
- ACK alerts: POST /admin/api/alerts/ack
- RESOLVE alerts: POST /admin/api/alerts/resolve
- User notifications:
  - GET /api/app/notifications/list
  - POST /api/app/notifications/read
  - POST /api/app/notifications/readAll

## Troubleshooting
- If alerts missing: verify HealthV2 warnings and crawler stale detection are triggered.
- If notifications not created: ensure userId is present and alert status is OPEN.
- For premium ratio warnings: ensure insurance gap metrics include premiumRatio + threshold.
