# Runbook: M8-3 Advice Engine v2

## Build / Test (Backend)
Run Maven from the backend directory (repo root has no pom.xml):

```bash
cd backend
mvn clean test
```

## Admin APIs
Rebalance templates:
- `GET /api/admin/rebalance-templates/list`
- `POST /api/admin/rebalance-templates/save`
- `POST /api/admin/rebalance-templates/publish`
- `POST /api/admin/rebalance-templates/reload`

## Advice Output
Health report `advice` includes:
- `adviceV2.advices[]`
- `adviceV2.meta` (ruleSet/template + thresholds + warnings)
