# FinCoach

FinCoach is a lightweight personal finance assistant that combines risk assessment,
asset health checks, and investment plan generation to guide users toward better
portfolio decisions.

This repository provides a Vue 3 frontend and a Spring Boot backend with MySQL
storage. The goal of this guide is to help you spin up a working local environment
quickly.

## Tech Stack
- Frontend: Vue 3 + Vite
- Backend: Spring Boot (Java 17+)
- Database: MySQL 8+

## Repository Structure
- `frontend/` - Vue 3 application
- `backend/` - Spring Boot service
- `sql/` - Schema and migration scripts
- `doc/` - Project documentation and delivery notes

## Quick Start

### 1) Prepare MySQL
Create the database:
```sql
CREATE DATABASE fincoach DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```
Initialize tables using `sql/init/01_initial_tables.sql` if your database is empty.

### 2) Configure Backend Environment
Copy `backend/.env.example` to `backend/.env` and fill in values:
```
DB_URL=jdbc:mysql://localhost:3306/fincoach?useSSL=false&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=your_password
DEEPSEEK_API_KEY=your_deepseek_api_key
SERVER_PORT=8080
```

### 3) Start Backend
```bash
cd backend
mvn spring-boot:run
```
Alternatively, package and run the jar:
```bash
mvn clean package
java -jar target/core-0.0.1-SNAPSHOT.jar
```

### 4) Configure Frontend Environment
Copy `frontend/.env.example` to `frontend/.env` and adjust if needed:
```
VITE_API_BASE_URL=http://localhost:8080
```

### 5) Start Frontend
```bash
cd frontend
npm install
npm run dev
```

## Verify APIs
Health check (requires token):
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8080/api/health/check
```

User profile (requires token):
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8080/api/user/profile
```

## FAQ
**Q: `invalid target release: 17` when building backend**  
A: Use JDK 17+ (JDK 21 is supported). Make sure `JAVA_HOME` points to the correct JDK.

**Q: Port conflicts on 8080/5173**  
A: Stop the conflicting service or change `SERVER_PORT` (backend) or Vite port in frontend.

**Q: DB connection failed**  
A: Verify `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and that MySQL is running.
