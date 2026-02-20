# Runbook: Switch DB (MySQL <-> PostgreSQL)

## MySQL (dev)
- profile: dev-mysql
- start: mvn -q spring-boot:run -Dspring-boot.run.profiles=dev-mysql

## PostgreSQL (prod/target)
- profile: prod-pg
- start: mvn -q spring-boot:run -Dspring-boot.run.profiles=prod-pg

## Notes
- Flyway uses profile-specific locations under classpath:db/migration/{mysql,postgresql}
- app.db controls MyBatis-Plus pagination DbType (mysql/postgres)
