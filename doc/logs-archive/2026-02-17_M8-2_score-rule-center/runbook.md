# Runbook: Score Rule Center (M8-2)

## Build / Test (Backend)
Run Maven from the backend directory (repo root has no pom.xml):

```bash
cd backend
mvn clean test
```

## Admin API Quick Check
Base path: `/api/admin/score-rules`

- Get active ruleset:
  - `GET /active`
- Save params (auto-draft if `ruleSetId` missing):
  - `POST /save-params`
- Publish:
  - `POST /publish`
- Manual reload:
  - `POST /reload`
