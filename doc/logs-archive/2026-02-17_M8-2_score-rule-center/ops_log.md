# Ops Log: M8-2 Score Rule Center

**Date:** 2026-02-17  
**Status:** Completed

## 1. Goal
Deliver a versioned, publishable score ruleset with hot reload and auditability:
- RuleSet + Params stored in DB
- Publish triggers in-memory reload
- ScoreEngine reads active snapshot with fallback defaults
- Admin debug includes ruleset code/version

## 2. DoD
- [x] RuleSet table + Param table (seed DEFAULT)
- [x] Registry snapshot + reload
- [x] Admin APIs (view/save/publish/reload) with RBAC
- [x] ScoreEngine reads active ruleset + fallback
- [x] Admin debug includes ruleset code/version
- [x] `mvn clean test` PASS

## 3. Execution Log
### Step 1: Migration + Seed
- Added `fc_score_rule_set` / `fc_score_rule_param`
- Seeded DEFAULT ruleset + params

### Step 2: Registry + Service
- Implemented `ScoreRuleSetRegistry` (AtomicReference snapshot)
- Fallback default on missing/DB error with warning `SCORE_RULESET_FALLBACK_DEFAULT`

### Step 3: Admin API + RBAC
- Added `/api/admin/score-rules/*` endpoints
- New permissions: `ADMIN_SCORE_RULE_VIEW/EDIT/PUBLISH`

### Step 4: Scoring Integration + Debug
- ScoreEngine reads ruleset snapshot + thresholds/weights
- Health report metrics include `scoreRuleSet`
- Admin market debug includes ruleset code/version/source

## 4. Verification
- `cd backend && mvn clean test`
