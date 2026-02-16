# 资产体检系统 2.0 — M1 工程扫描报告

> 扫描日期：2026-02-14

## 1. 技术栈确认

| 项目 | 值 |
|------|-----|
| Spring Boot | 3.2.2 |
| Java (runtime min) | 17 |
| Java (build/toolchain) | 21（maven-toolchains-plugin enforced） |
| MyBatis-Plus | 3.5.5（`mybatis-plus-spring-boot3-starter`） |
| 数据库 | MySQL（`mysql-connector-j`） |
| Lombok | ✔ |
| 参数校验 | `spring-boot-starter-validation`（Jakarta） |
| API 文档 | SpringDoc OpenAPI 2.3.0 |
| JWT | jjwt 0.12.3 |

## 2. 项目代码结构

```
com.fincoach
├── core
│   ├── CoreApplication.java       # 启动类
│   ├── common/                    # 统一返回体 Result<T>、UserContext、全局异常处理
│   ├── config/                    # WebConfig、SecurityConfig、AuthInterceptor
│   ├── controller/                # REST 控制器（AssetItem/Auth/HealthCheck/Market 等）
│   │   ├── dto/                   # 请求 DTO
│   │   └── vo/                    # 响应 VO
│   ├── interceptor/               # JwtInterceptor
│   ├── repository/
│   │   ├── entity/                # MyBatis-Plus Entity（AssetItem/MarketSecurity 等）
│   │   └── mapper/                # BaseMapper 接口（@Mapper 注解）
│   ├── service/                   # Service 接口
│   │   └── impl/                  # ServiceImpl 实现类
│   └── util/ & utils/             # 工具类（JwtUtils）
```

## 3. 关键规范

### 3.1 统一返回体
- **类名**：`com.fincoach.core.common.Result<T>`
- **字段**：`code`（200=成功）、`message`、`data`
- **静态方法**：`Result.success(data)`、`Result.error(code, message)`

### 3.2 用户上下文
- **类名**：`com.fincoach.core.common.UserContext`
- **获取方式**：`UserContext.getCurrentUserId()` → `Long`
- **注入时机**：`JwtInterceptor.preHandle()` 从 JWT Token 解析 userId 并 `setUserId()`

### 3.3 Entity 规范
- **包路径**：`com.fincoach.core.repository.entity`
- **无 BaseEntity 基类**：每个 Entity 自行定义字段
- **ID 策略**：`@TableId(type = IdType.AUTO)` — MySQL 自增
- **无逻辑删除字段**（无 `del_flag`）
- **时间字段**：使用 `updateTime`（`LocalDateTime`），无统一 `createTime`

### 3.4 Mapper 规范
- 位于 `com.fincoach.core.repository.mapper`
- 继承 `BaseMapper<XxxEntity>`
- 使用 `@Mapper` 注解
- **未使用** `IService` / `ServiceImpl` 模式（Service 手动注入 Mapper）

### 3.5 数据库命名规范
- **表名**：snake_case，**无固定前缀**（如 `asset_item`、`market_security`、`sys_user`）
- **字段命名**：snake_case（MyBatis-Plus `map-underscore-to-camel-case: true`）
- **ID 生成**：MySQL 自增

### 3.6 SQL 迁移方案
- **无 Flyway / Liquibase**
- 迁移脚本位于 `sql/migration/`，以日期命名（如 `20260211_xxx.sql`）
- 手动执行

> 备注：Spring Boot 3 要求 Java 17+。本项目为了依赖一致与 CI/本地一致，使用 toolchains 固定 JDK 21 编译；若本机 `mvn -v` 显示 Java 8/11，请按 `doc/build/java_toolchain.md` 修复。

## 4. 已有健康体检模块（旧版）

| 文件 | 说明 |
|------|------|
| `HealthCheckController` | `/api/health/check`，GET |
| `HealthCheckService` | 接口，`checkHealth(userId)` |
| `HealthReportVO` | 四维模型（流动性/风险匹配/保障力/分散度），无持久化 |

> 旧版仅在内存中计算，不落库，不涉及负债/现金流/目标/保险等数据。

## 5. M1 实际采用方案

| 决策项 | 方案 |
|--------|------|
| 新模块包名 | `com.fincoach.core.healthv2`（独立于旧 `core.service/controller`） |
| 表前缀 | `fc_`（与旧表区分，避免命名冲突） |
| ID 策略 | `IdType.AUTO`（MySQL 自增，与现有一致） |
| 逻辑删除 | 不使用（与现有一致） |
| 时间字段 | `create_time` / `update_time`（`LocalDateTime`） |
| SQL 迁移 | `sql/migration/20260214_health_v2_m1_tables.sql`（手动执行） |
| 审计 actor 获取 | `UserContext.getCurrentUserId()`（JWT 拦截器已注入） |
| JSON 序列化 | Jackson（Spring Boot 默认，无需额外依赖） |
| Service 模式 | 与现有一致：接口 + Impl，手动注入 Mapper（不使用 `IService/ServiceImpl`） |
| 返回体 | 复用 `Result<T>` |
| 参数校验 | `@Validated` + Jakarta Validation |

## M5-C Import/Export Logs

- 操作更改日志：[`doc/logs-archive/20260215_m5c_import_export_ops_log.md`](../logs-archive/20260215_m5c_import_export_ops_log.md)
- 接口操作手册：[`doc/logs-archive/20260215_m5c_import_export_runbook.md`](../logs-archive/20260215_m5c_import_export_runbook.md)

## M5-B Notifications Logs

- 操作更改日志：[`doc/logs-archive/20260215_m5b_notifications_ops_log.md`](../logs-archive/20260215_m5b_notifications_ops_log.md)
- 接口操作手册：[`doc/logs-archive/20260215_m5b_notifications_runbook.md`](../logs-archive/20260215_m5b_notifications_runbook.md)

## M5-A Behavior Events Logs

- 操作更改日志：[`doc/logs-archive/20260215_m5a_behavior_events_ops_log.md`](../logs-archive/20260215_m5a_behavior_events_ops_log.md)
- 接口操作手册：[`doc/logs-archive/20260215_m5a_behavior_events_runbook.md`](../logs-archive/20260215_m5a_behavior_events_runbook.md)

## M6-1 Trend Logs

- 操作更改日志：[`doc/logs-archive/20260215_m6_1_trend_ops_log.md`](../logs-archive/20260215_m6_1_trend_ops_log.md)
- 接口操作手册：[`doc/logs-archive/20260215_m6_1_trend_runbook.md`](../logs-archive/20260215_m6_1_trend_runbook.md)

## M6-2 Rebalance Confirm Logs

- 操作更改日志：[`doc/logs-archive/20260215_m6_2_rebalance_confirm_ops_log.md`](../logs-archive/20260215_m6_2_rebalance_confirm_ops_log.md)
- 接口操作手册：[`doc/logs-archive/20260215_m6_2_rebalance_confirm_runbook.md`](../logs-archive/20260215_m6_2_rebalance_confirm_runbook.md)

## M6-3 Admin- [Walkthrough/README HealthV2 Logs Index](../../README.md#healthv2-logs--runbooks)

### M7 Portfolio Analysis
- **M7-1 Portfolio Metrics**: [Ops Log](../logs-archive/20260216_m7_1_portfolio_metrics_ops_log.md) | [Runbook](../logs-archive/20260216_m7_1_portfolio_metrics_runbook.md)
- **M7-2 Portfolio History**: [Ops Log](../logs-archive/20260216_m7_2_portfolio_history_ops_log.md) | [Runbook](../logs-archive/20260216_m7_2_portfolio_history_runbook.md)

## M6-3 Admin Dashboard Stats Logs

- 操作更改日志：[`doc/logs-archive/20260216_m6_3_admin_dashboard_stats_ops_log.md`](../logs-archive/20260216_m6_3_admin_dashboard_stats_ops_log.md)
- 接口操作手册：[`doc/logs-archive/20260216_m6_3_admin_dashboard_stats_runbook.md`](../logs-archive/20260216_m6_3_admin_dashboard_stats_runbook.md)
- 自测指南：[`doc/logs-archive/20260216_m6_3_admin_dashboard_stats_selftest.md`](../logs-archive/20260216_m6_3_admin_dashboard_stats_selftest.md)
