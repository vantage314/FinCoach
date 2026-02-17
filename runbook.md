# Runbook

## Build / Test (Backend)
This repository root has **no pom.xml**. Run Maven from the `backend/` directory.

```bash
cd backend
mvn clean test
```

## M8-3 Advice Engine v2
Advice v2 output is attached to health report `advice`:
- `adviceV2.advices[]`
- `adviceV2.meta` (ruleSet/template/thresholds/warnings)

Rebalance template admin APIs:
- `GET /api/admin/rebalance-templates/list`
- `POST /api/admin/rebalance-templates/save`
- `POST /api/admin/rebalance-templates/publish`
- `POST /api/admin/rebalance-templates/reload`
