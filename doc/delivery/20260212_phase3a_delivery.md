# Phase 3A Delivery Hardening (2026-02-12)

## Summary
- Added repository-level `.gitignore` to keep the workspace clean.
- Added backend and frontend `.env.example` files for reproducible environment setup.
- Added delivery-grade `README.md` with quickstart and troubleshooting.
- Added `scripts/dev.ps1` for Windows one-click local startup.

## .gitignore Additions
- Backend: `backend/target/`, `**/target/`
- Frontend: `frontend/node_modules/`, `frontend/dist/`, `frontend/.vite/`, `frontend/.cache/`
- Logs: `log/`, `logs/`, `*.log`, `*.pid`
- IDE: `.idea/`, `.vscode/`, `*.iml`
- OS/Temp: `.DS_Store`, `Thumbs.db`, `tmp/`

## Environment Examples
- `backend/.env.example`
  - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DEEPSEEK_API_KEY`, `SERVER_PORT`
- `frontend/.env.example`
  - `VITE_API_BASE_URL`

## Windows Dev Script
File: `scripts/dev.ps1`

Usage:
```
# Start both backend and frontend
.\scripts\dev.ps1 -Mode all

# Start only backend
.\scripts\dev.ps1 -Mode backend

# Start only frontend
.\scripts\dev.ps1 -Mode frontend

# Use a specific JDK
.\scripts\dev.ps1 -Mode backend -JavaHome "D:\software\jdk21"
```

Notes:
- The script reads `backend/.env` if present; otherwise it warns to copy from `.env.example`.
- Backend runs with `mvn -q -DskipTests spring-boot:run`.
- Frontend installs dependencies if `node_modules` is missing.

## Verification
Backend build:
```
$env:JAVA_HOME="D:\software\jdk21"; $env:Path="$env:JAVA_HOME\bin;$env:Path"; cd backend; mvn clean package
```
Result: `BUILD SUCCESS` (JDK 21)

Frontend build:
```
cd frontend
npm run build
```
Result: success (warnings: Vite CJS deprecation, Sass legacy API, large chunk size warning).

## Known Limits
- AI chat requires a valid `DEEPSEEK_API_KEY` to return real responses.

## Troubleshooting
- Error: `Access denied (using password: NO)`
  - Cause: `backend/.env` was not loaded or `DB_PASSWORD` is missing.
  - Fix: copy `backend/.env.example` to `backend/.env` and set `DB_PASSWORD`.
  - PowerShell (temporary):
    ```
    $env:DB_PASSWORD="your_password"
    $env:DB_USERNAME="root"
    $env:DB_URL="jdbc:mysql://localhost:3306/fincoach?useSSL=false&serverTimezone=UTC"
    ```
