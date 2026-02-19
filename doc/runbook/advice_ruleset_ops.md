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

## Safe Rollout Steps
1. Create draft ruleset with a new version code (do not enable yet).
2. Update params and validate JSON/numeric formats.
3. Verify `/api/advice/rebalance` in staging or a test user to confirm payload shape.
4. Enable the new ruleset during a low-traffic window.
5. Rollback by re-enabling the previous ruleset if metrics regress.

## Verification Curl
```bash
curl -X GET http://localhost:8080/admin/api/advice/rulesets
curl -X GET http://localhost:8080/admin/api/advice/rulesets/DEFAULT/params
curl -X PUT http://localhost:8080/admin/api/advice/rulesets/1/enable
curl -X PUT http://localhost:8080/admin/api/advice/rulesets/DEFAULT/params \
  -H "Content-Type: application/json" \
  -d '[{\"paramKey\":\"REBALANCE_DRIFT_PCT\",\"paramValue\":\"0.06\",\"valueType\":\"DECIMAL\"}]'
curl -X GET http://localhost:8080/api/advice/rebalance
```

## Notes
- Enabling a ruleset disables the previous active one.
- Updates to params trigger registry reload.
