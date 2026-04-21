# 2026-02-24 MySQL → PostgreSQL 迁移

## 变更内容

- 新增 PG 基础表迁移脚本 (`V20260215_01__base_tables.sql`)
- 新增 `application-dev-pg.yml` profile
- 新增 `MyBatisAutoFillHandler` (替代 MySQL ON UPDATE)
- 修改 `User.java` 表名为 PG 兼容格式
- 修改 `AdminDashboardStatsMapper` SQL 为 PG 语法
- 修改 `pom.xml` 依赖配置
- 编译验证通过

## 影响范围

- 数据库连接从 MySQL 切换到 PostgreSQL
- Flyway 迁移脚本路径切换到 `db/migration/postgresql/`
- Java 中 MySQL 特有函数全部替换为 PG 等效函数
