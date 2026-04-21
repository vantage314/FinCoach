# 2026-02-24 MySQL → PostgreSQL 迁移操作日志

## 变更摘要

将 FinCoach 项目数据库从 MySQL 硬切到 PostgreSQL（本地开发环境）。

## 修改文件清单

| 文件 | 操作 | 说明 |
|---|---|---|
| `application.yml` | 修改 | `active: dev-mysql` → `dev-pg` |
| `application-dev-pg.yml` | 新增 | PG 数据源 + Flyway 配置 |
| `pom.xml` | 修改 | MySQL driver scope → `test`；移除不存在的 `flyway-mysql` / `flyway-postgresql` 依赖 |
| `V20260215_01__base_tables.sql` | 新增 | 16 张基础表 PG 语法 + 种子数据 |
| `MyBatisAutoFillHandler.java` | 新增 | 替代 MySQL `ON UPDATE CURRENT_TIMESTAMP` |
| `User.java` | 修改 | `@TableName("user")` → `@TableName("\"user\"")` + FieldFill 注解 |
| `AssetItem.java` | 修改 | 添加 FieldFill 注解 |
| `AdminDashboardStatsMapper.java` | 修改 | `DATE_SUB` → `make_interval`；`FROM user` → `FROM "user"` |

## 编译验证

- `mvn clean compile` ✅ 通过 (exit code 0)
- `mvn package` ❌ repackage 步骤失败（已有的 Java 8 vs Spring Boot 3.2 环境问题，非迁移引入）

## 回滚

将 `application.yml` 中 `spring.profiles.active` 改回 `dev-mysql` 即可。
