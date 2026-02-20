# Ops Log: Dual DB Profiles (MySQL default + PostgreSQL switch)

## Goal
- Default dev uses MySQL; production/target supports PostgreSQL via Spring profiles.
- Minimal code changes; isolate DB differences in config/migrations/mapper where needed.

## Changes
- Added Spring profiles: dev-mysql, prod-pg (default active: dev-mysql)
- Added runtime JDBC drivers: mysql + postgresql
- Pagination strategy: MyBatis-Plus PaginationInnerInterceptor with DbType from app.db
- Flyway: split migrations into mysql/ and postgresql/ locations
- Upsert: no ON DUPLICATE KEY UPDATE detected in current codebase

## Verification
- Build: mvn -q -DskipTests clean package
- Run MySQL: mvn -q spring-boot:run -Dspring-boot.run.profiles=dev-mysql
- Run PG: mvn -q spring-boot:run -Dspring-boot.run.profiles=prod-pg
