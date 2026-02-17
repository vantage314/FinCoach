# Implementation Plan: M8-3 Advice Engine v2

## Scope
- Rebalance template table + seed
- Rebalance template registry + publish/reload
- Admin APIs with RBAC
- Advice Engine v2 (cashflow/debt + rebalance)
- Debug output for template + thresholds

## Steps
1. Add migration for `fc_rebalance_template` and seed BALANCED template.
2. Implement `RebalanceTemplateRegistry` + service.
3. Add admin controller with RBAC.
4. Implement AdviceEngine v2 with explainable DTO + meta.
5. Extend admin debug to expose template + thresholds.
6. Add tests and run `mvn clean test`.
