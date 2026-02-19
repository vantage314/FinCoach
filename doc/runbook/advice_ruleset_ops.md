# Advice Ruleset Ops Runbook

## Purpose
Manage configurable advice rulesets (rebalance drift, risk bands, behavior scoring weights).

## Key Endpoints
- GET  /admin/api/advice/rulesets
- POST /admin/api/advice/rulesets
- PUT  /admin/api/advice/rulesets/{id}
- PUT  /admin/api/advice/rulesets/{id}/enable
- GET  /admin/api/advice/rulesets/{code}/params
- PUT  /admin/api/advice/rulesets/{code}/params
- GET  /api/advice/rebalance

## Default Params
- REBALANCE_DRIFT_PCT (default 0.05)
- RISK_SCORE_LOW/MID/HIGH (30/60/80)
- BEHAVIOR_SCORE_WEIGHTS_JSON
- MAX_POSITIONS (8)

## Workflow
1. Create new ruleset (POST).
2. Update params for the ruleset code (PUT params).
3. Enable the ruleset (PUT /enable).
4. Verify /api/advice/rebalance output.

## Notes
- Enabling a ruleset disables the previous active one.
- Updates to params trigger registry reload.
