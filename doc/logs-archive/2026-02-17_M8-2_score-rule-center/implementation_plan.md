# Implementation Plan: M8-2 Score Rule Center

## Scope
- RuleSet + Param persistence (Flyway)
- Registry snapshot + reload
- Admin API for view/save/publish/reload
- ScoreEngine reads active ruleset with fallback defaults
- Admin debug exposes ruleset metadata

## Steps
1. Add Flyway migration for `fc_score_rule_set` + `fc_score_rule_param` and seed DEFAULT rules.
2. Implement `ScoreRuleSetRegistry` (AtomicReference snapshot, reload on publish).
3. Implement `ScoreRuleSetService` (draft, upsert params, publish).
4. Add admin controller + RBAC permission codes.
5. Wire ScoreEngine to read ruleset params and include metadata in metrics/debug.
6. Add tests and run `mvn clean test`.
