# MySQL → PostgreSQL 迁移 Runbook

## 前置条件

1. 安装 PostgreSQL 14+
2. 创建数据库:

```sql
CREATE DATABASE fincoach ENCODING 'UTF8';
```

3. 确认连接信息：
   - Host: `localhost`
   - Port: `5432`
   - Database: `fincoach`
   - User: `postgres`
   - Password: (你的密码)

---

## 方案 A: 全新启动 (推荐)

Flyway 会自动执行 `db/migration/postgresql/` 目录下的迁移脚本创建所有表。

```powershell
# 1. 设置环境变量 (或修改 .env 文件)
$env:DB_URL = "jdbc:postgresql://localhost:5432/fincoach"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "你的密码"

# 2. 启动 (以 Java 17 为例)
cd d:\project\FinCoach\backend
mvn spring-boot:run
```

> Flyway 将自动在 PG 中创建所有表结构（`V20260215_01__base_tables.sql` + 后续 18 个脚本）。

---

## 方案 B: pgloader 数据迁移

如果需要保留 MySQL 中的已有数据：

```bash
# 安装 pgloader
# Ubuntu: sudo apt install pgloader
# macOS: brew install pgloader

# 迁移
pgloader mysql://root:password@localhost/fincoach \
         postgresql://postgres:password@localhost/fincoach
```

> **注意**: pgloader 迁移后需手动清理 Flyway 版本表 (`flyway_schema_history`)，删除所有记录后用 `flyway baseline` 重建基线。

---

## 方案 C: CSV 导入

```powershell
# 1. 从 MySQL 导出
mysqldump -u root -p fincoach --tab=/tmp/export --fields-terminated-by=',' --fields-enclosed-by='"'

# 2. 在 PG 中导入
psql -U postgres -d fincoach -c "\COPY \"user\" FROM '/tmp/export/user.txt' WITH (FORMAT csv, HEADER false)"
```

---

## 验证清单

| 检查项 | 命令/操作 | 预期 |
|---|---|---|
| Flyway 迁移 | 启动日志 | `Successfully applied N migrations` |
| 表是否创建 | `psql -c "\dt"` | 显示 30+ 张表 |
| 注册 | `POST /api/auth/register` | 201 Created |
| 登录 | `POST /api/auth/login` | 200 + JWT token |
| 资产列表 | `GET /api/assets` (带 Bearer token) | 200 + JSON |
| 健康报告 | 相关接口 | 正常返回 |

---

## 回滚方式

仅需修改一处配置即可回切 MySQL：

```yaml
# backend/src/main/resources/application.yml
spring:
  profiles:
    active: dev-mysql   # 改回这个
```
